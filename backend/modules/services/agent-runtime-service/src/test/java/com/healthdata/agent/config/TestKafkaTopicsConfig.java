package com.healthdata.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * Ensure required Kafka topics exist for test runs to avoid listener noise.
 */
@Configuration
@Profile("test")
@ConditionalOnProperty(prefix = "test.kafka.topics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TestKafkaTopicsConfig {

    @Bean
    KafkaAdmin testKafkaAdmin(KafkaProperties kafkaProperties) {
        return new KafkaAdmin(kafkaProperties.buildAdminProperties());
    }

    @Bean
    NewTopic aiDecisionsTopic(
            @Value("${audit.kafka.topic.ai-decisions:ai.agent.decisions}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic approvalEventsTopic() {
        return TopicBuilder.name("approval-events").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic agentConfigEventsTopic() {
        return TopicBuilder.name("agent-config-events").partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic configChangesTopic(
            @Value("${audit.kafka.topic.config-changes:configuration.engine.changes}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic userActionsTopic(
            @Value("${audit.kafka.topic.user-actions:user.configuration.actions}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }
}
