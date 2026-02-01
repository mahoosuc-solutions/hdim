package com.healthdata.audit.service.ai;

import com.healthdata.audit.client.AgentRuntimeClient;
import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for DecisionReplayService.
 * 
 * Tests cover:
 * - Single decision replay (with and without agent service)
 * - Batch decision replay
 * - Decision chain replay
 * - Drift detection
 * - Error handling
 * - Edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Decision Replay Service Tests")
class DecisionReplayServiceTest {

    @Mock
    private AIAgentDecisionEventRepository aiDecisionRepository;

    @Mock
    private AgentRuntimeClient agentRuntimeClient;

    private DecisionReplayService decisionReplayService;

    @BeforeEach
    void setUp() {
        decisionReplayService = new DecisionReplayService(aiDecisionRepository, agentRuntimeClient);
    }

    // ==================== Single Decision Replay Tests ====================

    @Test
    @DisplayName("Should successfully replay decision via agent service")
    void testReplayDecision_WithAgentService_Success() {
        // Given
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createValidDecisionEntity(eventId);
        original.setUserQuery("What is the recommended treatment?");
        original.setTenantId("tenant-1");
        original.setAgentType(AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW);

        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.of(original));

        AgentRuntimeClient.AgentExecutionResponse agentResponse = 
            AgentRuntimeClient.AgentExecutionResponse.success(
                "{\"recommendedValue\": \"Prescribe medication X\", \"confidence\": 0.92}",
                new AgentRuntimeClient.TokenUsage(100, 50, 150),
                "claude-3-5-sonnet"
            );
        when(agentRuntimeClient.executeAgent(anyString(), anyMap(), eq("tenant-1")))
            .thenReturn(agentResponse);

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getReplayedDecision());
        assertEquals(eventId, result.getOriginalEventId());
        assertNotNull(result.getComparison());
        verify(agentRuntimeClient).executeAgent(eq("clinical-workflow"), anyMap(), eq("tenant-1"));
    }

    @Test
    @DisplayName("Should fallback to validation replay when agent service unavailable")
    void testReplayDecision_WithoutAgentService_FallbackToValidation() {
        // Given
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createValidDecisionEntity(eventId);
        original.setUserQuery(null); // No user query
        original.setInputMetrics(null); // No input metrics

        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.of(original));

        DecisionReplayService serviceWithoutClient = 
            new DecisionReplayService(aiDecisionRepository);

        // When
        DecisionReplayService.ReplayResult result = serviceWithoutClient.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getReplayedDecision());
        assertEquals(DecisionReplayService.ReplayStatus.IDENTICAL, result.getStatus());
        verify(agentRuntimeClient, never()).executeAgent(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Should handle missing decision gracefully")
    void testReplayDecision_DecisionNotFound_ThrowsException() {
        // Given
        UUID eventId = UUID.randomUUID();
        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.empty());

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> decisionReplayService.replayDecision(eventId)
        );
        assertEquals("Decision not found: " + eventId, exception.getMessage());
    }

    @Test
    @DisplayName("Should detect drift when replayed decision differs")
    void testReplayDecision_DetectsDrift_DifferentRecommendation() {
        // Given
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createValidDecisionEntity(eventId);
        original.setUserQuery("What is the recommended treatment?");
        original.setTenantId("tenant-1");
        original.setAgentType(AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW);
        original.setRecommendedValue("Prescribe medication A");
        original.setConfidenceScore(0.85);

        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.of(original));

        // Replayed decision returns different recommendation
        AgentRuntimeClient.AgentExecutionResponse agentResponse = 
            AgentRuntimeClient.AgentExecutionResponse.success(
                "{\"recommendedValue\": \"Prescribe medication B\", \"confidence\": 0.90}",
                new AgentRuntimeClient.TokenUsage(100, 50, 150),
                "claude-3-5-sonnet"
            );
        when(agentRuntimeClient.executeAgent(anyString(), anyMap(), eq("tenant-1")))
            .thenReturn(agentResponse);

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(DecisionReplayService.ReplayStatus.DIFFERENT, result.getStatus());
        assertNotNull(result.getComparison());
        assertFalse(result.getComparison().isIdentical());
        assertFalse(result.getComparison().isRecommendationMatch());
        assertNotNull(result.getComparison().getDifferences());
        assertFalse(result.getComparison().getDifferences().isEmpty());
    }

    @Test
    @DisplayName("Should handle agent service failure gracefully")
    void testReplayDecision_AgentServiceFails_FallbackToValidation() {
        // Given
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createValidDecisionEntity(eventId);
        original.setUserQuery("What is the recommended treatment?");
        original.setTenantId("tenant-1");
        original.setAgentType(AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW);

        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.of(original));

        // Agent service returns failure
        AgentRuntimeClient.AgentExecutionResponse agentResponse = 
            AgentRuntimeClient.AgentExecutionResponse.failure("Agent service unavailable");
        when(agentRuntimeClient.executeAgent(anyString(), anyMap(), eq("tenant-1")))
            .thenReturn(agentResponse);

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess()); // Validation replay succeeds
        assertNotNull(result.getReplayedDecision());
    }

    // ==================== Batch Replay Tests ====================

    @Test
    @DisplayName("Should replay multiple decisions in batch")
    void testReplayDecisionBatch_Success() {
        // Given
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID eventId3 = UUID.randomUUID();

        AIAgentDecisionEventEntity entity1 = createValidDecisionEntity(eventId1);
        AIAgentDecisionEventEntity entity2 = createValidDecisionEntity(eventId2);
        AIAgentDecisionEventEntity entity3 = createValidDecisionEntity(eventId3);

        when(aiDecisionRepository.findById(eventId1)).thenReturn(Optional.of(entity1));
        when(aiDecisionRepository.findById(eventId2)).thenReturn(Optional.of(entity2));
        when(aiDecisionRepository.findById(eventId3)).thenReturn(Optional.of(entity3));

        // When
        List<UUID> eventIds = Arrays.asList(eventId1, eventId2, eventId3);
        List<DecisionReplayService.ReplayResult> results = 
            decisionReplayService.replayDecisionBatch(eventIds);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(DecisionReplayService.ReplayResult::isSuccess));
    }

    @Test
    @DisplayName("Should handle partial failures in batch replay")
    void testReplayDecisionBatch_PartialFailure() {
        // Given
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID eventId3 = UUID.randomUUID();

        AIAgentDecisionEventEntity entity1 = createValidDecisionEntity(eventId1);
        AIAgentDecisionEventEntity entity2 = createValidDecisionEntity(eventId2);

        when(aiDecisionRepository.findById(eventId1)).thenReturn(Optional.of(entity1));
        when(aiDecisionRepository.findById(eventId2)).thenReturn(Optional.of(entity2));
        when(aiDecisionRepository.findById(eventId3)).thenReturn(Optional.empty()); // Missing

        // When
        List<UUID> eventIds = Arrays.asList(eventId1, eventId2, eventId3);
        List<DecisionReplayService.ReplayResult> results = 
            decisionReplayService.replayDecisionBatch(eventIds);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(1).isSuccess());
        assertFalse(results.get(2).isSuccess()); // Failed due to missing decision
        assertEquals(DecisionReplayService.ReplayStatus.FAILED, results.get(2).getStatus());
    }

    // ==================== Chain Replay Tests ====================

    @Test
    @DisplayName("Should replay decision chain successfully")
    void testReplayDecisionChain_Success() {
        // Given
        String correlationId = "correlation-123";
        UUID eventId1 = UUID.randomUUID();
        UUID eventId2 = UUID.randomUUID();
        UUID eventId3 = UUID.randomUUID();

        AIAgentDecisionEventEntity entity1 = createValidDecisionEntity(eventId1);
        entity1.setCorrelationId(correlationId);
        entity1.setTimestamp(Instant.now().minusSeconds(300));

        AIAgentDecisionEventEntity entity2 = createValidDecisionEntity(eventId2);
        entity2.setCorrelationId(correlationId);
        entity2.setTimestamp(Instant.now().minusSeconds(200));

        AIAgentDecisionEventEntity entity3 = createValidDecisionEntity(eventId3);
        entity3.setCorrelationId(correlationId);
        entity3.setTimestamp(Instant.now().minusSeconds(100));

        List<AIAgentDecisionEventEntity> chain = Arrays.asList(entity1, entity2, entity3);
        when(aiDecisionRepository.findByCorrelationIdOrderByTimestampAsc(correlationId))
            .thenReturn(chain);

        when(aiDecisionRepository.findById(eventId1)).thenReturn(Optional.of(entity1));
        when(aiDecisionRepository.findById(eventId2)).thenReturn(Optional.of(entity2));
        when(aiDecisionRepository.findById(eventId3)).thenReturn(Optional.of(entity3));

        // When
        DecisionReplayService.ChainReplayResult result = 
            decisionReplayService.replayDecisionChain(correlationId);

        // Then
        assertNotNull(result);
        assertEquals(correlationId, result.getCorrelationId());
        assertEquals(3, result.getOriginalChainLength());
        assertNotNull(result.getDecisionResults());
        assertEquals(3, result.getDecisionResults().size());
        assertTrue(result.getOverallConsistency() >= 0.0 && result.getOverallConsistency() <= 1.0);
    }

    @Test
    @DisplayName("Should handle empty correlation chain")
    void testReplayDecisionChain_EmptyChain_ThrowsException() {
        // Given
        String correlationId = "non-existent";
        when(aiDecisionRepository.findByCorrelationIdOrderByTimestampAsc(correlationId))
            .thenReturn(Collections.emptyList());

        // When/Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> decisionReplayService.replayDecisionChain(correlationId)
        );
        assertEquals("No decisions found for correlation ID: " + correlationId, exception.getMessage());
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle null user query and input metrics")
    void testReplayDecision_NullInputs_FallbackToValidation() {
        // Given
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createValidDecisionEntity(eventId);
        original.setUserQuery(null);
        original.setInputMetrics(null);
        original.setTenantId("tenant-1");

        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.of(original));

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(agentRuntimeClient, never()).executeAgent(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("Should handle invalid confidence score in stored decision")
    void testReplayDecision_InvalidConfidence_ValidationFails() {
        // Given
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createValidDecisionEntity(eventId);
        original.setConfidenceScore(1.5); // Invalid: > 1.0
        original.setUserQuery(null);

        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.of(original));

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess()); // Still succeeds but flags inconsistencies
        assertNotNull(result.getReplayedDecision());
    }

    @Test
    @DisplayName("Should handle agent type with default slug mapping")
    void testReplayDecision_DefaultSlugMapping_CallsAgentService() {
        // Given
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createValidDecisionEntity(eventId);
        original.setUserQuery("Custom query");
        original.setTenantId("tenant-1");
        original.setAgentType(AIAgentDecisionEvent.AgentType.CONFIGURATION_ADVISOR);

        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.of(original));

        // Agent service returns success for default slug mapping
        AgentRuntimeClient.AgentExecutionResponse agentResponse = 
            AgentRuntimeClient.AgentExecutionResponse.success(
                "Configuration recommendation",
                null,
                null
            );
        when(agentRuntimeClient.executeAgent(eq("configuration-advisor"), anyMap(), eq("tenant-1")))
            .thenReturn(agentResponse);

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        // Default slug mapping should call agent service
        verify(agentRuntimeClient).executeAgent(eq("configuration-advisor"), anyMap(), eq("tenant-1"));
    }

    @Test
    @DisplayName("Should extract recommended value from JSON response")
    void testReplayDecision_ExtractValueFromJson() {
        // Given
        UUID eventId = UUID.randomUUID();
        AIAgentDecisionEventEntity original = createValidDecisionEntity(eventId);
        original.setUserQuery("Query");
        original.setTenantId("tenant-1");
        original.setAgentType(AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW);
        original.setRecommendedValue("Original value");

        when(aiDecisionRepository.findById(eventId)).thenReturn(Optional.of(original));

        AgentRuntimeClient.AgentExecutionResponse agentResponse = 
            AgentRuntimeClient.AgentExecutionResponse.success(
                "{\"recommendedValue\": \"New extracted value\", \"confidence\": 0.88}",
                null,
                null
            );
        when(agentRuntimeClient.executeAgent(anyString(), anyMap(), eq("tenant-1")))
            .thenReturn(agentResponse);

        // When
        DecisionReplayService.ReplayResult result = decisionReplayService.replayDecision(eventId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getReplayedDecision());
        assertTrue(result.getReplayedDecision().getRecommendedValue().contains("New extracted value"));
    }

    // ==================== Helper Methods ====================

    private AIAgentDecisionEventEntity createValidDecisionEntity(UUID eventId) {
        AIAgentDecisionEventEntity entity = new AIAgentDecisionEventEntity();
        entity.setEventId(eventId);
        entity.setTimestamp(Instant.now());
        entity.setTenantId("tenant-1");
        entity.setAgentType(AIAgentDecisionEvent.AgentType.CLINICAL_WORKFLOW);
        entity.setDecisionType(AIAgentDecisionEvent.DecisionType.CDS_RECOMMENDATION);
        entity.setResourceType("Patient");
        entity.setResourceId("patient-123");
        entity.setRecommendedValue("Test recommendation");
        entity.setConfidenceScore(0.85);
        entity.setReasoning("Test reasoning");
        entity.setModelName("claude-3-5-sonnet");
        entity.setCorrelationId("correlation-123");
        return entity;
    }
}
