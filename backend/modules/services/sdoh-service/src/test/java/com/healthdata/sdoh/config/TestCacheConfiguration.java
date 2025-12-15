package com.healthdata.sdoh.config;

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
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            // SDOH Assessments caches
            new ConcurrentMapCache("sdoh-assessments"),
            new ConcurrentMapCache("assessments"),
            new ConcurrentMapCache("patientAssessments"),
            // SDOH Risk Score caches
            new ConcurrentMapCache("sdoh-risk-scores"),
            new ConcurrentMapCache("riskScores"),
            new ConcurrentMapCache("patientRiskScores"),
            // Z-Code caches
            new ConcurrentMapCache("sdoh-diagnoses"),
            new ConcurrentMapCache("zCodes"),
            new ConcurrentMapCache("patientDiagnoses"),
            // Community Resource caches
            new ConcurrentMapCache("sdoh-resources"),
            new ConcurrentMapCache("communityResources"),
            new ConcurrentMapCache("resourcesByLocation"),
            new ConcurrentMapCache("resourcesByCategory"),
            // Referral caches
            new ConcurrentMapCache("sdoh-referrals"),
            new ConcurrentMapCache("referrals"),
            new ConcurrentMapCache("patientReferrals"),
            // Equity caches
            new ConcurrentMapCache("sdoh-equity"),
            new ConcurrentMapCache("equityReports"),
            // General caches
            new ConcurrentMapCache("sdohCache")
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
