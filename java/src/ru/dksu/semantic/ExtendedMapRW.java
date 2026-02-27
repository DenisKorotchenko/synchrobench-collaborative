package ru.dksu.semantic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class ExtendedMapRW implements ExtendedMap {
    private ConcurrentHashMap<Integer, Integer> _map = new ConcurrentHashMap<>();
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();

    @Override
    public Integer sum() {
        rwLock.writeLock().lock();
        try {
            Integer sum = 0;
            for (var el: _map.values()) {
                if (el != null) {
                    sum += el;
                }
            }
            return sum;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Integer cap(Integer maxValue) {
        rwLock.writeLock().lock();
        try {
            final int[] x = {0};
            _map.replaceAll((key, value) -> {
                if (maxValue > value) {
                    x[0]++;
                    return maxValue;
                }
                return value;
            });
            return x[0];
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        rwLock.readLock().lock();
        try {
            return _map.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        rwLock.readLock().lock();
        try {
            return _map.isEmpty();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        rwLock.readLock().lock();
        try {
            return _map.containsKey(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        rwLock.readLock().lock();
        try {
            return _map.containsValue(value);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Integer get(Object key) {
        rwLock.readLock().lock();
        try {
            return _map.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Integer put(Integer key, Integer value) {
        rwLock.readLock().lock();
        try {
            return _map.put(key, value);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Integer remove(Object key) {
        rwLock.readLock().lock();
        try {
            return _map.remove(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends Integer> m) {
        rwLock.readLock().lock();
        try {
            _map.putAll(m);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        rwLock.readLock().lock();
        try {
            _map.clear();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Set<Integer> keySet() {
        rwLock.readLock().lock();
        try {
            return _map.keySet();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Collection<Integer> values() {
        rwLock.readLock().lock();
        try {
            return _map.values();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Set<Entry<Integer, Integer>> entrySet() {
        rwLock.readLock().lock();
        try {
            return _map.entrySet();
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
