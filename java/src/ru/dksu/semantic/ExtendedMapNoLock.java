package ru.dksu.semantic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExtendedMapNoLock implements ExtendedMap {
    private ConcurrentHashMap<Integer, Integer> _map = new ConcurrentHashMap<>();

    @Override
    public Integer sum() {
        try {
            Integer sum = 0;
            for (var el: _map.values()) {
                if (el != null) {
                    sum += el;
                }
            }
            return sum;
        } finally {

        }
    }

    @Override
    public Integer cap(Integer maxValue) {
        final int[] x = {0};
        _map.replaceAll((key, value) -> {
            if (maxValue < value) {
                x[0]++;
                return maxValue;
            }
            return value;
        });
        return x[0];
    }


    @Override
    public int size() {
        try {
            return _map.size();
        } finally {
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            return _map.isEmpty();
        } finally {
        }
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            return _map.containsKey(key);
        } finally {
        }
    }

    @Override
    public boolean containsValue(Object value) {
        try {
            return _map.containsValue(value);
        } finally {
        }
    }

    @Override
    public Integer get(Object key) {
        try {
            return _map.get(key);
        } finally {
        }
    }

    @Override
    public Integer put(Integer key, Integer value) {
        try {
            return _map.put(key, value);
        } finally {
        }
    }

    @Override
    public Integer remove(Object key) {
        try {
            return _map.remove(key);
        } finally {
        }
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> m) {
        try {
            _map.putAll(m);
        } finally {
        }
    }

    @Override
    public void clear() {
        try {
            _map.clear();
        } finally {
        }
    }

    @Override
    public Set<Integer> keySet() {
        try {
            return _map.keySet();
        } finally {
        }
    }

    @Override
    public Collection<Integer> values() {
        try {
            return _map.values();
        } finally {
        }
    }

    @Override
    public Set<Entry<Integer, Integer>> entrySet() {
        try {
            return _map.entrySet();
        } finally {
        }
    }
}
