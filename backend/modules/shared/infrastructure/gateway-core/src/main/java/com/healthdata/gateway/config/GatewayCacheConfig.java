package com.healthdata.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provide a fallback CacheManager so CacheEvictionService can run even when
 * caching is disabled for the gateway services.
 */
@Configuration
public class GatewayCacheConfig {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
