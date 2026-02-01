package com.healthdata.caregap.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration for Care Gap Event Service
 *
 * Configures Kafka topics for care gap detection and notification streaming
 */
@Configuration
public class KafkaConfig {

    /**
     * Care gap events topic
     * Broadcasts care gap detection events to other services
     */
    @Bean
    public NewTopic careGapEventsTopic() {
        return TopicBuilder.name("caregap.events")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Gap detected topic
     * Publishes newly detected care gaps for intervention recommendation
     */
    @Bean
    public NewTopic gapDetectedTopic() {
        return TopicBuilder.name("gap.detected")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Gap closed topic
     * Publishes when care gaps are closed/remediated
     */
    @Bean
    public NewTopic gapClosedTopic() {
        return TopicBuilder.name("gap.closed")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Intervention recommended topic
     * Publishes recommended clinical interventions for detected gaps
     */
    @Bean
    public NewTopic interventionRecommendedTopic() {
        return TopicBuilder.name("intervention.recommended")
            .partitions(1)
            .replicas(1)
            .config("retention.ms", "604800000")  // 7 days
            .build();
    }
}
