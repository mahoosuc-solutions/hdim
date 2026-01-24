package com.healthdata.audit.dto.qa;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * QA trend analysis data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QATrendData {

    private List<DailyTrendPoint> dailyTrends;
    private Map<String, List<DailyTrendPoint>> byAgentType;
    private List<ConfidenceTrendPoint> confidenceTrends;
    private List<AccuracyTrendPoint> accuracyTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfidenceTrendPoint {
        private String date;
        private Double avgConfidence;
        private Long lowConfidenceCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccuracyTrendPoint {
        private String date;
        private Double approvalRate;
        private Double rejectionRate;
        private Double flaggedRate;
    }
}
