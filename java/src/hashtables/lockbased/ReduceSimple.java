package hashtables.lockbased;

import contention.abstractions.CompositionalMap;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ReduceSimple extends NonCollaborativeQueueHashMap<Integer, Integer> {
    @Override
    public int size() {
        return this.reduce(0, Integer::sum);
    }
}
