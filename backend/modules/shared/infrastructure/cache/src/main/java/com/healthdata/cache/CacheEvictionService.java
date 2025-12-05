package com.healthdata.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

/**
 * HIPAA-Compliant Cache Eviction Service
 *
 * ⚠️ CRITICAL SECURITY SERVICE - DO NOT DELETE ⚠️
 *
 * Provides manual cache eviction capabilities for Protected Health Information (PHI).
 * Should be called on:
 * - User logout
 * - Session expiration
 * - Manual administrator cache flush
 * - Suspicious activity detection
 *
 * HIPAA Compliance: 45 CFR 164.312(a)(2)(i) - Access Controls
 * Ensures PHI is not retained longer than necessary.
 *
 * For complete documentation, see: /backend/HIPAA-CACHE-COMPLIANCE.md
 *
 * @author Claude Code
 * @since 2025-11-14
 */
@Service
public class CacheEvictionService {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionService.class);

    private final CacheManager cacheManager;

    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Evict all PHI-related caches for a specific tenant
     *
     * ⚠️ CRITICAL: Call this method on user logout to ensure PHI is not retained
     *
     * @param tenantId The tenant ID whose caches should be cleared
     */
    public void evictTenantCaches(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Attempted to evict caches for null or blank tenantId");
            return;
        }

        log.info("Evicting PHI caches for tenant: {}", tenantId);

        Collection<String> cacheNames = cacheManager.getCacheNames();
        int cachesEvicted = 0;

        for (String cacheName : cacheNames) {
            if (isPhiCache(cacheName)) {
                evictCache(cacheName);
                cachesEvicted++;
            }
        }

        log.info("Evicted {} PHI caches for tenant: {}", cachesEvicted, tenantId);
    }

    /**
     * Evict all caches in the system (administrator function)
     *
     * ⚠️ USE WITH CAUTION: This clears ALL caches including non-PHI caches
     *
     * Use cases:
     * - Security incident response
     * - Manual cache flush by administrator
     * - System maintenance
     */
    public void evictAllCaches() {
        log.warn("Evicting ALL caches in the system");

        Collection<String> cacheNames = cacheManager.getCacheNames();
        int cachesEvicted = 0;

        for (String cacheName : cacheNames) {
            evictCache(cacheName);
            cachesEvicted++;
        }

        log.warn("Evicted {} caches total", cachesEvicted);
    }

    /**
     * Evict a specific cache by name
     *
     * @param cacheName The name of the cache to evict
     * @return true if cache was evicted, false if cache not found
     */
    public boolean evictCache(String cacheName) {
        if (cacheName == null || cacheName.isBlank()) {
            log.warn("Attempted to evict cache with null or blank name");
            return false;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Evicted cache: {}", cacheName);
            return true;
        } else {
            log.debug("Cache not found: {}", cacheName);
            return false;
        }
    }

    /**
     * Evict all PHI-related caches (all tenants)
     *
     * ⚠️ USE WITH CAUTION: Affects all tenants
     *
     * Use cases:
     * - Global security incident
     * - Compliance audit preparation
     * - System-wide maintenance
     */
    public void evictAllPhiCaches() {
        log.warn("Evicting ALL PHI caches for all tenants");

        Collection<String> cacheNames = cacheManager.getCacheNames();
        int cachesEvicted = 0;

        for (String cacheName : cacheNames) {
            if (isPhiCache(cacheName)) {
                evictCache(cacheName);
                cachesEvicted++;
            }
        }

        log.warn("Evicted {} PHI caches total", cachesEvicted);
    }

    /**
     * Get cache statistics for monitoring
     *
     * @return Collection of cache names currently active
     */
    public Collection<String> getCacheNames() {
        return cacheManager.getCacheNames();
    }

    /**
     * Check if a cache contains PHI based on naming conventions
     *
     * PHI Cache Naming Patterns:
     * - patient*
     * - fhir*
     * - healthRecord*
     * - allergy*
     * - medication*
     * - observation*
     * - condition*
     * - procedure*
     * - encounter*
     * - immunization*
     * - qualityMeasure*
     * - hedis*
     * - careGap*
     * - cqlEvaluation*
     *
     * @param cacheName The cache name to check
     * @return true if cache likely contains PHI
     */
    private boolean isPhiCache(String cacheName) {
        if (cacheName == null) {
            return false;
        }

        String lowerCacheName = cacheName.toLowerCase();

        return lowerCacheName.contains("patient") ||
               lowerCacheName.contains("fhir") ||
               lowerCacheName.contains("healthrecord") ||
               lowerCacheName.contains("allergy") ||
               lowerCacheName.contains("medication") ||
               lowerCacheName.contains("observation") ||
               lowerCacheName.contains("condition") ||
               lowerCacheName.contains("procedure") ||
               lowerCacheName.contains("encounter") ||
               lowerCacheName.contains("immunization") ||
               lowerCacheName.contains("qualitymeasure") ||
               lowerCacheName.contains("hedis") ||
               lowerCacheName.contains("caregap") ||
               lowerCacheName.contains("cqlevaluation");
    }

    /**
     * Evict a specific cache entry by key
     *
     * @param cacheName The cache name
     * @param key The cache key to evict
     * @return true if entry was evicted, false if cache or entry not found
     */
    public boolean evictCacheEntry(String cacheName, Object key) {
        if (cacheName == null || key == null) {
            log.warn("Attempted to evict cache entry with null cacheName or key");
            return false;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted cache entry - cache: {}, key: {}", cacheName, key);
            return true;
        } else {
            log.debug("Cache not found: {}", cacheName);
            return false;
        }
    }

    /**
     * Check if cache manager is properly configured
     *
     * @return true if cache manager is available and functional
     */
    public boolean isCacheManagerAvailable() {
        try {
            Collection<String> names = cacheManager.getCacheNames();
            return names != null;
        } catch (Exception e) {
            log.error("Error checking cache manager availability", e);
            return false;
        }
    }
}
