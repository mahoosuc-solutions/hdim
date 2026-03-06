package com.healthdata.events.intelligence.dto;

import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingType;
import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.Severity;

import java.time.Instant;
import java.util.UUID;

public record ValidationFindingResponse(
        UUID id,
        String sourceEventId,
        String ruleCode,
        String title,
        String description,
        Severity severity,
        FindingType findingType,
        FindingStatus status,
        String details,
        Instant createdAt
) {
}
