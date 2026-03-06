package com.healthdata.events.intelligence.dto;

import com.healthdata.eventsourcing.intelligence.RecommendationReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record RecommendationResponse(
        UUID id,
        String patientRef,
        String signalType,
        String title,
        String description,
        String riskTier,
        Double confidence,
        RecommendationReviewStatus status,
        String reviewedBy,
        String reviewNotes,
        Instant reviewedAt,
        Instant createdAt
) {
}
