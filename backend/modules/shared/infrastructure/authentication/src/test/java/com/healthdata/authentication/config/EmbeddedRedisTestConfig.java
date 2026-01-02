package com.healthdata.authentication.config;

import com.redis.testcontainers.RedisContainer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for Redis integration tests using Testcontainers.
 * Automatically starts a Redis container for testing.
 *
 * USAGE:
 * - Activated only in test-redis profile
 * - Automatically starts Redis container using Testcontainers
 * - Container is reused across tests for performance
 * - Container is cleaned up after test suite completes
 *
 * FEATURES:
 * - No manual Redis installation required
 * - Isolated test environment
 * - Automatic cleanup
 * - Reusable container for performance
 * - Works in CI/CD pipelines
 */
@TestConfiguration
@Profile("test-redis")
@Slf4j
public class EmbeddedRedisTestConfig {

    private static final RedisContainer REDIS_CONTAINER = new RedisContainer(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static void ensureContainerStarted() {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new IllegalStateException("Docker is required for Redis Testcontainers tests.");
        }
        if (!REDIS_CONTAINER.isRunning()) {
            log.info("Initializing Redis Testcontainer");
            REDIS_CONTAINER.start();
            log.info("Redis Testcontainer started on {}:{}",
                    REDIS_CONTAINER.getHost(),
                    REDIS_CONTAINER.getFirstMappedPort());
        }
    }

    /**
     * Creates Redis connection factory for tests.
     * Uses Testcontainers-managed Redis instance.
     *
     * @return Redis connection factory configured for testing
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection for tests using Testcontainers");
        ensureContainerStarted();

        // Get connection details from container
        String host = REDIS_CONTAINER.getHost();
        Integer port = REDIS_CONTAINER.getFirstMappedPort();

        log.info("Connecting to Redis container at {}:{}", host, port);

        // Create connection configuration
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setDatabase(0); // Use database 0 for tests (matches Bucket4j config)

        // Create connection factory
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        log.info("Redis connection factory created successfully for tests");

        return factory;
    }

    /**
     * Creates RedisTemplate for test utilities.
     * Used for cleaning up test data between tests.
     *
     * @param connectionFactory Redis connection factory
     * @return Configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> testRedisTemplate(RedisConnectionFactory connectionFactory) {
        ensureContainerStarted();
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();

        log.info("Test RedisTemplate created for cleanup operations");

        return template;
    }

    /**
     * Creates Lettuce Redis client for Bucket4j in tests.
     * Overrides the production bean to ensure it connects to testcontainers Redis on database 0.
     *
     * @return Redis client configured for testcontainers
     */
    @Bean(destroyMethod = "shutdown")
    @Primary
    public RedisClient lettuceRedisClient() {
        ensureContainerStarted();
        String host = REDIS_CONTAINER.getHost();
        Integer port = REDIS_CONTAINER.getFirstMappedPort();

        log.info("Creating Lettuce Redis client for tests: {}:{} (database: 0)", host, port);

        RedisURI redisURI = RedisURI.Builder.redis(host, port)
                .withDatabase(0)  // Use database 0 for tests
                .build();

        return RedisClient.create(redisURI);
    }

    /**
     * Gets the Redis container instance for advanced test scenarios.
     *
     * @return Redis container instance
     */
    public static RedisContainer getRedisContainer() {
        ensureContainerStarted();
        return REDIS_CONTAINER;
    }
}
