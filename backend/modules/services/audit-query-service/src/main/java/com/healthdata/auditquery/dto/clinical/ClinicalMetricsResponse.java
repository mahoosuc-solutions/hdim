package com.healthdata.auditquery.dto.clinical;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for clinical audit metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Clinical audit metrics")
public class ClinicalMetricsResponse {

    @Schema(description = "Total clinical decisions", example = "543")
    private Long totalDecisions;

    @Schema(description = "Accepted recommendations", example = "412")
    private Long acceptedRecommendations;

    @Schema(description = "Rejected recommendations", example = "87")
    private Long rejectedRecommendations;

    @Schema(description = "Modified recommendations", example = "44")
    private Long modifiedRecommendations;

    @Schema(description = "Pending review", example = "23")
    private Long pendingReview;

    @Schema(description = "Average acceptance rate (0-1)", example = "0.76")
    private Double averageAcceptanceRate;

    @Schema(description = "Critical severity count", example = "12")
    private Long criticalSeverityCount;

    @Schema(description = "High severity count", example = "89")
    private Long highSeverityCount;

    @Schema(description = "Moderate severity count", example = "234")
    private Long moderateSeverityCount;

    @Schema(description = "Low severity count", example = "208")
    private Long lowSeverityCount;

    @Schema(description = "Average confidence score (0-1)", example = "0.82")
    private Double averageConfidenceScore;

    @Schema(description = "Override rate (0-1)", example = "0.08")
    private Double overrideRate;

    @Schema(description = "Evidence grade A count", example = "234")
    private Long evidenceGradeACount;

    @Schema(description = "Evidence grade B count", example = "189")
    private Long evidenceGradeBCount;

    @Schema(description = "Evidence grade C count", example = "87")
    private Long evidenceGradeCCount;

    @Schema(description = "Evidence grade D count", example = "33")
    private Long evidenceGradeDCount;
}
