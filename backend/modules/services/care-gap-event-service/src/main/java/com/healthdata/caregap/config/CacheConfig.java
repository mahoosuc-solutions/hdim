package com.healthdata.caregap.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback cache configuration for local/test environments.
 * Uses NoOpCacheManager to prevent inadvertent PHI retention (HIPAA §164.312(e)(2)).
 */
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
