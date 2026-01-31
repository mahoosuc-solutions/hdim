package com.healthdata.cdr.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for CDR Processor Audit Integration with real Kafka.
 * 
 * Tests HL7v2/CDA clinical data ingestion audit events for HIPAA compliance and interoperability tracking.
 * 
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 */
@SpringBootTest
@ActiveProfiles("test")
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
        registry.add("healthdata.messaging.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.sync", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> TOPIC);
    }

    @Autowired
    private CdrProcessorAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "test-tenant-123";
    private static final String MESSAGE_ID = "HL7-MSG-12345";
    private static final String PATIENT_ID = "patient-456";
    private static final String TOPIC = "ai.agent.decisions.test." + UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createTopicIfMissing();
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
        awaitAssignment();
    }

    private void createTopicIfMissing() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.createTopics(Collections.singletonList(new NewTopic(TOPIC, 1, (short) 1)))
                .all()
                .get();
        } catch (Exception e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new RuntimeException("Failed to create test topic " + TOPIC, e);
            }
        }
    }

    private void awaitAssignment() {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
        while (consumer.assignment().isEmpty() && System.currentTimeMillis() < deadline) {
            consumer.poll(Duration.ofMillis(200));
        }
        if (consumer.assignment().isEmpty()) {
            throw new IllegalStateException("Kafka consumer assignment timed out for topic: " + TOPIC);
        }
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    private JsonNode awaitEvent(Predicate<JsonNode> matcher) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            for (ConsumerRecord<String, String> record : records) {
                JsonNode event = objectMapper.readTree(record.value());
                if (matcher.test(event)) {
                    return event;
                }
            }
        }
        return null;
    }

    private int[] awaitWorkflowCounts(String correlationId, long timeoutSeconds) throws Exception {
        int ingestCount = 0;
        int transformCount = 0;
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(timeoutSeconds).toMillis();
        while (System.currentTimeMillis() < deadline && (ingestCount < 2 || transformCount < 1)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            for (ConsumerRecord<String, String> record : records) {
                JsonNode event = objectMapper.readTree(record.value());
                if (!correlationId.equals(event.path("correlationId").asText())) {
                    continue;
                }
                String decisionType = event.path("decisionType").asText();
                if ("CDR_INGEST".equals(decisionType)) {
                    ingestCount++;
                } else if ("CDR_TRANSFORM".equals(decisionType)) {
                    transformCount++;
                }
            }
        }
        return new int[] { ingestCount, transformCount };
    }

    @Test
    @DisplayName("Should publish successful HL7 message ingest event to Kafka")
    void shouldPublishSuccessfulHl7IngestEvent() throws Exception {
        // Arrange
        String messageType = "ADT^A01"; // Admit patient
        String messageControlId = MESSAGE_ID + "-success";
        int segmentCount = 8;

        // Act
        auditIntegration.publishHl7MessageIngestEvent(
                TENANT_ID,
                messageType,
                messageControlId,
                PATIENT_ID,
                segmentCount,
                true,
                null,
                180L,
                "interface-engine"
        );

        // Assert
        JsonNode event = awaitEvent(node ->
                messageControlId.equals(node.path("inputMetrics").path("messageControlId").asText()));
        assertThat(event).isNotNull();

        assertThat(event.get("agentType").asText()).isEqualTo("PHI_ACCESS");
        assertThat(event.get("decisionType").asText()).isEqualTo("CDR_INGEST");
        assertThat(event.get("outcome").asText()).isEqualTo("APPROVED");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("messageType").asText()).isEqualTo(messageType);
        assertThat(metrics.get("messageControlId").asText()).isEqualTo(messageControlId);
        assertThat(metrics.get("segmentCount").asInt()).isEqualTo(segmentCount);
        assertThat(metrics.get("ingestSuccess").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Should publish failed HL7 message ingest event to Kafka")
    void shouldPublishFailedHl7IngestEvent() throws Exception {
        // Arrange
        String errorMessage = "Invalid MSH segment: missing required field";
        String failedMessageId = MESSAGE_ID + "-failed";

        // Act
        auditIntegration.publishHl7MessageIngestEvent(
                TENANT_ID,
                "UNKNOWN",
                failedMessageId,
                null,
                0,
                false,
                errorMessage,
                50L,
                "interface-engine"
        );

        // Assert
        JsonNode event = awaitEvent(node ->
                failedMessageId.equals(node.path("inputMetrics").path("messageControlId").asText()));
        assertThat(event).isNotNull();

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("ingestSuccess").asBoolean()).isFalse();
        assertThat(metrics.get("errorMessage").asText()).isEqualTo(errorMessage);
        assertThat(metrics.get("segmentCount").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should publish HL7 message ingest event to Kafka")
    void shouldPublishHl7MessageIngestEvent() throws Exception {
        // Arrange
        String messageType = "ORU^R01"; // Lab results
        String messageControlId = MESSAGE_ID + "-general";
        int segmentCount = 10;

        // Act
        auditIntegration.publishHl7MessageIngestEvent(
                TENANT_ID,
                messageType,
                messageControlId,
                PATIENT_ID,
                segmentCount,
                true,
                null,
                250L,
                "ingest-service"
        );

        // Assert
        JsonNode event = awaitEvent(node ->
                messageControlId.equals(node.path("inputMetrics").path("messageControlId").asText()));
        assertThat(event).isNotNull();

        assertThat(event.get("decisionType").asText()).isEqualTo("CDR_INGEST");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("messageType").asText()).isEqualTo(messageType);
        assertThat(metrics.get("segmentCount").asInt()).isEqualTo(segmentCount);
    }

    @Test
    @DisplayName("Should publish CDA document ingest event to Kafka")
    void shouldPublishCdaDocumentIngestEvent() throws Exception {
        // Arrange
        String documentId = "CDA-DOC-789";
        String documentType = "Continuity of Care Document";
        int resourceCount = 25;

        // Act
        auditIntegration.publishCdaDocumentIngestEvent(
                TENANT_ID,
                documentType,
                documentId,
                PATIENT_ID,
                resourceCount,
                true,
                null,
                400L,
                "cda-parser"
        );

        // Assert
        JsonNode event = awaitEvent(node ->
                documentId.equals(node.path("inputMetrics").path("documentId").asText()));
        assertThat(event).isNotNull();

        assertThat(event.get("decisionType").asText()).isEqualTo("CDR_INGEST");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("documentId").asText()).isEqualTo(documentId);
        assertThat(metrics.get("documentType").asText()).isEqualTo(documentType);
        assertThat(metrics.get("resourceCount").asInt()).isEqualTo(resourceCount);
    }

    @Test
    @DisplayName("Should publish data transformation event to Kafka")
    void shouldPublishDataTransformationEvent() throws Exception {
        // Arrange
        String sourceFormat = "HL7v2";
        String targetFormat = "FHIR_R4";
        String transformId = MESSAGE_ID + "-transform";
        int resourcesConverted = 15;

        // Act
        auditIntegration.publishDataTransformationEvent(
                TENANT_ID,
                sourceFormat,
                targetFormat,
                transformId,
                resourcesConverted,
                true,
                null,
                500L,
                "transformer"
        );

        // Assert
        JsonNode event = awaitEvent(node ->
                transformId.equals(node.path("inputMetrics").path("sourceIdentifier").asText()));
        assertThat(event).isNotNull();

        assertThat(event.get("decisionType").asText()).isEqualTo("CDR_TRANSFORM");
        assertThat(event.get("outcome").asText()).isEqualTo("APPROVED");

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.get("sourceFormat").asText()).isEqualTo(sourceFormat);
        assertThat(metrics.get("targetFormat").asText()).isEqualTo(targetFormat);
        assertThat(metrics.get("transformedResourceCount").asInt()).isEqualTo(resourcesConverted);
    }

    @Test
    @DisplayName("Should handle high-volume HL7 message processing")
    void shouldHandleHighVolumeHL7Messages() throws Exception {
        // Arrange - Simulate processing 100 HL7 messages
        int messageCount = 100;
        String bulkPrefix = MESSAGE_ID + "-bulk-";

        // Act
        for (int i = 0; i < messageCount; i++) {
            String msgType = i % 2 == 0 ? "ADT^A01" : "ORU^R01";
            auditIntegration.publishHl7MessageIngestEvent(
                    TENANT_ID,
                    msgType,
                    bulkPrefix + i,
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
            for (ConsumerRecord<String, String> record : records) {
                JsonNode event = objectMapper.readTree(record.value());
                String correlationId = event.path("correlationId").asText();
                if (correlationId.startsWith(bulkPrefix)) {
                    receivedCount++;
                }
            }
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
                    MESSAGE_ID + "-perf-" + msgInfo[0],
                    PATIENT_ID,
                    10,
                    true,
                    null,
                    Long.parseLong(msgInfo[1]),
                    "perf-test"
            );
            Thread.sleep(50); // Small delay to ensure ordering
        }

        // Assert - Verify different processing times for each message type
        Set<String> expectedTypes = Arrays.stream(messageTypes)
                .map(msgInfo -> msgInfo[0])
                .collect(java.util.stream.Collectors.toSet());
        Map<String, JsonNode> eventsByType = new HashMap<>();
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline && eventsByType.size() < expectedTypes.size()) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            for (ConsumerRecord<String, String> record : records) {
                JsonNode event = objectMapper.readTree(record.value());
                String messageType = event.path("inputMetrics").path("messageType").asText(null);
                if (messageType != null && expectedTypes.contains(messageType)) {
                    eventsByType.putIfAbsent(messageType, event);
                }
            }
        }

        assertThat(eventsByType.keySet()).containsExactlyInAnyOrderElementsOf(expectedTypes);
        for (JsonNode event : eventsByType.values()) {
            long processingTime = event.path("inferenceTimeMs").asLong();
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

        // Step 2: Ingest CDA Document
        Thread.sleep(50);
        auditIntegration.publishCdaDocumentIngestEvent(
                TENANT_ID, "CCD", workflowMessageId, PATIENT_ID,
                5, true, null, 150L, "ingest"
        );

        // Step 3: Transform Data
        Thread.sleep(50);
        auditIntegration.publishDataTransformationEvent(
                TENANT_ID, "HL7v2", "FHIR_R4", workflowMessageId,
                5, true, null, 200L, "transform"
        );

        // Assert - All 3 events published
        int[] counts = awaitWorkflowCounts(workflowMessageId, 20);
        assertThat(counts[0]).isGreaterThanOrEqualTo(2);
        assertThat(counts[1]).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should track data quality metrics in audit events")
    void shouldTrackDataQualityMetrics() throws Exception {
        // Arrange - Transformation with quality metrics
        auditIntegration.publishDataTransformationEvent(
                TENANT_ID,
                "HL7v2",
                "FHIR_R4",
                MESSAGE_ID + "-quality",
                10,
                true,
                null,
                250L,
                "transform"
        );

        // Assert
        String qualityId = MESSAGE_ID + "-quality";
        JsonNode event = awaitEvent(node ->
                qualityId.equals(node.path("correlationId").asText()));
        assertThat(event).isNotNull();

        JsonNode metrics = event.get("inputMetrics");
        assertThat(metrics.has("transformedResourceCount")).isTrue();
        assertThat(metrics.get("transformSuccess").asBoolean()).isTrue();
    }
}
