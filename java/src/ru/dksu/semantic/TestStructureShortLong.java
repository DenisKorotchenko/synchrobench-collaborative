package ru.dksu.semantic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class TestStructureShortLong implements ITestStructure {
    AtomicInteger[] elements;

    private final int size;

    private final int shortBucketSize;
    Lock[] shortLocks;
    private final int MULTIPLICATOR = 10000;

    private int SHORT_UPDATE = 0;
    private int LONG_UPDATE = 1;
    private int SHORT_ADD = 2;
    private int LONG_ADD = 3;
    private int SHORT_GET = 4;
    private int LONG_GET = 5;

    private int divUp(int a, int b) {
        return (a + b - 1) / b;
    }

    SemanticLock semanticLock = new SemanticLock(6,
            new int[][] {
                    {0, 1, 0, 1, 0, 1},
                    {1, 1, 1, 1, 1, 1},
                    {0, 1, 0, 1, 0, 1},
                    {1, 1, 1, 0, 1, 1},
                    {0, 1, 0, 1, 0, 1},
                    {1, 1, 1, 1, 1, 0}
    },
            true);

    public TestStructureShortLong(Integer size) {
        this.size = size;
        elements = new AtomicInteger[size];
        for (int i = 0; i < size; i++) {
            elements[i] = new AtomicInteger(0);
        }
        this.shortBucketSize = divUp(size, (MULTIPLICATOR * 2));
        shortLocks = new Lock[divUp(this.size, this.shortBucketSize)];
        for (int i = 0; i < shortLocks.length; i++) {
            shortLocks[i] = new ReentrantLock();
        }
    }

//    @Override
//    public void updateElement(int index, int value) {
//        elements[index].set(value);
//    }
//
//    @Override
//    public void addElement(int index, int add) {
//        elements[index].addAndGet(add);
//    }
    boolean isLongRange(int from, int to) {
        return to - from > this.size / MULTIPLICATOR;
    }

    @Override
    public void updateRange(int from, int to, int value) {
        if (isLongRange(from, to)) {
            // LONG
            semanticLock.lock(LONG_UPDATE);
            try {
                for (int i = from; i <= to; i++) {
                    elements[i].set(value);
                }
            } finally {
                semanticLock.unlock(LONG_UPDATE);
            }
        } else {
            // SHORT
            semanticLock.lock(SHORT_UPDATE);
            try {
                int firstBucket = from / this.shortBucketSize;
                int lastBucket = to / this.shortBucketSize;
                for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                    this.shortLocks[bucket].lock();
                }
                try {
                    for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                        for (int i = max(from, bucket * this.shortBucketSize); i <= min(to, bucket * this.shortBucketSize + this.shortBucketSize - 1); i++) {
                            this.elements[i].set(value);
                        }
                    }
                } finally {
                    for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                        this.shortLocks[bucket].unlock();
                    }
                }
            } finally {
                semanticLock.unlock(SHORT_UPDATE);
            }
        }
    }

    @Override
    public void addRange(int from, int to, int add) {
        if (isLongRange(from, to)) {
            // LONG
            semanticLock.lock(LONG_ADD);
            try {
                for (int i = from; i <= to; i++) {
                    elements[i].addAndGet(add);
                }
            } finally {
                semanticLock.unlock(LONG_ADD);
            }
        } else {
            // SHORT
            semanticLock.lock(SHORT_ADD);
            try {
                int firstBucket = from / this.shortBucketSize;
                int lastBucket = to / this.shortBucketSize;
                for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                    this.shortLocks[bucket].lock();
                }
                try {
                    for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                        for (int i = max(from, bucket * this.shortBucketSize); i <= min(to, bucket * this.shortBucketSize + this.shortBucketSize - 1); i++) {
                            this.elements[i].addAndGet(add);
                        }
                    }
                } finally {
                    for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                        this.shortLocks[bucket].unlock();
                    }
                }
            } finally {
                semanticLock.unlock(SHORT_ADD);
            }
        }
    }

    @Override
    public int getRangeSum(int from, int to) {
        if (isLongRange(from, to)) {
            // LONG
            semanticLock.lock(LONG_GET);
            int sum = 0;
            try {
                for (int i = from; i <= to; i++) {
                    sum += elements[i].get();
                }
                return sum;
            } finally {
                semanticLock.unlock(LONG_GET);
            }
        } else {
            // SHORT
            semanticLock.lock(SHORT_GET);
            try {
                int firstBucket = from / this.shortBucketSize;
                int lastBucket = to / this.shortBucketSize;
                for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                    this.shortLocks[bucket].lock();
                }
                try {
                    int sum = 0;
                    for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                        for (int i = max(from, bucket * this.shortBucketSize); i <= min(to, bucket * this.shortBucketSize + this.shortBucketSize - 1); i++) {
                            sum += this.elements[i].get();
                        }
                    }
                    return sum;
                } finally {
                    for (int bucket = firstBucket; bucket <= lastBucket; bucket++) {
                        this.shortLocks[bucket].unlock();
                    }
                }
            } finally {
                semanticLock.unlock(SHORT_GET);
            }
        }
    }

    @Override
    public void clear() {
        for (AtomicInteger element : elements) {
            element.set(0);
        }
    }

//    @Override
//    public int getElement(int index) {
//        return elements[index].get();
//    }
}
