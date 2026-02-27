package ru.dksu.semantic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExtendedMapSL implements ExtendedMap {
    private ConcurrentHashMap<Integer, Integer> _map = new ConcurrentHashMap<>();
    private SemanticLock semanticLock = new SemanticLock(
            3,
            new int[][] {
                    {0, 1, 1},
                    {1, 0, 1},
                    {1, 1, 1}
            }
    );

    @Override
    public Integer sum() {
        semanticLock.lock(1);
        try {
            Integer sum = 0;
            for (var el: _map.values()) {
                if (el != null) {
                    sum += el;
                }
            }
            return sum;
        } finally {
            semanticLock.unlock(1);
        }
    }

    @Override
    public Integer cap(Integer maxValue) {
        semanticLock.lock(2);
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
            semanticLock.unlock(2);
        }
    }

    @Override
    public int size() {
        semanticLock.lock(0);
        try {
            return _map.size();
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public boolean isEmpty() {
        semanticLock.lock(0);
        try {
            return _map.isEmpty();
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        semanticLock.lock(0);
        try {
            return _map.containsKey(key);
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        semanticLock.lock(0);
        try {
            return _map.containsValue(value);
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public Integer get(Object key) {
        semanticLock.lock(0);
        try {
            return _map.get(key);
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public Integer put(Integer key, Integer value) {
        semanticLock.lock(0);
        try {
            return _map.put(key, value);
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public Integer remove(Object key) {
        semanticLock.lock(0);
        try {
            return _map.remove(key);
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> m) {
        semanticLock.lock(0);
        try {
            _map.putAll(m);
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public void clear() {
        semanticLock.lock(0);
        try {
            _map.clear();
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public Set<Integer> keySet() {
        semanticLock.lock(0);
        try {
            return _map.keySet();
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public Collection<Integer> values() {
        semanticLock.lock(0);
        try {
            return _map.values();
        } finally {
            semanticLock.unlock(0);
        }
    }

    @Override
    public Set<Entry<Integer, Integer>> entrySet() {
        semanticLock.lock(0);
        try {
            return _map.entrySet();
        } finally {
            semanticLock.unlock(0);
        }
    }
}
