package com.healthdata.quality.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Result for a single sub-measure within a HEDIS measure.
 * Example: "HbA1c Testing" is a sub-measure of "Comprehensive Diabetes Care"
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubMeasureResult {

    /**
     * Is patient in the numerator for this sub-measure?
     * (i.e., did they meet the criteria?)
     */
    private boolean numeratorMembership;

    /**
     * Measured value (if applicable)
     * Example: HbA1c value = 7.5, BP = "130/85"
     */
    private String value;

    /**
     * Date when the observation/procedure was performed
     */
    private LocalDate date;

    /**
     * Method or type of measurement
     * Example: "procedure" vs "observation"
     */
    private String method;

    /**
     * Numeric value (for calculations)
     */
    private Double numericValue;

    /**
     * Additional metadata
     */
    private String metadata;
}
