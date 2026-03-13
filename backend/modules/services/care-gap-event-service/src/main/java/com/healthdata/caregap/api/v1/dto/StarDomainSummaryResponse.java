package com.healthdata.caregap.api.v1.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StarDomainSummaryResponse {
    String domain;
    double domainStars;
    int measureCount;
    double averagePerformanceRate;
}
