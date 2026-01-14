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
    private static final String REQUEST_ID = "approval-request-789";
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
        String requestedBy = "provider-123";
        String requestDescription = "Approve high-risk medication";

        // Act
        auditIntegration.publishApprovalRequestEvent(
                TENANT_ID,
                REQUEST_ID,
                requestType,
                requestedBy,
                requestDescription,
                150L,
                requestedBy
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("agentType").asText()).isEqualTo("APPROVAL_SERVICE");
        assertThat(event.get("decisionType").asText()).isEqualTo("APPROVAL_REQUEST");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("SUBMITTED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("requestType").asText()).isEqualTo(requestType);
        assertThat(context.get("requestedBy").asText()).isEqualTo(requestedBy);
        assertThat(context.get("requestDescription").asText()).isEqualTo(requestDescription);
    }

    @Test
    @DisplayName("Should publish approval granted decision event to Kafka")
    void shouldPublishApprovalGrantedEvent() throws Exception {
        // Arrange
        String decision = "APPROVED";
        String approvalNotes = "Clinical justification verified";

        // Act
        auditIntegration.publishApprovalDecisionEvent(
                TENANT_ID,
                REQUEST_ID,
                APPROVER_ID,
                decision,
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

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("approverId").asText()).isEqualTo(APPROVER_ID);
        assertThat(context.get("decision").asText()).isEqualTo(decision);
        assertThat(context.get("approvalNotes").asText()).isEqualTo(approvalNotes);
    }

    @Test
    @DisplayName("Should publish approval rejected decision event to Kafka")
    void shouldPublishApprovalRejectedEvent() throws Exception {
        // Arrange
        String decision = "REJECTED";
        String rejectionReason = "Insufficient clinical evidence";

        // Act
        auditIntegration.publishApprovalDecisionEvent(
                TENANT_ID,
                REQUEST_ID,
                APPROVER_ID,
                decision,
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
    @DisplayName("Should publish approval assignment event to Kafka")
    void shouldPublishAssignmentEvent() throws Exception {
        // Arrange
        String assignedFrom = "approver-001";
        String assignedTo = "approver-002";

        // Act
        auditIntegration.publishApprovalAssignmentEvent(
                TENANT_ID,
                REQUEST_ID,
                assignedFrom,
                assignedTo,
                "Reassigned for specialized review",
                100L,
                assignedFrom
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("APPROVAL_ASSIGNMENT");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("ASSIGNED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("assignedFrom").asText()).isEqualTo(assignedFrom);
        assertThat(context.get("assignedTo").asText()).isEqualTo(assignedTo);
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
                APPROVER_ID,
                escalatedTo,
                escalationReason,
                120L,
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
        // Simulate complete workflow: Request -> Assignment -> Escalation -> Approval
        String workflowRequestId = REQUEST_ID + "-workflow";

        // Step 1: Request
        auditIntegration.publishApprovalRequestEvent(
                TENANT_ID, workflowRequestId, "CLINICAL_DECISION",
                "provider", "High-risk decision", 100L, "provider"
        );

        // Step 2: Assignment
        Thread.sleep(100);
        auditIntegration.publishApprovalAssignmentEvent(
                TENANT_ID, workflowRequestId, "system",
                "approver-001", "Assigned", 50L, "system"
        );

        // Step 3: Escalation
        Thread.sleep(100);
        auditIntegration.publishApprovalEscalationEvent(
                TENANT_ID, workflowRequestId, "approver-001",
                "senior-approver", "Complex case", 80L, "approver-001"
        );

        // Step 4: Decision
        Thread.sleep(100);
        auditIntegration.publishApprovalDecisionEvent(
                TENANT_ID, workflowRequestId, "senior-approver",
                "APPROVED", "Approved by senior", 150L, "senior-approver"
        );

        // Assert - All 4 events published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(4);

        // Verify event types
        String[] expectedTypes = {
            "APPROVAL_REQUEST",
            "APPROVAL_ASSIGNMENT",
            "APPROVAL_ESCALATION",
            "APPROVAL_DECISION"
        };
        int index = 0;
        for (ConsumerRecord<String, String> record : records) {
            if (index < 4) {
                JsonNode event = objectMapper.readTree(record.value());
                assertThat(event.get("decisionType").asText()).isEqualTo(expectedTypes[index]);
                index++;
            }
        }
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
                    REQUEST_ID + "-" + i,
                    "CLINICAL_DECISION",
                    "provider-" + i,
                    "Request " + i,
                    80L,
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
        // Simulate request that gets reassigned multiple times
        String chainRequestId = REQUEST_ID + "-chain";

        // Initial request
        auditIntegration.publishApprovalRequestEvent(
                TENANT_ID, chainRequestId, "CLINICAL", "provider", "Request", 50L, "provider"
        );

        // Multiple assignments
        Thread.sleep(50);
        auditIntegration.publishApprovalAssignmentEvent(
                TENANT_ID, chainRequestId, "system", "approver-001", "First assignment", 50L, "system"
        );

        Thread.sleep(50);
        auditIntegration.publishApprovalAssignmentEvent(
                TENANT_ID, chainRequestId, "approver-001", "approver-002", "Second assignment", 50L, "approver-001"
        );

        Thread.sleep(50);
        auditIntegration.publishApprovalDecisionEvent(
                TENANT_ID, chainRequestId, "approver-002", "APPROVED", "Final approval", 100L, "approver-002"
        );

        // Assert - All events present
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(4);

        // Verify chain of custody
        boolean hasRequest = false;
        int assignmentCount = 0;
        boolean hasDecision = false;

        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            String type = event.get("decisionType").asText();

            if (type.equals("APPROVAL_REQUEST")) hasRequest = true;
            if (type.equals("APPROVAL_ASSIGNMENT")) assignmentCount++;
            if (type.equals("APPROVAL_DECISION")) hasDecision = true;
        }

        assertThat(hasRequest).isTrue();
        assertThat(assignmentCount).isGreaterThanOrEqualTo(2);
        assertThat(hasDecision).isTrue();
    }
}
