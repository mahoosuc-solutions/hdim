package com.healthdata.quality.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Test Messaging Configuration for Quality Measure Service Tests
 *
 * Provides test-friendly Kafka configuration that doesn't require a running Kafka broker.
 * Uses in-memory message handling for unit and integration tests.
 *
 * Key Features:
 * - Mock Kafka producers and consumers
 * - No external Kafka dependency
 * - Simplified error handling for tests
 * - Configurable for embedded Kafka if needed
 *
 * Note: For true integration tests with Kafka, use @EmbeddedKafka annotation
 * and override these beans in the specific test class.
 */
@TestConfiguration
public class TestMessagingConfiguration {

    /**
     * Test Kafka producer factory with minimal configuration.
     */
    @Bean
    @Primary
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "0"); // Fire and forget for tests
        configProps.put(ProducerConfig.RETRIES_CONFIG, 0); // No retries in tests
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 0); // Send immediately
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Test Kafka template (String, String) that doesn't fail when Kafka is unavailable.
     */
    @Bean
    @Primary
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Test Kafka template (String, Object) for services that need generic objects.
     */
    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplateObject() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "0"); // Fire and forget for tests
        configProps.put(ProducerConfig.RETRIES_CONFIG, 0); // No retries in tests
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 0); // Send immediately
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configProps));
    }

    /**
     * Test Kafka consumer factory with minimal configuration.
     */
    @Bean
    @Primary
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Test Kafka listener container factory.
     */
    @Bean
    @Primary
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(1); // Single thread for tests
        factory.setAutoStartup(false); // Don't auto-start in tests
        return factory;
    }
}
