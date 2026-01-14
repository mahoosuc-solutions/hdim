package com.healthdata.priorauth.audit;

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
 * Audit integration for prior authorization service
 * Publishes AI decision events for prior auth requests, approvals, and denials
 * Critical for CMS Interoperability and Prior Authorization Rule (CMS-0057-F) compliance
 */
@Service
@Slf4j
public class PriorAuthAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    private static final String AGENT_ID = "prior-auth-workflow";
    private static final String AGENT_VERSION = "1.0.0";

    @Autowired
    public PriorAuthAuditIntegration(
            AIAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper) {
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish prior auth request creation event
     */
    public void publishPriorAuthRequestEvent(
            String tenantId,
            UUID requestId,
            UUID patientId,
            String payerId,
            String serviceCode,
            String urgency,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping prior auth request event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("requestId", requestId.toString());
            inputMetrics.put("patientId", patientId.toString());
            inputMetrics.put("payerId", payerId);
            inputMetrics.put("serviceCode", serviceCode);
            inputMetrics.put("urgency", urgency);
            inputMetrics.put("executingUser", executingUser);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(requestId.toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CLINICAL_WORKFLOW)
                    .agentVersion(AGENT_VERSION)
                    .modelName("cms-pas-davinci")
                    .decisionType(DecisionType.PRIOR_AUTH_REQUEST)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("Prior auth request created for service %s (urgency: %s)",
                            serviceCode, urgency))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.PENDING)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published prior auth request audit event: tenant={}, patient={}, request={}, service={}",
                    tenantId, patientId, requestId, serviceCode);

        } catch (Exception e) {
            log.error("Failed to publish prior auth request audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish prior auth decision event (approval/denial)
     */
    public void publishPriorAuthDecisionEvent(
            String tenantId,
            UUID requestId,
            UUID patientId,
            String payerId,
            String decision,
            String decisionReason,
            boolean approved,
            long processingTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping prior auth decision event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("requestId", requestId.toString());
            inputMetrics.put("patientId", patientId.toString());
            inputMetrics.put("payerId", payerId);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("decision", decision);
            inputMetrics.put("decisionReason", decisionReason);
            inputMetrics.put("approved", approved);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(requestId.toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CLINICAL_WORKFLOW)
                    .agentVersion(AGENT_VERSION)
                    .modelName("cms-pas-davinci")
                    .decisionType(DecisionType.PRIOR_AUTH_DECISION)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(processingTimeMs)
                    .reasoning(String.format("Prior auth decision: %s (reason: %s)",
                            decision, decisionReason))
                    .outcome(approved ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPROVED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published prior auth decision audit event: tenant={}, patient={}, request={}, decision={}",
                    tenantId, patientId, requestId, decision);

        } catch (Exception e) {
            log.error("Failed to publish prior auth decision audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish prior auth submission event
     */
    public void publishPriorAuthSubmissionEvent(
            String tenantId,
            UUID requestId,
            UUID patientId,
            String payerId,
            boolean submissionSuccess,
            String errorMessage,
            long submissionTimeMs,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping prior auth submission event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("requestId", requestId.toString());
            inputMetrics.put("patientId", patientId.toString());
            inputMetrics.put("payerId", payerId);
            inputMetrics.put("executingUser", executingUser);
            
            // Output/result data
            inputMetrics.put("submissionSuccess", submissionSuccess);
            if (!submissionSuccess && errorMessage != null) {
                inputMetrics.put("errorMessage", errorMessage);
            }

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(requestId.toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CLINICAL_WORKFLOW)
                    .agentVersion(AGENT_VERSION)
                    .modelName("payer-api-" + payerId.toLowerCase())
                    .decisionType(DecisionType.PRIOR_AUTH_REQUEST)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .inferenceTimeMs(submissionTimeMs)
                    .reasoning(String.format("Submitted prior auth to payer %s: %s",
                            payerId, submissionSuccess ? "SUCCESS" : "FAILED"))
                    .outcome(submissionSuccess ? 
                            AIAgentDecisionEvent.DecisionOutcome.APPLIED : 
                            AIAgentDecisionEvent.DecisionOutcome.BLOCKED)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published prior auth submission audit event: tenant={}, patient={}, request={}, payer={}, success={}",
                    tenantId, patientId, requestId, payerId, submissionSuccess);

        } catch (Exception e) {
            log.error("Failed to publish prior auth submission audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish prior auth appeal event
     */
    public void publishPriorAuthAppealEvent(
            String tenantId,
            UUID requestId,
            UUID patientId,
            String appealReason,
            String supportingInfo,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping prior auth appeal event");
            return;
        }

        try {
            Map<String, Object> inputMetrics = new HashMap<>();
            // Input data
            inputMetrics.put("requestId", requestId.toString());
            inputMetrics.put("patientId", patientId.toString());
            inputMetrics.put("appealReason", appealReason);
            inputMetrics.put("supportingInfo", supportingInfo);
            inputMetrics.put("executingUser", executingUser);

            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(tenantId)
                    .correlationId(requestId.toString())
                    .agentId(AGENT_ID)
                    .agentType(AgentType.CLINICAL_WORKFLOW)
                    .agentVersion(AGENT_VERSION)
                    .modelName("cms-pas-appeal")
                    .decisionType(DecisionType.PRIOR_AUTH_REQUEST)
                    .resourceType("Patient")
                    .resourceId(patientId.toString())
                    .inputMetrics(inputMetrics)
                    .reasoning(String.format("Prior auth appeal initiated: %s", appealReason))
                    .outcome(AIAgentDecisionEvent.DecisionOutcome.PENDING)
                    .build();

            auditEventPublisher.publishAIDecision(event);
            log.info("Published prior auth appeal audit event: tenant={}, patient={}, request={}, reason={}",
                    tenantId, patientId, requestId, appealReason);

        } catch (Exception e) {
            log.error("Failed to publish prior auth appeal audit event: {}", e.getMessage(), e);
        }
    }
}
