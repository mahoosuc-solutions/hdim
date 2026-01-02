package com.healthdata.quality.config;

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
            // Quality measure caches
            new ConcurrentMapCache("qualityReport"),
            new ConcurrentMapCache("populationQualityReport"),
            new ConcurrentMapCache("measureResults"),
            new ConcurrentMapCache("allMeasureResults"),
            // Care gap caches
            new ConcurrentMapCache("careGaps"),
            new ConcurrentMapCache("patientCareGaps"),
            // Health score caches
            new ConcurrentMapCache("healthScores"),
            new ConcurrentMapCache("healthScoreHistory"),
            // Risk assessment caches
            new ConcurrentMapCache("riskAssessments"),
            new ConcurrentMapCache("patientRiskAssessments"),
            // Clinical alerts caches
            new ConcurrentMapCache("clinicalAlerts"),
            new ConcurrentMapCache("activeAlerts"),
            // Custom measures caches
            new ConcurrentMapCache("customMeasures"),
            new ConcurrentMapCache("tenantCustomMeasures"),
            // Patient data caches
            new ConcurrentMapCache("patientNames"),
            new ConcurrentMapCache("patientData"),
            // Generic cache for other uses
            new ConcurrentMapCache("default")
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
