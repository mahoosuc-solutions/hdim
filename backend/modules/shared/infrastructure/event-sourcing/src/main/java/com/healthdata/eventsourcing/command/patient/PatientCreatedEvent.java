package com.healthdata.eventsourcing.command.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthdata.eventsourcing.event.AbstractDomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Domain event fired when a patient is created.
 *
 * This event:
 * - Represents immutable historical fact
 * - Contains all data needed to recreate patient state
 * - Supports multiple FHIR-compliant identifiers (MRN, SSN, Enterprise ID, etc.)
 * - Links to FHIR Patient resource for interoperability
 * - Uses deterministic aggregate ID for idempotency
 * - Includes HIPAA sensitivity and compliance markers
 *
 * ★ Insight ─────────────────────────────────────
 * - Deterministic ID: "patient-{tenantId}-{mrn}" allows safe retries
 * - Multi-identifier support: identifiers[] contains all patient IDs
 * - FHIR linkage: fhirResourceId tracks FHIR Patient resource
 * - Immutability ensures event sourcing integrity
 * - HIPAA markers enable compliance tracking
 * - Supports patient merge chains via identifier history
 * ─────────────────────────────────────────────────
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCreatedEvent extends AbstractDomainEvent {

    /**
     * Tenant identifier (multi-tenant isolation)
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * Patient identifier (unique per tenant)
     */
    @JsonProperty("patient_id")
    private String patientId;

    /**
     * Patient's first name
     */
    @JsonProperty("first_name")
    private String firstName;

    /**
     * Patient's last name
     */
    @JsonProperty("last_name")
    private String lastName;

    /**
     * Patient's date of birth
     */
    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Patient's gender
     */
    @JsonProperty("gender")
    private String gender;

    /**
     * Medical Record Number (MRN)
     * Part of deterministic aggregate ID
     */
    @JsonProperty("mrn")
    private String mrn;

    /**
     * Insurance member ID
     */
    @JsonProperty("insurance_member_id")
    private String insuranceMemberId;

    /**
     * Multiple patient identifiers (FHIR-compliant)
     * Supports MRN, SSN, Enterprise ID, and other identifier types
     * Each identifier has system, value, type, and use properties
     */
    @JsonProperty("identifiers")
    private List<PatientIdentifier> identifiers;

    /**
     * FHIR Patient Resource ID (UUID from fhir-service)
     * Links event-sourced patient to FHIR resource
     */
    @JsonProperty("fhir_resource_id")
    private UUID fhirResourceId;

    /**
     * Timestamp when FHIR resource was last synchronized
     */
    @JsonProperty("fhir_last_updated")
    private Instant fhirLastUpdated;

    /**
     * HIPAA sensitivity level
     */
    @JsonProperty("sensitivity_level")
    @Builder.Default
    private String sensitivityLevel = "SENSITIVE";

    /**
     * Indicates this is a compliant PHI creation event
     */
    @JsonProperty("hipaa_compliant")
    @Builder.Default
    private boolean hipaaCompliant = true;

    /**
     * Generate deterministic aggregate ID for idempotency
     * @return aggregate ID in format "patient-{tenantId}-{mrn}"
     */
    @Override
    public String getAggregateId() {
        return "patient-" + tenantId + "-" + mrn;
    }

    /**
     * Event type for this event
     * @return "PatientCreated"
     */
    @Override
    public String getEventType() {
        return "PatientCreated";
    }

    /**
     * Resource type for this event
     * @return "Patient"
     */
    @Override
    public String getResourceType() {
        return "Patient";
    }
}
