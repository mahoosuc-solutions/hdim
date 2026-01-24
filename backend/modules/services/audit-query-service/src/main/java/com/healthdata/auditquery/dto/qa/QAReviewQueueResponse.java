package com.healthdata.auditquery.dto.qa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for QA review queue items.
 *
 * <p>Represents an AI decision pending QA review, with confidence score,
 * reasoning, and review status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI decision pending QA review")
public class QAReviewQueueResponse {

    @Schema(description = "Event ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID eventId;

    @Schema(description = "Timestamp of decision", example = "2026-01-23T10:30:00Z")
    private Instant timestamp;

    @Schema(description = "AI agent type", example = "CARE_GAP_DETECTION")
    private String agentType;

    @Schema(description = "Decision type", example = "AUTOMATED_CARE_GAP_IDENTIFICATION")
    private String decisionType;

    @Schema(description = "Decision outcome", example = "ACCEPTED")
    private String outcome;

    @Schema(description = "Confidence score (0-1)", example = "0.87")
    private Double confidenceScore;

    @Schema(description = "AI reasoning explanation")
    private String reasoning;

    @Schema(description = "Input context provided to AI")
    private Map<String, Object> inputContext;

    @Schema(description = "Output data from AI decision")
    private Map<String, Object> outputData;

    @Schema(description = "Model version used", example = "gpt-4-turbo-2024-04-09")
    private String modelVersion;

    @Schema(description = "QA review status", example = "PENDING")
    private String qaReviewStatus;

    @Schema(description = "QA reviewer user ID", example = "qa-analyst-001")
    private String qaReviewedBy;

    @Schema(description = "QA review timestamp")
    private Instant qaReviewedAt;

    @Schema(description = "QA review notes")
    private String qaReviewNotes;

    @Schema(description = "Whether marked as false positive")
    private Boolean falsePositive;

    @Schema(description = "Whether marked as false negative")
    private Boolean falseNegative;

    @Schema(description = "Related patient ID (if applicable)")
    private String relatedPatientId;

    @Schema(description = "Related resource ID")
    private String relatedResourceId;

    @Schema(description = "Related resource type")
    private String relatedResourceType;

    @Schema(description = "Validation errors (if any)")
    private List<String> validationErrors;
}
