package com.healthdata.agent.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for LLM providers.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hdim.agent.llm")
public class LLMProviderConfig {

    /**
     * Default provider to use.
     */
    private String defaultProvider = "claude";

    /**
     * Provider-specific configurations.
     */
    private Map<String, ProviderSettings> providers = new HashMap<>();

    /**
     * Fallback chain for provider failures.
     */
    private List<String> fallbackChain = List.of("claude", "azure-openai", "bedrock");

    /**
     * Global rate limiting.
     */
    private RateLimitSettings rateLimiting = new RateLimitSettings();

    /**
     * Settings for a specific provider.
     */
    @Data
    public static class ProviderSettings {
        /**
         * Whether the provider is enabled.
         */
        private boolean enabled = false;

        /**
         * API key for authentication.
         */
        private String apiKey;

        /**
         * API base URL.
         */
        private String apiUrl;

        /**
         * Endpoint URL (for Azure).
         */
        private String endpoint;

        /**
         * Deployment ID (for Azure).
         */
        private String deploymentId;

        /**
         * API version (for Azure).
         */
        private String apiVersion;

        /**
         * Model ID to use.
         */
        private String model;

        /**
         * Model ID (alias for Bedrock).
         */
        private String modelId;

        /**
         * AWS region (for Bedrock).
         */
        private String region;

        /**
         * Maximum tokens to generate.
         */
        private int maxTokens = 4096;

        /**
         * Temperature for randomness.
         */
        private double temperature = 0.3;

        /**
         * Timeout in seconds.
         */
        private int timeoutSeconds = 60;

        /**
         * Maximum retry attempts.
         */
        private int maxRetries = 3;

        /**
         * Additional headers.
         */
        private Map<String, String> additionalHeaders = new HashMap<>();
    }

    /**
     * Rate limiting settings.
     */
    @Data
    public static class RateLimitSettings {
        private int requestsPerMinute = 60;
        private int tokensPerMinute = 100000;
    }

    /**
     * Get provider settings by name.
     */
    public ProviderSettings getProvider(String name) {
        return providers.get(name);
    }

    /**
     * Check if a provider is enabled.
     */
    public boolean isProviderEnabled(String name) {
        ProviderSettings settings = providers.get(name);
        return settings != null && settings.isEnabled();
    }
}
