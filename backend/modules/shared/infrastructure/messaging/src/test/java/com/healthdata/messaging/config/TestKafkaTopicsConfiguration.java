package com.healthdata.messaging.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Shared test configuration for pre-creating common Kafka topics.
 * <p>
 * This configuration creates topics ahead of time to prevent "topic not found" warnings
 * and reduce test initialization delays caused by Kafka's auto-topic-creation mechanism.
 * <p>
 * <strong>Usage:</strong>
 * Import this configuration in your test class:
 * <pre>{@code
 * @SpringBootTest
 * @Import(TestKafkaTopicsConfiguration.class)
 * class MyKafkaIntegrationTest {
 *     // ...
 * }
 * }</pre>
 * <p>
 * Or enable via property:
 * <pre>{@code
 * test.kafka.topics.preCreate=true
 * }</pre>
 *
 * @see com.healthdata.messaging.annotation.EnableEmbeddedKafka
 * @since 1.0
 */
@Configuration
@Profile("test")
@ConditionalOnProperty(
    prefix = "test.kafka.topics",
    name = "preCreate",
    havingValue = "true",
    matchIfMissing = true
)
public class TestKafkaTopicsConfiguration {

    private static final int DEFAULT_PARTITIONS = 3;
    private static final short DEFAULT_REPLICAS = 1;

    // =========================================================================
    // Patient Events Topics
    // =========================================================================

    @Bean
    public NewTopic patientEventsTopic() {
        return TopicBuilder.name("patient.events")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic patientCreatedEventsTopic() {
        return TopicBuilder.name("patient.created")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic patientUpdatedEventsTopic() {
        return TopicBuilder.name("patient.updated")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    // =========================================================================
    // Care Gap Events Topics
    // =========================================================================

    @Bean
    public NewTopic careGapEventsTopic() {
        return TopicBuilder.name("care-gap.events")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic careGapClosedEventsTopic() {
        return TopicBuilder.name("care-gap.closed")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic careGapIdentifiedEventsTopic() {
        return TopicBuilder.name("care-gap.identified")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    // =========================================================================
    // Clinical Workflow Events Topics
    // =========================================================================

    @Bean
    public NewTopic clinicalWorkflowEventsTopic() {
        return TopicBuilder.name("clinical-workflow.events")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    // =========================================================================
    // Quality Measure Events Topics
    // =========================================================================

    @Bean
    public NewTopic qualityMeasureEventsTopic() {
        return TopicBuilder.name("quality-measure.events")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic qualityMeasureEvaluatedEventsTopic() {
        return TopicBuilder.name("quality-measure.evaluated")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    // =========================================================================
    // Audit Events Topics
    // =========================================================================

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("audit.events")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic aiDecisionsTopic() {
        return TopicBuilder.name("ai.agent.decisions")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic configChangesTopic() {
        return TopicBuilder.name("configuration.engine.changes")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic userActionsTopic() {
        return TopicBuilder.name("user.configuration.actions")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    // =========================================================================
    // Agent Events Topics
    // =========================================================================

    @Bean
    public NewTopic agentConfigEventsTopic() {
        return TopicBuilder.name("agent-config-events")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    @Bean
    public NewTopic approvalEventsTopic() {
        return TopicBuilder.name("approval-events")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }

    // =========================================================================
    // Dead Letter Queue Topics
    // =========================================================================

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name("dlq.events")
            .partitions(DEFAULT_PARTITIONS)
            .replicas(DEFAULT_REPLICAS)
            .build();
    }
}
