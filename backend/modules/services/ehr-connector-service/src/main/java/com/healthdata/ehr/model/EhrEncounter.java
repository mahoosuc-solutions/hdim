package com.healthdata.ehr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Normalized encounter model that abstracts vendor-specific encounter representations.
 * Represents a patient visit or interaction with the healthcare system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EhrEncounter {

    /**
     * EHR-specific encounter identifier
     */
    private String ehrEncounterId;

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
     * Encounter status (planned, in-progress, finished, cancelled, etc.)
     */
    private String status;

    /**
     * Encounter class (inpatient, outpatient, emergency, etc.)
     */
    private String encounterClass;

    /**
     * Type of encounter
     */
    private String type;

    /**
     * Priority (routine, urgent, emergency, etc.)
     */
    private String priority;

    /**
     * Service type
     */
    private String serviceType;

    /**
     * Start time of the encounter
     */
    private LocalDateTime startTime;

    /**
     * End time of the encounter
     */
    private LocalDateTime endTime;

    /**
     * Length of encounter in minutes
     */
    private Integer lengthMinutes;

    /**
     * Primary diagnosis codes
     */
    private List<DiagnosisCode> diagnosisCodes;

    /**
     * Procedure codes performed during encounter
     */
    private List<ProcedureCode> procedureCodes;

    /**
     * Location/facility where encounter occurred
     */
    private Location location;

    /**
     * Participating practitioners
     */
    private List<Participant> participants;

    /**
     * Reason for the visit
     */
    private String reasonForVisit;

    /**
     * Admission source (if inpatient)
     */
    private String admissionSource;

    /**
     * Discharge disposition (if applicable)
     */
    private String dischargeDisposition;

    /**
     * Account/billing identifier
     */
    private String accountId;

    /**
     * Raw FHIR resource (if applicable)
     */
    private String rawFhirResource;

    /**
     * Diagnosis code information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagnosisCode {
        private String code;
        private String system; // ICD-10, SNOMED, etc.
        private String display;
        private String use; // primary, secondary, etc.
        private Integer rank;
    }

    /**
     * Procedure code information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcedureCode {
        private String code;
        private String system; // CPT, HCPCS, etc.
        private String display;
        private LocalDateTime performedDateTime;
    }

    /**
     * Location information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location {
        private String locationId;
        private String name;
        private String type;
        private String department;
        private String room;
        private String bed;
    }

    /**
     * Participant (practitioner) information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Participant {
        private String practitionerId;
        private String name;
        private String role; // primary, consulting, attending, etc.
        private String specialty;
    }
}
