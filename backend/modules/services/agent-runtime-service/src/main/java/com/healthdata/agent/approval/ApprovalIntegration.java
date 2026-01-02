package com.healthdata.agent.approval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agent.client.ApprovalServiceClient;
import com.healthdata.agent.client.ApprovalServiceClient.*;
import com.healthdata.agent.core.AgentContext;
import com.healthdata.agent.tool.ToolDefinition;
import com.healthdata.agent.tool.ToolDefinition.ApprovalCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integration layer between Agent Runtime and Approval Service.
 * Handles creation of approval requests for tools requiring HITL review.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalIntegration {

    private final ApprovalServiceClient approvalServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${hdim.approval.default-timeout-hours:24}")
    private int defaultTimeoutHours;

    @Value("${hdim.approval.enabled:true}")
    private boolean approvalEnabled;

    /**
     * Check if a tool execution needs approval and create a request if so.
     */
    public ApprovalResult checkAndCreateApprovalRequest(
            ToolDefinition tool,
            JsonNode arguments,
            AgentContext context) {

        if (!approvalEnabled) {
            log.debug("Approval disabled, skipping for tool: {}", tool.getName());
            return ApprovalResult.notRequired();
        }

        if (!tool.needsApproval()) {
            return ApprovalResult.notRequired();
        }

        // Determine risk level based on approval category
        RiskLevel riskLevel = mapApprovalCategoryToRisk(tool.getApprovalCategory());

        // Build payload with tool execution context
        Map<String, Object> payload = buildPayload(tool, arguments, context);

        // Create approval request
        CreateApprovalRequest request = new CreateApprovalRequest(
            RequestType.AGENT_ACTION,
            tool.getName(),
            context.getPatientId(),
            "EXECUTE",
            payload,
            null, // No confidence score for tool execution
            riskLevel,
            "agent-runtime-service",
            context.getCorrelationId(),
            tool.getRequiredApprovalRole(),
            Instant.now().plus(Duration.ofHours(defaultTimeoutHours))
        );

        try {
            ApprovalResponse response = approvalServiceClient.createApprovalRequest(
                context.getTenantId(),
                context.getUserId(),
                request
            );

            if (response.isFallback()) {
                log.warn("Approval service fallback triggered: {}", response.getFallbackReason());
                // In fallback mode, block the execution for safety
                return ApprovalResult.blocked(
                    "Approval service unavailable. Tool execution blocked for safety.",
                    null
                );
            }

            log.info("Created approval request: id={}, tool={}, status={}",
                response.getId(), tool.getName(), response.getStatus());

            return ApprovalResult.pending(response.getId(), response.getStatus().name());

        } catch (Exception e) {
            log.error("Failed to create approval request for tool: {}", tool.getName(), e);
            return ApprovalResult.blocked(
                "Failed to create approval request: " + e.getMessage(),
                null
            );
        }
    }

    /**
     * Create an approval request for guardrail-flagged content.
     */
    public ApprovalResult createGuardrailApprovalRequest(
            String content,
            String violations,
            AgentContext context) {

        if (!approvalEnabled) {
            return ApprovalResult.notRequired();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", content);
        payload.put("violations", violations);
        payload.put("patientId", context.getPatientId());
        payload.put("agentType", context.getAgentType());

        CreateApprovalRequest request = new CreateApprovalRequest(
            RequestType.GUARDRAIL_REVIEW,
            "AI_RESPONSE",
            context.getPatientId(),
            "DELIVER_TO_USER",
            payload,
            null,
            RiskLevel.HIGH, // Guardrail violations are high risk
            "agent-runtime-service",
            context.getCorrelationId(),
            "CLINICAL_REVIEWER",
            Instant.now().plus(Duration.ofHours(defaultTimeoutHours))
        );

        try {
            ApprovalResponse response = approvalServiceClient.createApprovalRequest(
                context.getTenantId(),
                context.getUserId(),
                request
            );

            if (response.isFallback()) {
                return ApprovalResult.blocked(
                    "Approval service unavailable. Flagged content blocked.",
                    null
                );
            }

            return ApprovalResult.pending(response.getId(), response.getStatus().name());

        } catch (Exception e) {
            log.error("Failed to create guardrail approval request", e);
            return ApprovalResult.blocked(
                "Failed to create guardrail approval: " + e.getMessage(),
                null
            );
        }
    }

    /**
     * Check the status of an existing approval request.
     */
    public ApprovalStatus checkApprovalStatus(UUID approvalId, String tenantId) {
        try {
            ApprovalResponse response = approvalServiceClient.getApprovalRequest(tenantId, approvalId);
            return response.getStatus();
        } catch (Exception e) {
            log.error("Failed to check approval status: {}", approvalId, e);
            return ApprovalStatus.PENDING;
        }
    }

    private RiskLevel mapApprovalCategoryToRisk(ApprovalCategory category) {
        return switch (category) {
            case NONE -> RiskLevel.LOW;
            case LOW_RISK -> RiskLevel.LOW;
            case STANDARD -> RiskLevel.MEDIUM;
            case CLINICAL -> RiskLevel.HIGH;
            case CRITICAL -> RiskLevel.CRITICAL;
        };
    }

    private Map<String, Object> buildPayload(ToolDefinition tool, JsonNode arguments, AgentContext context) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("toolName", tool.getName());
        payload.put("toolDescription", tool.getDescription());
        payload.put("toolCategory", tool.getCategory() != null ? tool.getCategory().name() : "UNKNOWN");
        payload.put("arguments", objectMapper.convertValue(arguments, Map.class));
        payload.put("agentType", context.getAgentType());
        payload.put("userId", context.getUserId());
        payload.put("sessionId", context.getSessionId());
        if (context.getPatientId() != null) {
            payload.put("patientId", context.getPatientId());
        }
        if (context.getEncounterId() != null) {
            payload.put("encounterId", context.getEncounterId());
        }
        return payload;
    }

    /**
     * Result of approval check.
     */
    public record ApprovalResult(
        boolean required,
        boolean approved,
        boolean blocked,
        UUID approvalId,
        String status,
        String message
    ) {
        public static ApprovalResult notRequired() {
            return new ApprovalResult(false, true, false, null, null, null);
        }

        public static ApprovalResult alreadyApproved() {
            return new ApprovalResult(true, true, false, null, "APPROVED", null);
        }

        public static ApprovalResult pending(UUID approvalId, String status) {
            return new ApprovalResult(true, false, false, approvalId, status,
                "Tool execution requires human approval. Approval request ID: " + approvalId);
        }

        public static ApprovalResult blocked(String message, UUID approvalId) {
            return new ApprovalResult(true, false, true, approvalId, "BLOCKED", message);
        }

        public boolean canProceed() {
            return !required || approved;
        }
    }
}
