package com.healthdata.quality.measure;

import com.healthdata.quality.model.MeasureResult;
import com.healthdata.quality.model.PatientData;

/**
 * Interface for HEDIS quality measure calculators.
 * Each HEDIS measure (CDC, CBP, BCS, etc.) implements this interface.
 */
public interface MeasureCalculator {

    /**
     * Calculate the measure for a given patient.
     *
     * @param patientData Patient's FHIR resources and demographic data
     * @return Measure calculation result with care gaps and recommendations
     */
    MeasureResult calculate(PatientData patientData);

    /**
     * Get the measure ID (e.g., "CDC", "CBP", "BCS")
     */
    String getMeasureId();

    /**
     * Get the full measure name
     */
    String getMeasureName();

    /**
     * Get the measure version year
     */
    String getVersion();
}
