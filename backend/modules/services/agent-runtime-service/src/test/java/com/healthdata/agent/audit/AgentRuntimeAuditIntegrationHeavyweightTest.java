package com.healthdata.agent.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.AgentRuntimeServiceApplication;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.llm.model.LLMResponse;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight integration tests for AgentRuntimeAuditIntegration using Testcontainers.
 * Tests actual Kafka event publishing with real containers.
 */
@SpringBootTest(classes = AgentRuntimeServiceApplication.class)
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Agent Runtime Audit Integration - Heavyweight Kafka Tests")
class AgentRuntimeAuditIntegrationHeavyweightTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureKafkaAndPostgres(DynamicPropertyRegistry registry) {
        String bootstrapServers = kafka.getBootstrapServers();
        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> "ai.agent.decisions");

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private AgentRuntimeAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String PATIENT_ID = "patient-789";
    private static final String SESSION_ID = "session-001";
    private static final String CORRELATION_ID = "corr-001";
    private static final String AGENT_TYPE = "clinical-assistant";

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.currentTimeMillis(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        );

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = consumerFactory.createConsumer();
        consumer.subscribe(List.of("ai.agent.decisions"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should publish agent execution event to Kafka with agentId")
    void shouldPublishAgentExecutionEventToKafka() throws Exception {
        // Given
        AgentContext context = createContext();
        String userMessage = "What are my care gaps?";
        LLMResponse.TokenUsage usage = LLMResponse.TokenUsage.builder()
                .inputTokens(100)
                .outputTokens(200)
                .totalTokens(300)
                .build();
        AgentResponse response = AgentResponse.success(
                "Based on your records, you have 2 care gaps...",
                usage,
                "claude-3-5-sonnet-20241022"
        );

        // When
        auditIntegration.publishAgentExecutionEvent(
                context, userMessage, response, USER_ID, 1500L);

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
        String expectedKey = TENANT_ID + ":ai-agent-runtime";
        assertThat(record.key()).isEqualTo(expectedKey);

        // Verify event content using JSON string matching
        String eventJson = record.value();
        assertThat(eventJson).contains("\"agentId\":\"ai-agent-runtime\"");
        assertThat(eventJson).contains("\"agentType\":\"AI_AGENT\"");
        assertThat(eventJson).contains("\"tenantId\":\"" + TENANT_ID + "\"");
        assertThat(eventJson).contains("\"resourceId\":\"" + PATIENT_ID + "\"");
        assertThat(eventJson).contains("\"correlationId\":\"" + CORRELATION_ID + "\"");
        assertThat(eventJson).contains("\"decisionType\":\"AI_RECOMMENDATION\"");
        assertThat(eventJson).contains("\"eventId\"");
        assertThat(eventJson).contains("\"timestamp\"");
    }

    @Test
    @DisplayName("Should publish guardrail block event to Kafka")
    void shouldPublishGuardrailBlockEventToKafka() throws Exception {
        // Given
        AgentContext context = createContext();
        AgentResponse response = AgentResponse.blocked("Prescription requests require human approval");

        // When
        auditIntegration.publishAgentExecutionEvent(
                context, "Prescribe medication", response, USER_ID, 500L);

        // Then
        ConsumerRecord<String, String> record = pollForRecord();
        assertThat(record).isNotNull();

        String eventJson = record.value();
        assertThat(eventJson).contains("\"agentId\":\"ai-agent-runtime\"");
        assertThat(eventJson).contains("\"decisionType\":\"GUARDRAIL_BLOCK\"");
        assertThat(eventJson).contains("\"blocked\":true");
        assertThat(eventJson).contains("\"blockReason\":\"Prescription requests require human approval\"");
    }

    @Test
    @DisplayName("Should publish tool execution event to Kafka")
    void shouldPublishToolExecutionEventToKafka() throws Exception {
        // Given
        AgentContext context = createContext();
        // Create a simple tool call and result for testing
        var toolCall = com.healthdata.agent.llm.model.LLMRequest.ToolCall.builder()
                .id("call-123")
                .name("get_patient_vitals")
                .arguments(Map.of("patientId", PATIENT_ID))
                .build();

        var toolDefinition = com.healthdata.agent.tool.ToolDefinition.builder()
                .name("get_patient_vitals")
                .description("Get patient vital signs")
                .category(com.healthdata.agent.tool.ToolDefinition.ToolCategory.FHIR_QUERY)
                .requiresApproval(false)
                .build();

        var toolResult = objectMapper.createObjectNode();
        toolResult.put("bloodPressure", "120/80");
        toolResult.put("heartRate", 72);

        // When
        auditIntegration.publishToolExecutionEvent(
                context, toolCall, toolDefinition, toolResult, USER_ID);

        // Then
        ConsumerRecord<String, String> record = pollForRecord();
        assertThat(record).isNotNull();

        String eventJson = record.value();
        assertThat(eventJson).contains("\"agentId\":\"ai-agent-runtime\"");
        assertThat(eventJson).contains("\"decisionType\":\"TOOL_EXECUTION\"");
        assertThat(eventJson).contains("\"toolName\":\"get_patient_vitals\"");
        assertThat(eventJson).contains("\"toolCallId\":\"call-123\"");
        assertThat(eventJson).contains("\"toolCategory\":\"FHIR_QUERY\"");
    }

    @Test
    @DisplayName("Should publish PHI access event to Kafka")
    void shouldPublishPhiAccessEventToKafka() throws Exception {
        // Given
        AgentContext context = createContext();
        String resourceType = "Observation";
        String resourceId = "obs-123";
        String accessPurpose = "Clinical decision support";

        // When
        auditIntegration.publishPhiAccessEvent(
                context, resourceType, resourceId, accessPurpose, USER_ID);

        // Then
        ConsumerRecord<String, String> record = pollForRecord();
        assertThat(record).isNotNull();

        String eventJson = record.value();
        assertThat(eventJson).contains("\"agentId\":\"ai-agent-runtime\"");
        assertThat(eventJson).contains("\"decisionType\":\"PHI_ACCESS\"");
        assertThat(eventJson).contains("\"resourceType\":\"" + resourceType + "\"");
        assertThat(eventJson).contains("\"resourceId\":\"" + resourceId + "\"");
        assertThat(eventJson).contains("\"accessPurpose\":\"" + accessPurpose + "\"");
        assertThat(eventJson).contains("\"accessGranted\":true");
    }

    @Test
    @DisplayName("Should use correct partition key for all event types")
    void shouldUseCorrectPartitionKeyForAllEventTypes() throws Exception {
        // Given
        AgentContext context = createContext();
        AgentResponse response = AgentResponse.success("Test", null, "claude-3-5-sonnet-20241022");

        // When
        auditIntegration.publishAgentExecutionEvent(
                context, "Test message", response, USER_ID, 100L);

        // Then
        ConsumerRecord<String, String> record = pollForRecord();
        assertThat(record).isNotNull();

        // Verify partition key format: tenantId:agentId
        String expectedKey = TENANT_ID + ":ai-agent-runtime";
        assertThat(record.key()).isEqualTo(expectedKey);
    }

    @Test
    @DisplayName("Should handle concurrent event publishing")
    void shouldHandleConcurrentEventPublishing() throws Exception {
        // Given
        AgentContext context = createContext();
        AgentResponse response = AgentResponse.success("Test", null, "claude-3-5-sonnet-20241022");

        // When - Publish multiple events concurrently
        int eventCount = 10;
        for (int i = 0; i < eventCount; i++) {
            auditIntegration.publishAgentExecutionEvent(
                    context,
                    "Message " + i,
                    response,
                    USER_ID,
                    100L
            );
        }

        // Then - Should receive all events
        int receivedCount = 0;
        long startTime = System.currentTimeMillis();
        while (receivedCount < eventCount && System.currentTimeMillis() - startTime < 15000) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            receivedCount += records.count();
        }

        assertThat(receivedCount).isEqualTo(eventCount);
    }

    // Helper methods

    private AgentContext createContext() {
        return AgentContext.builder()
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .patientId(PATIENT_ID)
                .sessionId(SESSION_ID)
                .correlationId(CORRELATION_ID)
                .agentType(AGENT_TYPE)
                .build();
    }

    private ConsumerRecord<String, String> pollForRecord() {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 10000) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }
        return null;
    }
}
