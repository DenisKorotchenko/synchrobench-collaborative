package ru.dksu.semantic;

import java.util.concurrent.atomic.AtomicInteger;

public class TestStructureRangeV3 implements ITestStructure {
    AtomicInteger[] elements;

    private final int size;
    private final int MULTIPLICATOR = 10;

    ISemanticLockRange semanticLock = new SemanticLockRangeV3(5,
            // updateRange, addRange, getRangeSum, get, set
            new int[][] {
                    {1, 1, 1, 1, 1},
                    {1, 0, 1, 1, 1},
                    {1, 1, 0, 0, 1},
                    {1, 1, 0, 0, 0},
                    {1, 1, 1, 0, 0}
            },
            false);

    public TestStructureRangeV3(Integer size) {
        this.size = size;
        elements = new AtomicInteger[size];
        for (int i = 0; i < size; i++) {
            elements[i] = new AtomicInteger(0);
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

    @Override
    public void updateRange(int from, int to, int value) {
        var p = semanticLock.lock(0, from, to);
        try {
            for (int i = from; i < to; i++) {
                elements[i].set(value);
            }
        } finally {
            semanticLock.unlock(p);
        }
    }

    @Override
    public void addRange(int from, int to, int add) {
        var p = semanticLock.lock(1, from, to);
        try {
            for (int i = from; i < to; i++) {
                elements[i].addAndGet(add);
            }
        } finally {
            semanticLock.unlock(p);
        }
    }

    @Override
    public int getRangeSum(int from, int to) {
        var p = semanticLock.lock(2, from, to);
        int sum = 0;
        try {
            for (int i = from; i < to; i++) {
                sum += elements[i].get();
            }
            return sum;
        } finally {
            semanticLock.unlock(p);
        }
    }

    @Override
    public void clear() {
        for (AtomicInteger element : elements) {
            element.set(0);
        }
    }

    @Override
    public int get(int index) {
        var r = semanticLock.lock(3, index, index+1);
        try {
            return elements[index].get();
        } finally {
            semanticLock.unlock(r);
        }
    }

    @Override
    public int set(int index, int value) {
        var r = semanticLock.lock(4, index, index+1);
        try {
            return elements[index].getAndSet(value);
        } finally {
            semanticLock.unlock(r);
        }
    }

//    @Override
//    public int getElement(int index) {
//        return elements[index].get();
//    }
}
