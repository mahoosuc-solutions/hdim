package com.healthdata.agent.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Unified response model from LLM providers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMResponse {

    /**
     * Response ID from provider.
     */
    private String id;

    /**
     * Model used for generation.
     */
    private String model;

    /**
     * Generated content.
     */
    private String content;

    /**
     * Tool calls requested by the model.
     */
    private List<LLMRequest.ToolCall> toolCalls;

    /**
     * Stop reason (end_turn, tool_use, max_tokens, etc.).
     */
    private String stopReason;

    /**
     * Token usage statistics.
     */
    private TokenUsage usage;

    /**
     * Provider name.
     */
    private String provider;

    /**
     * Response timestamp.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Response latency in milliseconds.
     */
    private long latencyMs;

    /**
     * Whether the response contains tool calls.
     */
    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    /**
     * Token usage statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        private int inputTokens;
        private int outputTokens;
        private int totalTokens;

        public static TokenUsage of(int inputTokens, int outputTokens) {
            return TokenUsage.builder()
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens)
                .build();
        }
    }
}
