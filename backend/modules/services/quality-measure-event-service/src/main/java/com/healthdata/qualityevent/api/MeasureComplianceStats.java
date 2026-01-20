package com.healthdata.qualityevent.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Measure Compliance Statistics DTO
 * Aggregated compliance statistics for a measure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasureComplianceStats {
    private String measureId;
    private long compliantCount;
    private long totalCount;
    private Double complianceRate;
    private Double averageScore;
}
