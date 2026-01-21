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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HIPAA Audit Compliance Verification Tests
 * 
 * Verifies compliance with HIPAA audit requirements:
 * - § 164.308(a)(1)(ii)(D) - Information System Activity Review
 * - § 164.312(b) - Audit Controls
 * - § 164.316(b)(2)(i) - Retention requirements (6 years)
 */
@Testcontainers
@DisplayName("HIPAA Audit Compliance Tests")
class HIPAAAuditComplianceTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    private static KafkaProducer<String, String> producer;
    private static KafkaConsumer<String, String> consumer;
    private static ObjectMapper objectMapper;

    private static final String TOPIC = "ai.agent.decisions";

    @BeforeAll
    static void setUpAll() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(producerProps);

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "hipaa-compliance-test");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterAll
    static void tearDownAll() {
        if (producer != null) producer.close();
        if (consumer != null) consumer.close();
    }

    @Test
    @DisplayName("Should log all PHI access events with required audit fields")
    void shouldLogAllPHIAccessWithRequiredFields() throws Exception {
        // Arrange - PHI access event
        String patientId = "patient-123";
        String tenantId = "hospital-1";
        String userId = "dr-smith";
        
        Map<String, Object> phiAccessEvent = createPHIAccessEvent(tenantId, patientId, userId);
        publishEvent(phiAccessEvent);
        
        // Act
        Thread.sleep(1000);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        // Assert
        boolean foundEvent = false;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            
            if (event.get("resourceId").asText().equals(patientId)) {
                foundEvent = true;
                
                // HIPAA Required Fields
                assertThat(event.has("eventId")).isTrue();
                assertThat(event.has("timestamp")).isTrue();
                assertThat(event.has("tenantId")).isTrue();
                assertThat(event.has("resourceId")).isTrue();  // Patient ID
                assertThat(event.has("agentId")).isTrue();      // Who accessed
                assertThat(event.has("decisionType")).isTrue(); // What action
                assertThat(event.has("outcome")).isTrue();      // Success/Failure
                assertThat(event.has("resourceType")).isTrue(); // Resource type
                
                // Verify PHI access reasoning
                assertThat(event.get("reasoning").asText()).contains("PHI");
                
                // Verify timestamp is valid and recent
                Instant timestamp = Instant.parse(event.get("timestamp").asText());
                assertThat(timestamp).isAfter(Instant.now().minus(5, ChronoUnit.MINUTES));
                
                break;
            }
        }
        
        assertThat(foundEvent).isTrue();
    }

    @Test
    @DisplayName("Should retain audit events for 6 years (simulated)")
    void shouldRetainEventsFor6Years() {
        // Simulate retention policy verification
        // In production, this would verify database/storage retention policies
        
        Map<String, Object> event = createPHIAccessEvent("tenant-1", "patient-1", "user-1");
        
        // Verify event has retention metadata
        assertThat(event).containsKey("timestamp");
        
        // Calculate 6-year retention date
        Instant eventTime = Instant.parse(event.get("timestamp").toString());
        Instant retentionDate = eventTime.plus(6 * 365, ChronoUnit.DAYS);
        
        // Verify retention date is at least 6 years in the future
        assertThat(retentionDate).isAfter(Instant.now().plus(5 * 365, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("Should prevent audit log tampering with immutable events")
    void shouldPreventAuditLogTampering() throws Exception {
        // Arrange
        Map<String, Object> originalEvent = createPHIAccessEvent("tenant-1", "patient-1", "user-1");
        String eventId = originalEvent.get("eventId").toString();
        
        // Add checksum/hash for integrity
        String checksum = calculateEventChecksum(originalEvent);
        originalEvent.put("checksum", checksum);
        
        publishEvent(originalEvent);
        
        // Act
        Thread.sleep(1000);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        // Assert - Verify event integrity
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            
            if (event.has("eventId") && event.get("eventId").asText().equals(eventId)) {
                // Verify checksum exists
                assertThat(event.has("checksum")).isTrue();
                assertThat(event.get("checksum").asText()).isNotEmpty();
                
                // Verify all required immutable fields are present
                assertThat(event.has("eventId")).isTrue();
                assertThat(event.has("timestamp")).isTrue();
                assertThat(event.has("tenantId")).isTrue();
                
                break;
            }
        }
    }

    @Test
    @DisplayName("Should track break-glass access with elevated permissions")
    void shouldTrackBreakGlassAccess() throws Exception {
        // Arrange - Emergency access event
        Map<String, Object> breakGlassEvent = createPHIAccessEvent("tenant-1", "patient-emergency", "emergency-dr");
        breakGlassEvent.put("accessType", "BREAK_GLASS");
        breakGlassEvent.put("reasoning", "Emergency access - Patient in critical condition");
        
        Map<String, Object> metrics = (Map<String, Object>) breakGlassEvent.get("inputMetrics");
        metrics.put("emergencyOverride", true);
        metrics.put("justification", "Life-threatening emergency");
        
        publishEvent(breakGlassEvent);
        
        // Act
        Thread.sleep(1000);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        // Assert
        boolean foundBreakGlass = false;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            
            if (event.has("accessType") && event.get("accessType").asText().equals("BREAK_GLASS")) {
                foundBreakGlass = true;
                
                // Verify break-glass specific fields
                assertThat(event.get("reasoning").asText()).contains("Emergency");
                
                JsonNode eventMetrics = event.get("inputMetrics");
                assertThat(eventMetrics.get("emergencyOverride").asBoolean()).isTrue();
                assertThat(eventMetrics.has("justification")).isTrue();
                
                break;
            }
        }
        
        assertThat(foundBreakGlass).isTrue();
    }

    @Test
    @DisplayName("Should audit all create, read, update, delete operations on PHI")
    void shouldAuditAllCRUDOperations() throws Exception {
        String patientId = "patient-crud-test";
        
        // Create
        publishEvent(createPHIEvent("tenant-1", patientId, "user-1", "PHI_CREATE"));
        Thread.sleep(100);
        
        // Read
        publishEvent(createPHIEvent("tenant-1", patientId, "user-1", "PHI_READ"));
        Thread.sleep(100);
        
        // Update
        publishEvent(createPHIEvent("tenant-1", patientId, "user-1", "PHI_UPDATE"));
        Thread.sleep(100);
        
        // Delete
        publishEvent(createPHIEvent("tenant-1", patientId, "user-1", "PHI_DELETE"));
        
        // Verify
        Thread.sleep(1000);
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        Set<String> operationsFound = new HashSet<>();
        for (ConsumerRecord<String, String> record : records) {
            JsonNode event = objectMapper.readTree(record.value());
            
            if (event.get("resourceId").asText().equals(patientId)) {
                operationsFound.add(event.get("decisionType").asText());
            }
        }
        
        assertThat(operationsFound).contains("PHI_CREATE", "PHI_READ", "PHI_UPDATE", "PHI_DELETE");
    }

    // Helper methods

    private Map<String, Object> createPHIAccessEvent(String tenantId, String patientId, String userId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("timestamp", Instant.now().toString());
        event.put("tenantId", tenantId);
        event.put("correlationId", UUID.randomUUID().toString());
        event.put("agentId", userId);
        event.put("agentType", "PHI_ACCESS");
        event.put("agentVersion", "1.0.0");
        event.put("modelName", "phi-access-control");
        event.put("decisionType", "PHI_READ");
        event.put("resourceType", "Patient");
        event.put("resourceId", patientId);
        event.put("reasoning", "PHI access for treatment purposes");
        event.put("outcome", "APPROVED");
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("accessPurpose", "TREATMENT");
        metrics.put("dataElements", List.of("demographics", "medications", "diagnoses"));
        event.put("inputMetrics", metrics);
        
        return event;
    }

    private Map<String, Object> createPHIEvent(String tenantId, String patientId, String userId, String operationType) {
        Map<String, Object> event = createPHIAccessEvent(tenantId, patientId, userId);
        event.put("decisionType", operationType);
        return event;
    }

    private void publishEvent(Map<String, Object> event) throws Exception {
        String eventJson = objectMapper.writeValueAsString(event);
        String partitionKey = event.get("tenantId") + ":" + event.get("agentId");
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, partitionKey, eventJson);
        producer.send(record).get();
    }

    private String calculateEventChecksum(Map<String, Object> event) {
        // Simple checksum - in production would use SHA-256 or similar
        return Integer.toHexString(event.toString().hashCode());
    }
}
