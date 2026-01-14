package com.healthdata.agent.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.core.AgentOrchestrator.AgentResponse;
import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.tool.ToolDefinition;
import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit integration for Agent Runtime Service.
 * Publishes AI agent decision events to the audit trail for:
 * - Agent execution completions
 * - Tool execution decisions
 * - Guardrail blocks
 * - PHI access by agents
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRuntimeAuditIntegration {

    private final AIAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;

    @Value("${audit.kafka.enabled:true}")
    private boolean auditEnabled;

    /**
     * Agent ID for all agent runtime decisions.
     */
    private static final String AGENT_ID = "ai-agent-runtime";

    /**
     * Publish an agent execution completion event.
     * This captures the full agent decision including the user prompt, agent response, and token usage.
     *
     * @param context Agent execution context
     * @param request User request to the agent
     * @param response Agent response
     * @param executingUser User who initiated the agent execution
     * @param executionTimeMs Time taken to execute the agent (milliseconds)
     */
    public void publishAgentExecutionEvent(
            AgentContext context,
            String userMessage,
            AgentResponse response,
            String executingUser,
            Long executionTimeMs) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping agent execution event");
            return;
        }

        try {
            // Determine decision type based on response
            AIAgentDecisionEvent.DecisionType decisionType = determineDecisionType(response);

            // Build metrics (combining input and output)
            Map<String, Object> metrics = new HashMap<>();
            // Input data
            metrics.put("message", userMessage);
            metrics.put("agentType", context.getAgentType());
            metrics.put("sessionId", context.getSessionId());
            if (context.getPatientId() != null) {
                metrics.put("patientId", context.getPatientId());
            }
            if (context.getEncounterId() != null) {
                metrics.put("encounterId", context.getEncounterId());
            }
            // Output data
            metrics.put("success", response.success());
            if (response.content() != null) {
                metrics.put("content", response.content());
            }
            if (response.error() != null) {
                metrics.put("error", response.error());
            }
            metrics.put("blocked", response.blocked());
            if (response.blockReason() != null) {
                metrics.put("blockReason", response.blockReason());
            }
            if (response.model() != null) {
                metrics.put("model", response.model());
            }
            if (response.usage() != null) {
                Map<String, Object> usage = new HashMap<>();
                usage.put("inputTokens", response.usage().getInputTokens());
                usage.put("outputTokens", response.usage().getOutputTokens());
                usage.put("totalTokens", response.usage().getTotalTokens());
                metrics.put("usage", usage);
            }
            if (executionTimeMs != null) {
                metrics.put("executionTimeMs", executionTimeMs);
            }

            // Build and publish audit event
            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(context.getTenantId())
                    .correlationId(context.getCorrelationId())
                    .agentId(AGENT_ID)
                    .agentType(AIAgentDecisionEvent.AgentType.AI_AGENT)
                    .agentVersion("1.0.0")
                    .modelName(response.model() != null ? response.model() : "unknown")
                    .decisionType(decisionType)
                    .resourceType("AgentExecution")
                    .resourceId(context.getPatientId() != null ? context.getPatientId() : "N/A")
                    .confidenceScore(response.success() ? 0.9 : 0.0)
                    .reasoning("AI agent execution for: " + context.getAgentType())
                    .inputMetrics(metrics)
                    .build();

            auditEventPublisher.publishAIDecision(event);

            log.debug("Published agent execution audit event: tenant={}, agentType={}, decisionType={}, correlationId={}",
                    context.getTenantId(), context.getAgentType(), decisionType, context.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to publish agent execution audit event: {}", e.getMessage(), e);
            // Don't throw - auditing should not break business logic
        }
    }

    /**
     * Publish a tool execution event.
     * This captures when an agent decides to execute a tool.
     *
     * @param context Agent execution context
     * @param toolCall Tool call from the LLM
     * @param toolDefinition Definition of the tool being executed
     * @param toolResult Result from the tool execution
     * @param executingUser User who initiated the agent execution
     */
    public void publishToolExecutionEvent(
            AgentContext context,
            LLMRequest.ToolCall toolCall,
            ToolDefinition toolDefinition,
            JsonNode toolResult,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping tool execution event");
            return;
        }

        try {
            // Build metrics (combining input and output)
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("toolName", toolCall.getName());
            metrics.put("toolCallId", toolCall.getId());
            metrics.put("toolCategory", toolDefinition.getCategory().name());
            metrics.put("requiresApproval", toolDefinition.isRequiresApproval());
            metrics.put("arguments", toolCall.getArguments());
            metrics.put("toolResult", objectMapper.convertValue(toolResult, Map.class));

            // Build and publish audit event
            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(context.getTenantId())
                    .correlationId(context.getCorrelationId())
                    .agentId(AGENT_ID)
                    .agentType(AIAgentDecisionEvent.AgentType.AI_AGENT)
                    .agentVersion("1.0.0")
                    .modelName("tool-executor")
                    .decisionType(AIAgentDecisionEvent.DecisionType.TOOL_EXECUTION)
                    .resourceType("ToolExecution")
                    .resourceId(context.getPatientId() != null ? context.getPatientId() : "N/A")
                    .confidenceScore(1.0) // Tool execution is deterministic
                    .reasoning("Agent tool execution: " + toolCall.getName())
                    .inputMetrics(metrics)
                    .build();

            auditEventPublisher.publishAIDecision(event);

            log.debug("Published tool execution audit event: tenant={}, tool={}, correlationId={}",
                    context.getTenantId(), toolCall.getName(), context.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to publish tool execution audit event: {}", e.getMessage(), e);
            // Don't throw - auditing should not break business logic
        }
    }

    /**
     * Publish a guardrail block event.
     * This captures when an agent's response is blocked by clinical safety guardrails.
     *
     * @param context Agent execution context
     * @param responseContent Content that was blocked
     * @param blockReason Reason for blocking
     * @param violations List of guardrail violations
     * @param executingUser User who initiated the agent execution
     */
    public void publishGuardrailBlockEvent(
            AgentContext context,
            String responseContent,
            String blockReason,
            List<String> violations,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping guardrail block event");
            return;
        }

        try {
            // Build metrics (combining input and output)
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("blockedContent", responseContent);
            metrics.put("agentType", context.getAgentType());
            metrics.put("blocked", true);
            metrics.put("blockReason", blockReason);
            metrics.put("violations", violations);

            // Build and publish audit event
            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(context.getTenantId())
                    .correlationId(context.getCorrelationId())
                    .agentId(AGENT_ID)
                    .agentType(AIAgentDecisionEvent.AgentType.AI_AGENT)
                    .agentVersion("1.0.0")
                    .modelName("guardrail-engine")
                    .decisionType(AIAgentDecisionEvent.DecisionType.GUARDRAIL_BLOCK)
                    .resourceType("GuardrailCheck")
                    .resourceId(context.getPatientId() != null ? context.getPatientId() : "N/A")
                    .confidenceScore(1.0) // Guardrail blocks are deterministic
                    .reasoning(blockReason)
                    .inputMetrics(metrics)
                    .build();

            auditEventPublisher.publishAIDecision(event);

            log.info("Published guardrail block audit event: tenant={}, reason={}, correlationId={}",
                    context.getTenantId(), blockReason, context.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to publish guardrail block audit event: {}", e.getMessage(), e);
            // Don't throw - auditing should not break business logic
        }
    }

    /**
     * Publish a PHI access event.
     * This captures when an agent accesses protected health information.
     *
     * @param context Agent execution context
     * @param resourceType Type of PHI resource accessed (e.g., Patient, Observation)
     * @param resourceId ID of the PHI resource
     * @param accessPurpose Purpose of accessing the PHI
     * @param executingUser User who initiated the agent execution
     */
    public void publishPhiAccessEvent(
            AgentContext context,
            String resourceType,
            String resourceId,
            String accessPurpose,
            String executingUser) {

        if (!auditEnabled) {
            log.debug("Audit disabled, skipping PHI access event");
            return;
        }

        try {
            // Build metrics (combining input and output)
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("resourceType", resourceType);
            metrics.put("resourceId", resourceId);
            metrics.put("accessPurpose", accessPurpose);
            metrics.put("agentType", context.getAgentType());
            metrics.put("accessGranted", true);
            metrics.put("timestamp", System.currentTimeMillis());

            // Build and publish audit event
            AIAgentDecisionEvent event = AIAgentDecisionEvent.builder()
                    .eventId(UUID.randomUUID())
                    .timestamp(Instant.now())
                    .tenantId(context.getTenantId())
                    .correlationId(context.getCorrelationId())
                    .agentId(AGENT_ID)
                    .agentType(AIAgentDecisionEvent.AgentType.AI_AGENT)
                    .agentVersion("1.0.0")
                    .modelName("phi-access-auditor")
                    .decisionType(AIAgentDecisionEvent.DecisionType.PHI_ACCESS)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .confidenceScore(1.0) // Access logging is deterministic
                    .reasoning("PHI access for: " + accessPurpose)
                    .inputMetrics(metrics)
                    .build();

            auditEventPublisher.publishAIDecision(event);

            log.debug("Published PHI access audit event: tenant={}, resourceType={}, resourceId={}, correlationId={}",
                    context.getTenantId(), resourceType, resourceId, context.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to publish PHI access audit event: {}", e.getMessage(), e);
            // Don't throw - auditing should not break business logic
        }
    }

    /**
     * Determine the decision type based on the agent response.
     */
    private AIAgentDecisionEvent.DecisionType determineDecisionType(AgentResponse response) {
        if (response.blocked()) {
            return AIAgentDecisionEvent.DecisionType.GUARDRAIL_BLOCK;
        } else if (!response.success()) {
            return AIAgentDecisionEvent.DecisionType.AI_DECISION_FAILED;
        } else {
            return AIAgentDecisionEvent.DecisionType.AI_RECOMMENDATION;
        }
    }
}
