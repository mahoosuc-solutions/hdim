package com.healthdata.test.containers;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton Kafka container for integration tests.
 *
 * Provides a shared Kafka instance across all test classes to improve performance.
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
 * static void configureKafka(DynamicPropertyRegistry registry) {
 *     registry.add("spring.kafka.bootstrap-servers",
 *         SharedKafkaContainer::getBootstrapServers);
 * }
 * }
 * </pre>
 */
public class SharedKafkaContainer {

    private static final String KAFKA_IMAGE = "apache/kafka:3.8.0";

    /**
     * Delay before stopping the container during JVM shutdown.
     * This allows Gradle time to write XML test results before connections are invalidated.
     */
    private static final long SHUTDOWN_DELAY_MS = 5000;

    private static KafkaContainer instance;
    private static final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

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
                    }, "SharedKafkaContainer-Shutdown"));
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

    /**
     * Check if shutdown has been initiated.
     * Useful for tests that need to know if cleanup is in progress.
     */
    public static boolean isShutdownInitiated() {
        return shutdownInitiated.get();
    }
}
