package com.healthdata.fhireventbridge.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for FHIR Event Bridge Service
 *
 * Configures Kafka topics for:
 * - Consuming FHIR Patient events
 * - Publishing domain events (PatientCreatedEvent, PatientMergedEvent, etc.)
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    // ===== INCOMING TOPICS (Consumed from FHIR Service) =====

    /**
     * FHIR Patient.created events
     * Published by fhir-service when patient is created
     */
    @Bean
    public NewTopic fhirPatientCreatedTopic() {
        return TopicBuilder.name("fhir.patient.created")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "604800000")  // 7 days
            .build();
    }

    /**
     * FHIR Patient.updated events
     * Published by fhir-service when patient is updated
     */
    @Bean
    public NewTopic fhirPatientUpdatedTopic() {
        return TopicBuilder.name("fhir.patient.updated")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "604800000")  // 7 days
            .build();
    }

    /**
     * FHIR Patient.linked events
     * Published by fhir-service when Patient.link is created/updated
     */
    @Bean
    public NewTopic fhirPatientLinkedTopic() {
        return TopicBuilder.name("fhir.patient.linked")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "604800000")  // 7 days
            .build();
    }

    // ===== OUTGOING TOPICS (Published by this service) =====

    /**
     * Domain PatientCreatedEvent
     * Published after converting FHIR patient creation
     */
    @Bean
    public NewTopic patientCreatedEventTopic() {
        return TopicBuilder.name("patient.created")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "2592000000")  // 30 days
            .build();
    }

    /**
     * Domain PatientMergedEvent
     * Published when FHIR Patient.link indicates merge
     */
    @Bean
    public NewTopic patientMergedEventTopic() {
        return TopicBuilder.name("patient.merged")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "2592000000")  // 30 days
            .build();
    }

    /**
     * Domain PatientIdentifierChangedEvent
     * Published when patient identifiers are updated in FHIR
     */
    @Bean
    public NewTopic patientIdentifierChangedEventTopic() {
        return TopicBuilder.name("patient.identifier.changed")
            .partitions(1)
            .replicas(1)
            .config("retention.ms", "604800000")  // 7 days
            .build();
    }

    /**
     * Domain PatientLinkedEvent
     * Published when FHIR Patient.link is processed
     */
    @Bean
    public NewTopic patientLinkedEventTopic() {
        return TopicBuilder.name("patient.linked")
            .partitions(1)
            .replicas(1)
            .config("retention.ms", "604800000")  // 7 days
            .build();
    }

    // ===== KAFKA CONSUMER FACTORY =====

    /**
     * Consumer factory for FHIR event topics
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "${spring.kafka.bootstrap-servers}");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "${spring.kafka.consumer.group-id}");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.healthdata.fhireventbridge.event.FhirPatientEvent");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Kafka listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
