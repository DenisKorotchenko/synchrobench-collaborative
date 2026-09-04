package ru.dksu.semantic;

import java.util.List;

/**
 * Optional benchmark telemetry for adaptive locks. Implementations must keep
 * collection off the operation hot path: snapshots are read by the benchmark
 * sampler and events are emitted only when a mode transition occurs.
 */
public interface LockModeTelemetryProvider {
    void startLockModeTelemetry();

    void stopLockModeTelemetry();

    LockModeSnapshot lockModeSnapshot();

    List<LockModeTransitionEvent> drainLockModeTransitionEvents();

    default String lockOperationName(int operationNumber) {
        return Integer.toString(operationNumber);
    }

    final class LockModeSnapshot {
        private final String mode;
        private final long completedTransitionSequence;
        private final int preciseParticipants;
        private final int preciseSuccessesWithoutBenefit;

        public LockModeSnapshot(
                String mode,
                long completedTransitionSequence,
                int preciseParticipants,
                int preciseSuccessesWithoutBenefit
        ) {
            this.mode = mode;
            this.completedTransitionSequence = completedTransitionSequence;
            this.preciseParticipants = preciseParticipants;
            this.preciseSuccessesWithoutBenefit = preciseSuccessesWithoutBenefit;
        }

        public String mode() {
            return mode;
        }

        public long completedTransitionSequence() {
            return completedTransitionSequence;
        }

        public int preciseParticipants() {
            return preciseParticipants;
        }

        public int preciseSuccessesWithoutBenefit() {
            return preciseSuccessesWithoutBenefit;
        }
    }

    final class LockModeTransitionEvent {
        private final long sequence;
        private final long startedNanos;
        private final long completedNanos;
        private final String fromMode;
        private final String transitionMode;
        private final String toMode;
        private final String reason;
        private final int triggerOperationNumber;
        private final long triggerValue;
        private final long threshold;
        private final int preciseParticipantsAtStart;

        public LockModeTransitionEvent(
                long sequence,
                long startedNanos,
                long completedNanos,
                String fromMode,
                String transitionMode,
                String toMode,
                String reason,
                int triggerOperationNumber,
                long triggerValue,
                long threshold,
                int preciseParticipantsAtStart
        ) {
            this.sequence = sequence;
            this.startedNanos = startedNanos;
            this.completedNanos = completedNanos;
            this.fromMode = fromMode;
            this.transitionMode = transitionMode;
            this.toMode = toMode;
            this.reason = reason;
            this.triggerOperationNumber = triggerOperationNumber;
            this.triggerValue = triggerValue;
            this.threshold = threshold;
            this.preciseParticipantsAtStart = preciseParticipantsAtStart;
        }

        public long sequence() {
            return sequence;
        }

        public long startedNanos() {
            return startedNanos;
        }

        public long completedNanos() {
            return completedNanos;
        }

        public String fromMode() {
            return fromMode;
        }

        public String transitionMode() {
            return transitionMode;
        }

        public String toMode() {
            return toMode;
        }

        public String reason() {
            return reason;
        }

        public int triggerOperationNumber() {
            return triggerOperationNumber;
        }

        public long triggerValue() {
            return triggerValue;
        }

        public long threshold() {
            return threshold;
        }

        public int preciseParticipantsAtStart() {
            return preciseParticipantsAtStart;
        }
    }
}
