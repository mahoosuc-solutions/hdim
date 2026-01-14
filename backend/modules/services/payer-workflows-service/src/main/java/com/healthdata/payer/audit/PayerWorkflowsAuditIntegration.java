package com.healthdata.payer.audit;

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
 * Audit integration for payer workflows service
 * Publishes AI decision events for Medicare Advantage Star Ratings and Medicaid compliance workflows
 */
@Service
@Slf4j
public class PayerWorkflowsAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "payer-workflows";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public PayerWorkflowsAuditIntegration(AIAuditEventPublisher auditEventPublisher, ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    public void publishStarRatingCalculationEvent(String tenantId, String planId, int starRating, 
            Map<String, Double> domainScores, long calculationTimeMs, String executingUser) {
        if (!auditEnabled) return;
        try {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("planId", planId);
            metrics.put("starRating", starRating);
            metrics.put("domainScores", domainScores);
            metrics.put("calculationTimeMs", calculationTimeMs);
            metrics.put("executingUser", executingUser);
            
            auditEventPublisher.publishAIDecision(AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID()).timestamp(Instant.now()).tenantId(tenantId).correlationId(planId)
                    .agentId(AGENT_ID).agentType(AgentType.PREDICTIVE_ANALYTICS).agentVersion(AGENT_VERSION)
                    .modelName("cms-star-rating-2024").decisionType(DecisionType.PAYER_WORKFLOW_STEP)
                    .resourceType("Plan").resourceId(planId).inputMetrics(metrics).inferenceTimeMs(calculationTimeMs)
                    .reasoning(String.format("Calculated %d-star rating for plan %s", starRating, planId))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPROVED).build());
            log.info("Published star rating event: tenant={}, plan={}, stars={}", tenantId, planId, starRating);
        } catch (Exception e) {
            log.error("Failed to publish star rating event: {}", e.getMessage(), e);
        }
    }

    public void publishMedicaidComplianceEvent(String tenantId, String state, String reportType, 
            boolean compliant, Map<String, Object> metrics, long processingTimeMs, String executingUser) {
        if (!auditEnabled) return;
        try {
            Map<String, Object> auditMetrics = new HashMap<>();
            auditMetrics.put("state", state);
            auditMetrics.put("reportType", reportType);
            auditMetrics.put("compliant", compliant);
            auditMetrics.putAll(metrics);
            auditMetrics.put("executingUser", executingUser);
            
            auditEventPublisher.publishAIDecision(AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID()).timestamp(Instant.now()).tenantId(tenantId).correlationId(UUID.randomUUID().toString())
                    .agentId(AGENT_ID).agentType(AgentType.CLINICAL_WORKFLOW).agentVersion(AGENT_VERSION)
                    .modelName("medicaid-compliance-" + state.toLowerCase()).decisionType(DecisionType.PAYER_WORKFLOW_STEP)
                    .resourceType("ComplianceReport").resourceId(state + "-" + reportType).inputMetrics(auditMetrics).inferenceTimeMs(processingTimeMs)
                    .reasoning(String.format("Medicaid %s compliance check for %s: %s", reportType, state, compliant ? "PASS" : "FAIL"))
                    .outcome(compliant ? AIAgentDecisionEvent.DecisionOutcome.APPROVED : AIAgentDecisionEvent.DecisionOutcome.BLOCKED).build());
            log.info("Published Medicaid compliance event: tenant={}, state={}, type={}, compliant={}", tenantId, state, reportType, compliant);
        } catch (Exception e) {
            log.error("Failed to publish Medicaid compliance event: {}", e.getMessage(), e);
        }
    }

    public void publishPayerWorkflowStepEvent(String tenantId, String workflowId, String stepName, 
            String stepStatus, Map<String, Object> stepData, String executingUser) {
        if (!auditEnabled) return;
        try {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("workflowId", workflowId);
            metrics.put("stepName", stepName);
            metrics.put("stepStatus", stepStatus);
            metrics.putAll(stepData);
            metrics.put("executingUser", executingUser);
            
            auditEventPublisher.publishAIDecision(AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID()).timestamp(Instant.now()).tenantId(tenantId).correlationId(workflowId)
                    .agentId(AGENT_ID).agentType(AgentType.CLINICAL_WORKFLOW).agentVersion(AGENT_VERSION)
                    .modelName("payer-workflow-orchestrator").decisionType(DecisionType.PAYER_WORKFLOW_STEP)
                    .resourceType("Workflow").resourceId(workflowId).inputMetrics(metrics)
                    .reasoning(String.format("Workflow step %s: %s", stepName, stepStatus))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.APPLIED).build());
            log.info("Published workflow step event: tenant={}, workflow={}, step={}, status={}", tenantId, workflowId, stepName, stepStatus);
        } catch (Exception e) {
            log.error("Failed to publish workflow step event: {}", e.getMessage(), e);
        }
    }
}
