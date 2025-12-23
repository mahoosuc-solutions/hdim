package com.healthdata.quality.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Measure Calculated Event
 * Published when a quality measure calculation completes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureCalculatedEvent {
    private UUID measureResultId;
    private String tenantId;
    private UUID patientId;
    private String measureId;
    private String result; // "PASS", "FAIL", "NOT_APPLICABLE"
}
