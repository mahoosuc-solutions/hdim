package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Health disparity measurement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisparityMetric {
    private String metricId;
    private String metricName;
    private StratificationType stratificationType;
    private String stratificationValue;
    private Double metricValue;
    private Double benchmarkValue;
    private Double disparityRatio;
    private String unit;
    private String description;

    public enum StratificationType {
        RACE,
        ETHNICITY,
        LANGUAGE,
        GEOGRAPHY,
        INSURANCE_STATUS,
        INCOME_LEVEL,
        AGE_GROUP,
        GENDER
    }
}
