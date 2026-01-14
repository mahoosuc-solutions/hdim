package com.healthdata.predictive.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.AgentType;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.DecisionType;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.predictive.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Predictive Analytics Audit Integration - Lightweight Tests")
class PredictiveAnalyticsAuditIntegrationTest {

    @Mock
    private AIAuditEventPublisher auditEventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    private PredictiveAnalyticsAuditIntegration auditIntegration;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-001";
    private static final String USER_ID = "user-456";

    @BeforeEach
    void setUp() {
        auditIntegration = new PredictiveAnalyticsAuditIntegration(auditEventPublisher, objectMapper);
        
        // Enable audit
        ReflectionTestUtils.setField(auditIntegration, "auditEnabled", true);
        
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("Should publish readmission prediction event with correct fields")
    void shouldPublishReadmissionPredictionEvent() {
        // Given
        ReadmissionRiskScore riskScore = createReadmissionRiskScore(30);
        long inferenceTimeMs = 150L;

        // When
        auditIntegration.publishReadmissionPredictionEvent(
                TENANT_ID, PATIENT_ID, riskScore, inferenceTimeMs, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentId()).isEqualTo("predictive-analytics");
        assertThat(event.getAgentType()).isEqualTo(AgentType.PREDICTIVE_ANALYTICS);
        assertThat(event.getDecisionType()).isEqualTo(DecisionType.HOSPITALIZATION_PREDICTION);
        assertThat(event.getResourceType()).isEqualTo("Patient");
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID);
        assertThat(event.getInferenceTimeMs()).isEqualTo(inferenceTimeMs);

        // Verify metrics (both input and output combined in inputMetrics)
        Map<String, Object> inputMetrics = event.getInputMetrics();
        assertThat(inputMetrics.get("predictionPeriodDays")).isEqualTo(30);
        assertThat(inputMetrics.get("score")).isEqualTo(75.0);
        assertThat(inputMetrics.get("riskTier")).isEqualTo("HIGH");
        assertThat(inputMetrics.get("readmissionProbability")).isEqualTo(0.75);
        assertThat(inputMetrics.get("laceIndex")).isEqualTo(12);
        assertThat(inputMetrics.get("confidence")).isEqualTo(0.85);
    }

    @Test
    @DisplayName("Should publish 90-day readmission prediction event")
    void shouldPublish90DayReadmissionPredictionEvent() {
        // Given
        ReadmissionRiskScore riskScore = createReadmissionRiskScore(90);
        long inferenceTimeMs = 200L;

        // When
        auditIntegration.publishReadmissionPredictionEvent(
                TENANT_ID, PATIENT_ID, riskScore, inferenceTimeMs, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getInputMetrics().get("predictionPeriodDays")).isEqualTo(90);
    }

    @Test
    @DisplayName("Should publish risk stratification event")
    void shouldPublishRiskStratificationEvent() {
        // Given
        int totalPatients = 100;
        Map<RiskTier, Integer> tierDistribution = new HashMap<>();
        tierDistribution.put(RiskTier.LOW, 30);
        tierDistribution.put(RiskTier.MODERATE, 40);
        tierDistribution.put(RiskTier.HIGH, 25);
        tierDistribution.put(RiskTier.VERY_HIGH, 5);
        long inferenceTimeMs = 500L;

        // When
        auditIntegration.publishRiskStratificationEvent(
                TENANT_ID, totalPatients, tierDistribution, inferenceTimeMs, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentId()).isEqualTo("predictive-analytics");
        assertThat(event.getAgentType()).isEqualTo(AgentType.PREDICTIVE_ANALYTICS);
        assertThat(event.getDecisionType()).isEqualTo(DecisionType.RISK_STRATIFICATION);
        assertThat(event.getResourceType()).isEqualTo("Population");
        assertThat(event.getResourceId()).isEqualTo(TENANT_ID);

        // Verify metrics (both input and output combined in inputMetrics)
        Map<String, Object> inputMetrics = event.getInputMetrics();
        assertThat(inputMetrics.get("totalPatients")).isEqualTo(totalPatients);
        assertThat(inputMetrics.get("tierDistribution")).isEqualTo(tierDistribution);
        assertThat(inputMetrics.get("highRiskPatientCount")).isEqualTo(30); // HIGH + VERY_HIGH
        assertThat(inputMetrics.get("highRiskPercentage")).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Should publish disease progression event")
    void shouldPublishDiseaseProgressionEvent() {
        // Given
        String diseaseCode = "E11.9"; // Type 2 diabetes
        Map<String, Object> progressionPrediction = new HashMap<>();
        progressionPrediction.put("currentStage", "A1");
        progressionPrediction.put("predictedStage", "A2");
        progressionPrediction.put("timeToProgression", "18 months");
        progressionPrediction.put("confidence", 0.75);
        long inferenceTimeMs = 300L;

        // When
        auditIntegration.publishDiseaseProgressionEvent(
                TENANT_ID, PATIENT_ID, diseaseCode, progressionPrediction, inferenceTimeMs, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentType()).isEqualTo(AgentType.PREDICTIVE_ANALYTICS);
        assertThat(event.getDecisionType()).isEqualTo(DecisionType.CLINICAL_DECISION);
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID);

        // Verify metrics (both input and output combined in inputMetrics)
        Map<String, Object> inputMetrics = event.getInputMetrics();
        assertThat(inputMetrics.get("diseaseCode")).isEqualTo(diseaseCode);
        assertThat(inputMetrics.get("currentStage")).isEqualTo("A1");
        assertThat(inputMetrics.get("predictedStage")).isEqualTo("A2");
    }

    @Test
    @DisplayName("Should publish cost prediction event")
    void shouldPublishCostPredictionEvent() {
        // Given
        double predictedCost = 125000.50;
        Map<String, Object> costBreakdown = new HashMap<>();
        costBreakdown.put("inpatient", 75000.0);
        costBreakdown.put("outpatient", 30000.0);
        costBreakdown.put("pharmacy", 15000.0);
        costBreakdown.put("other", 5000.50);
        long inferenceTimeMs = 250L;

        // When
        auditIntegration.publishCostPredictionEvent(
                TENANT_ID, PATIENT_ID, predictedCost, costBreakdown, inferenceTimeMs, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getAgentType()).isEqualTo(AgentType.PREDICTIVE_ANALYTICS);
        assertThat(event.getDecisionType()).isEqualTo(DecisionType.CLINICAL_DECISION);
        assertThat(event.getResourceId()).isEqualTo(PATIENT_ID);

        // Verify metrics (both input and output combined in inputMetrics)
        Map<String, Object> inputMetrics = event.getInputMetrics();
        assertThat(inputMetrics.get("predictedCost")).isEqualTo(predictedCost);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> breakdown = (Map<String, Object>) inputMetrics.get("costBreakdown");
        assertThat(breakdown.get("inpatient")).isEqualTo(75000.0);
        assertThat(breakdown.get("outpatient")).isEqualTo(30000.0);
    }

    @Test
    @DisplayName("Should not publish event when audit is disabled")
    void shouldNotPublishEventWhenAuditIsDisabled() {
        // Given
        ReflectionTestUtils.setField(auditIntegration, "auditEnabled", false);
        ReadmissionRiskScore riskScore = createReadmissionRiskScore(30);

        // When
        auditIntegration.publishReadmissionPredictionEvent(
                TENANT_ID, PATIENT_ID, riskScore, 100L, USER_ID);

        // Then
        verify(auditEventPublisher, never()).publishAIDecision(any());
    }

    @Test
    @DisplayName("Should not throw exception on audit failure")
    void shouldNotThrowExceptionOnAuditFailure() {
        // Given
        ReadmissionRiskScore riskScore = createReadmissionRiskScore(30);
        when(auditEventPublisher.publishAIDecision(any(AIAgentDecisionEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable")));

        // When
        auditIntegration.publishReadmissionPredictionEvent(
                TENANT_ID, PATIENT_ID, riskScore, 100L, USER_ID);

        // Then - No exception should propagate
        verify(auditEventPublisher).publishAIDecision(any());
    }

    @Test
    @DisplayName("Should include risk factors in readmission event")
    void shouldIncludeRiskFactorsInReadmissionEvent() {
        // Given
        ReadmissionRiskScore riskScore = createReadmissionRiskScore(30);
        
        // When
        auditIntegration.publishReadmissionPredictionEvent(
                TENANT_ID, PATIENT_ID, riskScore, 100L, USER_ID);

        // Then
        ArgumentCaptor<AIAgentDecisionEvent> eventCaptor = ArgumentCaptor.forClass(AIAgentDecisionEvent.class);
        verify(auditEventPublisher).publishAIDecision(eventCaptor.capture());

        AIAgentDecisionEvent event = eventCaptor.getValue();
        Map<String, Object> inputMetrics = event.getInputMetrics();
        
        assertThat(inputMetrics.get("lengthOfStay")).isEqualTo(5);
        assertThat(inputMetrics.get("charlsonIndex")).isEqualTo(4);
        assertThat(inputMetrics.get("edVisitsPast6Months")).isEqualTo(3);
        assertThat(inputMetrics.get("activeChronicConditions")).isEqualTo(3);
    }

    // Helper method to create a test ReadmissionRiskScore
    private ReadmissionRiskScore createReadmissionRiskScore(int predictionPeriodDays) {
        ReadmissionRiskFactors riskFactors = ReadmissionRiskFactors.builder()
                .lengthOfStay(5)
                .acuity("emergency")
                .charlsonComorbidityIndex(4)
                .edVisitsPast6Months(3)
                .activeChronicConditions(3)
                .activeMedications(8)
                .recentMedicationChanges(2)
                .age(68)
                .previousReadmissions(2)
                .socialRiskFactors(2)
                .featureImportance(new HashMap<>())
                .build();

        return ReadmissionRiskScore.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .score(75.0)
                .riskTier(RiskTier.HIGH)
                .predictionPeriodDays(predictionPeriodDays)
                .readmissionProbability(0.75)
                .laceIndex(12)
                .riskFactors(riskFactors)
                .confidence(0.85)
                .modelVersion("readmission-model-v2.1")
                .predictedAt(LocalDateTime.now())
                .metadata(new HashMap<>())
                .build();
    }
}
