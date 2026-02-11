package com.healthdata.audit.integration;

import com.healthdata.audit.client.AgentRuntimeClient;
import com.healthdata.audit.config.AuditClientConfig;
import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.audit.service.ai.DecisionReplayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Decision Replay Service.
 *
 * Tests with real PostgreSQL database using Testcontainers.
 * Validates decision replay functionality with actual database persistence.
 */
@SpringBootTest(
    classes = {AuditIntegrationTestConfiguration.class},
    properties = "spring.main.allow-bean-definition-overriding=true")
@Import({DecisionReplayService.class, AuditClientConfig.class})
@Testcontainers
@Transactional
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
@DisplayName("Decision Replay Service - Integration Tests")
class DecisionReplayServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("audit_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private DecisionReplayService decisionReplayService;

    @Autowired
    private AIAgentDecisionEventRepository aiDecisionRepository;

    @MockBean
    private AgentRuntimeClient agentRuntimeClient;

    private static final String TENANT_ID = "test-tenant-replay";

    @BeforeEach
    void setUp() {
        aiDecisionRepository.deleteAll();
    }

    @Test
    @DisplayName("Should replay decision from database with agent service")
    void testReplayDecision_FromDatabase_WithAgentService() {
        // Given - Create a decision event in database
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createDecisionEntity(
            eventId,
            AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW,
            "What is the recommended treatment?",
            0.9,
            "Prescribe medication X"
        );
        aiDecisionRepository.save(original);

        // Mock agent service response
        AgentRuntimeClient.AgentExecutionResponse agentResponse =
            AgentRuntimeClient.AgentExecutionResponse.success(
                "{\"recommendedValue\": \"Prescribe medication X\", \"confidence\": 0.92}",
                new AgentRuntimeClient.TokenUsage(100, 50, 150),
                "claude-3-5-sonnet"
            );
        when(agentRuntimeClient.executeAgent(eq("clinical-workflow"), anyMap(), eq(TENANT_ID)))
            .thenReturn(agentResponse);

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(eventId, result.getOriginalEventId());
        assertNotNull(result.getReplayedDecision());
        assertNotNull(result.getComparison());
        assertEquals(DecisionReplayService.ReplayStatus.IDENTICAL, result.getStatus());
        
        verify(agentRuntimeClient).executeAgent(eq("clinical-workflow"), anyMap(), eq(TENANT_ID));
    }

    @Test
    @DisplayName("Should fallback to validation replay when agent service unavailable")
    void testReplayDecision_FromDatabase_ValidationFallback() {
        // Given - Create a decision event without user query (triggers validation fallback)
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createDecisionEntity(
            eventId,
            AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER,
            null, // No user query
            0.85,
            "Close care gap"
        );
        aiDecisionRepository.save(original);

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(eventId, result.getOriginalEventId());
        assertNotNull(result.getReplayedDecision());
        assertNotNull(result.getComparison());
        // Should use validation replay (no agent service call)
        verify(agentRuntimeClient, never()).executeAgent(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Should replay batch of decisions from database")
    void testReplayDecisionBatch_FromDatabase() {
        // Given - Create multiple decision events
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID eventId3 = UUID.randomUUID();

        AIAgentDecisionEventEntity event1 = createDecisionEntity(
            eventId1, AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, "Query 1", 0.9, "Value 1");
        AIAgentDecisionEventEntity event2 = createDecisionEntity(
            eventId2, AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER, null, 0.85, "Value 2");
        AIAgentDecisionEventEntity event3 = createDecisionEntity(
            eventId3, AIAgentDecisionEvent.AgentType.AI_AGENT, "Query 3", 0.95, "Value 3");

        aiDecisionRepository.saveAll(List.of(event1, event2, event3));

        // Mock agent service for events that will use it
        AgentRuntimeClient.AgentExecutionResponse agentResponse =
            AgentRuntimeClient.AgentExecutionResponse.success(
                "{\"recommendedValue\": \"Test\", \"confidence\": 0.9}",
                new AgentRuntimeClient.TokenUsage(100, 50, 150),
                "claude-3-5-sonnet"
            );
        when(agentRuntimeClient.executeAgent(anyString(), anyMap(), eq(TENANT_ID)))
            .thenReturn(agentResponse);

        // When
        List<UUID> eventIds = List.of(eventId1, eventId2, eventId3);
        List<DecisionReplayService.ReplayResult> results = decisionReplayService.replayDecisionBatch(eventIds);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(DecisionReplayService.ReplayResult::isSuccess));
    }

    @Test
    @DisplayName("Should replay decision chain from database")
    void testReplayDecisionChain_FromDatabase() {
        // Given - Create a chain of related decisions
        String correlationId = "correlation-" + UUID.randomUUID();
        
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID eventId3 = UUID.randomUUID();

        AIAgentDecisionEventEntity event1 = createDecisionEntityWithCorrelation(
            eventId1, correlationId, AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, "Query 1", 0.9, "Value 1");
        AIAgentDecisionEventEntity event2 = createDecisionEntityWithCorrelation(
            eventId2, correlationId, AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, "Query 2", 0.85, "Value 2");
        AIAgentDecisionEventEntity event3 = createDecisionEntityWithCorrelation(
            eventId3, correlationId, AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW, "Query 3", 0.95, "Value 3");

        aiDecisionRepository.saveAll(List.of(event1, event2, event3));

        // Mock agent service
        AgentRuntimeClient.AgentExecutionResponse agentResponse =
            AgentRuntimeClient.AgentExecutionResponse.success(
                "{\"recommendedValue\": \"Test\", \"confidence\": 0.9}",
                new AgentRuntimeClient.TokenUsage(100, 50, 150),
                "claude-3-5-sonnet"
            );
        when(agentRuntimeClient.executeAgent(anyString(), anyMap(), eq(TENANT_ID)))
            .thenReturn(agentResponse);

        // When
        DecisionReplayService.ChainReplayResult chainResult = 
            decisionReplayService.replayDecisionChain(correlationId);

        // Then
        assertNotNull(chainResult);
        assertEquals(correlationId, chainResult.getCorrelationId());
        assertEquals(3, chainResult.getOriginalChainLength());
        assertNotNull(chainResult.getDecisionResults());
        assertEquals(3, chainResult.getDecisionResults().size());
        assertTrue(chainResult.getOverallConsistency() >= 0.0);
        assertTrue(chainResult.getOverallConsistency() <= 1.0);
    }

    @Test
    @DisplayName("Should persist and retrieve decision for replay")
    void testDecisionPersistence_AndRetrieval() {
        // Given - Create and save a decision
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createDecisionEntity(
            eventId,
            AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW,
            "Test query",
            0.9,
            "Test recommendation"
        );
        aiDecisionRepository.save(original);

        // When - Retrieve and replay
        Optional<AIAgentDecisionEventEntity> retrieved = aiDecisionRepository.findById(eventId);
        assertTrue(retrieved.isPresent());

        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertEquals(eventId, result.getOriginalEventId());
        assertNotNull(result.getOriginalDecision());
        assertEquals("CLINICAL_WORKFLOW", result.getOriginalDecision().getAgentType());
    }

    @Test
    @DisplayName("Should handle performance with multiple decisions")
    void testPerformance_WithMultipleDecisions() {
        // Given - Create 50 decision events
        List<UUID> eventIds = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            UUID eventId = UUID.randomUUID();
            AIAgentDecisionEvent.AgentType agentType = (i % 3 == 0)
                ? AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW
                : (i % 3 == 1)
                    ? AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER
                    : AIAgentDecisionEvent.AgentType.AI_AGENT;

            AIAgentDecisionEventEntity event = createDecisionEntity(
                eventId, agentType, "Query " + i, 0.8 + (i % 20) * 0.01, "Value " + i);
            aiDecisionRepository.save(event);
            eventIds.add(eventId);
        }

        // Mock agent service
        AgentRuntimeClient.AgentExecutionResponse agentResponse =
            AgentRuntimeClient.AgentExecutionResponse.success(
                "{\"recommendedValue\": \"Test\", \"confidence\": 0.9}",
                new AgentRuntimeClient.TokenUsage(100, 50, 150),
                "claude-3-5-sonnet"
            );
        when(agentRuntimeClient.executeAgent(anyString(), anyMap(), eq(TENANT_ID)))
            .thenReturn(agentResponse);

        // When
        long startTime = System.currentTimeMillis();
        List<DecisionReplayService.ReplayResult> results = 
            decisionReplayService.replayDecisionBatch(eventIds);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertNotNull(results);
        assertEquals(50, results.size());
        // Should complete in reasonable time (< 10 seconds for 50 decisions)
        assertTrue(duration < 10000, "Batch replay took " + duration + "ms, exceeds 10s target");
    }

    // ==================== Helper Methods ====================

    private AIAgentDecisionEventEntity createDecisionEntity(
            UUID eventId,
            AIAgentDecisionEvent.AgentType agentType,
            String userQuery,
            double confidence,
            String recommendedValue) {
        
        AIAgentDecisionEventEntity event = new AIAgentDecisionEventEntity();
        event.setEventId(eventId);
        event.setTimestamp(Instant.now());
        event.setTenantId(TENANT_ID);
        event.setAgentType(agentType);
        event.setDecisionType(AIAgentDecisionEvent.DecisionType.CDS_RECOMMENDATION);
        event.setConfidenceScore(confidence);
        event.setRecommendedValue(recommendedValue);
        event.setUserQuery(userQuery);
        event.setModelName("claude-3-5-sonnet");
        
        return event;
    }

    private AIAgentDecisionEventEntity createDecisionEntityWithCorrelation(
            UUID eventId,
            String correlationId,
            AIAgentDecisionEvent.AgentType agentType,
            String userQuery,
            double confidence,
            String recommendedValue) {
        
        AIAgentDecisionEventEntity event = createDecisionEntity(
            eventId, agentType, userQuery, confidence, recommendedValue);
        event.setCorrelationId(correlationId);
        
        return event;
    }
}
