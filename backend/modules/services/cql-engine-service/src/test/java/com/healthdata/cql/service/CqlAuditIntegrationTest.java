package com.healthdata.cql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.cql.measure.MeasureResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CqlAuditIntegration.
 * 
 * Verifies that audit events are properly constructed with all required fields,
 * including the agentId field that was previously missing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CQL Audit Integration Tests")
class CqlAuditIntegrationTest {

    @Mock
    private AIAuditEventPublisher auditEventPublisher;

    @Captor
    private ArgumentCaptor<AIAgentDecisionEvent> eventCaptor;

    private CqlAuditIntegration auditIntegration;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final String MEASURE_ID = "HEDIS_CDC_A1C";
    private static final String EVALUATION_ID = "eval-789";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditIntegration = new CqlAuditIntegration(auditEventPublisher, objectMapper);
    }

    @Test
    @DisplayName("Should publish CQL evaluation event with agentId")
    void shouldPublishCqlEvaluationEventWithAgentId() {
        // Given
        MeasureResult measureResult = createMeasureResult(true, false);
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCqlEvaluationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                measureResult, "user@example.com", 150L);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();

        // Verify agentId is set (this was the fix)
        assertThat(event.getAgentId()).isEqualTo("cql-engine");
        assertThat(event.getAgentType()).isEqualTo(AIAgentDecisionEvent.AgentType.CQL_ENGINE);
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID);
        assertThat(event.getCorrelationId()).isEqualTo(EVALUATION_ID);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should set MEASURE_MET decision type when in numerator")
    void shouldSetMeasureMetDecisionTypeWhenInNumerator() {
        // Given
        MeasureResult measureResult = createMeasureResult(true, true);
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCqlEvaluationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                measureResult, "user@example.com", 150L);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.MEASURE_MET);
        assertThat(event.getAgentId()).isEqualTo("cql-engine");
    }

    @Test
    @DisplayName("Should set MEASURE_NOT_MET decision type when not in numerator")
    void shouldSetMeasureNotMetDecisionTypeWhenNotInNumerator() {
        // Given
        MeasureResult measureResult = createMeasureResult(true, false);
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCqlEvaluationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                measureResult, "user@example.com", 150L);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.MEASURE_NOT_MET);
        assertThat(event.getAgentId()).isEqualTo("cql-engine");
    }

    @Test
    @DisplayName("Should publish batch evaluation event with agentId")
    void shouldPublishBatchEvaluationEventWithAgentId() {
        // Given
        String batchId = "batch-123";
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishBatchEvaluationEvent(
                TENANT_ID, PATIENT_ID, batchId, 10, 8, 2, "user@example.com");

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();

        // Verify agentId is set (this was the fix)
        assertThat(event.getAgentId()).isEqualTo("cql-engine");
        assertThat(event.getAgentType()).isEqualTo(AIAgentDecisionEvent.AgentType.CQL_ENGINE);
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.BATCH_EVALUATION);
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID);
        assertThat(event.getCorrelationId()).isEqualTo(batchId);
        assertThat(event.getConfidenceScore()).isEqualTo(0.8); // 8/10 success rate
    }

    @Test
    @DisplayName("Should not throw exception when publisher fails")
    void shouldNotThrowExceptionWhenPublisherFails() {
        // Given
        MeasureResult measureResult = createMeasureResult(true, false);
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenThrow(new RuntimeException("Kafka error"));

        // When/Then - Should not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            auditIntegration.publishCqlEvaluationEvent(
                    TENANT_ID, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                    measureResult, "user@example.com", 150L);
        });
    }

    @Test
    @DisplayName("Should calculate confidence score correctly")
    void shouldCalculateConfidenceScoreCorrectly() {
        // Given
        MeasureResult measureResult = createMeasureResult(true, false);
        measureResult.getDetails().put("dataCompleteness", "high");
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCqlEvaluationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                measureResult, "user@example.com", 150L);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getConfidenceScore()).isNotNull();
        assertThat(event.getConfidenceScore()).isGreaterThan(0.0);
        assertThat(event.getConfidenceScore()).isLessThanOrEqualTo(1.0);
        assertThat(event.getAgentId()).isEqualTo("cql-engine");
    }

    @Test
    @DisplayName("Should build reasoning string correctly")
    void shouldBuildReasoningStringCorrectly() {
        // Given
        MeasureResult measureResult = createMeasureResult(true, false);
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCqlEvaluationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, EVALUATION_ID,
                measureResult, "user@example.com", 150L);

        // Then
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getReasoning()).isNotNull();
        assertThat(event.getReasoning()).contains("Patient in denominator");
        assertThat(event.getAgentId()).isEqualTo("cql-engine");
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

