package com.healthdata.audit.dto.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA Metrics
 * 
 * Quality assurance metrics and statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAMetrics {
    private long totalDecisions;
    private long pendingReview;
    private long approved;
    private long rejected;
    private long flagged;
    
    private double approvalRate;
    private double rejectionRate;
    private double flagRate;
    
    private long falsePositives;
    private long falseNegatives;
    private double accuracy;
    
    private double averageConfidence;
    private double averageReviewTimeMinutes;
    
    private ConfidenceDistribution confidenceDistribution;
    private AgentPerformance agentPerformance;
}
