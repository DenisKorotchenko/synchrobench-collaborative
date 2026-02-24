package ru.dksu.semantic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExtendedMapSL implements ExtendedMap {
    private ConcurrentHashMap<Integer, Integer> _map = new ConcurrentHashMap<>();
    private SemanticLock semanticLock = new SemanticLock(
            2,
            new int[][] {
                    {0, 1},
                    {1, 0}
            }
    );

    @Override
    public Integer max() {
        semanticLock.lock(1);
        try {
            Integer max = null;
            for (var el: _map.values()) {
                if (max == null || el > max) {
                    max = el;
                }
            }
            return max;
        } finally {
            semanticLock.unlock(1);
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
