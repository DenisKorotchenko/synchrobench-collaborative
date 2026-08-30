package ru.dksu.semantic;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An adaptive semantic lock that uses operation counters in the uncontended
 * coarse mode and switches to range-aware reservations when operation-level
 * conflicts persist.
 */
public class SemanticLockRangeV3 implements ISemanticLockRange {
    private static final int COARSE_ACQUIRED = 0;
    private static final int COARSE_RETRY = -1;

    private static final int COARSE_FAILURES_BEFORE_PRECISE = 64;
    private static final int PRECISE_SUCCESSES_WITHOUT_BENEFIT_BEFORE_COARSE = 1024;

    private static final long COARSE_TOKEN_MARKER = Long.MIN_VALUE;
    private static final long COARSE_TOKEN_VALUE_MASK = 0xffff_ffffL;

    private enum Mode {
        COARSE,
        TO_PRECISE,
        PRECISE,
        TO_COARSE
    }

    private final int operationsNumber;
    private final int[][] conflicts;
    private final int[][] conflictAddrs;
    private final boolean[] selfConflict;
    private final Set<SLRanges>[] ops;

    /**
     * The storage layout and admission protocol are inherited from
     * SemanticLockAtomicCountersV2. V3 performs a single non-blocking
     * admission attempt so that a persistent operation-level conflict can
     * trigger the transition to precise mode instead of spinning inside the
     * coarse lock.
     */
    private final SemanticLockAtomicCountersV2 coarseLock;
    private final int coarseCounterDelta;

    private final AtomicReference<Mode> mode = new AtomicReference<>(Mode.COARSE);

    /**
     * Precise waiters and holders. A holder remains a participant until its
     * SLOp is unlocked, which lets TO_COARSE create a quiescent point safely.
     */
    private final AtomicInteger preciseParticipants = new AtomicInteger();

    /**
     * Hysteresis counter. It is reset when precise mode either observes an
     * actual range conflict or admits operations that coarse mode would have
     * serialized by operation type.
     */
    private final AtomicInteger preciseSuccessesWithoutBenefit = new AtomicInteger();

    public final boolean fairness;

    public SemanticLockRangeV3(
            int operationsNumber,
            int[][] conflicts
    ) {
        this(operationsNumber, conflicts, false);
    }

    @SuppressWarnings("unchecked")
    public SemanticLockRangeV3(
            int operationsNumber,
            int[][] conflicts,
            boolean fair
    ) {
        validateArguments(operationsNumber, conflicts);

        this.operationsNumber = operationsNumber;
        this.fairness = fair;
        this.conflicts = new int[operationsNumber][];
        this.conflictAddrs = new int[operationsNumber][];
        this.selfConflict = new boolean[operationsNumber];
        this.ops = new Set[operationsNumber];

        for (int i = 0; i < operationsNumber; i++) {
            this.ops[i] = ConcurrentHashMap.newKeySet();
            this.selfConflict[i] = conflicts[i][i] == 1;
        }

        fillConflictIndexes(conflicts);

        // tryCoarseLock performs the admission itself, so the internal fair
        // queue of SemanticLockAtomicCountersV2 is intentionally disabled.
        this.coarseLock = new SemanticLockAtomicCountersV2(
                operationsNumber,
                conflicts,
                false
        );
        this.coarseCounterDelta = coarseLock.lockCounts.length() / operationsNumber;
    }

    @Override
    public SLOp lock(int operationNumber, int left, int right) {
        checkOperationNumber(operationNumber);

        int coarseFailures = 0;
        while (true) {
            Mode currentMode = mode.get();

            if (currentMode == Mode.COARSE) {
                int coarseToken = tryCoarseLock(operationNumber);
                if (coarseToken == COARSE_ACQUIRED) {
                    // TO_PRECISE can start between the first mode read and
                    // the counter reservation. In that case the reservation
                    // must be rolled back before precise mode is entered.
                    if (mode.get() == Mode.COARSE) {
                        return coarseSLOp(operationNumber, left, right, coarseToken);
                    }

                    releaseCoarseLock(operationNumber);
                    coarseFailures = 0;
                    continue;
                }

                coarseFailures++;
                if (coarseFailures >= COARSE_FAILURES_BEFORE_PRECISE) {
                    requestPreciseMode();
                    coarseFailures = 0;
                } else {
                    Thread.yield();
                }
                continue;
            }

            if (currentMode == Mode.PRECISE) {
                if (preciseSuccessesWithoutBenefit.get()
                        >= PRECISE_SUCCESSES_WITHOUT_BENEFIT_BEFORE_COARSE) {
                    requestCoarseMode();
                    coarseFailures = 0;
                    continue;
                }

                preciseParticipants.incrementAndGet();
                if (mode.get() != Mode.PRECISE) {
                    preciseParticipants.decrementAndGet();
                    continue;
                }

                boolean acquired = false;
                try {
                    while (mode.get() == Mode.PRECISE) {
                        SLOp result = tryPreciseLock(operationNumber, left, right);
                        if (result != null) {
                            recordPreciseUsefulness(operationNumber, result.ranges());
                            acquired = true;
                            return result;
                        }

                        // A real overlapping conflict means that switching
                        // modes now would only cause transition thrashing.
                        preciseSuccessesWithoutBenefit.set(0);
                        Thread.yield();
                    }
                } finally {
                    if (!acquired) {
                        preciseParticipants.decrementAndGet();
                    }
                }
                continue;
            }

            // Admission is closed while the current mode drains.
            Thread.yield();
        }
    }

    @Override
    public void unlock(SLOp operation) {
        int operationNumber = operation.operationNumber();
        checkOperationNumber(operationNumber);

        if (isCoarse(operation)) {
            releaseCoarseLock(operationNumber);
            return;
        }

        this.ops[operationNumber].remove(operation.ranges());
        preciseParticipants.decrementAndGet();
    }

    /**
     * A non-blocking variant of the SemanticLockAtomicCountersV2 admission
     * protocol. On a conflict it rolls its reservation back and lets lock()
     * either retry briefly or initiate TO_PRECISE.
     */
    private int tryCoarseLock(int operationNumber) {
        for (int conflictInd : coarseLock.conflicts[operationNumber]) {
            if (coarseLock.lockCounts.get(conflictInd * coarseCounterDelta) > 0) {
                return COARSE_RETRY;
            }
        }
        for (int conflictInd : coarseLock.conflictAddrs[operationNumber]) {
            if (coarseLock.lockAdder[conflictInd * coarseCounterDelta].sum() > 0) {
                return COARSE_RETRY;
            }
        }

        boolean incremented = false;
        boolean acquired = false;
        try {
            if (coarseLock.selfConflict[operationNumber]) {
                if (!coarseLock.lockCounts.compareAndSet(
                        operationNumber * coarseCounterDelta,
                        0,
                        1
                )) {
                    return COARSE_RETRY;
                }
            } else {
                coarseLock.lockAdder[operationNumber * coarseCounterDelta].increment();
            }
            incremented = true;

            for (int conflictInd : coarseLock.conflicts[operationNumber]) {
                if (coarseLock.lockCounts.get(conflictInd * coarseCounterDelta) > 0) {
                    return COARSE_RETRY;
                }
            }
            for (int conflictInd : coarseLock.conflictAddrs[operationNumber]) {
                if (coarseLock.lockAdder[conflictInd * coarseCounterDelta].sum() > 0) {
                    return COARSE_RETRY;
                }
            }

            acquired = true;
            return COARSE_ACQUIRED;
        } finally {
            if (incremented && !acquired) {
                releaseCoarseLock(operationNumber);
            }
        }
    }

    private void releaseCoarseLock(int operationNumber) {
        if (coarseLock.selfConflict[operationNumber]) {
            coarseLock.lockCounts.decrementAndGet(operationNumber * coarseCounterDelta);
        } else {
            coarseLock.lockAdder[operationNumber * coarseCounterDelta].decrement();
        }
    }

    private SLOp tryPreciseLock(int operationNumber, int left, int right) {
        if (hasOverlappingConflict(operationNumber, left, right, null)) {
            return null;
        }

        SLRanges reservation = new SLRanges(
                ThreadLocalRandom.current().nextLong(Long.MAX_VALUE),
                left,
                right
        );
        this.ops[operationNumber].add(reservation);

        if (hasOverlappingConflict(operationNumber, left, right, reservation)) {
            this.ops[operationNumber].remove(reservation);
            return null;
        }

        return new SLOp(operationNumber, reservation);
    }

    private boolean hasOverlappingConflict(
            int operationNumber,
            int left,
            int right,
            SLRanges ownReservation
    ) {
        for (int conflictInd : this.conflicts[operationNumber]) {
            if (overlapsAny(this.ops[conflictInd], left, right, ownReservation)) {
                return true;
            }
        }
        for (int conflictInd : this.conflictAddrs[operationNumber]) {
            if (overlapsAny(this.ops[conflictInd], left, right, ownReservation)) {
                return true;
            }
        }
        return selfConflict[operationNumber]
                && overlapsAny(this.ops[operationNumber], left, right, ownReservation);
    }

    private static boolean overlapsAny(
            Set<SLRanges> ranges,
            int left,
            int right,
            SLRanges ownReservation
    ) {
        if (ranges.isEmpty()) {
            return false;
        }
        for (SLRanges range : ranges) {
            if (range == ownReservation) {
                continue;
            }
            if (range.left() < right && range.right() > left) {
                return true;
            }
        }
        return false;
    }

    private void recordPreciseUsefulness(int operationNumber, SLRanges ownReservation) {
        if (hasTypeLevelConflict(operationNumber, ownReservation)) {
            preciseSuccessesWithoutBenefit.set(0);
        } else {
            preciseSuccessesWithoutBenefit.incrementAndGet();
        }
    }

    /**
     * A successful precise operation is useful when another operation that
     * conflicts by type is active, but its descriptor does not overlap.
     */
    private boolean hasTypeLevelConflict(int operationNumber, SLRanges ownReservation) {
        for (int conflictInd : this.conflicts[operationNumber]) {
            if (!this.ops[conflictInd].isEmpty()) {
                return true;
            }
        }
        for (int conflictInd : this.conflictAddrs[operationNumber]) {
            if (!this.ops[conflictInd].isEmpty()) {
                return true;
            }
        }
        if (selfConflict[operationNumber]) {
            for (SLRanges range : this.ops[operationNumber]) {
                if (range != ownReservation) {
                    return true;
                }
            }
        }
        return false;
    }

    private void requestPreciseMode() {
        if (!mode.compareAndSet(Mode.COARSE, Mode.TO_PRECISE)) {
            return;
        }

        // Admission is already closed by TO_PRECISE. The per-operation
        // counters used by the coarse lock are also the exact in-flight
        // accounting we need here, so the hot path does not need a shared
        // global holder counter.
        while (!coarseModeIsQuiescent()) {
            Thread.yield();
        }

        preciseSuccessesWithoutBenefit.set(0);
        mode.set(Mode.PRECISE);
    }

    private boolean coarseModeIsQuiescent() {
        for (int operationNumber = 0;
                operationNumber < operationsNumber;
                operationNumber++) {
            int counterIndex = operationNumber * coarseCounterDelta;
            if (coarseLock.selfConflict[operationNumber]) {
                if (coarseLock.lockCounts.get(counterIndex) != 0) {
                    return false;
                }
            } else if (coarseLock.lockAdder[counterIndex].sum() != 0) {
                return false;
            }
        }
        return true;
    }

    private void requestCoarseMode() {
        if (!mode.compareAndSet(Mode.PRECISE, Mode.TO_COARSE)) {
            return;
        }

        while (preciseParticipants.get() != 0) {
            Thread.yield();
        }

        preciseSuccessesWithoutBenefit.set(0);
        mode.set(Mode.COARSE);
    }

    private SLOp coarseSLOp(
            int operationNumber,
            int left,
            int right,
            int coarseToken
    ) {
        long id = COARSE_TOKEN_MARKER | Integer.toUnsignedLong(coarseToken);
        return new SLOp(operationNumber, new SLRanges(id, left, right));
    }

    private static boolean isCoarse(SLOp operation) {
        return operation.ranges().id() < 0;
    }

    @SuppressWarnings("unused")
    private static int coarseToken(SLOp operation) {
        return (int) (operation.ranges().id() & COARSE_TOKEN_VALUE_MASK);
    }

    private void fillConflictIndexes(int[][] sourceConflicts) {
        for (int i = 0; i < operationsNumber; i++) {
            int exclusiveConflicts = 0;
            int sharedConflicts = 0;
            for (int j = 0; j < operationsNumber; j++) {
                if (i == j || sourceConflicts[i][j] != 1) {
                    continue;
                }
                if (selfConflict[j]) {
                    exclusiveConflicts++;
                } else {
                    sharedConflicts++;
                }
            }

            this.conflicts[i] = new int[exclusiveConflicts];
            this.conflictAddrs[i] = new int[sharedConflicts];

            int exclusiveIndex = 0;
            int sharedIndex = 0;
            for (int j = 0; j < operationsNumber; j++) {
                if (i == j || sourceConflicts[i][j] != 1) {
                    continue;
                }
                if (selfConflict[j]) {
                    this.conflicts[i][exclusiveIndex++] = j;
                } else {
                    this.conflictAddrs[i][sharedIndex++] = j;
                }
            }
        }
    }

    private static void validateArguments(int operationsNumber, int[][] conflicts) {
        if (operationsNumber <= 0) {
            throw new IllegalStateException("operationsNumber must be positive");
        }
        if (conflicts == null || conflicts.length != operationsNumber) {
            throw new IllegalStateException("conflicts matrix has invalid height");
        }
        for (int i = 0; i < operationsNumber; i++) {
            if (conflicts[i] == null || conflicts[i].length != operationsNumber) {
                throw new IllegalStateException(
                        "conflicts matrix has invalid width at row " + i
                );
            }
        }
    }

    private void checkOperationNumber(int operationNumber) {
        if (operationNumber < 0 || operationNumber >= operationsNumber) {
            throw new IllegalArgumentException(
                    "Invalid operation number: " + operationNumber
            );
        }
    }
}
