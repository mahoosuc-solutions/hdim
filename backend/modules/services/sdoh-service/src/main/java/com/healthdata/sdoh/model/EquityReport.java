package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive health equity report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquityReport {
    private String reportId;
    private String tenantId;
    private LocalDateTime reportDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<DisparityMetric> disparityMetrics;
    private Map<SdohCategory, Integer> sdohPrevalence;
    private Map<String, Map<String, Object>> stratifiedAnalysis;
    private List<String> keyFindings;
    private List<String> recommendations;
    private String generatedBy;
    private LocalDateTime createdAt;
}
