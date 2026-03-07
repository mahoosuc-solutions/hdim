package com.healthdata.corehiveadapter.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreCareGapsRequest {

    @NotBlank(message = "patientId is required")
    private String patientId;

    @NotNull(message = "careGaps list is required")
    private List<CareGapItem> careGaps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareGapItem {
        @NotBlank(message = "gapId is required")
        private String gapId;

        @NotBlank(message = "measureId is required")
        private String measureId;

        private String measureCode;
        private String gapStatus;
        private int daysSinceIdentified;
        private double complianceScore;
    }
}
