package com.healthdata.cql.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Assumptions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Testcontainers configuration for Kafka integration tests.
 */
@TestConfiguration
public class CqlTestContainersConfiguration {

    private static final DockerImageName KAFKA_IMAGE = DockerImageName
            .parse("apache/kafka:3.8.0");

    private static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(KAFKA_IMAGE)
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    private static final AtomicBoolean TOPICS_CREATED = new AtomicBoolean(false);

    public static void configureKafka(DynamicPropertyRegistry registry) {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker is required for Kafka Testcontainers.");
        if (!KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.start();
        }

        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);

        ensureTopicsCreated();
    }

    private static void ensureTopicsCreated() {
        if (!TOPICS_CREATED.compareAndSet(false, true)) {
            return;
        }

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());

        List<NewTopic> topics = List.of(
                new NewTopic("healthdata.audit.events", 1, (short) 1),
                new NewTopic("test-batch-progress", 1, (short) 1),
                new NewTopic("test-evaluation-update", 1, (short) 1),
                new NewTopic("test-evaluation-started", 1, (short) 1),
                new NewTopic("test-evaluation-completed", 1, (short) 1),
                new NewTopic("test-evaluation-failed", 1, (short) 1)
        );

        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(topics).all().get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            TOPICS_CREATED.set(false);
            throw new IllegalStateException("Failed to create Kafka topics for tests", ex);
        }
    }

    private static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception ex) {
            return false;
        }
    }
}
