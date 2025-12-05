package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Overall data quality assessment report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataQualityReport {
    private String reportId;
    private String patientId;
    private double overallScore;

    @Builder.Default
    private Map<QualityDimension, Double> dimensions = new HashMap<>();

    @Builder.Default
    private List<QualityIssue> issues = new ArrayList<>();

    @Builder.Default
    private List<RemediationAction> remediationActions = new ArrayList<>();

    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
}
