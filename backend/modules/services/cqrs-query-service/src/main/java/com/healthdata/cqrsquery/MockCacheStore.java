package com.healthdata.cqrsquery;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory cache store for CQRS read model.
 * Placeholder — production would use Redis via Spring Cache.
 */
public class MockCacheStore {
    private final Map<String, Object> cache = new HashMap<>();
    private final Map<String, Long> ttls = new HashMap<>();
    private int cacheHitCount = 0;

    public void put(String key, Object value, long ttlSeconds) {
        cache.put(key, value);
        ttls.put(key, ttlSeconds);
        cacheHitCount++;
    }

    public Object get(String key) {
        if (cache.containsKey(key)) {
            cacheHitCount++;
            return cache.get(key);
        }
        return null;
    }

    public long getTTL(String key) { return ttls.getOrDefault(key, 0L); }
    public int getCacheHitCount() { return cacheHitCount; }
}
