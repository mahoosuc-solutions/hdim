package com.healthdata.patientevent.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration for Patient Event Service
 *
 * CQRS read model services don't require distributed caching since they're
 * designed for fast reads from denormalized projections. This configuration
 * provides a no-op cache manager to satisfy Spring dependencies without
 * pulling in Redis.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Use in-memory cache for satisfying Spring dependencies
        // This is NOT used for actual caching in projection read models
        return new ConcurrentMapCacheManager();
    }
}
