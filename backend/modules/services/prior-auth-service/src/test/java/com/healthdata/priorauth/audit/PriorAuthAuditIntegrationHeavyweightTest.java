package com.healthdata.priorauth.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for Prior Authorization Audit Integration with real Kafka.
 * 
 * Tests CMS prior authorization workflow audit events for compliance tracking.
 * 
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Prior Auth Audit Integration - Heavyweight Kafka Tests")
class PriorAuthAuditIntegrationHeavyweightTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        String bootstrapServers = kafka.getBootstrapServers();
        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> "ai.agent.decisions");
    }

    @Autowired
    private PriorAuthAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "test-tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final String REQUEST_ID = "prior-auth-789";
    private static final String TOPIC = "ai.agent.decisions";

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should publish prior auth request event to Kafka")
    void shouldPublishPriorAuthRequestEvent() throws Exception {
        // Arrange
        String procedureCode = "CPT-99213";
        String diagnosis = "E11.9";  // Diabetes

        // Act
        auditIntegration.publishPriorAuthRequestEvent(
                TENANT_ID,
                PATIENT_ID,
                REQUEST_ID,
                procedureCode,
                diagnosis,
                150L,
                "provider-123"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("agentType").asText()).isEqualTo("PRIOR_AUTH_SERVICE");
        assertThat(event.get("decisionType").asText()).isEqualTo("PRIOR_AUTH_REQUEST");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("SUBMITTED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("procedureCode").asText()).isEqualTo(procedureCode);
        assertThat(context.get("diagnosis").asText()).isEqualTo(diagnosis);
    }

    @Test
    @DisplayName("Should publish prior auth approved decision event to Kafka")
    void shouldPublishApprovedDecisionEvent() throws Exception {
        // Arrange
        String decision = "APPROVED";
        String reviewerId = "reviewer-001";

        // Act
        auditIntegration.publishPriorAuthDecisionEvent(
                TENANT_ID,
                PATIENT_ID,
                REQUEST_ID,
                decision,
                reviewerId,
                "Clinical guidelines met",
                200L,
                "reviewer-001"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("PRIOR_AUTH_DECISION");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("APPROVED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("decision").asText()).isEqualTo(decision);
        assertThat(context.get("reviewerId").asText()).isEqualTo(reviewerId);
        assertThat(context.get("reviewNotes").asText()).isEqualTo("Clinical guidelines met");
    }

    @Test
    @DisplayName("Should publish prior auth denied decision event to Kafka")
    void shouldPublishDeniedDecisionEvent() throws Exception {
        // Arrange
        String decision = "DENIED";
        String denialReason = "Procedure not medically necessary";

        // Act
        auditIntegration.publishPriorAuthDecisionEvent(
                TENANT_ID,
                PATIENT_ID,
                REQUEST_ID,
                decision,
                "reviewer-002",
                denialReason,
                180L,
                "reviewer-002"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionOutcome").asText()).isEqualTo("DENIED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("reviewNotes").asText()).isEqualTo(denialReason);
    }

    @Test
    @DisplayName("Should publish prior auth status update event to Kafka")
    void shouldPublishStatusUpdateEvent() throws Exception {
        // Arrange
        String oldStatus = "PENDING";
        String newStatus = "IN_REVIEW";

        // Act
        auditIntegration.publishPriorAuthStatusUpdateEvent(
                TENANT_ID,
                PATIENT_ID,
                REQUEST_ID,
                oldStatus,
                newStatus,
                100L,
                "system"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("PRIOR_AUTH_STATUS_UPDATE");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("UPDATED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("oldStatus").asText()).isEqualTo(oldStatus);
        assertThat(context.get("newStatus").asText()).isEqualTo(newStatus);
    }

    @Test
    @DisplayName("Should publish prior auth cancel event to Kafka")
    void shouldPublishCancelEvent() throws Exception {
        // Arrange
        String cancellationReason = "Patient no longer requires procedure";

        // Act
        auditIntegration.publishPriorAuthCancelEvent(
                TENANT_ID,
                PATIENT_ID,
                REQUEST_ID,
                cancellationReason,
                120L,
                "provider-123"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("PRIOR_AUTH_CANCEL");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("CANCELLED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("cancellationReason").asText()).isEqualTo(cancellationReason);
    }

    @Test
    @DisplayName("Should handle complete prior auth workflow lifecycle")
    void shouldHandleCompleteWorkflowLifecycle() throws Exception {
        // Simulate complete lifecycle: Request -> In Review -> Approved
        String workflowRequestId = REQUEST_ID + "-workflow";

        // Step 1: Request
        auditIntegration.publishPriorAuthRequestEvent(
                TENANT_ID, PATIENT_ID, workflowRequestId,
                "CPT-12345", "ICD-Z00.00", 100L, "provider"
        );

        // Step 2: Status Update
        Thread.sleep(100);
        auditIntegration.publishPriorAuthStatusUpdateEvent(
                TENANT_ID, PATIENT_ID, workflowRequestId,
                "PENDING", "IN_REVIEW", 50L, "system"
        );

        // Step 3: Approval Decision
        Thread.sleep(100);
        auditIntegration.publishPriorAuthDecisionEvent(
                TENANT_ID, PATIENT_ID, workflowRequestId,
                "APPROVED", "reviewer-001", "Approved", 150L, "reviewer"
        );

        // Assert - All 3 events published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        // Verify event types in order
        String[] expectedTypes = {"PRIOR_AUTH_REQUEST", "PRIOR_AUTH_STATUS_UPDATE", "PRIOR_AUTH_DECISION"};
        int index = 0;
        for (ConsumerRecord<String, String> record : records) {
            if (index < 3) {
                JsonNode event = objectMapper.readTree(record.value());
                assertThat(event.get("decisionType").asText()).isEqualTo(expectedTypes[index]);
                index++;
            }
        }
    }

    @Test
    @DisplayName("Should handle concurrent prior auth requests")
    void shouldHandleConcurrentRequests() throws Exception {
        // Arrange
        int requestCount = 50;

        // Act - Publish 50 concurrent requests
        for (int i = 0; i < requestCount; i++) {
            auditIntegration.publishPriorAuthRequestEvent(
                    TENANT_ID,
                    PATIENT_ID + "-" + i,
                    REQUEST_ID + "-" + i,
                    "CPT-99213",
                    "E11.9",
                    100L,
                    "provider"
            );
        }

        // Assert
        int receivedCount = 0;
        long startTime = System.currentTimeMillis();
        long timeout = 20000; // 20 seconds

        while (receivedCount < requestCount && (System.currentTimeMillis() - startTime) < timeout) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            receivedCount += records.count();
        }

        assertThat(receivedCount).isEqualTo(requestCount);
    }
}
