package ru.dksu.semantic;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SemanticLock {
    int operationsNumber;
    int[][] conflicts;

    Lock overallLock = new ReentrantLock();

    int[] lockCounts;

    public SemanticLock(
            int operationsNumber,
            int[][] conflicts
    ) {
        this(operationsNumber,
                conflicts,
                false);
    }

    private final boolean fairness;

    public SemanticLock(
            int operationsNumber,
            int[][] conflicts,
            boolean fair
    ) {
        this.fairness = fair;
        if (operationsNumber == 0) {
            throw new IllegalStateException();
        }
        if (conflicts.length != operationsNumber || conflicts[0].length != operationsNumber) {
            throw new IllegalStateException();
        }
        this.operationsNumber = operationsNumber;
        this.conflicts = conflicts;
        this.lockCounts = new int[operationsNumber];
    }

    public boolean tryLock(int operationNumber) {
        if (fairness && threadsQueue.peek() != Thread.currentThread().getId()) {
            return false;
        }
        overallLock.lock();
        try {
            for (int i = 0; i < operationsNumber; i++) {
                if (this.conflicts[operationNumber][i] > 0 && this.lockCounts[i] > 0) {
                    return false;
                }
            }
            this.lockCounts[operationNumber]++;
            if (fairness) {
                try {
                    threadsQueue.take();
                } catch (InterruptedException ignored) {}
            }
            return true;
        } finally {
            overallLock.unlock();
        }
    }

    private final BlockingQueue<Long> threadsQueue = new LinkedBlockingQueue<>();

    public void lock(int operationNumber) {
        if (fairness) {
            threadsQueue.add(Thread.currentThread().getId());
        }
        while (!tryLock(operationNumber)) {
            Thread.yield();
        }
    }

    public void unlock(int operationNumber) {
        overallLock.lock();
        try {
            this.lockCounts[operationNumber]--;
        } finally {
            overallLock.unlock();
        }
    }
}
