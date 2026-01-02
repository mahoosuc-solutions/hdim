package com.healthdata.ehr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Normalized observation model that abstracts vendor-specific observation/lab result representations.
 * Represents clinical observations, vital signs, and laboratory results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EhrObservation {

    /**
     * EHR-specific observation identifier
     */
    private String ehrObservationId;

    /**
     * Tenant identifier
     */
    private String tenantId;

    /**
     * Source EHR vendor
     */
    private EhrVendorType sourceVendor;

    /**
     * Associated patient ID
     */
    private String ehrPatientId;

    /**
     * Associated encounter ID (if applicable)
     */
    private String ehrEncounterId;

    /**
     * Observation status (final, preliminary, amended, cancelled, etc.)
     */
    private String status;

    /**
     * Category (vital-signs, laboratory, imaging, survey, etc.)
     */
    private String category;

    /**
     * Observation code
     */
    private Code code;

    /**
     * Time observation was made
     */
    private LocalDateTime effectiveDateTime;

    /**
     * When observation was issued/reported
     */
    private LocalDateTime issuedDateTime;

    /**
     * Value type (Quantity, String, Boolean, etc.)
     */
    private String valueType;

    /**
     * Numeric value (if applicable)
     */
    private Double valueQuantity;

    /**
     * Unit of measure
     */
    private String valueUnit;

    /**
     * String value (if applicable)
     */
    private String valueString;

    /**
     * Coded value (if applicable)
     */
    private Code valueCodeableConcept;

    /**
     * Interpretation (normal, abnormal, critical, etc.)
     */
    private String interpretation;

    /**
     * Reference ranges
     */
    private List<ReferenceRange> referenceRanges;

    /**
     * Body site where observation was made
     */
    private String bodySite;

    /**
     * Method used to obtain the observation
     */
    private String method;

    /**
     * Specimen information (for lab results)
     */
    private Specimen specimen;

    /**
     * Performing organization/lab
     */
    private String performer;

    /**
     * Notes or comments
     */
    private String note;

    /**
     * Component observations (for multi-component results like blood pressure)
     */
    private List<Component> components;

    /**
     * Raw FHIR resource (if applicable)
     */
    private String rawFhirResource;

    /**
     * Code information (LOINC, SNOMED, etc.)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Code {
        private String code;
        private String system; // LOINC, SNOMED, etc.
        private String display;
    }

    /**
     * Reference range information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceRange {
        private Double low;
        private Double high;
        private String unit;
        private String type; // normal, critical, absolute, etc.
        private String appliesTo; // age, gender, etc.
        private String text;
    }

    /**
     * Specimen information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Specimen {
        private String specimenId;
        private String type;
        private LocalDateTime collectedDateTime;
        private String bodySite;
    }

    /**
     * Component observation (for multi-part results)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Component {
        private Code code;
        private Double valueQuantity;
        private String valueUnit;
        private String valueString;
        private String interpretation;
    }
}
