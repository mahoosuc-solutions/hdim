package com.healthdata.events.intelligence.dto;

import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewRecommendationRequest(
        @NotNull RecommendationReviewStatus status,
        String reviewedBy,
        String notes
) {
}
