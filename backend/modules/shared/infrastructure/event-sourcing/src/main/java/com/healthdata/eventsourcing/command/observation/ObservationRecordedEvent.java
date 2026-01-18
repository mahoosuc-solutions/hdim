package com.healthdata.eventsourcing.command.observation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthdata.eventsourcing.event.FhirResourceEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event fired when an observation (vital sign) is recorded.
 *
 * This event:
 * - Extends FhirResourceEvent for FHIR integration
 * - Cascades to Quality Measure, Care Gap, and Analytics services
 * - Indexed fields enable efficient queries
 * - Includes LOINC code for clinical standardization
 *
 * ★ Insight ─────────────────────────────────────
 * - FhirResourceEvent enables multi-service integration
 * - Indexed fields (patientId, loincCode) optimize projections
 * - Value is immutable once recorded (audit trail)
 * ─────────────────────────────────────────────────
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationRecordedEvent extends FhirResourceEvent {

    /**
     * Tenant identifier (multi-tenant isolation)
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * Patient UUID (indexed for queries)
     */
    @JsonProperty("patient_id")
    private String patientId;

    /**
     * LOINC code (indexed for queries)
     */
    @JsonProperty("loinc_code")
    private String loincCode;

    /**
     * Observation value (indexed for range queries)
     */
    @JsonProperty("value")
    private BigDecimal value;

    /**
     * Unit of measurement (e.g., "°C", "/min", "mg/dL")
     */
    @JsonProperty("unit")
    private String unit;

    /**
     * When observation was made
     */
    @JsonProperty("observation_date")
    private Instant observationDate;

    /**
     * Optional clinical notes
     */
    @JsonProperty("notes")
    private String notes;

    /**
     * FHIR resource type
     */
    @Override
    public String getResourceType() {
        return "Observation";
    }

    /**
     * Event type identifier
     */
    @Override
    public String getEventType() {
        return "ObservationRecorded";
    }

    /**
     * Aggregate ID based on patient and observation
     */
    @Override
    public String getAggregateId() {
        return "patient-" + tenantId + "-" + patientId;
    }
}
