package com.healthdata.caregapevent.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration for Care Gap Event Service
 *
 * CQRS read model services don't require distributed caching since they're
 * designed for fast reads from denormalized projections. Uses NoOpCacheManager
 * to satisfy Spring @Cacheable dependencies while storing nothing in memory,
 * preventing inadvertent PHI retention (HIPAA §164.312(e)(2)).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
