package com.healthdata.aiassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for AI chat interactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * Unique response ID.
     */
    private String id;

    /**
     * Query type that was processed.
     */
    private String queryType;

    /**
     * AI-generated response text.
     */
    private String response;

    /**
     * Model used to generate response.
     */
    private String model;

    /**
     * Number of input tokens processed.
     */
    private int inputTokens;

    /**
     * Number of output tokens generated.
     */
    private int outputTokens;

    /**
     * Whether response was served from cache.
     */
    @Builder.Default
    private boolean cached = false;

    /**
     * Timestamp of response generation.
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Processing time in milliseconds.
     */
    private long processingTimeMs;

    /**
     * Whether an error occurred.
     */
    @Builder.Default
    private boolean error = false;

    /**
     * Error message if error occurred.
     */
    private String errorMessage;

    /**
     * Additional metadata.
     */
    private Map<String, Object> metadata;

    /**
     * Session ID for conversation tracking.
     */
    private String sessionId;

    /**
     * Suggested follow-up questions.
     */
    private String[] suggestions;
}
