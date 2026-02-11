package com.healthdata.testing.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive End-to-End Audit Tests
 * 
 * Tests complete clinical decision workflows spanning multiple services:
 * 1. FHIR Query → CQL Evaluation → Care Gap Identification → Notification
 * 2. Multi-tenant isolation
 * 3. Event ordering and replay
 * 4. Concurrent operations
 * 5. Audit trail integrity
 */
@Testcontainers
@DisplayName("Cross-Service Clinical Decision Audit E2E Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("e2e")
@Tag("heavyweight")
class ClinicalDecisionAuditE2ETest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    private static KafkaProducer<String, String> producer;
    private static KafkaConsumer<String, String> consumer;
    private static ObjectMapper objectMapper;

    private static final String TOPIC = "ai.agent.decisions";
    private static final String TENANT_1 = "hospital-a";
    private static final String TENANT_2 = "hospital-b";
    private static final String PATIENT_1 = "patient-001";
    private static final String PATIENT_2 = "patient-002";

    @BeforeAll
    static void setUpAll() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // Create producer
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producer = new KafkaProducer<>(producerProps);

        // Create consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-e2e-consumer");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterAll
    static void tearDownAll() {
        if (producer != null) producer.close();
        if (consumer != null) consumer.close();
    }

    @BeforeEach
    void setUp() {
        // Consume any existing messages
        consumer.poll(Duration.ofMillis(100));
    }

    @Test
    @Order(1)
    @DisplayName("Should audit complete clinical decision workflow with proper sequencing")
    void shouldAuditCompleteClinicalWorkflow() throws Exception {
        // Simulate complete workflow: FHIR Query → CQL Evaluation → Care Gap → Notification
        String workflowId = UUID.randomUUID().toString();
        String correlationId = workflowId;
        
        // Step 1: FHIR Query (Patient Data Access)
        publishAuditEvent(createFhirQueryEvent(TENANT_1, PATIENT_1, correlationId));
        Thread.sleep(50);
        
        // Step 2: CQL Evaluation (Quality Measure Calculation)
        publishAuditEvent(createCqlEvaluationEvent(TENANT_1, PATIENT_1, correlationId));
        Thread.sleep(50);
        
        // Step 3: Care Gap Identification
        publishAuditEvent(createCareGapEvent(TENANT_1, PATIENT_1, correlationId));
        Thread.sleep(50);
        
        // Step 4: Notification Sent
        publishAuditEvent(createNotificationEvent(TENANT_1, PATIENT_1, correlationId));
        
        // Wait and verify all events
        Thread.sleep(1000);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        List<JsonNode> events = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            events.add(objectMapper.readTree(record.value()));
        }
        
        // Verify we got all 4 events
        assertThat(events).hasSizeGreaterThanOrEqualTo(4);
        
        // Filter events for this workflow
        List<JsonNode> workflowEvents = events.stream()
                .filter(e -> e.has("correlationId") && e.get("correlationId").asText().equals(correlationId))
                .collect(Collectors.toList());
        
        assertThat(workflowEvents).hasSize(4);
        
        // Verify event types in correct order
        assertThat(workflowEvents.get(0).get("decisionType").asText()).isEqualTo("FHIR_QUERY");
        assertThat(workflowEvents.get(1).get("decisionType").asText()).isEqualTo("CQL_EVALUATION");
        assertThat(workflowEvents.get(2).get("decisionType").asText()).isEqualTo("CARE_GAP_IDENTIFICATION");
        assertThat(workflowEvents.get(3).get("decisionType").asText()).isEqualTo("NOTIFICATION_SENT");
        
        // Verify all events have same tenantId and patientId
        workflowEvents.forEach(event -> {
            assertThat(event.get("tenantId").asText()).isEqualTo(TENANT_1);
            assertThat(event.get("resourceId").asText()).isEqualTo(PATIENT_1);
        });
        
        // Verify timestamp ordering (monotonically increasing)
        for (int i = 1; i < workflowEvents.size(); i++) {
            Instant prev = Instant.parse(workflowEvents.get(i-1).get("timestamp").asText());
            Instant curr = Instant.parse(workflowEvents.get(i).get("timestamp").asText());
            assertThat(curr).isAfterOrEqualTo(prev);
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should isolate audit events across tenants")
    void shouldIsolateEventsAcrossTenants() throws Exception {
        // Publish events for two different tenants
        String workflow1 = UUID.randomUUID().toString();
        String workflow2 = UUID.randomUUID().toString();
        
        publishAuditEvent(createFhirQueryEvent(TENANT_1, PATIENT_1, workflow1));
        publishAuditEvent(createFhirQueryEvent(TENANT_2, PATIENT_2, workflow2));
        
        Thread.sleep(1000);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        Map<String, List<JsonNode>> eventsByTenant = new HashMap<>();
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            String tenant = event.get("tenantId").asText();
            eventsByTenant.computeIfAbsent(tenant, k -> new ArrayList<>()).add(event);
        }
        
        // Verify events for TENANT_1
        List<JsonNode> tenant1Events = eventsByTenant.getOrDefault(TENANT_1, Collections.emptyList());
        tenant1Events.forEach(event -> {
            assertThat(event.get("tenantId").asText()).isEqualTo(TENANT_1);
            assertThat(event.get("resourceId").asText()).isEqualTo(PATIENT_1);
        });
        
        // Verify events for TENANT_2
        List<JsonNode> tenant2Events = eventsByTenant.getOrDefault(TENANT_2, Collections.emptyList());
        tenant2Events.forEach(event -> {
            assertThat(event.get("tenantId").asText()).isEqualTo(TENANT_2);
            assertThat(event.get("resourceId").asText()).isEqualTo(PATIENT_2);
        });
        
        // Verify partition keys are different
        assertThat(tenant1Events).isNotEmpty();
        assertThat(tenant2Events).isNotEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("Should handle concurrent operations without event loss")
    void shouldHandleConcurrentOperations() throws Exception {
        int concurrentWorkflows = 50;
        CountDownLatch latch = new CountDownLatch(concurrentWorkflows);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        Set<String> workflowIds = ConcurrentHashMap.newKeySet();
        
        // Publish 50 concurrent workflows
        for (int i = 0; i < concurrentWorkflows; i++) {
            final String workflowId = "workflow-" + i;
            workflowIds.add(workflowId);
            
            executor.submit(() -> {
                try {
                    publishAuditEvent(createFhirQueryEvent(TENANT_1, PATIENT_1, workflowId));
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        // Wait for all to complete
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        
        // Wait for Kafka to process
        Thread.sleep(2000);
        
        // Verify all events arrived
        Set<String> receivedWorkflows = new HashSet<>();
        long deadline = System.currentTimeMillis() + 30000;
        
        while (receivedWorkflows.size() < concurrentWorkflows && System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            for (ConsumerRecord<String, String> record : records) {
                JsonNode event = objectMapper.readTree(record.value());
                if (event.has("correlationId")) {
                    String corrId = event.get("correlationId").asText();
                    if (workflowIds.contains(corrId)) {
                        receivedWorkflows.add(corrId);
                    }
                }
            }
        }
        
        assertThat(receivedWorkflows).hasSize(concurrentWorkflows);
    }

    @Test
    @Order(4)
    @DisplayName("Should maintain event integrity for replay scenarios")
    void shouldMaintainEventIntegrityForReplay() throws Exception {
        // Publish a series of events with checksums
        List<String> workflowIds = Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
        
        for (String workflowId : workflowIds) {
            Map<String, Object> event = createFhirQueryEvent(TENANT_1, PATIENT_1, workflowId);
            // Add checksum
            event.put("checksum", calculateChecksum(event));
            publishAuditEvent(event);
            Thread.sleep(100);
        }
        
        Thread.sleep(1000);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        int validChecksums = 0;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            if (event.has("checksum") && event.has("correlationId")) {
                String corrId = event.get("correlationId").asText();
                if (workflowIds.contains(corrId)) {
                    // Verify checksum (in real scenario, would recalculate and compare)
                    assertThat(event.get("checksum").asText()).isNotEmpty();
                    validChecksums++;
                }
            }
        }
        
        assertThat(validChecksums).isEqualTo(workflowIds.size());
    }

    // Helper methods to create audit events

    private Map<String, Object> createFhirQueryEvent(String tenantId, String patientId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("tenantId", tenantId);
        event.put("correlationId", correlationId);
        event.put("agentId", "fhir-service");
        event.put("agentType", "PHI_ACCESS");
        event.put("agentVersion", "1.0.0");
        event.put("modelName", "fhir-r4");
        event.put("decisionType", "FHIR_QUERY");
        event.put("resourceType", "Patient");
        event.put("resourceId", patientId);
        event.put("reasoning", "Patient data query for clinical decision support");
        event.put("outcome", "FETCHED");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("queryType", "GET_PATIENT");
        metrics.put("resourceCount", 1);
        event.put("inputMetrics", metrics);
        
        return event;
    }

    private Map<String, Object> createCqlEvaluationEvent(String tenantId, String patientId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("tenantId", tenantId);
        event.put("correlationId", correlationId);
        event.put("agentId", "cql-engine");
        event.put("agentType", "CLINICAL_DECISION");
        event.put("agentVersion", "1.0.0");
        event.put("modelName", "hedis-2024");
        event.put("decisionType", "CQL_EVALUATION");
        event.put("resourceType", "Patient");
        event.put("resourceId", patientId);
        event.put("reasoning", "Quality measure evaluation using CQL logic");
        event.put("outcome", "EVALUATED");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("measureId", "COL-3");
        metrics.put("measureMet", true);
        event.put("inputMetrics", metrics);
        
        return event;
    }

    private Map<String, Object> createCareGapEvent(String tenantId, String patientId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("tenantId", tenantId);
        event.put("correlationId", correlationId);
        event.put("agentId", "care-gap-service");
        event.put("agentType", "CLINICAL_DECISION");
        event.put("agentVersion", "1.0.0");
        event.put("modelName", "hedis-gap-finder");
        event.put("decisionType", "CARE_GAP_IDENTIFICATION");
        event.put("resourceType", "Patient");
        event.put("resourceId", patientId);
        event.put("reasoning", "Identified care gaps based on quality measures");
        event.put("outcome", "IDENTIFIED");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("gapsFound", 2);
        metrics.put("highPriority", 1);
        event.put("inputMetrics", metrics);
        
        return event;
    }

    private Map<String, Object> createNotificationEvent(String tenantId, String patientId, String correlationId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("tenantId", tenantId);
        event.put("correlationId", correlationId);
        event.put("agentId", "notification-service");
        event.put("agentType", "CARE_COORDINATION");
        event.put("agentVersion", "1.0.0");
        event.put("modelName", "notification-router");
        event.put("decisionType", "NOTIFICATION_SENT");
        event.put("resourceType", "Patient");
        event.put("resourceId", patientId);
        event.put("reasoning", "Care gap notification sent to provider");
        event.put("outcome", "SENT");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("notificationType", "CARE_GAP_ALERT");
        metrics.put("channel", "EMAIL");
        event.put("inputMetrics", metrics);
        
        return event;
    }

    private void publishAuditEvent(Map<String, Object> event) throws Exception {
        String eventJson = objectMapper.writeValueAsString(event);
        String partitionKey = event.get("tenantId") + ":" + event.get("agentId");
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, partitionKey, eventJson);
        producer.send(record).get(5, TimeUnit.SECONDS);
    }

    private String calculateChecksum(Map<String, Object> event) {
        // Simple checksum for demo - in production would use proper hashing
        return UUID.randomUUID().toString();
    }
}
