package com.healthdata.caregap.api.v1.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class StarRatingTrendPointResponse {
    LocalDate snapshotDate;
    String granularity;
    double overallRating;
    double roundedRating;
    int openGapCount;
    int closedGapCount;
    boolean qualityBonusEligible;
}
