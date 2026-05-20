package ru.dksu.semantic;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


// Lock-based semantic lock, fair
public class TestStructureAF implements ITestStructure {
    AtomicInteger[] elements;

    private final int size;
    private final int MULTIPLICATOR = 10;

    SemanticLockFair semanticLock = new SemanticLockFair(3,
            // updateRange, addRange, getRangeSum
            new int[][] {
                    {1, 1, 1},
                    {1, 0, 1},
                    {1, 1, 0}
    },
            true);

    public TestStructureAF(Integer size) {
        this.size = size;
        elements = new AtomicInteger[size];

        ArrayList<Thread> list = new ArrayList<Thread>();
        int D = size / 64;
        for (int i = 0; i < 64; i++) {
            int finalI = i;
            var t = new Thread(() -> {
                Random r = new Random();
                int end = (finalI + 1) * D;
                if (finalI == 63) {
                    end = size;
                }
                for (int j = finalI * D; j < end; j++) {
                    elements[j] = new AtomicInteger(r.nextInt() % size);
                }
            });
            list.add(t);
            t.start();
        }
        for (var t: list) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
                elements[i].set(value);
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
                elements[i].addAndGet(add);
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
                sum += elements[i].get();
            }
            return sum;
        } finally {
            semanticLock.unlock(2);
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
