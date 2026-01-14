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
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final String PAYER_ID = "payer-123";
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
                REQUEST_ID,
                PATIENT_ID,
                PAYER_ID,
                procedureCode,
                "ROUTINE", // urgency
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
                REQUEST_ID,
                PATIENT_ID,
                PAYER_ID,
                decision,
                "Clinical guidelines met", // decisionReason
                true, // approved
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
                REQUEST_ID,
                PATIENT_ID,
                PAYER_ID,
                decision,
                denialReason,
                false, // approved
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
    @DisplayName("Should publish prior auth submission event to Kafka")
    void shouldPublishSubmissionEvent() throws Exception {
        // Arrange
        boolean submissionSuccess = true;

        // Act
        auditIntegration.publishPriorAuthSubmissionEvent(
                TENANT_ID,
                REQUEST_ID,
                PATIENT_ID,
                PAYER_ID,
                submissionSuccess,
                null,
                100L,
                "system"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("PRIOR_AUTH_REQUEST");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("submissionSuccess").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Should publish prior auth appeal event to Kafka")
    void shouldPublishAppealEvent() throws Exception {
        // Arrange
        String appealReason = "New clinical evidence available";
        String supportingInfo = "Updated lab results show medical necessity";

        // Act
        auditIntegration.publishPriorAuthAppealEvent(
                TENANT_ID,
                REQUEST_ID,
                PATIENT_ID,
                appealReason,
                supportingInfo,
                "provider-123"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("PRIOR_AUTH_REQUEST");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("appealReason").asText()).isEqualTo(appealReason);
        assertThat(metrics.get("supportingInfo").asText()).isEqualTo(supportingInfo);
    }

    @Test
    @DisplayName("Should handle complete prior auth workflow lifecycle")
    void shouldHandleCompleteWorkflowLifecycle() throws Exception {
        // Simulate complete lifecycle: Request -> Submission -> Decision
        UUID workflowRequestId = UUID.randomUUID();

        // Step 1: Request
        auditIntegration.publishPriorAuthRequestEvent(
                TENANT_ID, workflowRequestId, PATIENT_ID, PAYER_ID,
                "CPT-12345", "ROUTINE", "provider"
        );

        // Step 2: Submission
        Thread.sleep(100);
        auditIntegration.publishPriorAuthSubmissionEvent(
                TENANT_ID, workflowRequestId, PATIENT_ID, PAYER_ID,
                true, null, 50L, "system"
        );

        // Step 3: Approval Decision
        Thread.sleep(100);
        auditIntegration.publishPriorAuthDecisionEvent(
                TENANT_ID, workflowRequestId, PATIENT_ID, PAYER_ID,
                "APPROVED", "All criteria met", true, 150L, "reviewer"
        );

        // Assert - All 3 events published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        // All events should be PRIOR_AUTH_REQUEST type
        int priorAuthEventCount = 0;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            if (event.get("decisionType").asText().equals("PRIOR_AUTH_REQUEST")) {
                priorAuthEventCount++;
            }
        }
        assertThat(priorAuthEventCount).isGreaterThanOrEqualTo(3);
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
                    UUID.randomUUID(), // requestId
                    UUID.randomUUID(), // patientId
                    PAYER_ID,
                    "CPT-99213",
                    "ROUTINE", // urgency
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
