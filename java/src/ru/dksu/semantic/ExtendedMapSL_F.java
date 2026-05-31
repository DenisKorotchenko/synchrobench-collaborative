package ru.dksu.semantic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExtendedMapSL_F implements ExtendedMap {
    private ConcurrentHashMap<Integer, Integer> _map = new ConcurrentHashMap<>();
    private SemanticLockAtomicCounters semanticLock = new SemanticLockAtomicCounters(
            3,
            new int[][] {
                    {0, 1, 1},
                    {1, 0, 0},
                    {1, 0, 1}
            },
            true
    );

    @Override
    public Integer sum() {
        int r = semanticLock.lock(1);
        try {
            final Integer[] sum = {0};
            _map.keys().asIterator().forEachRemaining( el ->
                    sum[0] += el
            );
            return sum[0];
        } finally {
            semanticLock.unlock(1, r);
        }
    }

    @Override
    public Integer cap(Integer maxValue) {
        int r = semanticLock.lock(2);
        try {
            final int[] x = {0};
            _map.replaceAll((key, value) -> {
                if (maxValue < value) {
                    x[0]++;
                    return maxValue;
                }
                return value;
            });
            return x[0];
        } finally {
            semanticLock.unlock(2, r);
        }
    }

    @Override
    public int size() {
        int r = semanticLock.lock(0);
        try {
            return _map.size();
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public boolean isEmpty() {
        int r = semanticLock.lock(0);
        try {
            return _map.isEmpty();
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        int r = semanticLock.lock(0);
        try {
            return _map.containsKey(key);
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        int r = semanticLock.lock(0);
        try {
            return _map.containsValue(value);
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public Integer get(Object key) {
        int r = semanticLock.lock(0);
        try {
            return _map.get(key);
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public Integer put(Integer key, Integer value) {
        int r = semanticLock.lock(0);
        try {
            return _map.put(key, value);
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public Integer remove(Object key) {
        int r = semanticLock.lock(0);
        try {
            return _map.remove(key);
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> m) {
        int r = semanticLock.lock(0);
        try {
            _map.putAll(m);
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public void clear() {
        int r = semanticLock.lock(0);
        try {
            _map.clear();
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public Set<Integer> keySet() {
        int r = semanticLock.lock(0);
        try {
            return _map.keySet();
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public Collection<Integer> values() {
        int r = semanticLock.lock(0);
        try {
            return _map.values();
        } finally {
            semanticLock.unlock(0, r);
        }
    }

    @Override
    public Set<Entry<Integer, Integer>> entrySet() {
        int r = semanticLock.lock(0);
        try {
            return _map.entrySet();
        } finally {
            semanticLock.unlock(0, r);
        }
    }
}
