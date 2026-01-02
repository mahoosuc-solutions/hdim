package com.healthdata.aiassistant.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for Anthropic Claude API integration.
 *
 * Claude is used for:
 * - Natural language query understanding
 * - Clinical question answering
 * - Care gap analysis interpretation
 * - Measure result summarization
 *
 * Example configuration:
 * <pre>
 * claude:
 *   enabled: true
 *   api-key: ${ANTHROPIC_API_KEY}
 *   model: claude-3-5-sonnet-20241022
 *   max-tokens: 4096
 *   temperature: 0.3
 * </pre>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "claude")
public class ClaudeConfig {

    /**
     * Enable/disable Claude AI integration.
     */
    private boolean enabled = false;

    /**
     * Anthropic API key.
     * Should be set via environment variable ANTHROPIC_API_KEY.
     */
    private String apiKey;

    /**
     * Anthropic API base URL.
     */
    private String apiUrl = "https://api.anthropic.com/v1";

    /**
     * Claude model to use.
     * Options: claude-3-5-sonnet-20241022, claude-3-opus-20240229, claude-3-haiku-20240307
     */
    private String model = "claude-3-5-sonnet-20241022";

    /**
     * Maximum tokens in response.
     */
    private int maxTokens = 4096;

    /**
     * Temperature for response generation (0.0-1.0).
     * Lower = more focused, higher = more creative.
     */
    private double temperature = 0.3;

    /**
     * Top P (nucleus sampling).
     */
    private double topP = 0.9;

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 60;

    /**
     * Maximum retries on failure.
     */
    private int maxRetries = 3;

    /**
     * Rate limit (requests per minute).
     */
    private int rateLimitPerMinute = 60;

    /**
     * System prompt for clinical assistant.
     */
    private String systemPrompt = """
        You are a clinical AI assistant for the Health Data Integration Module (HDIM).
        Your role is to help healthcare providers understand quality measures, care gaps,
        and patient health data.

        IMPORTANT GUIDELINES:
        1. Always provide accurate, evidence-based information
        2. Never provide medical diagnoses or treatment recommendations
        3. Reference specific quality measures (HEDIS, CMS) when relevant
        4. Use clear, professional medical terminology
        5. If uncertain, acknowledge limitations
        6. Protect patient privacy - never expose PII in responses
        7. Format responses for clinical workflows

        Available data sources:
        - Quality measures (52 HEDIS/CMS measures)
        - Care gap detection and status
        - Patient demographics and conditions
        - Encounter history
        - Lab results and observations

        When users ask about patients, always include:
        - Current care gaps and priority
        - Relevant quality measure compliance
        - Recommended actions based on evidence
        """;

    /**
     * Allowed query patterns for security.
     */
    private Set<String> allowedQueryTypes = new HashSet<>(Set.of(
        "care_gaps",
        "quality_measures",
        "patient_summary",
        "measure_compliance",
        "population_health",
        "care_recommendations"
    ));

    /**
     * Whether to enable response caching.
     */
    private boolean cachingEnabled = true;

    /**
     * Cache TTL in seconds.
     */
    private int cacheTtlSeconds = 300; // 5 minutes

    /**
     * Whether to log all API interactions (for debugging).
     */
    private boolean debugLogging = false;

    /**
     * Validate configuration on startup.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (!enabled) {
            log.info("Claude AI integration is disabled");
            return;
        }

        log.info("Initializing Claude AI configuration...");

        if (apiKey == null || apiKey.isBlank()) {
            log.error("Claude API key is not configured. Set ANTHROPIC_API_KEY environment variable.");
            throw new IllegalStateException("Claude API key is required when claude.enabled=true");
        }

        // Mask API key for logging
        String maskedKey = apiKey.length() > 8
            ? apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4)
            : "***";

        log.info("Claude AI configuration:");
        log.info("  API URL: {}", apiUrl);
        log.info("  Model: {}", model);
        log.info("  Max Tokens: {}", maxTokens);
        log.info("  Temperature: {}", temperature);
        log.info("  API Key: {}", maskedKey);
        log.info("  Caching: {}", cachingEnabled ? "enabled" : "disabled");

        log.info("Claude AI configuration validated successfully");
    }

    /**
     * Check if a query type is allowed.
     */
    public boolean isQueryTypeAllowed(String queryType) {
        return allowedQueryTypes.contains(queryType);
    }
}
