package ru.dksu.semantic;

public class RWInt {
    private final NonFairSemanticLock lock = new NonFairSemanticLock(
            2,
            new int[][]{
                    {1, 1},
                    {1, 0}
            }
    );

    private int value;
    public int set(int v) {
        lock.lock(0);
        try {
            int prev = value;
            value = v;
            return prev;
        } finally {
            lock.unlock(0);
        }
    }

    public int get() {
        lock.lock(1);
        try {
            return value;
        } finally {
            lock.unlock(1);
        }
    }
}
