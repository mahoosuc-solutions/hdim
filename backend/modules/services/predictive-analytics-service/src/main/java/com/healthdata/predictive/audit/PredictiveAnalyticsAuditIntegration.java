package com.healthdata.predictive.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.AgentType;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.DecisionType;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.predictive.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit integration for predictive analytics service
 * Publishes AI decision events for all predictive models
 */
@Service
@Slf4j
public class PredictiveAnalyticsAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "predictive-analytics";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public PredictiveAnalyticsAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish readmission risk prediction event
     */
    public void publishReadmissionPredictionEvent(
            String tenantId,
            String patientId,
            ReadmissionRiskScore riskScore,
            long inferenceTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping readmission prediction event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("predictionPeriodDays", riskScore.getPredictionPeriodDays());
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("score", riskScore.getScore());
            inputMetrics.put("riskTier", riskScore.getRiskTier().name());
            inputMetrics.put("readmissionProbability", riskScore.getReadmissionProbability());
            inputMetrics.put("laceIndex", riskScore.getLaceIndex());
            inputMetrics.put("confidence", riskScore.getConfidence());
            inputMetrics.put("modelVersion", riskScore.getModelVersion());
            
            // Add risk factors
            ReadmissionRiskFactors factors = riskScore.getRiskFactors();
            if (factors != null) {
                inputMetrics.put("lengthOfStay", factors.getLengthOfStay());
                inputMetrics.put("charlsonIndex", factors.getCharlsonComorbidityIndex());
                inputMetrics.put("edVisitsPast6Months", factors.getEdVisitsPast6Months());
                inputMetrics.put("activeChronicConditions", factors.getActiveChronicConditions());
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PREDICTIVE_ANALYTICS)
                    .agentVersion(AGENT_VERSION)
                    .modelName(riskScore.getModelVersion())
                    .decisionType(DecisionType.HOSPITALIZATION_PREDICTION)
                    .resourceType("Patient")
                    .resourceId(patientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(inferenceTimeMs)
                    .confidenceScore(riskScore.getConfidence())
                    .reasoning(String.format("Predicted %d-day readmission risk: %s (score: %.2f, LACE: %d)",
                            riskScore.getPredictionPeriodDays(),
                            riskScore.getRiskTier().name(),
                            riskScore.getScore(),
                            riskScore.getLaceIndex()))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published readmission prediction audit event: tenant={}, patient={}, score={}, tier={}",
                    tenantId, patientId, riskScore.getScore(), riskScore.getRiskTier());

        } catch (Exception e) {
            log.error("Failed to publish readmission prediction audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish population risk stratification event
     */
    public void publishRiskStratificationEvent(
            String tenantId,
            int totalPatients,
            Map<RiskTier, Integer> tierDistribution,
            long inferenceTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping risk stratification event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("totalPatients", totalPatients);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("tierDistribution", tierDistribution);
            
            // Calculate percentages
            Map<String, Double> tierPercentages = new HashMap<>();
            for (Map.Entry<RiskTier, Integer> entry : tierDistribution.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / totalPatients;
                tierPercentages.put(entry.getKey().name(), percentage);
            }
            inputMetrics.put("tierPercentages", tierPercentages);

            // Count high-risk patients
            int highRiskCount = tierDistribution.getOrDefault(RiskTier.HIGH, 0) +
                               tierDistribution.getOrDefault(RiskTier.VERY_HIGH, 0);
            inputMetrics.put("highRiskPatientCount", highRiskCount);
            inputMetrics.put("highRiskPercentage", (highRiskCount * 100.0) / totalPatients);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PREDICTIVE_ANALYTICS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("risk-stratification-v1")
                    .decisionType(DecisionType.RISK_STRATIFICATION)
                    .resourceType("Population")
                    .resourceId(tenantId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(inferenceTimeMs)
                    .reasoning(String.format("Risk stratified %d patients: %d (%.1f%%) high-risk",
                            totalPatients,
                            highRiskCount,
                            (highRiskCount * 100.0) / totalPatients))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published risk stratification audit event: tenant={}, totalPatients={}, highRisk={}",
                    tenantId, totalPatients, highRiskCount);

        } catch (Exception e) {
            log.error("Failed to publish risk stratification audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish disease progression prediction event
     */
    public void publishDiseaseProgressionEvent(
            String tenantId,
            String patientId,
            String diseaseCode,
            Map<String, Object> progressionPrediction,
            long inferenceTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping disease progression event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("diseaseCode", diseaseCode);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.putAll(progressionPrediction);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PREDICTIVE_ANALYTICS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("disease-progression-v1")
                    .decisionType(DecisionType.CLINICAL_DECISION)
                    .resourceType("Patient")
                    .resourceId(patientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(inferenceTimeMs)
                    .reasoning(String.format("Predicted disease progression for %s", diseaseCode))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published disease progression audit event: tenant={}, patient={}, disease={}",
                    tenantId, patientId, diseaseCode);

        } catch (Exception e) {
            log.error("Failed to publish disease progression audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish cost prediction event
     */
    public void publishCostPredictionEvent(
            String tenantId,
            String patientId,
            double predictedCost,
            Map<String, Object> costBreakdown,
            long inferenceTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping cost prediction event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("patientId", patientId);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("predictedCost", predictedCost);
            inputMetrics.put("costBreakdown", costBreakdown);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PREDICTIVE_ANALYTICS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("cost-prediction-v1")
                    .decisionType(DecisionType.CLINICAL_DECISION)
                    .resourceType("Patient")
                    .resourceId(patientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(inferenceTimeMs)
                    .reasoning(String.format("Predicted healthcare cost: $%.2f", predictedCost))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published cost prediction audit event: tenant={}, patient={}, cost=${}",
                    tenantId, patientId, predictedCost);

        } catch (Exception e) {
            log.error("Failed to publish cost prediction audit event: {}", e.getMessage(), e);
        }
    }
}
