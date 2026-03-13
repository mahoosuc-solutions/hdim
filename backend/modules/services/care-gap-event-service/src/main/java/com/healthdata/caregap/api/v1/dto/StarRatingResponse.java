package com.healthdata.caregap.api.v1.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class StarRatingResponse {
    String tenantId;
    double overallRating;
    double roundedRating;
    int measureCount;
    int openGapCount;
    int closedGapCount;
    boolean qualityBonusEligible;
    String lastTriggerEvent;
    Instant calculatedAt;
    List<StarDomainSummaryResponse> domains;
    List<StarMeasureSummaryResponse> measures;
}
