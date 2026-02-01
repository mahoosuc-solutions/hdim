package com.healthdata.auditquery.dto.qa;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for QA audit metrics.
 *
 * <p>Aggregated statistics for QA audit dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "QA audit metrics")
public class QAMetricsResponse {

    @Schema(description = "Total AI decisions reviewed", example = "247")
    private Long totalReviewed;

    @Schema(description = "Approved decisions", example = "189")
    private Long approvedDecisions;

    @Schema(description = "Rejected decisions", example = "34")
    private Long rejectedDecisions;

    @Schema(description = "Flagged for manual review", example = "24")
    private Long flaggedDecisions;

    @Schema(description = "False positives identified", example = "12")
    private Long falsePositives;

    @Schema(description = "False negatives identified", example = "8")
    private Long falseNegatives;

    @Schema(description = "Pending QA review", example = "15")
    private Long pendingReview;

    @Schema(description = "Average confidence score (0-1)", example = "0.82")
    private Double averageConfidenceScore;

    @Schema(description = "High confidence decisions (>0.8)", example = "156")
    private Long highConfidenceCount;

    @Schema(description = "Medium confidence decisions (0.5-0.8)", example = "67")
    private Long mediumConfidenceCount;

    @Schema(description = "Low confidence decisions (<0.5)", example = "24")
    private Long lowConfidenceCount;

    @Schema(description = "Approval rate (0-1)", example = "0.77")
    private Double approvalRate;

    @Schema(description = "False positive rate (0-1)", example = "0.05")
    private Double falsePositiveRate;

    @Schema(description = "False negative rate (0-1)", example = "0.03")
    private Double falseNegativeRate;
}
