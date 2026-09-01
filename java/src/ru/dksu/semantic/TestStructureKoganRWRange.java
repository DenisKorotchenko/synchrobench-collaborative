package ru.dksu.semantic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The benchmark structure protected by {@link KoganRWRange}.
 *
 * <p>Updates are writers, while {@code getRangeSum} and {@code get} are
 * readers. Consequently, overlapping {@code addRange} calls are serialized
 * by this conventional reader-writer lock even though they commute in the
 * semantic-lock implementations.</p>
 */
public class TestStructureKoganRWRange implements ITestStructure {
    private final AtomicInteger[] elements;
    private final KoganRWRange rangeLock = new KoganRWRange();

    public TestStructureKoganRWRange(Integer size) {
        elements = new AtomicInteger[size];
        for (int index = 0; index < size; index++) {
            elements[index] = new AtomicInteger(0);
        }
    }

    @Override
    public void updateRange(int from, int to, int value) {
        KoganRWRange.LockHandle handle = rangeLock.writeLock(from, to);
        try {
            for (int index = from; index < to; index++) {
                elements[index].set(value);
            }
        } finally {
            rangeLock.unlock(handle);
        }
    }

    @Override
    public void addRange(int from, int to, int add) {
        KoganRWRange.LockHandle handle = rangeLock.writeLock(from, to);
        try {
            for (int index = from; index < to; index++) {
                elements[index].addAndGet(add);
            }
        } finally {
            rangeLock.unlock(handle);
        }
    }

    @Override
    public int getRangeSum(int from, int to) {
        KoganRWRange.LockHandle handle = rangeLock.readLock(from, to);
        try {
            int sum = 0;
            for (int index = from; index < to; index++) {
                sum += elements[index].get();
            }
            return sum;
        } finally {
            rangeLock.unlock(handle);
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
        KoganRWRange.LockHandle handle = rangeLock.readLock(index, index + 1);
        try {
            return elements[index].get();
        } finally {
            rangeLock.unlock(handle);
        }
    }

    @Override
    public int set(int index, int value) {
        KoganRWRange.LockHandle handle = rangeLock.writeLock(index, index + 1);
        try {
            return elements[index].getAndSet(value);
        } finally {
            rangeLock.unlock(handle);
        }
    }
}
