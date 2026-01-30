package ru.dksu.semantic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestStructureSimple implements ITestStructure {
    AtomicInteger[] elements;

    private final int size;
    // private final int MULTIPLICATOR = 10;

    Lock lock = new ReentrantLock();

    public TestStructureSimple(Integer size) {
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
        lock.lock();
        try {
            for (int i = from; i <= to; i++) {
                elements[i].set(value);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addRange(int from, int to, int add) {
        lock.lock();
        try {
            for (int i = from; i <= to; i++) {
                elements[i].addAndGet(add);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getRangeSum(int from, int to) {
        lock.lock();
        int sum = 0;
        try {
            for (int i = from; i <= to; i++) {
                sum += elements[i].get();
            }
            return sum;
        } finally {
            lock.unlock();
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
