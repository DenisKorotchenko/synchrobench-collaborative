package ru.dksu.semantic;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SemanticLock {
    int operationsNumber;
    int[][] conflicts;

    AtomicInteger[] lockCounts;

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
        this.lockCounts = new AtomicInteger[operationsNumber];
        for (int i = 0; i < operationsNumber; i++) {
            this.lockCounts[i] = new AtomicInteger();
        }
    }


    private boolean tryLock(int operationNumber) {
        if (fairness && threadsQueue.peek() != Thread.currentThread().getId()) {
            return false;
        }
        int value = this.lockCounts[operationNumber].incrementAndGet();

        if (this.conflicts[operationNumber][operationNumber] > 0 && value > 1) {
            this.lockCounts[operationNumber].decrementAndGet();
            return false;
        }
        for (int i = 0; i < operationsNumber; i++) {
            if (i == operationNumber)
                continue;
            if (this.conflicts[operationNumber][i] > 0 && this.lockCounts[i].get() > 0) {
                this.lockCounts[operationNumber].decrementAndGet();
                return false;
            }
        }
        if (fairness) {
            if (threadsQueue.poll() != Thread.currentThread().getId()) {
                System.err.println("Thread is wrong!");
                throw new RuntimeException("Thread is wrong");
            }
        }
        return true;
    }

    private final Queue<Long> threadsQueue = new ConcurrentLinkedQueue<>();

    public void lock(int operationNumber) {
        if (fairness) {
            threadsQueue.add(Thread.currentThread().getId());
        }
        while (!tryLock(operationNumber)) {
            Thread.yield();
        }
    }
    
    public void unlock(int operationNumber) {
        this.lockCounts[operationNumber].decrementAndGet();
    }
}
