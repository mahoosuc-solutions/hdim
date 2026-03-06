package com.healthdata.events.intelligence.dto;

import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record RecommendationLineageResponse(
        UUID recommendationId,
        String sourceEventId,
        String patientRef,
        String signalType,
        String evidence,
        String riskTier,
        Double confidence,
        RecommendationReviewStatus status,
        Instant createdAt,
        Instant reviewedAt,
        String reviewedBy
) {
}
