package com.healthdata.agent.llm;

import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.llm.model.LLMResponse;
import com.healthdata.agent.llm.model.LLMStreamChunk;
import com.healthdata.agent.tool.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Abstraction layer for LLM providers.
 * Supports Claude, Azure OpenAI, and AWS Bedrock.
 */
public interface LLMProvider {

    /**
     * Provider name (claude, azure-openai, bedrock).
     */
    String getName();

    /**
     * Available models for this provider.
     */
    List<String> getAvailableModels();

    /**
     * Generate completion.
     */
    LLMResponse complete(LLMRequest request);

    /**
     * Generate completion with streaming.
     */
    Flux<LLMStreamChunk> completeStreaming(LLMRequest request);

    /**
     * Generate completion with tool use.
     */
    LLMResponse completeWithTools(LLMRequest request, List<ToolDefinition> tools);

    /**
     * Count tokens in text (approximate).
     */
    int countTokens(String text);

    /**
     * Provider health status.
     */
    HealthStatus health();

    /**
     * Check if provider is available.
     */
    default boolean isAvailable() {
        return health().isHealthy();
    }

    /**
     * Health status for provider.
     */
    record HealthStatus(boolean healthy, String status, long latencyMs) {
        public static HealthStatus healthy(long latencyMs) {
            return new HealthStatus(true, "UP", latencyMs);
        }

        public static HealthStatus unhealthy(String reason) {
            return new HealthStatus(false, reason, -1);
        }

        public boolean isHealthy() {
            return healthy;
        }
    }
}
