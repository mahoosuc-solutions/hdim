package com.healthdata.agent.tool;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Definition of a tool that can be used by LLM agents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    /**
     * Unique tool name.
     */
    private String name;

    /**
     * Human-readable description.
     */
    private String description;

    /**
     * JSON Schema for input parameters.
     */
    private Map<String, Object> inputSchema;

    /**
     * Required parameters.
     */
    private List<String> requiredParams;

    /**
     * Whether this tool requires human approval.
     */
    @Builder.Default
    private boolean requiresApproval = false;

    /**
     * Approval category for HITL workflow.
     */
    @Builder.Default
    private ApprovalCategory approvalCategory = ApprovalCategory.NONE;

    /**
     * Required role for approval (when approvalCategory != NONE).
     */
    private String requiredApprovalRole;

    /**
     * Tool category for organization.
     */
    private ToolCategory category;

    /**
     * Tool categories.
     */
    public enum ToolCategory {
        FHIR_QUERY,
        CQL_EXECUTION,
        DATA_RETRIEVAL,
        DATA_MUTATION,
        NOTIFICATION,
        REPORTING,
        EXTERNAL_API
    }

    /**
     * Approval categories for Human-in-the-Loop workflows.
     */
    public enum ApprovalCategory {
        /** No approval required */
        NONE,
        /** Low risk - auto-approve after short delay */
        LOW_RISK,
        /** Standard - single approver required */
        STANDARD,
        /** Clinical - clinical role required for approval */
        CLINICAL,
        /** Critical - multiple approvers required */
        CRITICAL
    }

    /**
     * Check if this tool needs any form of approval.
     */
    public boolean needsApproval() {
        return requiresApproval || approvalCategory != ApprovalCategory.NONE;
    }

    /**
     * Convert to Claude API format.
     */
    public Map<String, Object> toClaudeFormat() {
        return Map.of(
            "name", name,
            "description", description,
            "input_schema", inputSchema
        );
    }

    /**
     * Convert to OpenAI function format.
     */
    public Map<String, Object> toOpenAIFormat() {
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", name,
                "description", description,
                "parameters", inputSchema
            )
        );
    }
}
