package com.healthdata.auditquery.dto.mpi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for MPI merge events.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MPI patient merge event")
public class MPIMergeEventResponse {

    @Schema(description = "Merge event ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Source patient ID", example = "PAT-12345")
    private String sourcePatientId;

    @Schema(description = "Target patient ID", example = "PAT-67890")
    private String targetPatientId;

    @Schema(description = "Merge type", example = "AUTOMATIC")
    private String mergeType;  // AUTOMATIC, MANUAL, ASSISTED

    @Schema(description = "Confidence score (0-1)", example = "0.92")
    private Double confidenceScore;

    @Schema(description = "Merge status", example = "PENDING")
    private String mergeStatus;  // PENDING, VALIDATED, ROLLED_BACK, FAILED

    @Schema(description = "Validation status", example = "NOT_VALIDATED")
    private String validationStatus;  // NOT_VALIDATED, VALIDATED, VALIDATION_FAILED

    @Schema(description = "Merge timestamp")
    private LocalDateTime mergeTimestamp;

    @Schema(description = "Performed by user", example = "system-mpi")
    private String performedBy;

    @Schema(description = "Source patient data snapshot")
    private Map<String, Object> sourcePatientSnapshot;

    @Schema(description = "Target patient data snapshot")
    private Map<String, Object> targetPatientSnapshot;

    @Schema(description = "Merged patient data snapshot")
    private Map<String, Object> mergedPatientSnapshot;

    @Schema(description = "Matching algorithm details")
    private Map<String, Object> matchingDetails;

    @Schema(description = "Validated by user", example = "mpi-analyst-001")
    private String validatedBy;

    @Schema(description = "Validated at timestamp")
    private LocalDateTime validatedAt;

    @Schema(description = "Validation notes")
    private String validationNotes;

    @Schema(description = "Whether merge has errors")
    private Boolean hasMergeErrors;

    @Schema(description = "Whether data quality issues detected")
    private Boolean hasDataQualityIssues;

    @Schema(description = "Data quality assessment", example = "HIGH")
    private String dataQualityAssessment;  // HIGH, MEDIUM, LOW

    @Schema(description = "Rollback reason")
    private String rollbackReason;

    @Schema(description = "Rolled back at timestamp")
    private LocalDateTime rolledBackAt;

    @Schema(description = "Rolled back by user")
    private String rolledBackBy;
}
