package ru.dksu.semantic;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NonFairSemanticLock {
    int operationsNumber;
    int[][] conflicts;

    Lock overallLock = new ReentrantLock();

    int[] lockCounts;
    public NonFairSemanticLock(
            int operationsNumber,
            int[][] conflicts
    ) {
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
        overallLock.lock();
        try {
            for (int i = 0; i < operationsNumber; i++) {
                if (this.conflicts[operationNumber][i] > 0 && this.lockCounts[i] > 0) {
                    return false;
                }
            }
            this.lockCounts[operationNumber]++;
            return true;
        } finally {
            overallLock.unlock();
        }
    }

    public void lock(int operationNumber) {
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
