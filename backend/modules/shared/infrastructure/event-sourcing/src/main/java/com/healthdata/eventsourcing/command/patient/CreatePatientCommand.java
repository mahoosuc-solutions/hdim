package com.healthdata.eventsourcing.command.patient;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to create a new patient in the system.
 *
 * Immutable command object following CQRS pattern:
 * - Contains all data needed to create a patient
 * - Validated independently before aggregate creation
 * - Multi-tenant isolated via tenantId
 * - Deterministic aggregate ID: "patient-{tenantId}-{mrn}"
 *
 * ★ Insight ─────────────────────────────────────
 * - Immutable command ensures idempotency
 * - Deterministic ID allows safe retries (same MRN → same aggregate ID)
 * - Multi-tenant isolation starts at command level
 * ─────────────────────────────────────────────────
 */
@Getter
@ToString
@Builder
public class CreatePatientCommand {

    /**
     * Tenant identifier (multi-tenant isolation)
     * Required for all commands to enforce tenant boundaries
     */
    private final String tenantId;

    /**
     * Patient's first name
     * Validated: 1-100 characters, letters/hyphens only
     */
    private final String firstName;

    /**
     * Patient's last name
     * Validated: 1-100 characters, letters/hyphens only
     */
    private final String lastName;

    /**
     * Patient's date of birth
     * Validated: within 150 years, not in future
     */
    private final LocalDate dateOfBirth;

    /**
     * Patient's gender code
     * Valid values: MALE, FEMALE, OTHER, UNKNOWN
     */
    private final String gender;

    /**
     * Medical Record Number (MRN)
     * Validated: unique per tenant (same MRN allowed across tenants)
     * Used as part of deterministic aggregate ID
     */
    private final String mrn;

    /**
     * Insurance member ID
     * External identifier for insurance systems
     */
    private final String insuranceMemberId;
}
