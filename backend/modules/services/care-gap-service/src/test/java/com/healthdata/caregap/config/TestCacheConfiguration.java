package com.healthdata.caregap.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test Cache Configuration for Care Gap Service Integration Tests
 *
 * Provides an in-memory cache implementation for testing:
 * - Uses ConcurrentMapCacheManager for fast, in-memory caching
 * - No Redis dependency required for tests
 * - Caches are cleared between tests via @Transactional rollback
 */
@TestConfiguration
public class TestCacheConfiguration {

    @Bean
    @Primary
    public CacheManager testCacheManager() {
        return new ConcurrentMapCacheManager(
                // CareGapIdentificationService caches
                "patientCareGaps",
                // CareGapReportService caches
                "careGapSummary",
                "populationGapReport"
        );
    }
}
