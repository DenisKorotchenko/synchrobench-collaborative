package ru.dksu.semantic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class TestStructureShortLongRW implements ITestStructure {
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

    ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public TestStructureShortLongRW(Integer size) {
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
            rwLock.writeLock().lock();
            try {
                for (int i = from; i <= to; i++) {
                    elements[i].set(value);
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        } else {
            // SHORT
            rwLock.writeLock().lock();
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
                rwLock.writeLock().unlock();
            }
        }
    }

    @Override
    public void addRange(int from, int to, int add) {
        if (isLongRange(from, to)) {
            // LONG
            rwLock.writeLock().lock();
            try {
                for (int i = from; i <= to; i++) {
                    elements[i].addAndGet(add);
                }
            } finally {
                rwLock.writeLock().unlock();
            }
        } else {
            // SHORT
            rwLock.writeLock().lock();
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
                rwLock.writeLock().unlock();
            }
        }
    }

    @Override
    public int getRangeSum(int from, int to) {
        if (isLongRange(from, to)) {
            // LONG
            rwLock.readLock().lock();
            int sum = 0;
            try {
                for (int i = from; i <= to; i++) {
                    sum += elements[i].get();
                }
                return sum;
            } finally {
                rwLock.readLock().unlock();
            }
        } else {
            // SHORT
            rwLock.readLock().lock();
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
                rwLock.readLock().unlock();
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
