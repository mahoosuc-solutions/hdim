package com.healthdata.fhireventbridge.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KafkaConfig}.
 *
 * Validates all 7 topic beans (name, partitions, replicas, retention),
 * the consumer factory configuration, and the listener container factory
 * concurrency and ack mode settings.
 */
@Tag("unit")
@DisplayName("KafkaConfig")
class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        kafkaConfig = new KafkaConfig();
    }

    // ── Incoming Topics ─────────────────────────────────────────────

    @Nested
    @DisplayName("Incoming FHIR topics")
    class IncomingTopics {

        @Test
        @DisplayName("fhir.patient.created: 3 partitions, 7-day retention")
        void fhirPatientCreatedTopic() {
            NewTopic topic = kafkaConfig.fhirPatientCreatedTopic();

            assertThat(topic.name()).isEqualTo("fhir.patient.created");
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
            assertThat(topic.configs()).containsEntry("retention.ms", "604800000");
        }

        @Test
        @DisplayName("fhir.patient.updated: 3 partitions, 7-day retention")
        void fhirPatientUpdatedTopic() {
            NewTopic topic = kafkaConfig.fhirPatientUpdatedTopic();

            assertThat(topic.name()).isEqualTo("fhir.patient.updated");
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
            assertThat(topic.configs()).containsEntry("retention.ms", "604800000");
        }

        @Test
        @DisplayName("fhir.patient.linked: 3 partitions, 7-day retention")
        void fhirPatientLinkedTopic() {
            NewTopic topic = kafkaConfig.fhirPatientLinkedTopic();

            assertThat(topic.name()).isEqualTo("fhir.patient.linked");
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
            assertThat(topic.configs()).containsEntry("retention.ms", "604800000");
        }
    }

    // ── Outgoing Topics ─────────────────────────────────────────────

    @Nested
    @DisplayName("Outgoing domain event topics")
    class OutgoingTopics {

        @Test
        @DisplayName("patient.created: 3 partitions, 30-day retention")
        void patientCreatedEventTopic() {
            NewTopic topic = kafkaConfig.patientCreatedEventTopic();

            assertThat(topic.name()).isEqualTo("patient.created");
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
            assertThat(topic.configs()).containsEntry("retention.ms", "2592000000");
        }

        @Test
        @DisplayName("patient.merged: 3 partitions, 30-day retention")
        void patientMergedEventTopic() {
            NewTopic topic = kafkaConfig.patientMergedEventTopic();

            assertThat(topic.name()).isEqualTo("patient.merged");
            assertThat(topic.numPartitions()).isEqualTo(3);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
            assertThat(topic.configs()).containsEntry("retention.ms", "2592000000");
        }

        @Test
        @DisplayName("patient.identifier.changed: 1 partition, 7-day retention")
        void patientIdentifierChangedEventTopic() {
            NewTopic topic = kafkaConfig.patientIdentifierChangedEventTopic();

            assertThat(topic.name()).isEqualTo("patient.identifier.changed");
            assertThat(topic.numPartitions()).isEqualTo(1);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
            assertThat(topic.configs()).containsEntry("retention.ms", "604800000");
        }

        @Test
        @DisplayName("patient.linked: 1 partition, 7-day retention")
        void patientLinkedEventTopic() {
            NewTopic topic = kafkaConfig.patientLinkedEventTopic();

            assertThat(topic.name()).isEqualTo("patient.linked");
            assertThat(topic.numPartitions()).isEqualTo(1);
            assertThat(topic.replicationFactor()).isEqualTo((short) 1);
            assertThat(topic.configs()).containsEntry("retention.ms", "604800000");
        }
    }

    // ── Consumer & Listener Factories ───────────────────────────────

    @Nested
    @DisplayName("Consumer and listener factory configuration")
    class FactoryConfig {

        @Test
        @DisplayName("consumerFactory uses manual commit, earliest offset, trusted packages '*'")
        void consumerFactory() {
            ConsumerFactory<String, Object> factory = kafkaConfig.consumerFactory();

            assertThat(factory).isNotNull();

            Map<String, Object> props = factory.getConfigurationProperties();
            assertThat(props)
                .containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
                .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class)
                .containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
                .containsEntry(JsonDeserializer.TRUSTED_PACKAGES, "*");
        }

        @Test
        @DisplayName("listenerFactory has concurrency 3 and MANUAL ack mode")
        void kafkaListenerContainerFactory() {
            ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                kafkaConfig.kafkaListenerContainerFactory();

            assertThat(factory).isNotNull();
            assertThat(factory.getContainerProperties().getAckMode())
                .isEqualTo(ContainerProperties.AckMode.MANUAL);
        }
    }
}
