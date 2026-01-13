package com.healthdata.audit.dto.mpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MPI merge metrics and statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIMetrics {
    private Long totalMerges;
    private Long validatedMerges;
    private Long rolledBackMerges;
    private Long pendingValidation;
    
    private Double validationRate;
    private Double rollbackRate;
    private Double averageConfidenceScore;
    private Long averageValidationTimeMinutes;
    
    private MergeTypeDistribution mergeTypeDistribution;
    private DataQualityMetrics dataQualityMetrics;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MergeTypeDistribution {
        private Long automatic;
        private Long manual;
        private Long assisted;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQualityMetrics {
        private Long totalIssues;
        private Long criticalIssues;
        private Long resolvedIssues;
        private Double resolutionRate;
    }
}
