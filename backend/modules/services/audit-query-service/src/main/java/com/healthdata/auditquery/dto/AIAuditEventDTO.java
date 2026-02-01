package com.healthdata.auditquery.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object for AI audit events streamed via SSE.
 *
 * <p>Represents an AI decision event with all relevant metadata for real-time monitoring.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "AI audit event for real-time streaming")
public class AIAuditEventDTO {

    @Schema(description = "Unique event identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private String eventId;

    @Schema(description = "Event timestamp", example = "2026-01-24T10:15:30Z")
    private Instant timestamp;

    @Schema(description = "AI agent type", example = "CLINICAL_DECISION")
    private String agentType;

    @Schema(description = "Decision type", example = "ALERT_GENERATION")
    private String decisionType;

    @Schema(description = "Resource type affected", example = "Patient")
    private String resourceType;

    @Schema(description = "Resource identifier", example = "patient-123")
    private String resourceId;

    @Schema(description = "AI recommendation details")
    private Map<String, Object> recommendation;

    @Schema(description = "Confidence score (0.0 to 1.0)", example = "0.92")
    private Double confidenceScore;

    @Schema(description = "Decision outcome", example = "ACCEPTED")
    private String outcome;

    @Schema(description = "Estimated cost in USD", example = "0.0015")
    private Double costEstimate;

    @Schema(description = "Processing time in milliseconds", example = "1250")
    private Long processingTimeMs;

    @Schema(description = "User feedback rating (1-5)", example = "4")
    private Integer userFeedbackRating;

    @Schema(description = "User feedback text")
    private String userFeedbackText;

    @Schema(description = "Severity level (future enhancement)", example = "HIGH")
    private String severity;
}
