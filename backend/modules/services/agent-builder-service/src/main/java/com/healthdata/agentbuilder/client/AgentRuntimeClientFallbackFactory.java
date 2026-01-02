package com.healthdata.agentbuilder.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fallback factory for Agent Runtime Client to handle service unavailability.
 */
@Slf4j
@Component
public class AgentRuntimeClientFallbackFactory implements FallbackFactory<AgentRuntimeClient> {

    @Override
    public AgentRuntimeClient create(Throwable cause) {
        log.error("Agent Runtime Service unavailable: {}", cause.getMessage());

        return new AgentRuntimeClient() {

            @Override
            public Map<String, Object> executeAgent(String agentSlug, Map<String, Object> request, String tenantId) {
                log.warn("Fallback: executeAgent called for agent {} but runtime is unavailable", agentSlug);
                return Map.of(
                    "error", true,
                    "message", "Agent Runtime Service is currently unavailable. Please try again later.",
                    "code", "RUNTIME_UNAVAILABLE"
                );
            }

            @Override
            public Map<String, Object> streamAgent(String agentSlug, Map<String, Object> request, String tenantId) {
                log.warn("Fallback: streamAgent called for agent {} but runtime is unavailable", agentSlug);
                return Map.of(
                    "error", true,
                    "message", "Agent Runtime Service is currently unavailable. Please try again later.",
                    "code", "RUNTIME_UNAVAILABLE"
                );
            }

            @Override
            public List<ToolInfo> getAvailableTools(String tenantId) {
                log.warn("Fallback: getAvailableTools called but runtime is unavailable");
                return Collections.emptyList();
            }

            @Override
            public ValidationResult validateAgentConfiguration(Map<String, Object> configuration, String tenantId) {
                log.warn("Fallback: validateAgentConfiguration called but runtime is unavailable");
                return new ValidationResult(
                    false,
                    List.of(new ValidationError("runtime", "Agent Runtime Service is unavailable", "RUNTIME_UNAVAILABLE")),
                    Collections.emptyList()
                );
            }

            @Override
            public List<ProviderInfo> getSupportedProviders() {
                log.warn("Fallback: getSupportedProviders called but runtime is unavailable");
                // Return cached/known providers as fallback
                return List.of(
                    new ProviderInfo("anthropic", "Anthropic Claude", false, true, List.of("us-east-1")),
                    new ProviderInfo("azure-openai", "Azure OpenAI", false, true, List.of("eastus", "westus2")),
                    new ProviderInfo("bedrock", "AWS Bedrock", false, true, List.of("us-east-1", "us-west-2"))
                );
            }

            @Override
            public List<ModelInfo> getSupportedModels(String provider) {
                log.warn("Fallback: getSupportedModels called for provider {} but runtime is unavailable", provider);
                // Return cached/known models as fallback
                return switch (provider) {
                    case "anthropic" -> List.of(
                        new ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", 200000, true, true),
                        new ModelInfo("claude-3-sonnet-20240229", "Claude 3 Sonnet", 200000, true, true),
                        new ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku", 200000, true, true)
                    );
                    case "azure-openai" -> List.of(
                        new ModelInfo("gpt-4-turbo", "GPT-4 Turbo", 128000, true, true),
                        new ModelInfo("gpt-4", "GPT-4", 8192, true, true)
                    );
                    case "bedrock" -> List.of(
                        new ModelInfo("anthropic.claude-3-sonnet", "Claude 3 Sonnet (Bedrock)", 200000, true, true),
                        new ModelInfo("amazon.titan-text-express-v1", "Amazon Titan Express", 8000, true, false)
                    );
                    default -> Collections.emptyList();
                };
            }

            @Override
            public Map<String, Object> healthCheck() {
                return Map.of(
                    "status", "DOWN",
                    "error", cause.getMessage()
                );
            }
        };
    }
}
