package com.healthdata.test.containers;

import com.redis.testcontainers.RedisContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton Redis container for integration tests.
 *
 * Provides a shared Redis instance across all test classes to improve performance.
 * The container starts once and is reused for all tests in the JVM.
 *
 * <h2>Lifecycle Management</h2>
 * The container uses a delayed shutdown hook to prevent race conditions with Gradle's
 * XML test result writing. When tests complete, Gradle needs time to write XML results
 * before containers shut down. The shutdown hook introduces a small delay to ensure
 * XML files are fully written before the container stops.
 *
 * Usage:
 * <pre>
 * {@code
 * @DynamicPropertySource
 * static void configureRedis(DynamicPropertyRegistry registry) {
 *     registry.add("spring.data.redis.host", SharedRedisContainer::getHost);
 *     registry.add("spring.data.redis.port", SharedRedisContainer::getPort);
 * }
 * }
 * </pre>
 */
public class SharedRedisContainer {

    private static final String REDIS_IMAGE = "redis:7-alpine";

    /**
     * Delay before stopping the container during JVM shutdown.
     * This allows Gradle time to write XML test results before connections are invalidated.
     */
    private static final long SHUTDOWN_DELAY_MS = 5000;

    private static RedisContainer instance;
    private static final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

    private SharedRedisContainer() {
        // Prevent instantiation
    }

    /**
     * Get the singleton Redis container instance.
     * Starts the container on first access.
     */
    public static RedisContainer getInstance() {
        if (instance == null) {
            synchronized (SharedRedisContainer.class) {
                if (instance == null) {
                    instance = new RedisContainer(DockerImageName.parse(REDIS_IMAGE))
                        .withStartupTimeout(Duration.ofMinutes(2))
                        .withReuse(true);

                    instance.start();

                    // Register shutdown hook with delay to allow XML result writing
                    // The delay prevents race condition where container stops before
                    // Gradle finishes writing XML test results
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (shutdownInitiated.compareAndSet(false, true)) {
                            try {
                                // Wait for Gradle to finish writing XML results
                                Thread.sleep(SHUTDOWN_DELAY_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                if (instance != null && instance.isRunning()) {
                                    instance.stop();
                                }
                            }
                        }
                    }, "SharedRedisContainer-Shutdown"));
                }
            }
        }
        return instance;
    }

    /**
     * Get Redis host.
     */
    public static String getHost() {
        return getInstance().getHost();
    }

    /**
     * Get Redis port.
     */
    public static Integer getPort() {
        return getInstance().getFirstMappedPort();
    }

    /**
     * Get Redis connection string.
     */
    public static String getConnectionString() {
        return getInstance().getRedisURI();
    }

    /**
     * Check if container is running.
     */
    public static boolean isRunning() {
        return instance != null && instance.isRunning();
    }

    /**
     * Check if shutdown has been initiated.
     * Useful for tests that need to know if cleanup is in progress.
     */
    public static boolean isShutdownInitiated() {
        return shutdownInitiated.get();
    }
}
