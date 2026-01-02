package com.healthdata.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * Request DTO for AI chat interactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * Type of query for routing and validation.
     * Examples: care_gaps, quality_measures, patient_summary
     */
    @NotBlank(message = "Query type is required")
    private String queryType;

    /**
     * The user's query/question.
     */
    @NotBlank(message = "Query is required")
    @Size(max = 10000, message = "Query must be 10000 characters or less")
    private String query;

    /**
     * Conversation history for context.
     */
    private List<ChatMessage> messages;

    /**
     * Patient ID for patient-specific queries.
     */
    private String patientId;

    /**
     * Tenant ID for data isolation.
     */
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    /**
     * Additional context data (JSON).
     */
    private String context;

    /**
     * Whether to use streaming response.
     */
    @Builder.Default
    private boolean streaming = false;

    /**
     * Session ID for conversation tracking.
     */
    private String sessionId;

    /**
     * Generate a cache key for this request.
     */
    public String cacheKey() {
        return String.format("%s:%s:%s",
            tenantId,
            queryType,
            Objects.hash(query, patientId)
        );
    }
}
