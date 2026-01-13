package com.healthdata.cql.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthdata.audit.event.AIAgentDecisionEvent;
import com.healthdata.audit.event.AIAgentType;
import com.healthdata.audit.publisher.AIAuditEventPublisher;
import com.healthdata.cql.measure.MeasureResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CQL Engine Audit Integration
 * 
 * Publishes AI audit events for Clinical Quality Language (CQL) evaluation decisions.
 * Tracks measure evaluations, quality measure results, and clinical logic execution
 * for compliance and quality improvement purposes.
 * 
 * Audit Scenarios:
 * - CQL measure evaluation (HEDIS, CMS, custom measures)
 * - Clinical logic execution (care gap identification, risk stratification)
 * - Quality measure compliance assessment
 * - Template-based decision making
 * 
 * SOC 2 Compliance: CC7.2, CC8.1 (Audit logging)
 * HIPAA: 45 CFR § 164.312(b) - Audit controls for clinical decisions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CqlAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Publish audit event for CQL measure evaluation
     * 
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param measureId Measure ID (e.g., "HEDIS_CDC_A1C", "CMS_125")
     * @param evaluationId Evaluation ID for correlation
     * @param measureResult CQL evaluation result
     * @param evaluatedBy User or system performing evaluation
     * @param durationMs Evaluation execution time in milliseconds
     */
    public void publishCqlEvaluationEvent(
            String tenantId,
            String patientId,
            String measureId,
            String evaluationId,
            MeasureResult measureResult,
            String evaluatedBy,
            long durationMs
    ) {
        try {
            log.debug("Publishing CQL evaluation audit event for measure: {} patient: {}", measureId, patientId);

            // Build customer profile
            Map<String, Object> customerProfile = buildCustomerProfile(patientId, tenantId);

            // Build recommendation from measure result
            Map<String, Object> recommendation = buildMeasureRecommendation(measureResult, measureId);

            // Build reasoning from measure details
            List<String> reasoning = buildMeasureReasoning(measureResult);

            // Calculate confidence score from measure certainty
            double confidenceScore = calculateConfidenceScore(measureResult);

            // Build input parameters
            Map<String, Object> inputParams = buildInputParameters(measureResult);

            // Build performance metrics
            Map<String, Object> performanceMetrics = buildPerformanceMetrics(durationMs, measureResult);

            // Create audit event
            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .timestamp(new Date())
                    .agentType(AIAgentType.CQL_ENGINE)
                    .agentVersion("1.0.0")
                    .decisionId(evaluationId)
                    .correlationId(evaluationId)
                    .userId(evaluatedBy)
                    .customerProfile(customerProfile)
                    .inputParameters(inputParams)
                    .decisionType(measureResult.isInNumerator() ? "MEASURE_MET" : "MEASURE_NOT_MET")
                    .recommendation(recommendation)
                    .confidenceScore(confidenceScore)
                    .reasoning(reasoning)
                    .performanceMetrics(performanceMetrics)
                    .build();

            auditEventPublisher.publishDecisionEvent(event);
            log.debug("Published CQL evaluation audit event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to publish CQL evaluation audit event for measure: {} - {}", 
                    measureId, e.getMessage(), e);
            // Don't throw - audit failures should not break CQL evaluation
        }
    }

    /**
     * Publish audit event for batch CQL evaluation
     * 
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param batchId Batch evaluation ID
     * @param evaluationCount Number of measures evaluated
     * @param successCount Number of successful evaluations
     * @param failureCount Number of failed evaluations
     * @param executedBy User or system performing batch evaluation
     */
    public void publishBatchEvaluationEvent(
            String tenantId,
            String patientId,
            String batchId,
            int evaluationCount,
            int successCount,
            int failureCount,
            String executedBy
    ) {
        try {
            log.debug("Publishing batch CQL evaluation audit event for patient: {}", patientId);

            Map<String, Object> customerProfile = buildCustomerProfile(patientId, tenantId);

            ObjectNode recommendation = objectMapper.createObjectNode();
            recommendation.put("batchId", batchId);
            recommendation.put("totalEvaluations", evaluationCount);
            recommendation.put("successCount", successCount);
            recommendation.put("failureCount", failureCount);
            recommendation.put("successRate", evaluationCount > 0 ? 
                    (double) successCount / evaluationCount : 0.0);

            List<String> reasoning = Arrays.asList(
                    String.format("Batch evaluation of %d measures for patient %s", evaluationCount, patientId),
                    String.format("Success rate: %.1f%%", evaluationCount > 0 ? 
                            (successCount * 100.0 / evaluationCount) : 0.0),
                    String.format("Failed evaluations: %d", failureCount)
            );

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .tenantId(tenantId)
                    .timestamp(new Date())
                    .agentType(AIAgentType.CQL_ENGINE)
                    .agentVersion("1.0.0")
                    .decisionId(batchId)
                    .correlationId(batchId)
                    .userId(executedBy)
                    .customerProfile(customerProfile)
                    .inputParameters(Map.of(
                            "evaluationType", "BATCH",
                            "evaluationCount", evaluationCount
                    ))
                    .decisionType("BATCH_EVALUATION")
                    .recommendation(objectMapper.convertValue(recommendation, Map.class))
                    .confidenceScore(evaluationCount > 0 ? 
                            (double) successCount / evaluationCount : 0.0)
                    .reasoning(reasoning)
                    .performanceMetrics(Map.of(
                            "totalEvaluations", evaluationCount,
                            "successCount", successCount,
                            "failureCount", failureCount
                    ))
                    .build();

            auditEventPublisher.publishDecisionEvent(event);
            log.debug("Published batch CQL evaluation audit event: {}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to publish batch CQL evaluation audit event - {}", e.getMessage(), e);
        }
    }

    /**
     * Build customer profile for audit event
     */
    private Map<String, Object> buildCustomerProfile(String patientId, String tenantId) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("customerId", patientId);
        profile.put("tenantId", tenantId);
        profile.put("customerType", "PATIENT");
        return profile;
    }

    /**
     * Build recommendation from measure result
     */
    private Map<String, Object> buildMeasureRecommendation(MeasureResult result, String measureId) {
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("measureId", measureId);
        recommendation.put("inDenominator", result.isInDenominator());
        recommendation.put("inNumerator", result.isInNumerator());
        recommendation.put("inExclusion", result.isInExclusion());
        recommendation.put("measureMet", result.isInNumerator() && result.isInDenominator());
        
        if (result.getScore() != null) {
            recommendation.put("score", result.getScore());
        }
        
        if (result.getDetails() != null && !result.getDetails().isEmpty()) {
            recommendation.put("details", result.getDetails());
        }
        
        return recommendation;
    }

    /**
     * Build reasoning from measure result
     */
    private List<String> buildMeasureReasoning(MeasureResult result) {
        List<String> reasoning = new ArrayList<>();
        
        reasoning.add(String.format("Patient in denominator: %s", result.isInDenominator()));
        reasoning.add(String.format("Patient in numerator: %s", result.isInNumerator()));
        
        if (result.isInExclusion()) {
            reasoning.add("Patient in exclusion population");
        }
        
        if (result.getDetails() != null && !result.getDetails().isEmpty()) {
            result.getDetails().forEach((key, value) -> {
                reasoning.add(String.format("%s: %s", key, value));
            });
        }
        
        if (result.isInDenominator() && !result.isInNumerator()) {
            reasoning.add("Quality measure not met - care gap identified");
        } else if (result.isInNumerator()) {
            reasoning.add("Quality measure met - patient compliant");
        }
        
        return reasoning;
    }

    /**
     * Calculate confidence score from measure result
     */
    private double calculateConfidenceScore(MeasureResult result) {
        // Base confidence on data completeness and result clarity
        double confidence = 0.7; // Base confidence for CQL logic
        
        // Increase confidence if patient is clearly in denominator
        if (result.isInDenominator()) {
            confidence += 0.1;
        }
        
        // Increase confidence if we have detailed results
        if (result.getDetails() != null && !result.getDetails().isEmpty()) {
            confidence += 0.1;
        }
        
        // Increase confidence if measure has a score
        if (result.getScore() != null) {
            confidence += 0.1;
        }
        
        return Math.min(confidence, 1.0);
    }

    /**
     * Build input parameters from measure result
     */
    private Map<String, Object> buildInputParameters(MeasureResult result) {
        Map<String, Object> params = new HashMap<>();
        params.put("measureType", "CQL");
        params.put("evaluationDate", new Date());
        
        if (result.getDetails() != null) {
            params.putAll(result.getDetails());
        }
        
        return params;
    }

    /**
     * Build performance metrics
     */
    private Map<String, Object> buildPerformanceMetrics(long durationMs, MeasureResult result) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("executionTimeMs", durationMs);
        metrics.put("dataPointsEvaluated", result.getDetails() != null ? result.getDetails().size() : 0);
        metrics.put("evaluationComplexity", result.isInExclusion() ? "HIGH" : "MEDIUM");
        return metrics;
    }
}
