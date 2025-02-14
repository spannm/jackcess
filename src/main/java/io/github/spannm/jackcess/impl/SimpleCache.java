package io.github.spannm.jackcess.impl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple LRU cache implementation which keeps at most the configured maximum number of elements.
 *
 * @author James Ahlborn
 */
public class SimpleCache<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 20180313L;

    private final int         _maxSize;

    public SimpleCache(int maxSize) {
        super(16, 0.75f, true);
        _maxSize = maxSize;
    }

    protected int getMaxSize() {
        return _maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> e) {
        return size() > _maxSize;
    }
}
