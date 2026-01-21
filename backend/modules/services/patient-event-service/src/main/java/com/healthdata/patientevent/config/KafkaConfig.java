package com.healthdata.patientevent.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration for Patient Event Service
 *
 * Configures Kafka topics for patient event streaming
 */
@Configuration
public class KafkaConfig {

    /**
     * Patient events topic
     * Broadcasts all patient lifecycle events to other services
     */
    @Bean
    public NewTopic patientEventsTopic() {
        return TopicBuilder.name("patient.events")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Patient projections topic
     * Publishes denormalized patient state for caching and aggregation
     */
    @Bean
    public NewTopic patientProjectionsTopic() {
        return TopicBuilder.name("patient.projections")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }
}
