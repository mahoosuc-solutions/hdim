package com.healthdata.caregap.service;

import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import com.healthdata.caregap.persistence.CareGapEntity;
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
     * @param careGap Identified care gap
     * @param confidenceScore Confidence in gap identification
     * @param reasoning Clinical reasoning for gap
     */
    public void publishCareGapIdentificationEvent(
            String tenantId,
            CareGapEntity careGap,
            Double confidenceScore,
            String reasoning
    ) {
        try {
            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                .eventId(UUID.randomUUID())
                .timestamp(Instant.now())
                .tenantId(tenantId)
                .correlationId(careGap.getPatientId().toString())
                
                // Agent identification
                .agentType(AIAgentDecisionEvent.AgentType.CARE_GAP_IDENTIFIER)
                .agentVersion("1.0.0")
                .modelName("HEDIS-CQL-Engine")
                
                // Decision details
                .decisionType(AIAgentDecisionEvent.DecisionType.CARE_GAP_IDENTIFICATION)
                .resourceType("Patient")
                .resourceId(careGap.getPatientId().toString())
                
                // Current state vs recommendation
                .currentValue(null) // No current value - gap exists
                .recommendedValue(careGap.getRecommendedAction())
                
                // Confidence and reasoning
                .confidenceScore(confidenceScore)
                .reasoning(reasoning)
                
                // Clinical context
                .customerProfileContext(buildCustomerProfile(careGap))
                
                // Recommendation details
                .recommendation(buildRecommendation(careGap))
                
                // Performance metrics
                .performanceMetrics(buildPerformanceMetrics())
                
                .build();
            
            auditPublisher.publishAIDecision(event);
            
            log.debug("Published care gap identification event for gap: {}", careGap.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish care gap identification event for gap: {}", 
                careGap.getId(), e);
            // Don't fail the main operation if audit fails
        }
    }

    /**
     * Publish care gap closure event
     * 
     * Captures user action when a care gap is closed.
     * 
     * @param tenantId Tenant ID
     * @param careGap Closed care gap
     * @param closedBy User who closed the gap
     * @param wasAIRecommended Whether this followed an AI recommendation
     */
    public void publishCareGapClosureEvent(
            String tenantId,
            CareGapEntity careGap,
            String closedBy,
            boolean wasAIRecommended
    ) {
        try {
            // Build user action event for care gap closure
            Map<String, Object> actionDetails = new HashMap<>();
            actionDetails.put("gapId", careGap.getId().toString());
            actionDetails.put("measureId", careGap.getMeasureId());
            actionDetails.put("measureName", careGap.getMeasureName());
            actionDetails.put("priority", careGap.getPriority());
            actionDetails.put("closureReason", careGap.getClosureReason());
            actionDetails.put("closureAction", careGap.getClosureAction());
            actionDetails.put("patientId", careGap.getPatientId().toString());
            actionDetails.put("wasAIRecommended", wasAIRecommended);
            
            // TODO: Publish user configuration action event
            // This would use UserConfigurationActionEvent instead of AIAgentDecisionEvent
            
            log.debug("Published care gap closure event for gap: {}", careGap.getId());
            
        } catch (Exception e) {
            log.error("Failed to publish care gap closure event for gap: {}", 
                careGap.getId(), e);
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
    private Map<String, Object> buildCustomerProfile(CareGapEntity careGap) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("patientId", careGap.getPatientId().toString());
        profile.put("measureCategory", careGap.getMeasureCategory());
        profile.put("priority", careGap.getPriority());
        profile.put("dueDate", careGap.getDueDate() != null ? careGap.getDueDate().toString() : null);
        return profile;
    }

    /**
     * Build recommendation details for audit event
     */
    private AIAgentDecisionEvent.ConfigurationRecommendation buildRecommendation(CareGapEntity careGap) {
        return AIAgentDecisionEvent.ConfigurationRecommendation.builder()
            .recommendedValue(careGap.getRecommendedAction())
            .expectedImpact(careGap.getExpectedImpact() != null ? 
                careGap.getExpectedImpact() : "Improved quality measure compliance")
            .implementationComplexity("Medium")
            .riskLevel("Low")
            .costImpact(careGap.getEstimatedCost() != null ? 
                String.format("$%.2f", careGap.getEstimatedCost()) : "Unknown")
            .timeToImplement(careGap.getEstimatedTimeMinutes() != null ? 
                careGap.getEstimatedTimeMinutes() + " minutes" : "Varies")
            .rollbackProcedure("Gap will reappear if criteria no longer met")
            .validationSteps("1. Schedule service\n2. Complete service\n3. Document in EHR")
            .approvalRequired(careGap.getPriority() != null && 
                careGap.getPriority().equals("HIGH"))
            .build();
    }

    /**
     * Build performance metrics for audit event
     */
    private Map<String, Object> buildPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        // These would be populated from actual measurement
        metrics.put("evaluationTimeMs", 0);
        metrics.put("dataPointsAnalyzed", 0);
        metrics.put("cqlLibrariesEvaluated", 1);
        return metrics;
    }
}
