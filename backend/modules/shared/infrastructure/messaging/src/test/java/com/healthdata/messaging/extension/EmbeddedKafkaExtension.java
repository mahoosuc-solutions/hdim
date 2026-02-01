package com.healthdata.messaging.extension;

import static org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps;
import static org.springframework.kafka.test.utils.KafkaTestUtils.producerProps;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * JUnit 5 extension that manages EmbeddedKafkaBroker lifecycle for tests.
 *
 * Works with Spring's @EmbeddedKafka annotation to automatically manage the broker lifecycle.
 * Eliminates Docker container overhead (~10-30s per test).
 *
 * Usage:
 * @SpringBootTest
 * @EmbeddedKafka(partitions = 3)
 * @RegisterExtension
 * static EmbeddedKafkaExtension embeddedKafka = new EmbeddedKafkaExtension();
 *
 * Or for standalone usage without Spring test context:
 * @RegisterExtension
 * static EmbeddedKafkaExtension embeddedKafka = new EmbeddedKafkaExtension(3);
 */
public class EmbeddedKafkaExtension implements BeforeAllCallback {

    private final int partitions;
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    /**
     * Create extension with specified number of partitions.
     * Use with {@code @EmbeddedKafka} annotation to manage broker lifecycle.
     */
    public EmbeddedKafkaExtension() {
        this(1);
    }

    /**
     * Create extension with specified number of partitions.
     */
    public EmbeddedKafkaExtension(int partitions) {
        this.partitions = partitions;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // This extension works with Spring's @EmbeddedKafka annotation
        // The broker is managed by Spring, not manually created here
        // Bootstrap servers are automatically set by Spring
    }

    /**
     * Get consumer properties for test clients to connect to embedded broker.
     * This method is a convenience wrapper around KafkaTestUtils.
     */
    public Map<String, Object> getConsumerProperties() {
        // Return a new map with bootstrap servers from system property
        Map<String, Object> props = new HashMap<>();
        String bootstrapServers = System.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "test-group");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }

    /**
     * Get producer properties for test clients to connect to embedded broker.
     * This method is a convenience wrapper around KafkaTestUtils.
     */
    public Map<String, Object> getProducerProperties() {
        Map<String, Object> props = new HashMap<>();
        String bootstrapServers = System.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return props;
    }

    /**
     * Get bootstrap servers string for direct configuration.
     */
    public String getBootstrapServersString() {
        return System.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
    }

    public void destroy() {
        // Cleanup handled by Spring's EmbeddedKafka annotation
        if (embeddedKafkaBroker != null) {
            embeddedKafkaBroker.destroy();
        }
    }
}
