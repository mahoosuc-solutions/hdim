package com.healthdata.caregap.config;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers Kafka provider for all care-gap service tests.
 *
 * Starts a single Kafka container per test JVM and reuses it across tests.
 */
public final class TestKafkaContainerProvider {

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0")
                    .asCompatibleSubstituteFor("confluentinc/cp-kafka"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    private static final Object LOCK = new Object();

    private TestKafkaContainerProvider() {
    }

    public static String getBootstrapServers() {
        ensureStarted();
        return KAFKA_CONTAINER.getBootstrapServers();
    }

    public static void ensureStarted() {
        if (!KAFKA_CONTAINER.isRunning()) {
            synchronized (LOCK) {
                if (!KAFKA_CONTAINER.isRunning()) {
                    KAFKA_CONTAINER.start();
                }
            }
        }
    }

    public static void stop() {
        if (KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.stop();
        }
    }
}
