package com.healthdata.cql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.cql.measure.MeasureResult;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight Integration Test for CQL Audit Integration with real Kafka.
 * 
 * This test requires Docker and uses Testcontainers to spin up a real Kafka instance.
 * It verifies end-to-end audit event publishing to Kafka.
 * 
 * Test Categories:
 * - Lightweight: CqlAuditIntegrationTest (uses mocks, no Docker)
 * - Heavyweight: This class (uses real Kafka via Testcontainers, requires Docker)
 * 
 * To run:
 * - Ensure Docker is running
 * - Run: ./gradlew test --tests CqlAuditIntegrationHeavyweightTest
 */
@SpringBootTest(
    classes = com.healthdata.cql.CqlEngineServiceApplication.class,
    properties = {
        "spring.test.context.cache.maxSize=1"
    }
)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@ActiveProfiles("test")
@Testcontainers
@DisplayName("CQL Audit Integration - Heavyweight Kafka Tests")
@ComponentScan(
    basePackages = {"com.healthdata.cql", "com.healthdata.audit.service.ai"},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.healthdata.cql.TestCqlEngineApplication.class
    )
)
class CqlAuditIntegrationHeavyweightTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Kafka properties
        String bootstrapServers = kafka.getBootstrapServers();
        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> "ai.agent.decisions");
    }

    @Autowired
    private CqlAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "test-tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final String MEASURE_ID = "HEDIS_CDC_A1C";
    private static final String EVALUATION_ID = "eval-789";
    private static final String TOPIC = "ai.agent.decisions";

    @BeforeEach
    void setUp() {
        // ObjectMapper is now autowired from Spring, so it's configured the same way as the publisher
        
        // Create Kafka consumer to verify published events
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
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
    @DisplayName("Should publish CQL evaluation event to Kafka with agentId")
    void shouldPublishCqlEvaluationEventToKafka() throws Exception {
        // Given
        MeasureResult measureResult = createMeasureResult(true, false);

        // When
        auditIntegration.publishCqlEvaluationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                measureResult, "user@example.com", 150L);

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
        String expectedKey = TENANT_ID + ":cql-engine";
        assertThat(record.key()).isEqualTo(expectedKey);
        
        // Verify event content by JSON string matching (simpler than full deserialization)
        String jsonValue = record.value();
        assertThat(jsonValue).contains("cql-engine");
        assertThat(jsonValue).contains("CQL_ENGINE");
        assertThat(jsonValue).contains(TENANT_ID);
        assertThat(jsonValue).contains(PATIENT_ID);
        assertThat(jsonValue).contains(EVALUATION_ID);
        assertThat(jsonValue).contains("MEASURE_NOT_MET");
        assertThat(jsonValue).contains("eventId");
        assertThat(jsonValue).contains("timestamp");
    }

    @Test
    @DisplayName("Should publish batch evaluation event to Kafka with agentId")
    void shouldPublishBatchEvaluationEventToKafka() throws Exception {
        // Given
        String batchId = "batch-123";

        // When
        auditIntegration.publishBatchEvaluationEvent(
                TENANT_ID, PATIENT_ID, batchId, 10, 8, 2, "user@example.com");

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
        
        // Verify partition key
        String expectedKey = TENANT_ID + ":cql-engine";
        assertThat(record.key()).isEqualTo(expectedKey);
        
        // Verify event content by JSON string matching
        String jsonValue = record.value();
        assertThat(jsonValue).contains("cql-engine");
        assertThat(jsonValue).contains("CQL_ENGINE");
        assertThat(jsonValue).contains("BATCH_EVALUATION");
        assertThat(jsonValue).contains(batchId);
        assertThat(jsonValue).contains("0.8"); // 8/10 success rate
    }

    @Test
    @DisplayName("Should use correct partition key format: tenantId:agentId")
    void shouldUseCorrectPartitionKeyFormat() throws Exception {
        // Given
        MeasureResult measureResult = createMeasureResult(true, false);
        String tenantId1 = "tenant-1";
        String tenantId2 = "tenant-2";

        // When
        auditIntegration.publishCqlEvaluationEvent(
                tenantId1, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                measureResult, "user1@example.com", 150L);
        auditIntegration.publishCqlEvaluationEvent(
                tenantId2, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                measureResult, "user2@example.com", 150L);

        // Then - Verify partition keys
        long startTime = System.currentTimeMillis();
        boolean foundTenant1 = false;
        boolean foundTenant2 = false;
        
        while (System.currentTimeMillis() - startTime < 10000 && (!foundTenant1 || !foundTenant2)) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                String key = record.key();
                if (key.equals(tenantId1 + ":cql-engine")) {
                    foundTenant1 = true;
                }
                if (key.equals(tenantId2 + ":cql-engine")) {
                    foundTenant2 = true;
                }
            }
        }
        
        assertThat(foundTenant1).isTrue();
        assertThat(foundTenant2).isTrue();
    }

    // Helper method to create MeasureResult for testing
    private MeasureResult createMeasureResult(boolean inDenominator, boolean inNumerator) {
        MeasureResult result = new MeasureResult();
        result.setInDenominator(inDenominator);
        result.setInNumerator(inNumerator);
        result.setDetails(new HashMap<>());
        result.getDetails().put("testKey", "testValue");
        return result;
    }
}

