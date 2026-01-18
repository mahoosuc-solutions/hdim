package com.healthdata.eventsourcing.command.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthdata.eventsourcing.event.AbstractDomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Domain event fired when a patient is created.
 *
 * This event:
 * - Represents immutable historical fact
 * - Contains all data needed to recreate patient state
 * - Uses deterministic aggregate ID for idempotency
 * - Includes HIPAA sensitivity and compliance markers
 *
 * ★ Insight ─────────────────────────────────────
 * - Deterministic ID: "patient-{tenantId}-{mrn}" allows safe retries
 * - Immutability ensures event sourcing integrity
 * - HIPAA markers enable compliance tracking
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
}
