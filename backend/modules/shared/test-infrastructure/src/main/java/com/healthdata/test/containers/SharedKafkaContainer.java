package com.healthdata.test.containers;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Singleton Kafka container for integration tests.
 * 
 * Provides a shared Kafka instance across all test classes to improve performance.
 * The container starts once and is reused for all tests in the JVM.
 * 
 * Usage:
 * <pre>
 * {@code
 * @DynamicPropertySource
 * static void configureKafka(DynamicPropertyRegistry registry) {
 *     registry.add("spring.kafka.bootstrap-servers", 
 *         SharedKafkaContainer::getBootstrapServers);
 * }
 * }
 * </pre>
 */
public class SharedKafkaContainer {
    
    private static final String KAFKA_IMAGE = "apache/kafka:3.8.0";
    private static KafkaContainer instance;
    
    private SharedKafkaContainer() {
        // Prevent instantiation
    }
    
    /**
     * Get the singleton Kafka container instance.
     * Starts the container on first access.
     */
    public static KafkaContainer getInstance() {
        if (instance == null) {
            synchronized (SharedKafkaContainer.class) {
                if (instance == null) {
                    instance = new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
                        .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
                        .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                        .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                        .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
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
     * Get Kafka bootstrap servers URL.
     */
    public static String getBootstrapServers() {
        return getInstance().getBootstrapServers();
    }
    
    /**
     * Check if container is running.
     */
    public static boolean isRunning() {
        return instance != null && instance.isRunning();
    }
}

