package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Clinical decision metrics aggregation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalMetrics {
    
    private Long totalDecisions;
    private Long approvedDecisions;
    private Long rejectedDecisions;
    private Long pendingReview;
    private Double approvalRate;
    private Double overrideRate;
    private Double averageConfidenceScore;
    private Integer averageReviewTimeHours;
    
    // Decision type distribution
    private DecisionTypeDistribution decisionTypeDistribution;
    
    // Severity distribution
    private SeverityDistribution severityDistribution;
    
    // Evidence grade distribution
    private EvidenceGradeDistribution evidenceGradeDistribution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionTypeDistribution {
        private Long medicationAlerts;
        private Long careGaps;
        private Long riskStratifications;
        private Long clinicalPathways;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeverityDistribution {
        private Long critical;
        private Long high;
        private Long moderate;
        private Long low;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceGradeDistribution {
        private Long gradeA; // Strong evidence
        private Long gradeB; // Moderate evidence
        private Long gradeC; // Weak evidence
        private Long gradeD; // Expert opinion
    }
}
