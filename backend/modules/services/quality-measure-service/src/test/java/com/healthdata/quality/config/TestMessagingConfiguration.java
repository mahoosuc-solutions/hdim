package com.healthdata.quality.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test Messaging Configuration for Quality Measure Service Tests
 *
 * Provides mock Kafka configuration that doesn't require a running Kafka broker.
 * All Kafka operations are mocked to succeed silently, enabling tests to run
 * without external Kafka dependencies.
 *
 * Key Features:
 * - Mock Kafka producers and consumers (no network calls)
 * - No external Kafka dependency
 * - All send operations return successful futures
 * - Listeners disabled by default
 *
 * Note: For true integration tests with Kafka, use @EmbeddedKafka annotation
 * and override these beans in the specific test class.
 */
@TestConfiguration
public class TestMessagingConfiguration {

    /**
     * Mock Kafka producer factory - no actual Kafka connection.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ProducerFactory<String, String> producerFactory() {
        return Mockito.mock(ProducerFactory.class);
    }

    /**
     * Mock Kafka template (String, String) that silently succeeds on all sends.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> mockTemplate = Mockito.mock(KafkaTemplate.class);
        // Configure mock to return successful futures for all send operations
        SendResult<String, String> sendResult = Mockito.mock(SendResult.class);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);
        when(mockTemplate.send(anyString(), anyString())).thenReturn(future);
        when(mockTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        return mockTemplate;
    }

    /**
     * Mock Kafka template (String, Object) for services that need generic objects.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> kafkaTemplateObject() {
        KafkaTemplate<String, Object> mockTemplate = Mockito.mock(KafkaTemplate.class);
        // Configure mock to return successful futures for all send operations
        SendResult<String, Object> sendResult = Mockito.mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(mockTemplate.send(anyString(), any())).thenReturn(future);
        when(mockTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        return mockTemplate;
    }

    /**
     * Mock Kafka consumer factory - no actual Kafka connection.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ConsumerFactory<String, String> consumerFactory() {
        return Mockito.mock(ConsumerFactory.class);
    }

    /**
     * Mock Kafka listener container factory with listeners disabled.
     */
    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setAutoStartup(false); // Don't auto-start listeners in tests
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
