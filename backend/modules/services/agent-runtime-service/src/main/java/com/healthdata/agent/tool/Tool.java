package com.healthdata.agent.tool;

import com.healthdata.agent.core.AgentContext;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Interface for tools that can be invoked by AI agents.
 * Tools provide specific capabilities like querying FHIR data,
 * executing CQL measures, or sending notifications.
 */
public interface Tool {

    /**
     * Get the tool definition for LLM providers.
     */
    ToolDefinition getDefinition();

    /**
     * Execute the tool with given arguments.
     *
     * @param arguments Input arguments from the LLM
     * @param context   Agent execution context (tenant, user, session)
     * @return Tool execution result
     */
    Mono<ToolResult> execute(Map<String, Object> arguments, AgentContext context);

    /**
     * Validate input arguments before execution.
     *
     * @param arguments Input arguments to validate
     * @return Validation result with errors if any
     */
    default ValidationResult validate(Map<String, Object> arguments) {
        return ValidationResult.valid();
    }

    /**
     * Check if this tool is available in the current context.
     *
     * @param context Agent execution context
     * @return true if tool can be used
     */
    default boolean isAvailable(AgentContext context) {
        return true;
    }

    /**
     * Get the tool name (convenience method).
     */
    default String getName() {
        return getDefinition().getName();
    }

    /**
     * Result of tool execution.
     */
    record ToolResult(
        boolean success,
        String content,
        Map<String, Object> data,
        String errorMessage
    ) {
        public static ToolResult success(String content) {
            return new ToolResult(true, content, null, null);
        }

        public static ToolResult success(String content, Map<String, Object> data) {
            return new ToolResult(true, content, data, null);
        }

        public static ToolResult error(String errorMessage) {
            return new ToolResult(false, null, null, errorMessage);
        }

        /**
         * Convert to LLM tool result format.
         */
        public String toToolResultContent() {
            if (success) {
                return content != null ? content : "Operation completed successfully.";
            } else {
                return "Error: " + errorMessage;
            }
        }
    }

    /**
     * Result of argument validation.
     */
    record ValidationResult(
        boolean isValid,
        java.util.List<String> errors
    ) {
        public static ValidationResult valid() {
            return new ValidationResult(true, java.util.List.of());
        }

        public static ValidationResult invalid(String... errors) {
            return new ValidationResult(false, java.util.List.of(errors));
        }

        public static ValidationResult invalid(java.util.List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }
}
