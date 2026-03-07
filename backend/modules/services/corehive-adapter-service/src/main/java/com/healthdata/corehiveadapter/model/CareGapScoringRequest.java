package com.healthdata.corehiveadapter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * De-identified care gap scoring request sent to CoreHive AI.
 * MUST NOT contain any PHI — only synthetic IDs and clinical codes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareGapScoringRequest {
    private String syntheticPatientId;
    private String tenantId;
    private List<CareGapItem> careGaps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CareGapItem {
        private String syntheticGapId;
        private String measureId;
        private String measureCode;
        private String gapStatus;
        private int daysSinceIdentified;
        private double complianceScore;
    }
}
