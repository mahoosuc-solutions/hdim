package com.healthdata.fhir.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

/**
 * Base Testcontainers configuration for integration tests.
 * <p>
 * This configuration provides containerized infrastructure for integration tests:
 * <ul>
 *   <li>PostgreSQL database for persistence layer testing</li>
 *   <li>Redis for caching and session management</li>
 *   <li>Kafka for message broker testing</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 * {@code
 * @SpringBootTest
 * @Import(TestContainersConfiguration.class)
 * @Testcontainers
 * class MyIntegrationTest {
 *
 *     @DynamicPropertySource
 *     static void configureProperties(DynamicPropertyRegistry registry) {
 *         TestContainersConfiguration.configurePostgres(registry);
 *         // Optionally configure Redis and Kafka
 *     }
 *
 *     // Your tests
 * }
 * }
 * </pre>
 * <p>
 * The containers are started once and reused across all tests in the same JVM
 * for optimal performance. Use the static helper methods to configure Spring
 * properties in your test classes.
 *
 * @see org.testcontainers.junit.jupiter.Testcontainers
 * @see org.springframework.test.context.DynamicPropertySource
 */
@TestConfiguration
public class TestContainersConfiguration {

    /**
     * PostgreSQL container for database integration tests.
     * Uses PostgreSQL 16 with optimized settings for test performance.
     * The container is started once and reused across all tests.
     */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("healthdata_fhir")
            .withUsername("healthdata")
            .withPassword("healthdata_test")
            .withReuse(true);

    /**
     * Redis container for caching and session management tests.
     * Uses Redis 7 (latest stable version).
     * The container is started once and reused across all tests.
     */
    @Container
    private static final RedisContainer REDIS_CONTAINER = new RedisContainer(
            DockerImageName.parse("redis:7-alpine"))
            .withReuse(true);

    /**
     * Kafka container for message broker integration tests.
     * Uses Confluent Platform 7.6.0 which includes Kafka 3.6.
     * The container is started once and reused across all tests.
     */
    @Container
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
                    .asCompatibleSubstituteFor("apache/kafka"))
            .withReuse(true);

    static {
        // Start PostgreSQL container eagerly - required for most tests
        POSTGRES_CONTAINER.start();
        // Redis and Kafka are started lazily when first accessed
    }

    /**
     * Exposes the PostgreSQL container as a Spring bean.
     * This allows autowiring the container in test classes if needed.
     *
     * @return the PostgreSQL container instance
     */
    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES_CONTAINER;
    }

    /**
     * Exposes the Redis container as a Spring bean.
     * This allows autowiring the container in test classes if needed.
     * The container is started lazily on first access.
     *
     * @return the Redis container instance
     */
    @Bean
    public RedisContainer redisContainer() {
        if (!REDIS_CONTAINER.isRunning()) {
            REDIS_CONTAINER.start();
        }
        return REDIS_CONTAINER;
    }

    /**
     * Exposes the Kafka container as a Spring bean.
     * This allows autowiring the container in test classes if needed.
     * The container is started lazily on first access.
     *
     * @return the Kafka container instance
     */
    @Bean
    public KafkaContainer kafkaContainer() {
        if (!KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.start();
        }
        return KAFKA_CONTAINER;
    }

    /**
     * Configures PostgreSQL connection properties from the PostgreSQL container.
     * Call this method from your test class's {@code @DynamicPropertySource} method.
     * <p>
     * Example:
     * <pre>
     * {@code
     * @DynamicPropertySource
     * static void configureProperties(DynamicPropertyRegistry registry) {
     *     TestContainersConfiguration.configurePostgres(registry);
     * }
     * }
     * </pre>
     *
     * @param registry the dynamic property registry
     */
    public static void configurePostgres(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    /**
     * Configures Redis connection properties from the Redis container.
     * Call this method from your test class's {@code @DynamicPropertySource} method.
     * The container is started if not already running.
     * <p>
     * Example:
     * <pre>
     * {@code
     * @DynamicPropertySource
     * static void configureProperties(DynamicPropertyRegistry registry) {
     *     TestContainersConfiguration.configureRedis(registry);
     * }
     * }
     * </pre>
     *
     * @param registry the dynamic property registry
     */
    public static void configureRedis(org.springframework.test.context.DynamicPropertyRegistry registry) {
        if (!REDIS_CONTAINER.isRunning()) {
            REDIS_CONTAINER.start();
        }
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    /**
     * Configures Kafka connection properties from the Kafka container.
     * Call this method from your test class's {@code @DynamicPropertySource} method.
     * The container is started if not already running.
     * <p>
     * Example:
     * <pre>
     * {@code
     * @DynamicPropertySource
     * static void configureProperties(DynamicPropertyRegistry registry) {
     *     TestContainersConfiguration.configureKafka(registry);
     * }
     * }
     * </pre>
     *
     * @param registry the dynamic property registry
     */
    public static void configureKafka(org.springframework.test.context.DynamicPropertyRegistry registry) {
        if (!KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.start();
        }
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }

    /**
     * Gets the PostgreSQL container instance.
     * Useful for advanced test scenarios that need direct container access.
     *
     * @return the PostgreSQL container
     */
    public static PostgreSQLContainer<?> getPostgresContainer() {
        return POSTGRES_CONTAINER;
    }

    /**
     * Gets the Redis container instance.
     * Useful for advanced test scenarios that need direct container access.
     *
     * @return the Redis container
     */
    public static RedisContainer getRedisContainer() {
        return REDIS_CONTAINER;
    }

    /**
     * Gets the Kafka container instance.
     * Useful for advanced test scenarios that need direct container access.
     *
     * @return the Kafka container
     */
    public static KafkaContainer getKafkaContainer() {
        return KAFKA_CONTAINER;
    }
}
