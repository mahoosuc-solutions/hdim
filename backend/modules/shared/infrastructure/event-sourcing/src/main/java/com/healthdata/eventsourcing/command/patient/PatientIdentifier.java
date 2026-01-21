package com.healthdata.eventsourcing.command.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FHIR-compliant patient identifier value object.
 *
 * Supports multiple identifier types per patient:
 * - MRN (Medical Record Number): system="http://hospital.org/mrn", type="MR"
 * - SSN (Social Security Number): system="http://hl7.org/fhir/sid/us-ssn", type="SS"
 * - Enterprise ID: system="http://enterprise.org/patients", type="EN"
 *
 * Each identifier has:
 * - system: Identifier namespace/system (FHIR OID or URL)
 * - value: The actual identifier string
 * - type: FHIR identifier type (MR=Medical Record, SS=Social Security, EN=Employer)
 * - use: Purpose code (official, secondary, old)
 *
 * ★ Insight ─────────────────────────────────────
 * - FHIR allows Patient.identifier[] array for multiple IDs
 * - Each identifier has system (namespace) and type (classification)
 * - Use code tracks whether ID is current (official) or deprecated (old)
 * - Enables merge chains: old IDs remain searchable via deprecated identifiers
 * ─────────────────────────────────────────────────
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientIdentifier {

    /**
     * Identifier system/namespace (FHIR OID or URL)
     * Examples:
     * - http://hospital.org/mrn
     * - http://hl7.org/fhir/sid/us-ssn
     * - http://enterprise.org/patients
     */
    @JsonProperty("system")
    private String system;

    /**
     * The actual identifier value
     * Examples:
     * - MRN-12345
     * - 123-45-6789
     * - EMP-67890
     */
    @JsonProperty("value")
    private String value;

    /**
     * FHIR identifier type code
     * - MR: Medical Record Number
     * - SS: Social Security Number
     * - EN: Employer Number / Enterprise ID
     * - DL: Driver's License
     * - PPN: Passport Number
     * - BRN: Breed Registry Number
     * - MRT: Temporary Medical Record Number
     */
    @JsonProperty("type")
    private String type;

    /**
     * Purpose of identifier
     * - official: The main identifier for the resource
     * - secondary: Additional identifier
     * - old: Deprecated/superseded identifier (kept for historical queries)
     * - nickname: Informal identifier
     */
    @JsonProperty("use")
    @Builder.Default
    private String use = "official";

    /**
     * Whether this identifier is currently active
     * Useful for tracking deprecated identifiers
     */
    @JsonProperty("active")
    @Builder.Default
    private Boolean active = true;

    @Override
    public String toString() {
        return String.format("PatientIdentifier{system='%s', value='%s', type='%s', use='%s', active=%s}",
                system, value, type, use, active);
    }
}
