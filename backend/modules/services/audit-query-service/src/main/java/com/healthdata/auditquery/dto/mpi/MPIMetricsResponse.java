package com.healthdata.auditquery.dto.mpi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for MPI audit metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MPI audit metrics")
public class MPIMetricsResponse {

    @Schema(description = "Total merges performed", example = "342")
    private Long totalMerges;

    @Schema(description = "Automatic merges", example = "287")
    private Long automaticMerges;

    @Schema(description = "Manual merges", example = "43")
    private Long manualMerges;

    @Schema(description = "Assisted merges", example = "12")
    private Long assistedMerges;

    @Schema(description = "Validated merges", example = "315")
    private Long validatedMerges;

    @Schema(description = "Pending validation", example = "23")
    private Long pendingValidation;

    @Schema(description = "Rolled back merges", example = "4")
    private Long rolledBackMerges;

    @Schema(description = "Failed merges", example = "0")
    private Long failedMerges;

    @Schema(description = "Average confidence score (0-1)", example = "0.89")
    private Double averageConfidenceScore;

    @Schema(description = "High confidence merges (>0.9)", example = "234")
    private Long highConfidenceMerges;

    @Schema(description = "Medium confidence merges (0.7-0.9)", example = "89")
    private Long mediumConfidenceMerges;

    @Schema(description = "Low confidence merges (<0.7)", example = "19")
    private Long lowConfidenceMerges;

    @Schema(description = "Merges with errors", example = "8")
    private Long mergesWithErrors;

    @Schema(description = "Data quality issues detected", example = "27")
    private Long dataQualityIssues;

    @Schema(description = "High data quality count", example = "289")
    private Long highDataQualityCount;

    @Schema(description = "Medium data quality count", example = "42")
    private Long mediumDataQualityCount;

    @Schema(description = "Low data quality count", example = "11")
    private Long lowDataQualityCount;

    @Schema(description = "Validation success rate (0-1)", example = "0.98")
    private Double validationSuccessRate;

    @Schema(description = "Rollback rate (0-1)", example = "0.01")
    private Double rollbackRate;

    @Schema(description = "Average merge processing time (minutes)", example = "2.4")
    private Double averageMergeTime;
}
