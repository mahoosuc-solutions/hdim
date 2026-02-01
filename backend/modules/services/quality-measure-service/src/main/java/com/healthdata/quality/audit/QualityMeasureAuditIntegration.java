package com.healthdata.quality.audit;

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
import java.util.Map;
import java.util.UUID;

/**
 * Audit integration for quality measure service
 * Publishes AI decision events for quality measure calculations and CDS recommendations
 */
@Service
@Slf4j
public class QualityMeasureAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "quality-measure";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public QualityMeasureAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish quality measure calculation event
     */
    public void publishMeasureCalculationEvent(
            String tenantId,
            UUID patientId,
            String measureId,
            boolean measureMet,
            Map<String, Object> measureResult,
            long calculationTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping measure calculation event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("measureId", measureId);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("measureMet", measureMet);
            
            // Additional result details
            if (measureResult != null) {
                inputMetrics.putAll(measureResult);
            }

            DecisionType decisionType = measureMet ? DecisionType.MEASURE_MET : DecisionType.MEASURE_NOT_MET;

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CQL_ENGINE)
                    .agentVersion(AGENT_VERSION)
                    .modelName("quality-measure-evaluator")
                    .decisionType(decisionType)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(calculationTimeMs)
                    .reasoning(String.format("Quality measure %s evaluation: %s",
                            measureId, measureMet ? "MET" : "NOT MET"))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published measure calculation audit event: tenant={}, patient={}, measure={}, result={}",
                    tenantId, patientId, measureId, measureMet);

        } catch (Exception e) {
            log.error("Failed to publish measure calculation audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish CDS recommendation event
     */
    public void publishCdsRecommendationEvent(
            String tenantId,
            UUID patientId,
            String ruleCode,
            String recommendationType,
            String recommendation,
            String severity,
            Map<String, Object> ruleContext,
            long evaluationTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping CDS recommendation event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("ruleCode", ruleCode);
            inputMetrics.put("recommendationType", recommendationType);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("recommendation", recommendation);
            inputMetrics.put("severity", severity);
            
            // Rule context
            if (ruleContext != null) {
                inputMetrics.putAll(ruleContext);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CQL_ENGINE)
                    .agentVersion(AGENT_VERSION)
                    .modelName("cds-rules-engine")
                    .decisionType(DecisionType.CDS_RECOMMENDATION)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(evaluationTimeMs)
                    .reasoning(String.format("CDS rule %s triggered: %s (severity: %s)",
                            ruleCode, recommendationType, severity))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published CDS recommendation audit event: tenant={}, patient={}, rule={}, severity={}",
                    tenantId, patientId, ruleCode, severity);

        } catch (Exception e) {
            log.error("Failed to publish CDS recommendation audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish clinical alert event
     */
    public void publishClinicalAlertEvent(
            String tenantId,
            UUID patientId,
            String alertType,
            String alertSeverity,
            String alertMessage,
            Map<String, Object> alertContext,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping clinical alert event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("alertType", alertType);
            inputMetrics.put("alertSeverity", alertSeverity);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("alertMessage", alertMessage);
            
            // Alert context
            if (alertContext != null) {
                inputMetrics.putAll(alertContext);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.ANOMALY_DETECTOR)
                    .agentVersion(AGENT_VERSION)
                    .modelName("clinical-alert-system")
                    .decisionType(DecisionType.CLINICAL_DECISION)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("Clinical alert: %s (%s severity)",
                            alertType, alertSeverity))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published clinical alert audit event: tenant={}, patient={}, type={}, severity={}",
                    tenantId, patientId, alertType, alertSeverity);

        } catch (Exception e) {
            log.error("Failed to publish clinical alert audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish population measure calculation event
     */
    public void publishPopulationMeasureEvent(
            String tenantId,
            String measureId,
            int totalPatients,
            int numerator,
            int denominator,
            double performanceRate,
            long calculationTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping population measure event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("measureId", measureId);
            inputMetrics.put("totalPatients", totalPatients);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("numerator", numerator);
            inputMetrics.put("denominator", denominator);
            inputMetrics.put("performanceRate", performanceRate);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CQL_ENGINE)
                    .agentVersion(AGENT_VERSION)
                    .modelName("population-quality-analyzer")
                    .decisionType(DecisionType.QUALITY_MEASURE_RESULT)
                    .resourceType("Population")
                    .resourceId(tenantId)
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(calculationTimeMs)
                    .reasoning(String.format("Population measure %s: %d/%d = %.2f%% (%d total patients)",
                            measureId, numerator, denominator, performanceRate, totalPatients))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published population measure audit event: tenant={}, measure={}, rate={}%",
                    tenantId, measureId, performanceRate);

        } catch (Exception e) {
            log.error("Failed to publish population measure audit event: {}", e.getMessage(), e);
        }
    }
}
