package com.healthdata.testfixtures.containers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared TestContainers configuration for HDIM backend integration tests.
 * <p>
 * This configuration provides containerized infrastructure that can be reused
 * across all 28 microservices for consistent integration testing:
 * <ul>
 *   <li><strong>PostgreSQL 16</strong> - Primary database for persistence layer testing</li>
 *   <li><strong>Redis 7</strong> - Caching layer with HIPAA-compliant TTL verification</li>
 *   <li><strong>Kafka 3.6</strong> - Message broker for event-driven testing</li>
 * </ul>
 * <p>
 * <strong>HIPAA Compliance Note:</strong> This configuration enforces test isolation
 * and uses synthetic data patterns. All containers are ephemeral and destroyed after
 * test completion, ensuring no PHI persistence.
 * <p>
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @SpringBootTest
 * @Import(BaseTestContainersConfiguration.class)
 * @Testcontainers
 * class MyIntegrationTest {
 *
 *     @DynamicPropertySource
 *     static void configureProperties(DynamicPropertyRegistry registry) {
 *         BaseTestContainersConfiguration.configurePostgres(registry);
 *         BaseTestContainersConfiguration.configureRedis(registry);
 *         BaseTestContainersConfiguration.configureKafka(registry);
 *     }
 *
 *     @Test
 *     void shouldIntegrateWithDatabase() {
 *         // Test with real PostgreSQL container
 *     }
 * }
 * }</pre>
 * <p>
 * <h2>Container Lifecycle</h2>
 * Containers are started once per JVM and reused across all tests for performance.
 * The containers are automatically stopped when the JVM exits.
 * <p>
 * <h2>Multi-Tenant Testing</h2>
 * All database queries should include tenant filtering. Use the
 * {@link com.healthdata.testfixtures.data.SyntheticDataGenerator} to create
 * test data with proper tenant isolation.
 *
 * @see SyntheticDataGenerator
 * @see GatewayTrustTestHeaders
 * @since 1.0
 */
@TestConfiguration
public class BaseTestContainersConfiguration {

    private static final boolean DOCKER_AVAILABLE = isDockerAvailable();

    /**
     * Delay before stopping containers during JVM shutdown (in milliseconds).
     * This allows Gradle time to write XML test results before connections are invalidated.
     * Without this delay, container shutdown can race with XML result writing, causing
     * "Could not write XML test results" errors even when all tests pass.
     *
     * Increased from 5000ms to 10000ms to handle services with many nested test classes
     * (like consent-service) that generate many XML files.
     */
    private static final long SHUTDOWN_DELAY_MS = 10000;

    private static final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);
    private static volatile boolean shutdownHookRegistered = false;

    /**
     * PostgreSQL container for database integration tests.
     * Uses PostgreSQL 16-alpine for optimal test performance.
     * Database name follows HDIM naming convention.
     */
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("healthdata_test")
            .withUsername("healthdata")
            .withPassword("healthdata_test");

    /**
     * Redis container for caching integration tests.
     * Uses Redis 7-alpine for consistency with production.
     * Supports HIPAA-compliant PHI cache TTL verification tests.
     */
    private static final RedisContainer REDIS_CONTAINER = new RedisContainer(
            DockerImageName.parse("redis:7-alpine"));

    /**
     * Kafka container for messaging integration tests.
     * Uses Confluent Platform 7.6.0 (Kafka 3.6) for production parity.
     * Supports event-driven architecture testing across services.
     */
    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
                    .asCompatibleSubstituteFor("apache/kafka"));

    /**
     * Exposes the PostgreSQL container as a Spring bean.
     *
     * @return the PostgreSQL container instance
     */
    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        ensureStarted(POSTGRES_CONTAINER);
        return POSTGRES_CONTAINER;
    }

    /**
     * Exposes the Redis container as a Spring bean.
     *
     * @return the Redis container instance
     */
    @Bean
    public RedisContainer redisContainer() {
        ensureStarted(REDIS_CONTAINER);
        return REDIS_CONTAINER;
    }

    /**
     * Exposes the Kafka container as a Spring bean.
     *
     * @return the Kafka container instance
     */
    @Bean
    public KafkaContainer kafkaContainer() {
        ensureStarted(KAFKA_CONTAINER);
        return KAFKA_CONTAINER;
    }

    /**
     * Provides a MeterRegistry bean for test metrics collection.
     * <p>
     * SimpleMeterRegistry is an in-memory registry suitable for testing.
     * Services using Micrometer for metrics (e.g., authentication filters,
     * API endpoints) require a MeterRegistry bean in the test context.
     * <p>
     * This bean resolves "No qualifying bean of type 'MeterRegistry'" errors
     * in integration tests that load security configurations with metrics.
     *
     * @return a simple in-memory meter registry for tests
     */
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    // =========================================================================
    // Static Configuration Methods for @DynamicPropertySource
    // =========================================================================

    /**
     * Configures PostgreSQL connection properties from the container.
     * <p>
     * Sets the following Spring properties:
     * <ul>
     *   <li>{@code spring.datasource.url}</li>
     *   <li>{@code spring.datasource.username}</li>
     *   <li>{@code spring.datasource.password}</li>
     *   <li>{@code spring.datasource.driver-class-name}</li>
     *   <li>{@code spring.datasource.hikari.max-lifetime}</li>
     *   <li>{@code spring.datasource.hikari.connection-timeout}</li>
     *   <li>{@code spring.datasource.hikari.minimum-idle}</li>
     *   <li>{@code spring.datasource.hikari.maximum-pool-size}</li>
     *   <li>{@code spring.jpa.database-platform}</li>
     * </ul>
     * <p>
     * <strong>HikariCP Configuration Note:</strong> The max-lifetime is set to 10 minutes
     * (600000ms) which is shorter than the default 30 minutes. This prevents the
     * "maxLifetime is less than the server connection timeout" warning and ensures
     * connections are recycled before the database terminates them.
     *
     * @param registry the dynamic property registry
     */
    public static void configurePostgres(DynamicPropertyRegistry registry) {
        ensureDockerAvailable();
        ensureStarted(POSTGRES_CONTAINER);
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // HikariCP connection pool configuration for tests
        // Set max-lifetime to 10 minutes to prevent "maxLifetime is less than server connection timeout" warnings
        registry.add("spring.datasource.hikari.max-lifetime", () -> "600000");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    /**
     * Configures Redis connection properties from the container.
     * <p>
     * Sets the following Spring properties:
     * <ul>
     *   <li>{@code spring.data.redis.host}</li>
     *   <li>{@code spring.data.redis.port}</li>
     * </ul>
     * <p>
     * <strong>HIPAA Note:</strong> PHI cache TTL should be verified in tests
     * using the Redis container directly. Maximum TTL is 5 minutes.
     *
     * @param registry the dynamic property registry
     */
    public static void configureRedis(DynamicPropertyRegistry registry) {
        ensureDockerAvailable();
        ensureStarted(REDIS_CONTAINER);
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    /**
     * Configures Kafka connection properties from the container.
     * <p>
     * Sets the following Spring properties:
     * <ul>
     *   <li>{@code spring.kafka.bootstrap-servers}</li>
     *   <li>{@code spring.kafka.consumer.bootstrap-servers}</li>
     *   <li>{@code spring.kafka.producer.bootstrap-servers}</li>
     * </ul>
     *
     * @param registry the dynamic property registry
     */
    public static void configureKafka(DynamicPropertyRegistry registry) {
        ensureDockerAvailable();
        ensureStarted(KAFKA_CONTAINER);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
    }

    /**
     * Configures all infrastructure containers (PostgreSQL, Redis, Kafka).
     * Convenience method for tests that need the full stack.
     *
     * @param registry the dynamic property registry
     */
    public static void configureAll(DynamicPropertyRegistry registry) {
        configurePostgres(registry);
        configureRedis(registry);
        configureKafka(registry);
    }

    // =========================================================================
    // Container Access Methods
    // =========================================================================

    /**
     * Gets the PostgreSQL container instance for direct access.
     * Useful for advanced scenarios like executing raw SQL.
     *
     * @return the PostgreSQL container
     */
    public static PostgreSQLContainer<?> getPostgresContainer() {
        ensureDockerAvailable();
        ensureStarted(POSTGRES_CONTAINER);
        return POSTGRES_CONTAINER;
    }

    /**
     * Gets the Redis container instance for direct access.
     * Useful for verifying cache TTL compliance and cache operations.
     *
     * @return the Redis container
     */
    public static RedisContainer getRedisContainer() {
        ensureDockerAvailable();
        ensureStarted(REDIS_CONTAINER);
        return REDIS_CONTAINER;
    }

    /**
     * Gets the Kafka container instance for direct access.
     * Useful for consuming/producing messages in tests.
     *
     * @return the Kafka container
     */
    public static KafkaContainer getKafkaContainer() {
        ensureDockerAvailable();
        ensureStarted(KAFKA_CONTAINER);
        return KAFKA_CONTAINER;
    }

    /**
     * Checks if Docker is available on the test machine.
     *
     * @return true if Docker is available, false otherwise
     */
    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception ex) {
            return false;
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private static void ensureDockerAvailable() {
        if (!DOCKER_AVAILABLE) {
            throw new IllegalStateException(
                    "Docker is required for TestContainers integration tests. " +
                    "Please ensure Docker Desktop is running.");
        }
    }

    @SuppressWarnings("resource")
    private static void ensureStarted(org.testcontainers.containers.GenericContainer<?> container) {
        if (!container.isRunning()) {
            container.start();
            registerShutdownHookOnce();
        }
    }

    /**
     * Registers a shutdown hook with delayed cleanup to prevent race conditions
     * with Gradle's XML test result writing.
     * <p>
     * This is called once when the first container starts and ensures all containers
     * stay alive long enough for Gradle to finish writing test results.
     */
    private static synchronized void registerShutdownHookOnce() {
        if (!shutdownHookRegistered) {
            shutdownHookRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (shutdownInitiated.compareAndSet(false, true)) {
                    try {
                        // Wait for Gradle to finish writing XML results
                        Thread.sleep(SHUTDOWN_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // Containers will be stopped by Testcontainers/Ryuk after delay
                }
            }, "BaseTestContainersConfiguration-Shutdown"));
        }
    }

    /**
     * Check if shutdown has been initiated.
     * Useful for tests that need to know if cleanup is in progress.
     */
    public static boolean isShutdownInitiated() {
        return shutdownInitiated.get();
    }
}
