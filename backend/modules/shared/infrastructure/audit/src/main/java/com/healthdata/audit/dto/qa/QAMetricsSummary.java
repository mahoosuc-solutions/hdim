package com.healthdata.audit.dto.qa;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA quality metrics summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QAMetricsSummary {

    private Long totalReviewed;
    private Long approvedDecisions;
    private Long rejectedDecisions;
    private Long flaggedForEscalation;
    private Double averageConfidenceScore;
    private Long lowConfidenceCount;
    private Double falsePositiveRate;
    private Double falseNegativeRate;
    private Long totalPending;
    private Double approvalRate;
    private Double rejectionRate;
    private Double escalationRate;
}
