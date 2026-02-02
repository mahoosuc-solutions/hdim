package com.healthdata.ehr.audit;

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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for EHR Connector Audit Integration with real Kafka.
 * 
 * Tests external EHR PHI access audit events for HIPAA compliance.
 * 
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
@DisplayName("EHR Connector Audit Integration - Heavyweight Kafka Tests")
class EhrConnectorAuditIntegrationHeavyweightTest {

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
    private EhrConnectorAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "test-tenant-123";
    private static final String CONNECTION_ID = "epic-prod-001";
    private static final String PATIENT_ID = "patient-456";
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
    @DisplayName("Should publish successful EHR data sync event to Kafka")
    void shouldPublishSuccessfulDataSyncEvent() throws Exception {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        int encountersRetrieved = 15;
        int observationsRetrieved = 125;

        // Act
        auditIntegration.publishEhrDataSyncEvent(
                TENANT_ID,
                CONNECTION_ID,
                "Epic",
                PATIENT_ID,
                startDate,
                endDate,
                encountersRetrieved,
                observationsRetrieved,
                true,
                null,
                250L,
                "sync-job-001"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("agentType").asText()).isEqualTo("EHR_CONNECTOR_SERVICE");
        assertThat(event.get("decisionType").asText()).isEqualTo("EHR_DATA_SYNC");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("FETCHED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("connectionId").asText()).isEqualTo(CONNECTION_ID);
        assertThat(context.get("ehrVendor").asText()).isEqualTo("Epic");
        assertThat(context.get("encountersRetrieved").asInt()).isEqualTo(encountersRetrieved);
        assertThat(context.get("observationsRetrieved").asInt()).isEqualTo(observationsRetrieved);
        assertThat(context.get("success").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Should publish failed EHR data sync event to Kafka")
    void shouldPublishFailedDataSyncEvent() throws Exception {
        // Arrange
        String errorMessage = "Connection timeout to Epic server";

        // Act
        auditIntegration.publishEhrDataSyncEvent(
                TENANT_ID,
                CONNECTION_ID,
                "Epic",
                PATIENT_ID,
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now(),
                0,
                0,
                false,
                errorMessage,
                5000L,
                "sync-job-002"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionOutcome").asText()).isEqualTo("FETCHED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("success").asBoolean()).isFalse();
        assertThat(context.get("errorMessage").asText()).isEqualTo(errorMessage);
        assertThat(context.get("encountersRetrieved").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should publish successful patient fetch event to Kafka")
    void shouldPublishSuccessfulPatientFetchEvent() throws Exception {
        // Act
        auditIntegration.publishEhrPatientFetchEvent(
                TENANT_ID,
                CONNECTION_ID,
                "Cerner",
                PATIENT_ID,
                true,
                null,
                180L,
                "provider-123"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("EHR_PATIENT_FETCH");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("FETCHED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("patientId").asText()).isEqualTo(PATIENT_ID);
        assertThat(context.get("ehrVendor").asText()).isEqualTo("Cerner");
        assertThat(context.get("success").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Should publish failed patient fetch event to Kafka")
    void shouldPublishFailedPatientFetchEvent() throws Exception {
        // Arrange
        String errorMessage = "Patient not found in EHR system";

        // Act
        auditIntegration.publishEhrPatientFetchEvent(
                TENANT_ID,
                CONNECTION_ID,
                "Epic",
                "patient-not-found",
                false,
                errorMessage,
                120L,
                "provider-456"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("success").asBoolean()).isFalse();
        assertThat(context.get("errorMessage").asText()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("Should publish patient search event to Kafka")
    void shouldPublishPatientSearchEvent() throws Exception {
        // Arrange
        String familyName = "Smith";
        String givenName = "John";
        String dateOfBirth = "1980-05-15";
        int resultsCount = 3;

        // Act
        auditIntegration.publishEhrPatientSearchEvent(
                TENANT_ID,
                CONNECTION_ID,
                "Epic",
                familyName,
                givenName,
                dateOfBirth,
                resultsCount,
                true, // searchSuccess
                300L,
                "registration-user"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("EHR_PATIENT_SEARCH");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("familyName").asText()).isEqualTo(familyName);
        assertThat(context.get("givenName").asText()).isEqualTo(givenName);
        assertThat(context.get("dateOfBirth").asText()).isEqualTo(dateOfBirth);
        assertThat(context.get("resultsCount").asInt()).isEqualTo(resultsCount);
    }

    @Test
    @DisplayName("Should handle high-volume EHR sync operations")
    void shouldHandleHighVolumeSync() throws Exception {
        // Arrange - Simulate syncing 50 patients
        int patientCount = 50;

        // Act
        for (int i = 0; i < patientCount; i++) {
            auditIntegration.publishEhrDataSyncEvent(
                    TENANT_ID,
                    CONNECTION_ID,
                    "Epic",
                    PATIENT_ID + "-" + i,
                    LocalDateTime.now().minusDays(7),
                    LocalDateTime.now(),
                    10,
                    50,
                    true,
                    null,
                    200L,
                    "sync-job"
            );
        }

        // Assert
        int receivedCount = 0;
        long startTime = System.currentTimeMillis();
        long timeout = 20000; // 20 seconds

        while (receivedCount < patientCount && (System.currentTimeMillis() - startTime) < timeout) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            receivedCount += records.count();
        }

        assertThat(receivedCount).isEqualTo(patientCount);
    }

    @Test
    @DisplayName("Should track data sync performance metrics")
    void shouldTrackPerformanceMetrics() throws Exception {
        // Arrange - Different performance scenarios
        long fastSync = 100L;
        long normalSync = 500L;
        long slowSync = 2000L;

        // Act
        auditIntegration.publishEhrDataSyncEvent(TENANT_ID, CONNECTION_ID, "Epic", PATIENT_ID + "-fast",
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 5, 25, true, null, fastSync, "sync");

        auditIntegration.publishEhrDataSyncEvent(TENANT_ID, CONNECTION_ID, "Epic", PATIENT_ID + "-normal",
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 10, 50, true, null, normalSync, "sync");

        auditIntegration.publishEhrDataSyncEvent(TENANT_ID, CONNECTION_ID, "Epic", PATIENT_ID + "-slow",
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 20, 100, true, null, slowSync, "sync");

        // Assert - Verify processing times recorded
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        boolean foundFast = false, foundNormal = false, foundSlow = false;

        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            JsonNode context = event.get("decisionContext");
            long processingTime = context.get("processingTimeMs").asLong();

            if (processingTime == fastSync) foundFast = true;
            if (processingTime == normalSync) foundNormal = true;
            if (processingTime == slowSync) foundSlow = true;
        }

        assertThat(foundFast).isTrue();
        assertThat(foundNormal).isTrue();
        assertThat(foundSlow).isTrue();
    }

    // Note: publishEhrDataPushEvent method does not exist in EhrConnectorAuditIntegration
    // Data sync already covers bi-directional sync scenarios
}
