package com.healthdata.enrichment.config;

import java.util.Arrays;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that uses in-memory caching to avoid Redis connection issues in tests.
 * Uses SimpleCacheManager with ConcurrentMapCache and provides a CacheErrorHandler that ignores all cache errors.
 */
@TestConfiguration
@EnableCaching
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
public class TestCacheConfiguration implements CachingConfigurer {

    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        // Use SimpleCacheManager with in-memory caches for testing
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            // Data enrichment specific caches
            new ConcurrentMapCache("enrichment-codes"),
            new ConcurrentMapCache("enrichment-validations"),
            new ConcurrentMapCache("enrichment-suggestions"),
            new ConcurrentMapCache("enrichment-extractions"),
            new ConcurrentMapCache("enrichment-quality"),
            new ConcurrentMapCache("enrichment-completeness"),
            // ICD-10 caches
            new ConcurrentMapCache("icd10-codes"),
            new ConcurrentMapCache("icd10-validations"),
            // SNOMED caches
            new ConcurrentMapCache("snomed-codes"),
            new ConcurrentMapCache("snomed-validations"),
            // CPT caches
            new ConcurrentMapCache("cpt-codes"),
            new ConcurrentMapCache("cpt-validations"),
            // LOINC caches
            new ConcurrentMapCache("loinc-codes"),
            new ConcurrentMapCache("loinc-validations"),
            // Medical entity recognition caches
            new ConcurrentMapCache("medical-entities"),
            new ConcurrentMapCache("clinical-notes"),
            // Code hierarchy caches
            new ConcurrentMapCache("code-hierarchy"),
            new ConcurrentMapCache("code-relationships"),
            // General caches
            new ConcurrentMapCache("data-quality"),
            new ConcurrentMapCache("data-completeness")
        ));
        return cacheManager;
    }

    @Override
    public CacheResolver cacheResolver() {
        return null; // Use default
    }

    @Override
    public KeyGenerator keyGenerator() {
        return null; // Use default
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        // Return an error handler that silently ignores all cache errors
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Ignore error
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                // Ignore error
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // Ignore error
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                // Ignore error
            }
        };
    }
}
