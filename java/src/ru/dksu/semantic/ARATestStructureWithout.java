package ru.dksu.semantic;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ARATestStructureWithout implements ITestStructure {
    AtomicReferenceArray<Integer> elements;

    private final int size;
    private final int MULTIPLICATOR = 10;

    public ARATestStructureWithout(Integer size) {
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
        for (int i = from; i <= to; i++) {
            elements.updateAndGet(i, el -> value);
        }
    }

    @Override
    public void addRange(int from, int to, int add) {
        for (int i = from; i <= to; i++) {
            elements.updateAndGet(i, el -> el + add);
        }
    }

    @Override
    public int getRangeSum(int from, int to) {
        int sum = 0;

        for (int i = from; i <= to; i++) {
            sum += elements.get(i);
        }
        return sum;
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
