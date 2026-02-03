package com.healthdata.test.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton PostgreSQL container for integration tests.
 *
 * Provides a shared PostgreSQL instance across all test classes to improve performance.
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
 * static void configurePostgres(DynamicPropertyRegistry registry) {
 *     registry.add("spring.datasource.url", SharedPostgresContainer::getJdbcUrl);
 *     registry.add("spring.datasource.username", SharedPostgresContainer::getUsername);
 *     registry.add("spring.datasource.password", SharedPostgresContainer::getPassword);
 * }
 * }
 * </pre>
 */
public class SharedPostgresContainer {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final String DATABASE_NAME = "testdb";
    private static final String USERNAME = "test";
    private static final String PASSWORD = "test";

    /**
     * Delay before stopping the container during JVM shutdown.
     * This allows Gradle time to write XML test results before connections are invalidated.
     */
    private static final long SHUTDOWN_DELAY_MS = 5000;

    private static PostgreSQLContainer<?> instance;
    private static final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

    private SharedPostgresContainer() {
        // Prevent instantiation
    }

    /**
     * Get the singleton PostgreSQL container instance.
     * Starts the container on first access.
     */
    public static PostgreSQLContainer<?> getInstance() {
        if (instance == null) {
            synchronized (SharedPostgresContainer.class) {
                if (instance == null) {
                    instance = new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
                        .withDatabaseName(DATABASE_NAME)
                        .withUsername(USERNAME)
                        .withPassword(PASSWORD)
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
                    }, "SharedPostgresContainer-Shutdown"));
                }
            }
        }
        return instance;
    }

    /**
     * Get JDBC URL.
     */
    public static String getJdbcUrl() {
        return getInstance().getJdbcUrl();
    }

    /**
     * Get database username.
     */
    public static String getUsername() {
        return getInstance().getUsername();
    }

    /**
     * Get database password.
     */
    public static String getPassword() {
        return getInstance().getPassword();
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
