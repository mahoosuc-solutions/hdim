package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Care Gap Closure Event DTO
 * Published when a care gap is automatically closed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGapClosureEvent {

    private String eventId;
    private String eventType; // "care-gap.auto-closed"
    private String tenantId;
    private String patientId;
    private String careGapId;
    private String gapType;
    private String category;
    private String evidenceResourceType;
    private String evidenceResourceId;
    private Instant closedAt;
    private String closedBy;
}
