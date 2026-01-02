package com.healthdata.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiSummaryDto {

    private String metricType;

    private String metricName;

    private BigDecimal currentValue;

    private BigDecimal previousValue;

    private BigDecimal changePercent;

    private String trend; // UP, DOWN, STABLE

    private LocalDate asOfDate;

    private Map<String, Object> breakdown;

    private List<TrendPointDto> trendData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointDto {
        private LocalDate date;
        private BigDecimal value;
    }

    public String getTrend() {
        if (changePercent == null) {
            return "STABLE";
        }
        if (changePercent.compareTo(BigDecimal.valueOf(0.5)) > 0) {
            return "UP";
        } else if (changePercent.compareTo(BigDecimal.valueOf(-0.5)) < 0) {
            return "DOWN";
        }
        return "STABLE";
    }
}
