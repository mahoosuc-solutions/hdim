package com.healthdata.patient.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.AgentType;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent.DecisionType;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit integration for patient service
 * Publishes AI decision events for patient data aggregation and risk scoring
 */
@Service
@Slf4j
public class PatientAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "patient-aggregation";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public PatientAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish patient health record access event
     */
    public void publishHealthRecordAccessEvent(
            String tenantId,
            String patientId,
            List<String> resourceTypes,
            int totalResources,
            boolean consentApplied,
            String accessPurpose,
            long aggregationTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping health record access event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("accessPurpose", accessPurpose);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("resourceTypes", resourceTypes);
            inputMetrics.put("totalResources", totalResources);
            inputMetrics.put("consentApplied", consentApplied);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PHI_ACCESS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("patient-data-aggregator")
                    .decisionType(DecisionType.PHI_ACCESS)
                    .resourceType("Patient")
                    .resourceId(patientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(aggregationTimeMs)
                    .reasoning(String.format("Accessed patient health record: %d resources (%s) for purpose: %s",
                            totalResources, String.join(", ", resourceTypes), accessPurpose))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published health record access audit event: tenant={}, patient={}, resources={}",
                    tenantId, patientId, totalResources);

        } catch (Exception e) {
            log.error("Failed to publish health record access audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish patient risk score calculation event
     */
    public void publishRiskScoreCalculationEvent(
            String tenantId,
            String patientId,
            Map<String, Object> riskScores,
            Map<String, Object> healthMetrics,
            long calculationTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping risk score calculation event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            if (riskScores != null) {
                inputMetrics.putAll(riskScores);
            }
            if (healthMetrics != null) {
                inputMetrics.put("healthMetrics", healthMetrics);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.PREDICTIVE_ANALYTICS)
                    .agentVersion(AGENT_VERSION)
                    .modelName("patient-risk-calculator")
                    .decisionType(DecisionType.PATIENT_RISK_SCORE)
                    .resourceType("Patient")
                    .resourceId(patientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(calculationTimeMs)
                    .reasoning("Calculated patient health risk scores and metrics")
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published risk score calculation audit event: tenant={}, patient={}",
                    tenantId, patientId);

        } catch (Exception e) {
            log.error("Failed to publish risk score calculation audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish pre-visit planning access event
     */
    public void publishPreVisitPlanningEvent(
            String tenantId,
            String providerId,
            String patientId,
            int careGapCount,
            int medicationCount,
            int recentResultCount,
            int suggestedAgendaItems,
            long planningTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping pre-visit planning event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("providerId", providerId);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("careGapCount", careGapCount);
            inputMetrics.put("medicationCount", medicationCount);
            inputMetrics.put("recentResultCount", recentResultCount);
            inputMetrics.put("suggestedAgendaItems", suggestedAgendaItems);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.AI_AGENT)
                    .agentVersion(AGENT_VERSION)
                    .modelName("pre-visit-planner")
                    .decisionType(DecisionType.CDS_RECOMMENDATION)
                    .resourceType("Patient")
                    .resourceId(patientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(planningTimeMs)
                    .reasoning(String.format("Generated pre-visit summary: %d care gaps, %d medications, %d recent results, %d agenda items",
                            careGapCount, medicationCount, recentResultCount, suggestedAgendaItems))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published pre-visit planning audit event: tenant={}, patient={}, provider={}",
                    tenantId, patientId, providerId);

        } catch (Exception e) {
            log.error("Failed to publish pre-visit planning audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish consent-filtered data access event
     */
    public void publishConsentFilteredAccessEvent(
            String tenantId,
            String patientId,
            List<String> restrictedResourceTypes,
            List<String> sensitiveCategories,
            int filteredResourceCount,
            String accessPurpose,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping consent-filtered access event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("accessPurpose", accessPurpose);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("restrictedResourceTypes", restrictedResourceTypes);
            inputMetrics.put("sensitiveCategories", sensitiveCategories);
            inputMetrics.put("filteredResourceCount", filteredResourceCount);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CONSENT_VALIDATOR)
                    .agentVersion(AGENT_VERSION)
                    .modelName("consent-filter")
                    .decisionType(DecisionType.PHI_ACCESS)
                    .resourceType("Patient")
                    .resourceId(patientId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(0L)
                    .reasoning(String.format("Applied consent filters: %d resources filtered from %d restricted types",
                            filteredResourceCount, restrictedResourceTypes.size()))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published consent-filtered access audit event: tenant={}, patient={}, filtered={}",
                    tenantId, patientId, filteredResourceCount);

        } catch (Exception e) {
            log.error("Failed to publish consent-filtered access audit event: {}", e.getMessage(), e);
        }
    }
}
