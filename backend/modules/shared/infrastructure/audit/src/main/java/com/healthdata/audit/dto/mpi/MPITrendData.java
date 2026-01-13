package com.healthdata.audit.dto.mpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Historical trend data for MPI merge accuracy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPITrendData {
    private List<DailyMergeTrend> dailyTrends;
    private Double averageValidationRate;
    private Double averageRollbackRate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMergeTrend {
        private String date;
        private Long totalMerges;
        private Long validatedMerges;
        private Long rolledBackMerges;
        private Double validationRate;
        private Double averageConfidence;
    }
}
