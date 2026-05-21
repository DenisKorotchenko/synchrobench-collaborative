package ru.dksu.semantic;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SemanticLockAtomicCounters {
    int operationsNumber;
    int[][] conflicts;

    AtomicInteger[] lockCounts;

    private static final ConcurrentLinkedQueue<ThreadStatistics> ALL_THREAD_STATISTICS = new ConcurrentLinkedQueue<>();
    private static final ThreadLocal<ThreadStatistics> THREAD_STATISTICS = ThreadLocal.withInitial(() -> {
        ThreadStatistics statistics = new ThreadStatistics();
        ALL_THREAD_STATISTICS.add(statistics);
        return statistics;
    });

    public SemanticLockAtomicCounters(
            int operationsNumber,
            int[][] conflicts
    ) {
        this(operationsNumber,
                conflicts,
                false);
    }

    public final boolean fairness;

    public SemanticLockAtomicCounters(
            int operationsNumber,
            int[][] conflicts,
            boolean fair
    ) {
        this.fairness = fair;
        if (operationsNumber <= 0) {
            throw new IllegalStateException("operationsNumber must be positive");
        }
        if (conflicts == null || conflicts.length != operationsNumber) {
            throw new IllegalStateException("conflicts matrix has invalid height");
        }
        for (int i = 0; i < operationsNumber; i++) {
            if (conflicts[i] == null || conflicts[i].length != operationsNumber) {
                throw new IllegalStateException("conflicts matrix has invalid width at row " + i);
            }
        }
        this.operationsNumber = operationsNumber;
        this.conflicts = conflicts;
        this.lockCounts = new AtomicInteger[operationsNumber];
        for (int i = 0; i < operationsNumber; i++) {
            this.lockCounts[i] = new AtomicInteger();
        }
    }

    public boolean tryLock(int operationNumber) {
        checkOperationNumber(operationNumber);
        long startedAt = System.nanoTime();
        boolean locked = false;
        boolean incremented = false;
        try {
            if (fairness) {
                Long firstThreadId = threadsQueue.peek();
                if (firstThreadId == null || firstThreadId.longValue() != Thread.currentThread().getId()) {
                    return false;
                }
            }

            int value = this.lockCounts[operationNumber].incrementAndGet();
            incremented = true;

            if (this.conflicts[operationNumber][operationNumber] > 0 && value > 1) {
                return false;
            }
            for (int i = 0; i < operationsNumber; i++) {
                if (i == operationNumber) {
                    continue;
                }
                if (this.conflicts[operationNumber][i] > 0 && this.lockCounts[i].get() > 0) {
                    return false;
                }
            }
            if (fairness) {
                Long firstThreadId = threadsQueue.poll();
                if (firstThreadId == null || firstThreadId.longValue() != Thread.currentThread().getId()) {
                    System.err.println("Thread is wrong!");
                    throw new RuntimeException("Thread is wrong");
                }
            }
            locked = true;
            return true;
        } finally {
            if (!locked && incremented) {
                this.lockCounts[operationNumber].decrementAndGet();
            }
            THREAD_STATISTICS.get().record(operationNumber, locked, System.nanoTime() - startedAt);
        }
    }

    public final Queue<Long> threadsQueue = new ConcurrentLinkedQueue<>();

    public void lock(int operationNumber) {
        checkOperationNumber(operationNumber);
        if (fairness) {
            threadsQueue.add(Thread.currentThread().getId());
        }
        while (!tryLock(operationNumber)) {
            Thread.yield();
        }
    }
    
    public void unlock(int operationNumber) {
        checkOperationNumber(operationNumber);
        int value = this.lockCounts[operationNumber].decrementAndGet();
        if (value < 0) {
            this.lockCounts[operationNumber].incrementAndGet();
            throw new IllegalStateException("Unlock without matching lock for operation " + operationNumber);
        }
    }

    private void checkOperationNumber(int operationNumber) {
        if (operationNumber < 0 || operationNumber >= operationsNumber) {
            throw new IllegalArgumentException("Invalid operation number: " + operationNumber);
        }
    }

    public static LockStatistics getStatistics() {
        LockStatistics result = new LockStatistics();
        for (ThreadStatistics statistics : ALL_THREAD_STATISTICS) {
            statistics.addTo(result);
        }
        return result;
    }

    public static LockStatistics getAndResetStatistics() {
        LockStatistics result = getStatistics();
        resetStatistics();
        return result;
    }

    public static void resetStatistics() {
        for (ThreadStatistics statistics : ALL_THREAD_STATISTICS) {
            statistics.reset();
        }
    }

    private static final class ThreadStatistics {
        private long successfulRequests;
        private long failedRequests;
        private long successfulTimeNanos;
        private long failedTimeNanos;
        private long[] successfulRequestsByOperation = new long[0];
        private long[] failedRequestsByOperation = new long[0];
        private long[] successfulTimeNanosByOperation = new long[0];
        private long[] failedTimeNanosByOperation = new long[0];

        private void record(int operationNumber, boolean success, long timeNanos) {
            ensureCapacity(operationNumber + 1);
            if (success) {
                successfulRequests++;
                successfulTimeNanos += timeNanos;
                successfulRequestsByOperation[operationNumber]++;
                successfulTimeNanosByOperation[operationNumber] += timeNanos;
            } else {
                failedRequests++;
                failedTimeNanos += timeNanos;
                failedRequestsByOperation[operationNumber]++;
                failedTimeNanosByOperation[operationNumber] += timeNanos;
            }
        }

        private void addTo(LockStatistics result) {
            result.add(
                    successfulRequests,
                    failedRequests,
                    successfulTimeNanos,
                    failedTimeNanos,
                    successfulRequestsByOperation,
                    failedRequestsByOperation,
                    successfulTimeNanosByOperation,
                    failedTimeNanosByOperation
            );
        }

        private void reset() {
            successfulRequests = 0;
            failedRequests = 0;
            successfulTimeNanos = 0;
            failedTimeNanos = 0;
            Arrays.fill(successfulRequestsByOperation, 0);
            Arrays.fill(failedRequestsByOperation, 0);
            Arrays.fill(successfulTimeNanosByOperation, 0);
            Arrays.fill(failedTimeNanosByOperation, 0);
        }

        private void ensureCapacity(int capacity) {
            if (successfulRequestsByOperation.length >= capacity) {
                return;
            }
            successfulRequestsByOperation = Arrays.copyOf(successfulRequestsByOperation, capacity);
            failedRequestsByOperation = Arrays.copyOf(failedRequestsByOperation, capacity);
            successfulTimeNanosByOperation = Arrays.copyOf(successfulTimeNanosByOperation, capacity);
            failedTimeNanosByOperation = Arrays.copyOf(failedTimeNanosByOperation, capacity);
        }
    }

    public static final class LockStatistics {
        private long successfulRequests;
        private long failedRequests;
        private long successfulTimeNanos;
        private long failedTimeNanos;
        private long[] successfulRequestsByOperation = new long[0];
        private long[] failedRequestsByOperation = new long[0];
        private long[] successfulTimeNanosByOperation = new long[0];
        private long[] failedTimeNanosByOperation = new long[0];

        private void add(
                long successfulRequests,
                long failedRequests,
                long successfulTimeNanos,
                long failedTimeNanos,
                long[] successfulRequestsByOperation,
                long[] failedRequestsByOperation,
                long[] successfulTimeNanosByOperation,
                long[] failedTimeNanosByOperation
        ) {
            this.successfulRequests += successfulRequests;
            this.failedRequests += failedRequests;
            this.successfulTimeNanos += successfulTimeNanos;
            this.failedTimeNanos += failedTimeNanos;
            ensureCapacity(successfulRequestsByOperation.length);
            for (int i = 0; i < successfulRequestsByOperation.length; i++) {
                this.successfulRequestsByOperation[i] += successfulRequestsByOperation[i];
                this.failedRequestsByOperation[i] += failedRequestsByOperation[i];
                this.successfulTimeNanosByOperation[i] += successfulTimeNanosByOperation[i];
                this.failedTimeNanosByOperation[i] += failedTimeNanosByOperation[i];
            }
        }

        public boolean hasData() {
            return successfulRequests != 0 || failedRequests != 0;
        }

        public long getSuccessfulRequests() {
            return successfulRequests;
        }

        public long getFailedRequests() {
            return failedRequests;
        }

        public long getSuccessfulTimeNanos() {
            return successfulTimeNanos;
        }

        public long getFailedTimeNanos() {
            return failedTimeNanos;
        }

        public long[] getSuccessfulRequestsByOperation() {
            return Arrays.copyOf(successfulRequestsByOperation, successfulRequestsByOperation.length);
        }

        public long[] getFailedRequestsByOperation() {
            return Arrays.copyOf(failedRequestsByOperation, failedRequestsByOperation.length);
        }

        public long[] getSuccessfulTimeNanosByOperation() {
            return Arrays.copyOf(successfulTimeNanosByOperation, successfulTimeNanosByOperation.length);
        }

        public long[] getFailedTimeNanosByOperation() {
            return Arrays.copyOf(failedTimeNanosByOperation, failedTimeNanosByOperation.length);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("  SemanticLockAtomicCounters requests:    \t")
                    .append(successfulRequests + failedRequests)
                    .append('\n');
            appendLine(result, "    |--successful", successfulRequests, successfulTimeNanos);
            appendLine(result, "    |--unsuccessful", failedRequests, failedTimeNanos);
            appendTotalPerSuccessLine(result, "    |--total time per successful request", successfulRequests, successfulTimeNanos + failedTimeNanos);
            appendRequestsPerSuccessLine(result, "    |--requests per successful request", successfulRequests, successfulRequests + failedRequests);
            for (int i = 0; i < successfulRequestsByOperation.length; i++) {
                long operationRequests = successfulRequestsByOperation[i] + failedRequestsByOperation[i];
                if (operationRequests == 0) {
                    continue;
                }
                result.append("    |--operation ").append(i).append(":\n");
                appendLine(result, "       |--successful", successfulRequestsByOperation[i], successfulTimeNanosByOperation[i]);
                appendLine(result, "       |--unsuccessful", failedRequestsByOperation[i], failedTimeNanosByOperation[i]);
                appendTotalPerSuccessLine(
                        result,
                        "       |--total time per successful request",
                        successfulRequestsByOperation[i],
                        successfulTimeNanosByOperation[i] + failedTimeNanosByOperation[i]
                );
                appendRequestsPerSuccessLine(
                        result,
                        "       |--requests per successful request",
                        successfulRequestsByOperation[i],
                        operationRequests
                );
            }
            return result.toString();
        }

        private static void appendLine(StringBuilder result, String name, long requests, long timeNanos) {
            result.append(name)
                    .append(":\t")
                    .append(requests)
                    .append(" requests, time(ns)=")
                    .append(timeNanos)
                    .append(", avg(ns)=")
                    .append(requests == 0 ? 0 : timeNanos / requests)
                    .append('\n');
        }

        private static void appendTotalPerSuccessLine(StringBuilder result, String name, long successfulRequests, long totalTimeNanos) {
            result.append(name)
                    .append(":\t")
                    .append(successfulRequests == 0 ? 0 : totalTimeNanos / successfulRequests)
                    .append(" ns")
                    .append('\n');
        }

        private static void appendRequestsPerSuccessLine(StringBuilder result, String name, long successfulRequests, long totalRequests) {
            result.append(name)
                    .append(":\t")
                    .append(successfulRequests == 0 ? 0.0 : (double) totalRequests / successfulRequests)
                    .append(" requests")
                    .append('\n');
        }

        private void ensureCapacity(int capacity) {
            if (successfulRequestsByOperation.length >= capacity) {
                return;
            }
            successfulRequestsByOperation = Arrays.copyOf(successfulRequestsByOperation, capacity);
            failedRequestsByOperation = Arrays.copyOf(failedRequestsByOperation, capacity);
            successfulTimeNanosByOperation = Arrays.copyOf(successfulTimeNanosByOperation, capacity);
            failedTimeNanosByOperation = Arrays.copyOf(failedTimeNanosByOperation, capacity);
        }
    }
}
