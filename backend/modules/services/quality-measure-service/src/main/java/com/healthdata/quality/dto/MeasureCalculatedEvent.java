package com.healthdata.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event published when a quality measure is calculated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasureCalculatedEvent {

    /**
     * ID of the measure result
     */
    private UUID measureResultId;

    /**
     * Tenant ID for multi-tenancy
     */
    private String tenantId;

    /**
     * Patient FHIR ID
     */
    private String patientId;

    /**
     * Measure ID
     */
    private String measureId;

    /**
     * Calculation status
     */
    private String status;

    /**
     * Timestamp of calculation
     */
    private String calculatedAt;
}
