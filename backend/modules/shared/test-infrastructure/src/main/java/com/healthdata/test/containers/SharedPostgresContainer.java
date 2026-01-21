package com.healthdata.test.containers;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Singleton PostgreSQL container for integration tests.
 * 
 * Provides a shared PostgreSQL instance across all test classes to improve performance.
 * The container starts once and is reused for all tests in the JVM.
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
    
    private static PostgreSQLContainer<?> instance;
    
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
                    
                    // Register shutdown hook
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (instance != null && instance.isRunning()) {
                            instance.stop();
                        }
                    }));
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
}

