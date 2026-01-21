package com.healthdata.eventsourcing.command.observation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Command to record a patient observation (vital sign or clinical measurement).
 *
 * Immutable command for FHIR Observation recording:
 * - LOINC code identifies type of observation (vital sign)
 * - Value quantity with numerical value and unit
 * - Multi-tenant isolated via tenantId
 * - Patient reference validation required
 *
 * ★ Insight ─────────────────────────────────────
 * - LOINC codes map to standardized vital signs (8 codes supported)
 * - Value ranges enforced by ObservationValidator per LOINC code
 * - Observation date enables clinical timeline reconstruction
 * ─────────────────────────────────────────────────
 */
@Getter
@ToString
@Builder
public class RecordObservationCommand {

    /**
     * Tenant identifier (multi-tenant isolation)
     */
    private final String tenantId;

    /**
     * Patient UUID this observation belongs to
     * MUST exist before recording observation (referential integrity)
     */
    private final String patientId;

    /**
     * LOINC code identifying observation type
     * Supported: 8310-5 (Temperature), 8867-4 (Heart Rate), 2339-0 (Glucose), etc.
     */
    private final String loincCode;

    /**
     * Numerical value of the observation
     */
    private final BigDecimal value;

    /**
     * Unit of measurement (e.g., "°C", "/min", "mg/dL")
     */
    private final String unit;

    /**
     * Date/time observation was made
     */
    private final Instant observationDate;

    /**
     * Optional notes about the observation
     */
    private final String notes;
}
