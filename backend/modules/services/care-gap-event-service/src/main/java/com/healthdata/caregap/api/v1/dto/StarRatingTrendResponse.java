package com.healthdata.caregap.api.v1.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class StarRatingTrendResponse {
    String tenantId;
    List<StarRatingTrendPointResponse> points;
}
