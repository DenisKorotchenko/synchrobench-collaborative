package ru.dksu.semantic;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class ARATestStructure implements ITestStructure {
    AtomicReferenceArray<Integer> elements;

    private final int size;
    private final int MULTIPLICATOR = 10;

    SemanticLock semanticLock = new SemanticLock(3,
            // updateRange, addRange, getRangeSum
            new int[][] {
                    {1, 1, 1},
                    {1, 0, 1},
                    {1, 1, 0}
    },
            true);

    public ARATestStructure(Integer size) {
        this.size = size;
        this.elements = new AtomicReferenceArray<Integer>(new Integer[size]);
        for (int i = 0; i < size; i++) {
            this.elements.set(i, 0);
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
        semanticLock.lock(0);
        try {
            for (int i = from; i <= to; i++) {
                elements.updateAndGet(i, el -> value);
            }
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public void addRange(int from, int to, int add) {
        semanticLock.lock(1);
        try {
            for (int i = from; i <= to; i++) {
                elements.updateAndGet(i, el -> el + add);
            }
        } finally {
            semanticLock.unlock(1);
        }
    }

    @Override
    public int getRangeSum(int from, int to) {
        semanticLock.lock(2);
        int sum = 0;
        try {
            for (int i = from; i <= to; i++) {
                sum += elements.get(i);
            }
            return sum;
        } finally {
            semanticLock.unlock(2);
        }
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            elements.set(i, 0);
        }
    }

//    @Override
//    public int getElement(int index) {
//        return elements[index].get();
//    }
}
