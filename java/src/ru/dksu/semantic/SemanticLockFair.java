package ru.dksu.semantic;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SemanticLockFair {
    int operationsNumber;
    int[][] conflicts;

    AtomicInteger[] lockCounts;

    public SemanticLockFair(
            int operationsNumber,
            int[][] conflicts
    ) {
        this(operationsNumber,
                conflicts,
                false);
    }

    public final boolean fairness;

    public SemanticLockFair(
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

    private boolean fairnessCheck(OperationRequest operation) {
        var iterator = fairnessQueue.iterator();
        while (iterator.hasNext()) {
            OperationRequest next = iterator.next();
            if (next.threadId == Thread.currentThread().threadId()) {
                return true;
            }
            if (conflicts[next.operationNumber][operation.operationNumber] == 1) {
                return false;
            }
        }
        return false;
    }

    public boolean tryLock(OperationRequest operationRequest) {
        if (fairness && !fairnessCheck(operationRequest)) {
            return false;
        }
        int value = this.lockCounts[operationRequest.operationNumber].incrementAndGet();

        if (this.conflicts[operationRequest.operationNumber][operationRequest.operationNumber] > 0 && value > 1) {
            this.lockCounts[operationRequest.operationNumber].decrementAndGet();
            return false;
        }
        for (int i = 0; i < operationsNumber; i++) {
            if (i == operationRequest.operationNumber)
                continue;
            if (this.conflicts[operationRequest.operationNumber][i] > 0 && this.lockCounts[i].get() > 0) {
                this.lockCounts[operationRequest.operationNumber].decrementAndGet();
                return false;
            }
        }
        if (fairness) {
            if (!fairnessQueue.remove(operationRequest)) {
                System.err.println("Thread is wrong!");
                throw new RuntimeException("Thread is wrong");
            }
        }
        return true;
    }

    public record OperationRequest(
        Long threadId,
        Integer operationNumber
    ) {}

    public final ConcurrentLinkedDeque<OperationRequest> fairnessQueue = new ConcurrentLinkedDeque<>();

    public void lock(int operationNumber) {
        OperationRequest operationRequest = new OperationRequest(Thread.currentThread().threadId(), operationNumber);
        if (fairness) {
            fairnessQueue.add(operationRequest);
        }
        while (!tryLock(operationRequest)) {
            Thread.yield();
        }
    }
    
    public void unlock(int operationNumber) {
        this.lockCounts[operationNumber].decrementAndGet();
    }
}
