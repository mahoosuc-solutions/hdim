package com.healthdata.cdr.audit;

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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for CDR Processor Audit Integration with real Kafka.
 * 
 * Tests HL7v2/CDA clinical data ingestion audit events for HIPAA compliance and interoperability tracking.
 * 
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 */
@SpringBootTest
@Testcontainers
@DisplayName("CDR Processor Audit Integration - Heavyweight Kafka Tests")
class CdrProcessorAuditIntegrationHeavyweightTest {

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
    private CdrProcessorAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "test-tenant-123";
    private static final String MESSAGE_ID = "HL7-MSG-12345";
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
    @DisplayName("Should publish successful HL7 message ingest event to Kafka")
    void shouldPublishSuccessfulHl7IngestEvent() throws Exception {
        // Arrange
        String messageType = "ADT^A01"; // Admit patient
        int segmentCount = 8;

        // Act
        auditIntegration.publishHl7MessageIngestEvent(
                TENANT_ID,
                messageType,
                MESSAGE_ID,
                PATIENT_ID,
                segmentCount,
                true,
                null,
                180L,
                "interface-engine"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("agentType").asText()).isEqualTo("CDR_PROCESSOR_SERVICE");
        assertThat(event.get("decisionType").asText()).isEqualTo("HL7_MESSAGE_INGEST");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("PARSED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("messageType").asText()).isEqualTo(messageType);
        assertThat(context.get("messageId").asText()).isEqualTo(MESSAGE_ID);
        assertThat(context.get("segmentCount").asInt()).isEqualTo(segmentCount);
        assertThat(context.get("success").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Should publish failed HL7 message ingest event to Kafka")
    void shouldPublishFailedHl7IngestEvent() throws Exception {
        // Arrange
        String errorMessage = "Invalid MSH segment: missing required field";

        // Act
        auditIntegration.publishHl7MessageIngestEvent(
                TENANT_ID,
                "UNKNOWN",
                MESSAGE_ID + "-failed",
                null,
                0,
                false,
                errorMessage,
                50L,
                "interface-engine"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("success").asBoolean()).isFalse();
        assertThat(context.get("errorMessage").asText()).isEqualTo(errorMessage);
        assertThat(context.get("segmentCount").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should publish HL7v2 parse event to Kafka")
    void shouldPublishHl7v2ParseEvent() throws Exception {
        // Arrange
        String messageType = "ORU^R01"; // Lab results
        List<String> extractedResources = List.of("Patient", "Observation", "DiagnosticReport");

        // Act
        auditIntegration.publishHl7v2ParseEvent(
                TENANT_ID,
                messageType,
                MESSAGE_ID,
                PATIENT_ID,
                extractedResources,
                true,
                null,
                250L,
                "parser-service"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("HL7V2_PARSE");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("messageType").asText()).isEqualTo(messageType);
        assertThat(context.get("extractedResourcesCount").asInt()).isEqualTo(extractedResources.size());
    }

    @Test
    @DisplayName("Should publish CDA document parse event to Kafka")
    void shouldPublishCdaParseEvent() throws Exception {
        // Arrange
        String documentId = "CDA-DOC-789";
        String documentType = "Continuity of Care Document";
        List<String> sections = List.of("Medications", "Allergies", "Problems", "Procedures");

        // Act
        auditIntegration.publishCdaParseEvent(
                TENANT_ID,
                documentId,
                documentType,
                PATIENT_ID,
                sections,
                true,
                null,
                400L,
                "cda-parser"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("CDA_PARSE");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("documentId").asText()).isEqualTo(documentId);
        assertThat(context.get("documentType").asText()).isEqualTo(documentType);
        assertThat(context.get("sectionsCount").asInt()).isEqualTo(sections.size());
    }

    @Test
    @DisplayName("Should publish FHIR conversion event to Kafka")
    void shouldPublishFhirConversionEvent() throws Exception {
        // Arrange
        String sourceFormat = "HL7v2";
        String targetFormat = "FHIR R4";
        int resourcesConverted = 15;

        // Act
        auditIntegration.publishFhirConversionEvent(
                TENANT_ID,
                sourceFormat,
                targetFormat,
                MESSAGE_ID,
                PATIENT_ID,
                resourcesConverted,
                true,
                null,
                500L,
                "fhir-converter"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        assertThat(event.get("decisionType").asText()).isEqualTo("FHIR_CONVERSION");
        assertThat(event.get("decisionOutcome").asText()).isEqualTo("CONVERTED");

        JsonNode context = event.get("decisionContext");
        assertThat(context.get("sourceFormat").asText()).isEqualTo(sourceFormat);
        assertThat(context.get("targetFormat").asText()).isEqualTo(targetFormat);
        assertThat(context.get("resourcesConverted").asInt()).isEqualTo(resourcesConverted);
    }

    @Test
    @DisplayName("Should handle high-volume HL7 message processing")
    void shouldHandleHighVolumeHL7Messages() throws Exception {
        // Arrange - Simulate processing 100 HL7 messages
        int messageCount = 100;

        // Act
        for (int i = 0; i < messageCount; i++) {
            String msgType = i % 2 == 0 ? "ADT^A01" : "ORU^R01";
            auditIntegration.publishHl7MessageIngestEvent(
                    TENANT_ID,
                    msgType,
                    MESSAGE_ID + "-" + i,
                    PATIENT_ID + "-" + i,
                    8,
                    true,
                    null,
                    150L,
                    "interface"
            );
        }

        // Assert
        int receivedCount = 0;
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds

        while (receivedCount < messageCount && (System.currentTimeMillis() - startTime) < timeout) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            receivedCount += records.count();
        }

        assertThat(receivedCount).isEqualTo(messageCount);
    }

    @Test
    @DisplayName("Should track processing performance across different message types")
    void shouldTrackProcessingPerformance() throws Exception {
        // Arrange - Different message types with varying complexity
        String[][] messageTypes = {
            {"ADT^A01", "120"},  // Simple admit
            {"ORU^R01", "300"},  // Lab results (more complex)
            {"MDM^T02", "450"}   // Document (most complex)
        };

        // Act
        for (String[] msgInfo : messageTypes) {
            auditIntegration.publishHl7MessageIngestEvent(
                    TENANT_ID,
                    msgInfo[0],
                    MESSAGE_ID + "-perf",
                    PATIENT_ID,
                    10,
                    true,
                    null,
                    Long.parseLong(msgInfo[1]),
                    "perf-test"
            );
            Thread.sleep(50); // Small delay to ensure ordering
        }

        // Assert - Verify different processing times
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(3);

        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            JsonNode context = event.get("decisionContext");
            long processingTime = context.get("processingTimeMs").asLong();

            // Verify processing time is recorded
            assertThat(processingTime).isGreaterThan(0);
        }
    }

    @Test
    @DisplayName("Should audit complete CDR processing workflow")
    void shouldAuditCompleteWorkflow() throws Exception {
        // Simulate complete workflow: Ingest -> Parse -> Convert -> Transform
        String workflowMessageId = MESSAGE_ID + "-workflow";

        // Step 1: Ingest
        auditIntegration.publishHl7MessageIngestEvent(
                TENANT_ID, "ADT^A01", workflowMessageId, PATIENT_ID,
                8, true, null, 100L, "ingest"
        );

        // Step 2: Parse
        Thread.sleep(50);
        auditIntegration.publishHl7v2ParseEvent(
                TENANT_ID, "ADT^A01", workflowMessageId, PATIENT_ID,
                List.of("Patient", "Encounter"), true, null, 150L, "parse"
        );

        // Step 3: Convert to FHIR
        Thread.sleep(50);
        auditIntegration.publishFhirConversionEvent(
                TENANT_ID, "HL7v2", "FHIR R4", workflowMessageId, PATIENT_ID,
                5, true, null, 200L, "convert"
        );

        // Step 4: Transform
        Thread.sleep(50);
        auditIntegration.publishCdrTransformEvent(
                TENANT_ID, "ADT^A01", workflowMessageId, PATIENT_ID,
                5, true, null, 180L, "transform"
        );

        // Assert - All 4 events published
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.count()).isGreaterThanOrEqualTo(4);

        // Verify workflow stages
        String[] expectedTypes = {
            "HL7_MESSAGE_INGEST",
            "HL7V2_PARSE",
            "FHIR_CONVERSION",
            "CDR_TRANSFORM"
        };

        int foundCount = 0;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            String decisionType = event.get("decisionType").asText();
            for (String expected : expectedTypes) {
                if (decisionType.equals(expected)) {
                    foundCount++;
                    break;
                }
            }
        }

        assertThat(foundCount).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("Should track data quality metrics in audit events")
    void shouldTrackDataQualityMetrics() throws Exception {
        // Arrange - Message with validation warnings
        auditIntegration.publishCdrTransformEvent(
                TENANT_ID,
                "ORU^R01",
                MESSAGE_ID + "-quality",
                PATIENT_ID,
                10,
                true,
                null,
                250L,
                "transform"
        );

        // Assert
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        assertThat(records.isEmpty()).isFalse();

        ConsumerRecord<String, String> record = records.iterator().next();
        JsonNode event = objectMapper.readTree(record.value());

        JsonNode context = event.get("decisionContext");
        assertThat(context.has("resourcesTransformed")).isTrue();
        assertThat(context.get("success").asBoolean()).isTrue();
    }
}
