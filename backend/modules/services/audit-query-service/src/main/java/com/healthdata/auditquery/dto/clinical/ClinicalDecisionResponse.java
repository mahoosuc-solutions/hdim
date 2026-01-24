package com.healthdata.auditquery.dto.clinical;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for clinical decision review items.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Clinical AI decision for review")
public class ClinicalDecisionResponse {

    @Schema(description = "Decision ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Patient ID", example = "PAT-12345")
    private String patientId;

    @Schema(description = "Decision type", example = "MEDICATION_ALERT")
    private String decisionType;

    @Schema(description = "Alert severity", example = "HIGH")
    private String alertSeverity;

    @Schema(description = "Decision timestamp")
    private LocalDateTime decisionTimestamp;

    @Schema(description = "Review status", example = "PENDING")
    private String reviewStatus;

    @Schema(description = "Evidence grade", example = "A")
    private String evidenceGrade;

    @Schema(description = "Confidence score (0-1)", example = "0.87")
    private Double confidenceScore;

    @Schema(description = "Specialty area", example = "Cardiology")
    private String specialtyArea;

    @Schema(description = "Patient context")
    private Map<String, Object> patientContext;

    @Schema(description = "AI recommendation")
    private Map<String, Object> recommendation;

    @Schema(description = "Clinical evidence")
    private Map<String, Object> evidence;

    @Schema(description = "Clinical details")
    private Map<String, Object> clinicalDetails;

    @Schema(description = "Reviewed by", example = "dr-smith")
    private String reviewedBy;

    @Schema(description = "Reviewed at timestamp")
    private LocalDateTime reviewedAt;

    @Schema(description = "Review notes")
    private String reviewNotes;

    @Schema(description = "Has override")
    private Boolean hasOverride;

    @Schema(description = "Override reason")
    private String overrideReason;

    @Schema(description = "Override applied by")
    private String overrideAppliedBy;

    @Schema(description = "Override applied at")
    private LocalDateTime overrideAppliedAt;
}
