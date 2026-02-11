package com.healthdata.approval.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for Approval Service Audit Integration with real Kafka.
 * 
 * Tests Human-in-the-Loop (HITL) approval workflow audit events for compliance tracking.
 * 
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
@DisplayName("Approval Audit Integration - Heavyweight Kafka Tests")
class ApprovalAuditIntegrationHeavyweightTest {

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
    private ApprovalAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "test-tenant-123";
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final String APPROVER_ID = "approver-001";
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
    @DisplayName("Should publish approval request event to Kafka")
    void shouldPublishApprovalRequestEvent() throws Exception {
        // Arrange
        String requestType = "CLINICAL_DECISION";
        String entityType = "Medication";
        String entityId = "med-123";
        String riskLevel = "HIGH";
        double confidenceScore = 0.85;

        // Act
        auditIntegration.publishApprovalRequestEvent(
                TENANT_ID,
                REQUEST_ID,
                requestType,
                entityType,
                entityId,
                riskLevel,
                confidenceScore,
                "provider-123"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("agentType").asText()).isEqualTo("APPROVAL_SERVICE");
        assertThat(event.get("decisionType").asText()).isEqualTo("APPROVAL_REQUEST");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("SUBMITTED");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("requestType").asText()).isEqualTo(requestType);
        assertThat(metrics.get("entityType").asText()).isEqualTo(entityType);
        assertThat(metrics.get("entityId").asText()).isEqualTo(entityId);
        assertThat(metrics.get("riskLevel").asText()).isEqualTo(riskLevel);
    }

    @Test
    @DisplayName("Should publish approval granted decision event to Kafka")
    void shouldPublishApprovalGrantedEvent() throws Exception {
        // Arrange
        boolean approved = true;
        String approvalNotes = "Clinical justification verified";

        // Act
        auditIntegration.publishApprovalDecisionEvent(
                TENANT_ID,
                REQUEST_ID,
                "Decision",
                "decision-001",
                approved,
                APPROVER_ID,
                approvalNotes,
                200L,
                APPROVER_ID
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("APPROVAL_DECISION");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("APPROVED");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("decidedBy").asText()).isEqualTo(APPROVER_ID);
        assertThat(metrics.get("approved").asBoolean()).isTrue();
        assertThat(metrics.get("reason").asText()).isEqualTo(approvalNotes);
    }

    @Test
    @DisplayName("Should publish approval rejected decision event to Kafka")
    void shouldPublishApprovalRejectedEvent() throws Exception {
        // Arrange
        boolean approved = false;
        String rejectionReason = "Insufficient clinical evidence";

        // Act
        auditIntegration.publishApprovalDecisionEvent(
                TENANT_ID,
                REQUEST_ID,
                "Decision",
                "decision-002",
                approved,
                APPROVER_ID,
                rejectionReason,
                180L,
                APPROVER_ID
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionOutcome").asText()).isEqualTo("REJECTED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("approvalNotes").asText()).isEqualTo(rejectionReason);
    }

    @Test
    @DisplayName("Should publish approval escalation event to Kafka")
    void shouldPublishApprovalEscalationEventTest() throws Exception {
        // Arrange
        String escalationReason = "Requires senior approval";
        String escalatedTo = "senior-approver";

        // Act
        auditIntegration.publishApprovalEscalationEvent(
                TENANT_ID,
                REQUEST_ID,
                "Decision",
                "decision-001",
                escalationReason,
                escalatedTo,
                APPROVER_ID
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("APPROVAL_ESCALATION");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("escalationReason").asText()).isEqualTo(escalationReason);
        assertThat(metrics.get("escalatedTo").asText()).isEqualTo(escalatedTo);
    }

    @Test
    @DisplayName("Should publish approval escalation event to Kafka")
    void shouldPublishEscalationEvent() throws Exception {
        // Arrange
        String escalatedTo = "senior-approver-001";
        String escalationReason = "High-risk decision requires senior review";

        // Act
        auditIntegration.publishApprovalEscalationEvent(
                TENANT_ID,
                REQUEST_ID,
                "Decision",
                "decision-001",
                escalationReason,
                escalatedTo,
                APPROVER_ID
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("APPROVAL_ESCALATION");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("ESCALATED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("escalatedFrom").asText()).isEqualTo(APPROVER_ID);
        assertThat(context.get("escalatedTo").asText()).isEqualTo(escalatedTo);
        assertThat(context.get("escalationReason").asText()).isEqualTo(escalationReason);
    }

    @Test
    @DisplayName("Should handle complete approval workflow with escalation")
    void shouldHandleCompleteWorkflowWithEscalation() throws Exception {
        // Simulate complete workflow: Request -> Escalation -> Decision
        UUID workflowRequestId = UUID.randomUUID();

        // Step 1: Request
        auditIntegration.publishApprovalRequestEvent(
                TENANT_ID, workflowRequestId, "CLINICAL_DECISION",
                "Decision", "decision-001", "HIGH", 0.95, "provider"
        );

        // Step 2: Escalation
        Thread.sleep(100);
        auditIntegration.publishApprovalEscalationEvent(
                TENANT_ID, workflowRequestId, "Decision", "decision-001",
                "Complex case", "senior-approver", "system"
        );

        // Step 3: Decision
        Thread.sleep(100);
        auditIntegration.publishApprovalDecisionEvent(
                TENANT_ID, workflowRequestId, "Decision", "decision-001",
                true, "senior-approver", "Approved by senior", 150L, "senior-approver"
        );

        // Assert - All 3 events published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        // Count approval-related events
        int approvalEventCount = 0;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            String decisionType = event.get("decisionType").asText();
            if (decisionType.startsWith("APPROVAL_")) {
                approvalEventCount++;
            }
        }
        assertThat(approvalEventCount).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should handle high-volume approval requests")
    void shouldHandleHighVolumeApprovals() throws Exception {
        // Arrange
        int requestCount = 100;

        // Act - Publish 100 approval requests
        for (int i = 0; i < requestCount; i++) {
            auditIntegration.publishApprovalRequestEvent(
                    TENANT_ID,
                    UUID.randomUUID(),
                    "CLINICAL_DECISION",
                    "Decision",
                    "decision-" + i,
                    "MEDIUM",
                    0.80,
                    "provider"
            );
        }

        // Assert
        int receivedCount = 0;
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds

        while (receivedCount < requestCount && (System.currentTimeMillis() - startTime) < timeout) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            receivedCount += records.count();
        }

        assertThat(receivedCount).isEqualTo(requestCount);
    }

    @Test
    @DisplayName("Should maintain audit trail integrity across reassignments")
    void shouldMaintainAuditTrailIntegrity() throws Exception {
        // Simulate request with escalation chain
        UUID chainRequestId = UUID.randomUUID();

        // Initial request
        auditIntegration.publishApprovalRequestEvent(
                TENANT_ID, chainRequestId, "CLINICAL", "Decision", "decision-001", "MEDIUM", 0.75, "provider"
        );

        // First escalation
        Thread.sleep(50);
        auditIntegration.publishApprovalEscalationEvent(
                TENANT_ID, chainRequestId, "Decision", "decision-001",
                "Needs senior review", "approver-001", "system"
        );

        // Second escalation
        Thread.sleep(50);
        auditIntegration.publishApprovalEscalationEvent(
                TENANT_ID, chainRequestId, "Decision", "decision-001",
                "Escalated to director", "approver-002", "approver-001"
        );

        // Final decision
        Thread.sleep(50);
        auditIntegration.publishApprovalDecisionEvent(
                TENANT_ID, chainRequestId, "Decision", "decision-001",
                true, "approver-002", "Final approval", 100L, "approver-002"
        );

        // Assert - All events present
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(4);

        // Verify chain of custody
        boolean hasRequest = false;
        int escalationCount = 0;
        boolean hasDecision = false;

        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            String type = event.get("decisionType").asText();

            if (type.equals("APPROVAL_REQUEST")) hasRequest = true;
            if (type.equals("APPROVAL_ESCALATION")) escalationCount++;
            if (type.equals("APPROVAL_DECISION")) hasDecision = true;
        }

        assertThat(hasRequest).isTrue();
        assertThat(escalationCount).isGreaterThanOrEqualTo(2);
        assertThat(hasDecision).isTrue();
    }
}
