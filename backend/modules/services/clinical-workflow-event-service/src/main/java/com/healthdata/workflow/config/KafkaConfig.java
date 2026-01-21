package com.healthdata.workflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration for Clinical Workflow Event Service
 *
 * Configures Kafka topics for clinical workflow management and task orchestration
 */
@Configuration
public class KafkaConfig {

    /**
     * Workflow events topic
     * Broadcasts clinical workflow events to other services
     */
    @Bean
    public NewTopic workflowEventsTopic() {
        return TopicBuilder.name("workflow.events")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Workflow initiated topic
     * Published when a new clinical workflow is initiated
     */
    @Bean
    public NewTopic workflowInitiatedTopic() {
        return TopicBuilder.name("workflow.initiated")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Workflow completed topic
     * Published when a clinical workflow is completed or closed
     */
    @Bean
    public NewTopic workflowCompletedTopic() {
        return TopicBuilder.name("workflow.completed")
            .partitions(3)
            .replicas(1)
            .config("retention.ms", "86400000")  // 24 hours
            .build();
    }

    /**
     * Task assigned topic
     * Published when tasks are assigned within workflows
     */
    @Bean
    public NewTopic taskAssignedTopic() {
        return TopicBuilder.name("task.assigned")
            .partitions(1)
            .replicas(1)
            .config("retention.ms", "604800000")  // 7 days
            .build();
    }
}
