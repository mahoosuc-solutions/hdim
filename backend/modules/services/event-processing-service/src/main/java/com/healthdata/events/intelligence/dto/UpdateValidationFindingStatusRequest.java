package com.healthdata.events.intelligence.dto;

import com.healthdata.events.intelligence.entity.IntelligenceValidationFindingEntity.FindingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateValidationFindingStatusRequest(
        @NotNull FindingStatus status,
        String actedBy,
        String notes
) {
}
