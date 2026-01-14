package com.healthdata.consent.audit;

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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for Consent Audit Integration with real Kafka.
 * 
 * Tests HIPAA 42 CFR Part 2 consent management audit events.
 * 
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 * It verifies end-to-end audit event publishing to Kafka for consent operations.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Consent Audit Integration - Heavyweight Kafka Tests")
class ConsentAuditIntegrationHeavyweightTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        String bootstrapServers = kafka.getBootstrapServers();
        registry.add("spring.kafka.bootstrap-servers", () => bootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> "ai.agent.decisions");
    }

    @Autowired
    private ConsentAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "test-tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final String CONSENT_ID = "consent-789";
    private static final String TOPIC = "ai.agent.decisions";

    @BeforeEach
    void setUp() {
        // Create Kafka consumer to verify published events
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
    @DisplayName("Should publish consent grant event to Kafka")
    void shouldPublishConsentGrantEvent() throws Exception {
        // Arrange
        String purposeOfUse = "TREAT";
        String consentScope = "SUBSTANCE_ABUSE";

        // Act
        auditIntegration.publishConsentGrantEvent(
                TENANT_ID,
                PATIENT_ID,
                CONSENT_ID,
                purposeOfUse,
                consentScope,
                100L,
                "test-user"
        );

        // Assert - Poll for the event
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        // Verify event structure
        assertThat(event.has("eventId")).isTrue();
        assertThat(event.get("tenantId").asText()).isEqualTo(TENANT_ID);
        assertThat(event.get("agentType").asText()).isEqualTo("CONSENT_SERVICE");
        assertThat(event.get("decisionType").asText()).isEqualTo("CONSENT_GRANT");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("GRANTED");

        // Verify context
        JsonNode context = event.get("decisionContext");
        assertThat(context.get("patientId").asText()).isEqualTo(PATIENT_ID);
        assertThat(context.get("consentId").asText()).isEqualTo(CONSENT_ID);
        assertThat(context.get("purposeOfUse").asText()).isEqualTo(purposeOfUse);
        assertThat(context.get("consentScope").asText()).isEqualTo(consentScope);

        // Verify partition key format
        assertThat(record.key()).isEqualTo(TENANT_ID + ":consent-service");
    }

    @Test
    @DisplayName("Should publish consent revoke event to Kafka")
    void shouldPublishConsentRevokeEvent() throws Exception {
        // Arrange
        String revocationReason = "Patient requested";

        // Act
        auditIntegration.publishConsentRevokeEvent(
                TENANT_ID,
                PATIENT_ID,
                CONSENT_ID,
                revocationReason,
                80L,
                "test-user"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("CONSENT_REVOKE");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("REVOKED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("revocationReason").asText()).isEqualTo(revocationReason);
    }

    @Test
    @DisplayName("Should publish consent update event to Kafka")
    void shouldPublishConsentUpdateEvent() throws Exception {
        // Arrange
        Map<String, Object> updatedFields = new HashMap<>();
        updatedFields.put("status", "ACTIVE");
        updatedFields.put("expirationDate", "2026-12-31");

        // Act
        auditIntegration.publishConsentUpdateEvent(
                TENANT_ID,
                PATIENT_ID,
                CONSENT_ID,
                updatedFields,
                120L,
                "test-user"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("CONSENT_UPDATE");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("UPDATED");

        JsonNode context = event.get("decisionContext");
        JsonNode updated = context.get("updatedFields");
        assertThat(updated.get("status").asText()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should publish consent delete event to Kafka")
    void shouldPublishConsentDeleteEvent() throws Exception {
        // Arrange
        String deletionReason = "Data retention policy";

        // Act
        auditIntegration.publishConsentDeleteEvent(
                TENANT_ID,
                PATIENT_ID,
                CONSENT_ID,
                deletionReason,
                90L,
                "test-user"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("CONSENT_DELETE");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("DELETED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("deletionReason").asText()).isEqualTo(deletionReason);
    }

    @Test
    @DisplayName("Should handle high-volume consent events")
    void shouldHandleHighVolumeConsentEvents() throws Exception {
        // Arrange
        int eventCount = 100;

        // Act - Publish 100 consent events
        for (int i = 0; i < eventCount; i++) {
            auditIntegration.publishConsentGrantEvent(
                    TENANT_ID,
                    PATIENT_ID + "-" + i,
                    CONSENT_ID + "-" + i,
                    "TREAT",
                    "SUBSTANCE_ABUSE",
                    50L,
                    "test-user"
            );
        }

        // Assert - Poll until we get all events (with timeout)
        int receivedCount = 0;
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds

        while (receivedCount < eventCount && (System.currentTimeMillis() - startTime) < timeout) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            receivedCount += records.count();
        }

        assertThat(receivedCount).isEqualTo(eventCount);
    }

    @Test
    @DisplayName("Should maintain event ordering for same tenant")
    void shouldMaintainEventOrdering() throws Exception {
        // Arrange - Publish events in specific order
        String[] operations = {"GRANT", "UPDATE", "REVOKE"};

        // Act
        auditIntegration.publishConsentGrantEvent(TENANT_ID, PATIENT_ID, CONSENT_ID + "-1", "TREAT", "FULL", 50L, "user");
        Thread.sleep(100); // Small delay to ensure ordering
        auditIntegration.publishConsentUpdateEvent(TENANT_ID, PATIENT_ID, CONSENT_ID + "-1", Map.of("status", "ACTIVE"), 50L, "user");
        Thread.sleep(100);
        auditIntegration.publishConsentRevokeEvent(TENANT_ID, PATIENT_ID, CONSENT_ID + "-1", "Test", 50L, "user");

        // Assert - Events should be in order
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        int index = 0;
        for (ConsumerRecord<String, String> record : records) {
            if (index < 3) {
                JsonNode event = objectMapper.readTree(record.value());
                String decisionType = event.get("decisionType").asText();
                assertThat(decisionType).contains(operations[index].split("_")[0]);
                index++;
            }
        }
    }
}
