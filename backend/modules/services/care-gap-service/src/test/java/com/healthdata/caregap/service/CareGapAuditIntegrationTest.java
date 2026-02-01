package com.healthdata.caregap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Tag;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CareGapAuditIntegration.
 * 
 * Verifies that audit events are properly constructed with all required fields,
 * including the agentId field that was previously missing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Audit Integration Tests")
@Tag("unit")
class CareGapAuditIntegrationTest {

    @Mock
    private AIAuditEventPublisher auditPublisher;

    @Captor
    private ArgumentCaptor<AIAgentDecisionEvent> eventCaptor;

    private CareGapAuditIntegration auditIntegration;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final String MEASURE_ID = "HEDIS_CDC_A1C";
    private static final String GAP_ID = "gap-789";
    private static final String CREATED_BY = "user@example.com";

    @BeforeEach
    void setUp() {
        auditIntegration = new CareGapAuditIntegration(auditPublisher);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should publish care gap identification event with agentId")
    void shouldPublishCareGapIdentificationEventWithAgentId() {
        // Given
        JsonNode cqlResult = objectMapper.createObjectNode()
                .put("hasGap", true)
                .put("measureId", MEASURE_ID);

        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCareGapIdentificationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, GAP_ID, cqlResult, CREATED_BY);

        // Then
        verify(auditPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();

        // Verify agentId is set (this was the fix)
        assertThat(event.getAgentId()).isEqualTo("care-gap-identifier");
        assertThat(event.getAgentType()).isEqualTo(AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER);
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID);
        assertThat(event.getDecisionType()).isEqualTo(AIAgentDecisionEvent.DecisionType.CARE_GAP_IDENTIFICATION);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle null CQL result gracefully")
    void shouldHandleNullCqlResult() {
        // Given
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCareGapIdentificationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, GAP_ID, null, CREATED_BY);

        // Then
        verify(auditPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getAgentId()).isEqualTo("care-gap-identifier");
        assertThat(event.getInputMetrics()).isNotNull();
    }

    @Test
    @DisplayName("Should not throw exception when publisher fails")
    void shouldNotThrowExceptionWhenPublisherFails() {
        // Given
        JsonNode cqlResult = objectMapper.createObjectNode();
        when(auditPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenThrow(new RuntimeException("Kafka error"));

        // When/Then - Should not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            auditIntegration.publishCareGapIdentificationEvent(
                    TENANT_ID, PATIENT_ID, MEASURE_ID, GAP_ID, cqlResult, CREATED_BY);
        });
    }

    @Test
    @DisplayName("Should build customer profile correctly")
    void shouldBuildCustomerProfileCorrectly() {
        // Given
        JsonNode cqlResult = objectMapper.createObjectNode();
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCareGapIdentificationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, GAP_ID, cqlResult, CREATED_BY);

        // Then
        verify(auditPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getCustomerProfile()).isNotNull();
        assertThat(event.getCustomerProfile().getCustomerTier()).isEqualTo("STANDARD");
    }

    @Test
    @DisplayName("Should build recommendation correctly")
    void shouldBuildRecommendationCorrectly() {
        // Given
        JsonNode cqlResult = objectMapper.createObjectNode();
        CompletableFuture future = CompletableFuture.completedFuture(null);
        when(auditPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(future);

        // When
        auditIntegration.publishCareGapIdentificationEvent(
                TENANT_ID, PATIENT_ID, MEASURE_ID, GAP_ID, cqlResult, CREATED_BY);

        // Then
        verify(auditPublisher).publishAIDecision(eventCaptor.capture());
        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getRecommendation()).isNotNull();
        assertThat(event.getRecommendation().getRiskLevel())
                .isEqualTo(AIAgentDecisionEvent.RiskLevel.LOW);
    }
}

