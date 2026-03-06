package com.healthdata.events.intelligence.dto;

import java.time.Instant;
import java.util.UUID;

public record SignalResponse(
        UUID recommendationId,
        String patientRef,
        String signalType,
        String riskTier,
        Double confidence,
        String summary,
        Instant observedAt
) {
}
