package com.healthdata.caregap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.caregap.config.BaseIntegrationTest;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for Care Gap Audit Integration with real Kafka.
 *
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 * It verifies end-to-end audit event publishing to Kafka.
 *
 * Test Categories:
 * - Lightweight: CareGapAuditIntegrationTest (uses mocks, no Docker)
 * - Heavyweight: This class (uses real Kafka via Testcontainers, requires Docker)
 *
 * To run:
 * - Ensure Docker is running
 * - Run: ./gradlew test --tests CareGapAuditIntegrationHeavyweightTest
 */
@BaseIntegrationTest
@Tag("heavyweight")
@DisplayName("Care Gap Audit Integration - Heavyweight Kafka Tests")
@org.springframework.context.annotation.ComponentScan(basePackages = {
    "com.healthdata.caregap",
    "com.healthdata.audit.service.ai"
})
class CareGapAuditIntegrationHeavyweightTest {

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        String bootstrapServers = System.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> "ai.agent.decisions");
    }

    @Autowired
    private CareGapAuditIntegration auditIntegration;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "test-tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final String MEASURE_ID = "HEDIS_CDC_A1C";
    private static final String GAP_ID = "gap-789";
    private static final String TOPIC = "ai.agent.decisions";

    @BeforeEach
    void setUp() {
        // ObjectMapper is now autowired from Spring, so it's configured the same way as the publisher

        // Create Kafka consumer to verify published events
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.kafka.bootstrap-servers", "localhost:9092"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

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
    @DisplayName("Should publish care gap identification event to Kafka with agentId")
    void shouldPublishCareGapIdentificationEventToKafka() throws Exception {
        // Given
        JsonNode cqlResult = objectMapper.createObjectNode()
                .put("hasGap", true)
                .put("measureId", MEASURE_ID);

        // When
        auditIntegration.publishCareGapIdentificationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, GAP_ID, cqlResult, "user@example.com");

        // Then - Wait for event to be published and consumed
        long startTime = System.currentTimeMillis();
        ConsumerRecord<String, String> record = null;
        while (System.currentTimeMillis() - startTime < 10000) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                record = records.iterator().next();
                break;
            }
        }
        assertThat(record).isNotNull();
        
        // Verify partition key contains tenantId and agentId
        String expectedKey = TENANT_ID + ":care-gap-identifier";
        assertThat(record.key()).isEqualTo(expectedKey);
        
        // Verify event content by JSON string matching (simpler than full deserialization)
        String jsonValue = record.value();
        assertThat(jsonValue).contains("care-gap-identifier");
        assertThat(jsonValue).contains("CARE_GAP_IDENTIFIER");
        assertThat(jsonValue).contains(TENANT_ID);
        assertThat(jsonValue).contains(PATIENT_ID);
        assertThat(jsonValue).contains("CARE_GAP_IDENTIFICATION");
        assertThat(jsonValue).contains("eventId");
        assertThat(jsonValue).contains("timestamp");
    }

    @Test
    @DisplayName("Should use correct partition key format: tenantId:agentId")
    void shouldUseCorrectPartitionKeyFormat() throws Exception {
        // Given
        JsonNode cqlResult = objectMapper.createObjectNode();
        String tenantId1 = "tenant-1";
        String tenantId2 = "tenant-2";

        // When
        auditIntegration.publishCareGapIdentificationEvent(
                tenantId1, PATIENT_ID, MEASURE_ID, GAP_ID, cqlResult, "user1@example.com");
        auditIntegration.publishCareGapIdentificationEvent(
                tenantId2, PATIENT_ID, MEASURE_ID, GAP_ID, cqlResult, "user2@example.com");

        // Then - Verify partition keys
        long startTime = System.currentTimeMillis();
        boolean foundTenant1 = false;
        boolean foundTenant2 = false;
        
        while (System.currentTimeMillis() - startTime < 10000 && (!foundTenant1 || !foundTenant2)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                String key = record.key();
                if (key.equals(tenantId1 + ":care-gap-identifier")) {
                    foundTenant1 = true;
                }
                if (key.equals(tenantId2 + ":care-gap-identifier")) {
                    foundTenant2 = true;
                }
            }
        }
        
        assertThat(foundTenant1).isTrue();
        assertThat(foundTenant2).isTrue();
    }

    @Test
    @DisplayName("Should handle null CQL result gracefully")
    void shouldHandleNullCqlResult() throws Exception {
        // When
        auditIntegration.publishCareGapIdentificationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, GAP_ID, null, "user@example.com");

        // Then
        long startTime = System.currentTimeMillis();
        ConsumerRecord<String, String> record = null;
        while (System.currentTimeMillis() - startTime < 10000) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                record = records.iterator().next();
                break;
            }
        }
        assertThat(record).isNotNull();
        
        // Verify event content by JSON string matching
        String jsonValue = record.value();
        assertThat(jsonValue).contains("care-gap-identifier");
        assertThat(jsonValue).contains("inputMetrics");
    }
}
