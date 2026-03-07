package com.healthdata.corehiveadapter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI-ranked care gap priorities returned from CoreHive.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareGapScoringResponse {
    private String syntheticPatientId;
    private List<ScoredCareGap> rankedGaps;
    private double overallRiskScore;
    private String modelVersion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScoredCareGap {
        private String syntheticGapId;
        private double priorityScore;
        private int rank;
        private String recommendation;
        private double estimatedImpact;
    }
}
