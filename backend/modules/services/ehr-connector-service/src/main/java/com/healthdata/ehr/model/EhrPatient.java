package com.healthdata.ehr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Normalized patient model that abstracts vendor-specific patient representations.
 * This model provides a consistent interface across different EHR systems.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EhrPatient {

    /**
     * EHR-specific patient identifier
     */
    private String ehrPatientId;

    /**
     * Tenant identifier
     */
    private String tenantId;

    /**
     * Source EHR vendor
     */
    private EhrVendorType sourceVendor;

    /**
     * Medical Record Number (MRN)
     */
    private String mrn;

    /**
     * Patient's family name
     */
    private String familyName;

    /**
     * Patient's given name(s)
     */
    private String givenName;

    /**
     * Patient's middle name
     */
    private String middleName;

    /**
     * Date of birth
     */
    private LocalDate dateOfBirth;

    /**
     * Administrative gender (male, female, other, unknown)
     */
    private String gender;

    /**
     * Social Security Number (encrypted)
     */
    private String ssn;

    /**
     * Contact phone number
     */
    private String phone;

    /**
     * Contact email
     */
    private String email;

    /**
     * Address line 1
     */
    private String addressLine1;

    /**
     * Address line 2
     */
    private String addressLine2;

    /**
     * City
     */
    private String city;

    /**
     * State or province
     */
    private String state;

    /**
     * Postal code
     */
    private String postalCode;

    /**
     * Country
     */
    private String country;

    /**
     * Patient identifiers from other systems
     */
    private List<Identifier> identifiers;

    /**
     * Whether patient is active in the EHR
     */
    private Boolean active;

    /**
     * Whether patient is deceased
     */
    private Boolean deceased;

    /**
     * Date of death if deceased
     */
    private LocalDate deceasedDate;

    /**
     * Marital status
     */
    private String maritalStatus;

    /**
     * Preferred language
     */
    private String language;

    /**
     * Race
     */
    private String race;

    /**
     * Ethnicity
     */
    private String ethnicity;

    /**
     * Raw FHIR resource (if applicable)
     */
    private String rawFhirResource;

    /**
     * Patient identifier from other systems
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Identifier {
        private String system;
        private String value;
        private String type;
    }
}
