package com.healthdata.gateway.config;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Redis Test Configuration using Testcontainers.
 *
 * Provides a real Redis instance for integration tests, ensuring:
 * - Rate limiting behavior matches production
 * - Distributed bucket state is correctly managed
 * - Cache eviction policies work as expected
 *
 * The Redis container is shared across tests in the same JVM for performance.
 */
@TestConfiguration
public class RedisTestConfig {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    /**
     * Shared Redis container for all tests.
     * Reusable across test classes for faster execution.
     */
    @Container
    public static final RedisContainer REDIS_CONTAINER;

    static {
        RedisContainer container = new RedisContainer(REDIS_IMAGE)
                .withExposedPorts(6379);

        boolean reuseEnabled = Boolean.parseBoolean(
            TestcontainersConfiguration.getInstance()
                .getProperties()
                .getProperty("testcontainers.reuse.enable", "false"));

        if (reuseEnabled) {
            // Enable reuse only when the host explicitly opts in via testcontainers configuration.
            container.withReuse(true);
        }

        REDIS_CONTAINER = container;
        REDIS_CONTAINER.start();
    }

    /**
     * Redis connection factory configured to use the Testcontainers Redis instance.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(REDIS_CONTAINER.getHost());
        config.setPort(REDIS_CONTAINER.getFirstMappedPort());
        config.setDatabase(15); // Test database to avoid conflicts

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Redis template for test operations like flushing data between tests.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
