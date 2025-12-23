package com.healthdata.cql.measure;

import java.util.UUID;

/**
 * Interface for HEDIS Quality Measures
 *
 * All HEDIS measures must implement this interface to provide
 * consistent evaluation logic and result formatting.
 */
public interface HedisMeasure {

    /**
     * Get the HEDIS measure ID (e.g., "CDC", "CBP", "BCS")
     */
    String getMeasureId();

    /**
     * Get the human-readable measure name
     */
    String getMeasureName();

    /**
     * Get the HEDIS measure version
     */
    String getVersion();

    /**
     * Evaluate the measure for a specific patient
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return Measure evaluation result
     */
    MeasureResult evaluate(String tenantId, UUID patientId);

    /**
     * Check if patient is eligible for this measure (denominator)
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return true if patient should be evaluated for this measure
     */
    boolean isEligible(String tenantId, UUID patientId);
}
