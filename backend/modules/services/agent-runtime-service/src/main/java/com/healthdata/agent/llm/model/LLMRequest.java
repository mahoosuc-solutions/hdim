package com.healthdata.agent.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unified request model for all LLM providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMRequest {

    /**
     * System prompt for the model.
     */
    private String systemPrompt;

    /**
     * Conversation messages.
     */
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    /**
     * Maximum tokens to generate.
     */
    @Builder.Default
    private int maxTokens = 4096;

    /**
     * Temperature for response randomness (0.0 - 1.0).
     */
    @Builder.Default
    private double temperature = 0.3;

    /**
     * Optional model override.
     */
    private String model;

    /**
     * Stop sequences.
     */
    private List<String> stopSequences;

    /**
     * Additional provider-specific parameters.
     */
    private Map<String, Object> additionalParams;

    /**
     * Tenant ID for multi-tenant context.
     */
    private String tenantId;

    /**
     * Session ID for conversation tracking.
     */
    private String sessionId;

    /**
     * Request correlation ID for tracing.
     */
    private String correlationId;

    /**
     * Add a user message.
     */
    public LLMRequest addUserMessage(String content) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(Message.user(content));
        return this;
    }

    /**
     * Add an assistant message.
     */
    public LLMRequest addAssistantMessage(String content) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(Message.assistant(content));
        return this;
    }

    /**
     * Message in conversation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
        private List<ToolCall> toolCalls;
        private List<ToolResult> toolResults;

        public static Message system(String content) {
            return Message.builder().role("system").content(content).build();
        }

        public static Message user(String content) {
            return Message.builder().role("user").content(content).build();
        }

        public static Message assistant(String content) {
            return Message.builder().role("assistant").content(content).build();
        }

        public static Message assistant(String content, List<ToolCall> toolCalls) {
            return Message.builder().role("assistant").content(content).toolCalls(toolCalls).build();
        }

        public static Message toolResults(List<ToolResult> results) {
            return Message.builder().role("tool").toolResults(results).build();
        }
    }

    /**
     * Tool call from assistant.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String id;
        private String name;
        private Map<String, Object> arguments;
    }

    /**
     * Tool execution result.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        private String toolCallId;
        private String toolName;
        private String content;
        private boolean isError;
    }
}
