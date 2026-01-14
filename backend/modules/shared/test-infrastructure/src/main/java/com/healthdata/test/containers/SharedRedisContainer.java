package com.healthdata.test.containers;

import com.redis.testcontainers.RedisContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Singleton Redis container for integration tests.
 * 
 * Provides a shared Redis instance across all test classes to improve performance.
 * The container starts once and is reused for all tests in the JVM.
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
    private static RedisContainer instance;
    
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
}

