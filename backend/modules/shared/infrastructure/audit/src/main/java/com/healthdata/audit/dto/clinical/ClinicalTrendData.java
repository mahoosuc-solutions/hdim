package com.healthdata.audit.dto.clinical;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Clinical decision trend analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalTrendData {
    
    private List<DailyClinicalTrend> dailyTrends;
    private Double averageApprovalRate;
    private Double averageOverrideRate;
    private Double averageConfidenceScore;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyClinicalTrend {
        private LocalDate date;
        private Long totalDecisions;
        private Long approved;
        private Long rejected;
        private Long overridden;
        private Double approvalRate;
        private Double overrideRate;
        private Double averageConfidence;
    }
}
