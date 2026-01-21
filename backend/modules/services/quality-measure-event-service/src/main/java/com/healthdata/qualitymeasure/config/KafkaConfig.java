package com.healthdata.qualitymeasure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration for Quality Measure Event Service
 *
 * Configures Kafka topics for measure event streaming and aggregation
 */
@Configuration
public class KafkaConfig {

    /**
     * Measure events topic
     * Broadcasts measure evaluation events to other services
     */
    @Bean
    public NewTopic measureEventsTopic() {
        return TopicBuilder.name("measure.events")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Measure evaluation topic
     * Publishes measure evaluation results for aggregation
     */
    @Bean
    public NewTopic measureEvaluationTopic() {
        return TopicBuilder.name("measure.evaluations")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Cohort metrics topic
     * Publishes aggregated cohort compliance metrics
     */
    @Bean
    public NewTopic cohortMetricsTopic() {
        return TopicBuilder.name("cohort.metrics")
            .partitions(1)
            .replicas(1)
            .config("retention.ms", "604800000")  // 7 days
            .build();
    }
}
