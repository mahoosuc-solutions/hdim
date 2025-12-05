package com.healthdata.cql.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for publishing evaluation events.
 * Configured for JSON serialization and optimal performance.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Performance tuning
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");  // Wait for all replicas (required for idempotence)
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);  // Unlimited retries with idempotence
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // Batch size in bytes
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);  // Wait up to 10ms to batch messages
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");  // Compress messages
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);  // 32MB buffer

        // Idempotence for exactly-once semantics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // JSON serializer configuration
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);  // Don't add type headers
        configProps.put(JsonSerializer.TYPE_MAPPINGS, "");  // No type mappings needed

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
