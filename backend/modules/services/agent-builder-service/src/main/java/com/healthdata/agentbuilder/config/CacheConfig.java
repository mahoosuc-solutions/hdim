package com.healthdata.agentbuilder.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for Agent Builder Service.
 *
 * Cache strategies:
 * - prompt-templates: Long TTL (30 min) - templates rarely change
 * - active-agents: Medium TTL (5 min) - balance freshness vs performance
 * - agent-config: Short TTL (2 min) - single agent lookups
 * - available-tools: Long TTL (1 hour) - tool definitions are stable
 * - providers: Long TTL (1 hour) - provider list rarely changes
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_PROMPT_TEMPLATES = "prompt-templates";
    public static final String CACHE_ACTIVE_AGENTS = "active-agents";
    public static final String CACHE_AGENT_CONFIG = "agent-config";
    public static final String CACHE_AVAILABLE_TOOLS = "available-tools";
    public static final String CACHE_PROVIDERS = "providers";
    public static final String CACHE_SYSTEM_TEMPLATES = "system-templates";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Prompt templates - rarely change, cache for 30 minutes
        cacheConfigurations.put(CACHE_PROMPT_TEMPLATES, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // System templates - global, rarely change, cache for 1 hour
        cacheConfigurations.put(CACHE_SYSTEM_TEMPLATES, defaultConfig.entryTtl(Duration.ofHours(1)));

        // Active agents list - moderate freshness needed
        cacheConfigurations.put(CACHE_ACTIVE_AGENTS, defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Individual agent config - shorter TTL for more freshness
        cacheConfigurations.put(CACHE_AGENT_CONFIG, defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // Available tools - stable, cache for 1 hour
        cacheConfigurations.put(CACHE_AVAILABLE_TOOLS, defaultConfig.entryTtl(Duration.ofHours(1)));

        // LLM providers - very stable, cache for 1 hour
        cacheConfigurations.put(CACHE_PROVIDERS, defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }
}
