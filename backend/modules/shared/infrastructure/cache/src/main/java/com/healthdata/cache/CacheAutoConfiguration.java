package com.healthdata.cache;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Auto-configuration for Redis caching with sensible defaults for the platform.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisConnectionFactory redisConnectionFactory(CacheProperties properties) {
        CacheProperties.Redis redis = properties.getRedis();
        RedisStandaloneConfiguration standaloneConfiguration =
                new RedisStandaloneConfiguration(redis.getHost(), redis.getPort());
        if (redis.getPassword() != null && !redis.getPassword().isBlank()) {
            standaloneConfiguration.setPassword(RedisPassword.of(redis.getPassword()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(redis.getTimeout());

        if (redis.isSsl()) {
            clientConfigBuilder.useSsl();
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfiguration, clientConfigBuilder.build());
        factory.setValidateConnection(false);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisCacheObjectMapper")
    public ObjectMapper redisCacheObjectMapper() {
        // Create a dedicated ObjectMapper for cache serialization
        // Avoid injecting ObjectProvider<ObjectMapper> to prevent circular reference
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisCacheConfiguration")
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties properties) {
        // Create ObjectMapper directly to avoid circular dependency
        ObjectMapper mapper = redisCacheObjectMapper();

        RedisSerializationContext.SerializationPair<Object> valueSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(mapper));

        return createConfiguration(properties.getRedis().getDefaultTtl(), properties.getRedis().isCacheNulls(), valueSerializer);
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            RedisCacheConfiguration redisCacheConfiguration,
            CacheProperties properties) {

        Map<String, RedisCacheConfiguration> configuredCaches = new LinkedHashMap<>();
        RedisSerializationContext.SerializationPair<?> serializationPair = redisCacheConfiguration.getValueSerializationPair();

        properties.getCaches().forEach((cacheName, spec) -> {
            Duration ttl = spec.getTtl() != null ? spec.getTtl() : properties.getRedis().getDefaultTtl();
            boolean cacheNulls = spec.getCacheNulls() != null ? spec.getCacheNulls() : properties.getRedis().isCacheNulls();
            configuredCaches.put(cacheName, createConfiguration(ttl, cacheNulls,
                    cast(serializationPair)));
        });

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration);

        if (!configuredCaches.isEmpty()) {
            builder.withInitialCacheConfigurations(configuredCaches);
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private RedisSerializationContext.SerializationPair<Object> cast(
            RedisSerializationContext.SerializationPair<?> serializationPair) {
        return (RedisSerializationContext.SerializationPair<Object>) serializationPair;
    }

    private RedisCacheConfiguration createConfiguration(Duration ttl, boolean cacheNulls,
                                                        RedisSerializationContext.SerializationPair<Object> serializer) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(serializer)
                .entryTtl(ttl);

        if (!cacheNulls) {
            configuration = configuration.disableCachingNullValues();
        }

        return configuration;
    }
}
