package com.healthdata.approval.audit;

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
 * Audit integration for approval service
 * Publishes AI decision events for Human-in-the-Loop approval workflows
 */
@Service
@Slf4j
public class ApprovalAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "approval-workflow";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public ApprovalAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    public void publishApprovalRequestEvent(String tenantId, UUID requestId, String requestType, 
            String entityType, String entityId, String riskLevel, double confidenceScore, String executingUser) {
        if (!auditEnabled) return;
        try {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("requestId", requestId.toString());
            metrics.put("requestType", requestType);
            metrics.put("entityType", entityType);
            metrics.put("entityId", entityId);
            metrics.put("riskLevel", riskLevel);
            metrics.put("confidenceScore", confidenceScore);
            metrics.put("executingUser", executingUser);
            
            auditEventPublisher.publishAIDecision(AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID()).timestamp(Instant.now()).tenantId(tenantId).correlationId(requestId.toString())
                    .agentId(AGENT_ID).agentType(AgentType.CLINICAL_WORKFLOW).agentVersion(AGENT_VERSION)
                    .modelName("human-in-the-loop").decisionType(DecisionType.APPROVAL_REQUEST)
                    .resourceType(entityType).resourceId(entityId).inputMetrics(metrics)
                    .reasoning(String.format("Approval requested: %s (risk: %s, confidence: %.2f)", requestType, riskLevel, confidenceScore))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.PENDING).build());
            log.info("Published approval request event: tenant={}, request={}, type={}", tenantId, requestId, requestType);
        } catch (Exception e) {
            log.error("Failed to publish approval request event: {}", e.getMessage(), e);
        }
    }

    public void publishApprovalDecisionEvent(String tenantId, UUID requestId, String entityType, String entityId, 
            boolean approved, String decidedBy, String reason, long decisionTimeMs, String executingUser) {
        if (!auditEnabled) return;
        try {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("requestId", requestId.toString());
            metrics.put("entityType", entityType);
            metrics.put("entityId", entityId);
            metrics.put("approved", approved);
            metrics.put("decidedBy", decidedBy);
            metrics.put("reason", reason);
            metrics.put("executingUser", executingUser);
            
            auditEventPublisher.publishAIDecision(AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID()).timestamp(Instant.now()).tenantId(tenantId).correlationId(requestId.toString())
                    .agentId(AGENT_ID).agentType(AgentType.CLINICAL_WORKFLOW).agentVersion(AGENT_VERSION)
                    .modelName("human-reviewer").decisionType(DecisionType.APPROVAL_DECISION)
                    .resourceType(entityType).resourceId(entityId).inputMetrics(metrics).inferenceTimeMs(decisionTimeMs)
                    .reasoning(String.format("Approval %s by %s: %s", approved ? "GRANTED" : "REJECTED", decidedBy, reason))
                    .outcome(approved ? AIAgentDecisionEvent.DecisionOutcome.APPROVED : AIAgentDecisionEvent.DecisionOutcome.REJECTED_BY_USER).build());
            log.info("Published approval decision event: tenant={}, request={}, approved={}, by={}", tenantId, requestId, approved, decidedBy);
        } catch (Exception e) {
            log.error("Failed to publish approval decision event: {}", e.getMessage(), e);
        }
    }

    public void publishApprovalEscalationEvent(String tenantId, UUID requestId, String entityType, String entityId,
            String escalationReason, String escalatedTo, String executingUser) {
        if (!auditEnabled) return;
        try {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("requestId", requestId.toString());
            metrics.put("entityType", entityType);
            metrics.put("entityId", entityId);
            metrics.put("escalationReason", escalationReason);
            metrics.put("escalatedTo", escalatedTo);
            metrics.put("executingUser", executingUser);
            
            auditEventPublisher.publishAIDecision(AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID()).timestamp(Instant.now()).tenantId(tenantId).correlationId(requestId.toString())
                    .agentId(AGENT_ID).agentType(AgentType.CLINICAL_WORKFLOW).agentVersion(AGENT_VERSION)
                    .modelName("escalation-manager").decisionType(DecisionType.APPROVAL_REQUEST)
                    .resourceType(entityType).resourceId(entityId).inputMetrics(metrics)
                    .reasoning(String.format("Approval escalated to %s: %s", escalatedTo, escalationReason))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.PENDING).build());
            log.info("Published approval escalation event: tenant={}, request={}, to={}", tenantId, requestId, escalatedTo);
        } catch (Exception e) {
            log.error("Failed to publish approval escalation event: {}", e.getMessage(), e);
        }
    }
}
