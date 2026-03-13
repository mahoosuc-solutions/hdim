package com.healthdata.caregap.api.v1.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StarMeasureSummaryResponse {
    String measureCode;
    String measureName;
    String domain;
    int numerator;
    int denominator;
    double performanceRate;
    int stars;
}
