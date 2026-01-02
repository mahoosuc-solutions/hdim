package com.healthdata.hedis.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a HEDIS (Healthcare Effectiveness Data and Information Set) quality measure.
 * HEDIS is a tool used by health plans to measure performance on dimensions of care and service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HedisMeasure {

    /**
     * HEDIS measure identifier (e.g., "CBP", "CDC", "COL")
     */
    private String measureId;

    /**
     * Full name of the measure
     */
    private String name;

    /**
     * Description of what the measure evaluates
     */
    private String description;

    /**
     * Domain category (e.g., "Effectiveness of Care", "Access/Availability of Care")
     */
    private String domain;

    /**
     * Specific sub-domain
     */
    private String subDomain;

    /**
     * HEDIS version year
     */
    private Integer year;

    /**
     * Whether this measure is used for STAR ratings
     */
    private boolean usedForStarRatings;

    /**
     * Numerator criteria description
     */
    private String numeratorCriteria;

    /**
     * Denominator criteria description
     */
    private String denominatorCriteria;

    /**
     * Exclusion criteria
     */
    private String exclusionCriteria;

    /**
     * List of required data elements (e.g., diagnoses, procedures, medications)
     */
    private List<String> requiredDataElements;

    /**
     * Measurement period start date
     */
    private LocalDate measurementPeriodStart;

    /**
     * Measurement period end date
     */
    private LocalDate measurementPeriodEnd;

    /**
     * National benchmark rate (if available)
     */
    private Double nationalBenchmark;

    /**
     * CQL library name for this measure
     */
    private String cqlLibraryName;

    /**
     * FHIR-based measure resource URL
     */
    private String fhirMeasureUrl;

    /**
     * Validates required fields and business rules for the HEDIS specification.
     * Returns a list of human-readable errors; an empty list indicates the measure is valid.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(measureId)) {
            errors.add("measureId is required");
        }
        if (StringUtils.isBlank(name)) {
            errors.add("name is required");
        }
        if (StringUtils.isBlank(numeratorCriteria)) {
            errors.add("numeratorCriteria is required");
        }
        if (StringUtils.isBlank(denominatorCriteria)) {
            errors.add("denominatorCriteria is required");
        }
        if (requiredDataElements == null || requiredDataElements.isEmpty()) {
            errors.add("requiredDataElements must contain at least one element");
        }

        if (measurementPeriodStart == null || measurementPeriodEnd == null) {
            errors.add("measurementPeriodStart and measurementPeriodEnd are required");
        } else {
            if (measurementPeriodEnd.isBefore(measurementPeriodStart)) {
                errors.add("measurementPeriodEnd must be on or after measurementPeriodStart");
            }
            if (year != null) {
                if (!Objects.equals(year, measurementPeriodStart.getYear())
                        || !Objects.equals(year, measurementPeriodEnd.getYear())) {
                    errors.add("year must match the measurement period year");
                }
            }
        }

        return errors;
    }
}
