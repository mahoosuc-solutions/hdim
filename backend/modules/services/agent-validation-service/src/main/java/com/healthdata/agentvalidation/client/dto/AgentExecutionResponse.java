package com.healthdata.agentvalidation.client.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Response DTO from agent execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentExecutionResponse {

    /**
     * Unique execution ID.
     */
    private String executionId;

    /**
     * The agent's response text.
     */
    private String response;

    /**
     * Status of the execution.
     */
    private String status;

    /**
     * LLM provider used for this execution.
     */
    private String llmProvider;

    /**
     * Model version used.
     */
    private String modelVersion;

    /**
     * OpenTelemetry trace ID for Jaeger correlation.
     */
    private String traceId;

    /**
     * Total execution duration in milliseconds.
     */
    private Long durationMs;

    /**
     * Input token count.
     */
    private Integer inputTokens;

    /**
     * Output token count.
     */
    private Integer outputTokens;

    /**
     * List of tool calls made during execution.
     */
    private List<ToolCallInfo> toolCalls;

    /**
     * Number of agent iterations.
     */
    private Integer iterationCount;

    /**
     * Whether any guardrails were triggered.
     */
    private boolean guardrailsTriggered;

    /**
     * Guardrail violation details if any.
     */
    private List<String> guardrailViolations;

    /**
     * Error message if execution failed.
     */
    private String errorMessage;

    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;

    /**
     * Information about a tool call made by the agent.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolCallInfo {
        private String toolName;
        private Map<String, Object> input;
        private Object output;
        private Long durationMs;
        private boolean success;
        private String errorMessage;
    }
}
