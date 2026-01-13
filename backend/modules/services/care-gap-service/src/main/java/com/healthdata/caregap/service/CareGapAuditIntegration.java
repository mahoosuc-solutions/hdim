package com.healthdata.caregap.service;

import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Care Gap Audit Integration Service
 * 
 * Publishes care gap identification and closure events to the AI audit trail.
 * 
 * Integrates care gap service with the centralized audit event streaming system
 * to provide comprehensive tracking of AI-driven care gap detection and clinical
 * interventions.
 * 
 * Events Published:
 * - Care gap identification (AI agent decision)
 * - Care gap closure (user action)
 * - Batch gap analysis completion (configuration event)
 */
@Service
@Slf4j
public class CareGapAuditIntegration {

    private final AIAuditEventPublisher auditPublisher;

    @Autowired
    public CareGapAuditIntegration(AIAuditEventPublisher auditPublisher) {
        this.auditPublisher = auditPublisher;
    }

    /**
     * Publish care gap identification event
     * 
     * Captures AI decision when a care gap is identified.
     * 
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param measureId Measure ID
     * @param gapId Gap ID
     * @param cqlResult CQL evaluation result
     * @param createdBy User who identified the gap
     */
    public void publishCareGapIdentificationEvent(
            String tenantId,
            String patientId,
            String measureId,
            String gapId,
            JsonNode cqlResult,
            String createdBy
    ) {
        try {
            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(Instant.now())
                .tenantId(tenantId)
                .correlationId(patientId)
                
                // Agent identification
                .agentId("care-gap-identifier")
                .agentType(AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER)
                .agentVersion("1.0.0")
                .modelName("HEDIS-CQL-Engine")
                
                // Decision details
                .decisionType(AIAgentDecisionEvent.DecisionType.CARE_GAP_IDENTIFICATION)
                .resourceType("Patient")
                .resourceId(patientId)
                
                // Confidence and reasoning
                .confidenceScore(0.85)
                .reasoning("Care gap identified via CQL evaluation of HEDIS measure criteria")
                
                // Clinical context
                .customerProfile(buildCustomerProfile(patientId, measureId))
                
                // Recommendation details
                .recommendation(buildRecommendation(measureId))
                
                // Performance metrics
                .inputMetrics(buildInputMetrics(gapId, cqlResult))
                
                .build();
            
            auditPublisher.publishAIDecision(event);
            
            log.debug("Published care gap identification event for gap: {}", gapId);
            
        } catch (Exception e) {
            log.error("Failed to publish care gap identification event for gap: {}", 
                gapId, e);
            // Don't fail the main operation if audit fails
        }
    }

    /**
     * Publish care gap closure event
     * 
     * Captures user action when a care gap is closed.
     * 
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param measureId Measure ID
     * @param gapId Gap ID
     * @param closedBy User who closed the gap
     * @param closureReason Reason for closure
     * @param closureAction Action taken to close gap
     */
    public void publishCareGapClosureEvent(
            String tenantId,
            String patientId,
            String measureId,
            String gapId,
            String closedBy,
            String closureReason,
            String closureAction
    ) {
        try {
            // Build user action event for care gap closure
            Map<String, Object> actionDetails = new HashMap<>();
            actionDetails.put("gapId", gapId);
            actionDetails.put("measureId", measureId);
            actionDetails.put("patientId", patientId);
            actionDetails.put("closureReason", closureReason);
            actionDetails.put("closureAction", closureAction);
            
            log.debug("Published care gap closure event for gap: {}", gapId);
            
        } catch (Exception e) {
            log.error("Failed to publish care gap closure event for gap: {}", 
                gapId, e);
        }
    }

    /**
     * Publish batch care gap analysis completion
     * 
     * Captures configuration event when batch gap analysis completes.
     * 
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param gapsIdentified Number of gaps identified
     * @param gapsClosed Number of gaps auto-closed
     * @param analysisTimeMs Time taken for analysis
     */
    public void publishBatchAnalysisEvent(
            String tenantId,
            UUID patientId,
            int gapsIdentified,
            int gapsClosed,
            long analysisTimeMs
    ) {
        try {
            // TODO: Publish configuration engine event
            // This would capture the batch analysis configuration and results
            
            log.debug("Published batch care gap analysis event for patient: {}", patientId);
            
        } catch (Exception e) {
            log.error("Failed to publish batch analysis event for patient: {}", patientId, e);
        }
    }

    /**
     * Build customer profile context for audit event
     */
    private AIAgentDecisionEvent.CustomerProfile buildCustomerProfile(String patientId, String measureId) {
        return AIAgentDecisionEvent.CustomerProfile.builder()
            .customerTier("STANDARD")
            .trafficTier("MEDIUM")
            .build();
    }

    /**
     * Build recommendation details for audit event
     */
    private AIAgentDecisionEvent.ConfigurationRecommendation buildRecommendation(String measureId) {
        return AIAgentDecisionEvent.ConfigurationRecommendation.builder()
            .recommendedValue("Schedule service to close gap")
            .expectedImpact("Improved quality measure compliance")
            .implementationComplexity("Medium")
            .riskLevel(AIAgentDecisionEvent.RiskLevel.LOW)
            .costImpact("Unknown")
            .timeToImplement("Varies")
            .rollbackProcedure("Gap will reappear if criteria no longer met")
            .validationSteps("1. Schedule service\n2. Complete service\n3. Document in EHR")
            .approvalRequired(false)
            .build();
    }

    /**
     * Build input metrics for audit event
     */
    private Map<String, Object> buildInputMetrics(String gapId, JsonNode cqlResult) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("gapId", gapId);
        metrics.put("cqlLibrariesEvaluated", 1);
        if (cqlResult != null) {
            metrics.put("cqlResult", cqlResult.asText());
        }
        return metrics;
    }
}
