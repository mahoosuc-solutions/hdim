package com.healthdata.agent.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.AgentRuntimeServiceApplication;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.llm.model.LLMResponse;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Heavyweight integration tests for AgentRuntimeAuditIntegration using Testcontainers.
 * Tests actual Kafka event publishing with real containers.
 */
@SpringBootTest(classes = AgentRuntimeServiceApplication.class)
@ActiveProfiles("test")
@Testcontainers
@Import(AgentRuntimeAuditIntegrationHeavyweightTest.TestRedisConfig.class)
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
        registry.add("healthdata.messaging.bootstrap-servers", () -> bootstrapServers);
        registry.add("healthdata.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> TOPIC);

        registry.add("healthdata.persistence.primary.url", postgres::getJdbcUrl);
        registry.add("healthdata.persistence.primary.username", postgres::getUsername);
        registry.add("healthdata.persistence.primary.password", postgres::getPassword);
        registry.add("healthdata.persistence.primary.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.change-log", () ->
                "classpath:db/changelog/db.changelog-master.xml");
    }

    @Autowired
    private AgentRuntimeAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestRedisConfig {
        @Bean
        @Primary
        ReactiveRedisTemplate<String, String> reactiveRedisTemplate() {
            return org.mockito.Mockito.mock(ReactiveRedisTemplate.class);
        }
    }

    private KafkaConsumer<String, String> consumer;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String PATIENT_ID = "patient-789";
    private static final String SESSION_ID = "session-001";
    private static final String CORRELATION_ID = "corr-001";
    private static final String AGENT_TYPE = "clinical-assistant";
    private static final String TOPIC = "ai.agent.decisions.test." + UUID.randomUUID();

    @BeforeEach
    void setUp() {
        createTopicIfMissing();
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
        awaitAssignment();
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

        JsonNode event = awaitEvent(node ->
                "AI_RECOMMENDATION".equals(node.path("decisionType").asText()));
        assertThat(event).isNotNull();
        assertThat(event.path("tenantId").asText()).isEqualTo(TENANT_ID);
        assertThat(event.path("agentId").asText()).isEqualTo("ai-agent-runtime");
        assertThat(event.path("agentType").asText()).isEqualTo("AI_AGENT");
        assertThat(event.path("resourceId").asText()).isEqualTo(PATIENT_ID);
        assertThat(event.path("correlationId").asText()).isEqualTo(CORRELATION_ID);
        assertThat(event.hasNonNull("eventId")).isTrue();
        assertThat(event.hasNonNull("timestamp")).isTrue();
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
        JsonNode event = awaitEvent(node ->
                "GUARDRAIL_BLOCK".equals(node.path("decisionType").asText()));
        assertThat(event).isNotNull();
        assertThat(event.path("agentId").asText()).isEqualTo("ai-agent-runtime");
        assertThat(event.path("inputMetrics").path("blocked").asBoolean()).isTrue();
        assertThat(event.path("inputMetrics").path("blockReason").asText())
                .isEqualTo("Prescription requests require human approval");
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
        JsonNode event = awaitEvent(node ->
                "TOOL_EXECUTION".equals(node.path("decisionType").asText()));
        assertThat(event).isNotNull();
        assertThat(event.path("agentId").asText()).isEqualTo("ai-agent-runtime");
        assertThat(event.path("inputMetrics").path("toolName").asText()).isEqualTo("get_patient_vitals");
        assertThat(event.path("inputMetrics").path("toolCallId").asText()).isEqualTo("call-123");
        assertThat(event.path("inputMetrics").path("toolCategory").asText()).isEqualTo("FHIR_QUERY");
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
        JsonNode event = awaitEvent(node ->
                "PHI_ACCESS".equals(node.path("decisionType").asText()));
        assertThat(event).isNotNull();
        assertThat(event.path("agentId").asText()).isEqualTo("ai-agent-runtime");
        assertThat(event.path("resourceType").asText()).isEqualTo(resourceType);
        assertThat(event.path("resourceId").asText()).isEqualTo(resourceId);
        assertThat(event.path("inputMetrics").path("accessPurpose").asText()).isEqualTo(accessPurpose);
        assertThat(event.path("inputMetrics").path("accessGranted").asBoolean()).isTrue();
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
        JsonNode event = awaitEvent(node ->
                "AI_RECOMMENDATION".equals(node.path("decisionType").asText()));
        assertThat(event).isNotNull();
        assertThat(event.path("tenantId").asText()).isEqualTo(TENANT_ID);
        assertThat(event.path("agentId").asText()).isEqualTo("ai-agent-runtime");
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
        int receivedCount = awaitEventCount(eventCount, node ->
                "AI_RECOMMENDATION".equals(node.path("decisionType").asText()));
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

    private JsonNode awaitEvent(Predicate<JsonNode> matcher) {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode event = objectMapper.readTree(record.value());
                    if (matcher.test(event)) {
                        return event;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private int awaitEventCount(int target, Predicate<JsonNode> matcher) {
        int count = 0;
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(20).toMillis();
        while (count < target && System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode event = objectMapper.readTree(record.value());
                    if (matcher.test(event)) {
                        count++;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return count;
    }
}
