package com.healthdata.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Connection Configuration for Rate Limiting
 *
 * <p>Creates RedisConnectionFactory bean explicitly because spring.cache.type=none
 * disables Redis auto-configuration. This allows rate limiting to use Redis
 * without enabling Spring Cache (which could cause circular dependencies).
 *
 * <p>The gateway service uses Redis for:
 * <ul>
 *   <li>Rate limiting (RateLimitService) - prevents brute force attacks</li>
 *   <li>Session token blacklisting - logout invalidation</li>
 * </ul>
 *
 * <p>Spring Cache is disabled to avoid circular dependencies during bean initialization.
 * This configuration provides the Redis connection infrastructure without enabling caching.
 *
 * @see com.healthdata.gateway.config.RateLimitConfiguration
 * @see com.healthdata.gateway.service.RateLimitService
 */
@Configuration
public class RedisConfiguration {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Create RedisConnectionFactory bean for rate limiting and token blacklisting
     *
     * <p>Uses Lettuce (default Spring Boot Redis client) for:
     * <ul>
     *   <li>Connection pooling - efficient resource usage</li>
     *   <li>Non-blocking I/O - better performance under load</li>
     *   <li>Automatic reconnection - resilience to Redis restarts</li>
     * </ul>
     *
     * @return configured Lettuce connection factory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(
            redisHost,
            redisPort
        );

        // Lettuce is the default Redis client in Spring Boot
        // Provides connection pooling and automatic reconnection
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);

        // Initialize the factory to establish the connection pool
        factory.afterPropertiesSet();

        return factory;
    }

    /**
     * Create general-purpose RedisTemplate for token revocation
     *
     * <p>Used by TokenRevocationServiceImpl to blacklist JWT tokens after logout.
     * Tokens are stored as String keys with Object values (token metadata).
     *
     * @param redisConnectionFactory connection factory created above
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Use String serializer for keys (token IDs)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serializer for values (token metadata)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
