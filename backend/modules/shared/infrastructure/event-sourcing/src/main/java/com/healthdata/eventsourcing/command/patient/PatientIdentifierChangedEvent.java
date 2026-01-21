package com.healthdata.eventsourcing.command.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthdata.eventsourcing.event.AbstractDomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Domain event fired when a patient's identifier is added, updated, or deprecated.
 *
 * This event tracks the complete lifecycle of patient identifiers:
 * - New identifier added (e.g., new Enterprise ID assigned)
 * - Identifier superseded (e.g., old MRN deprecated after merge)
 * - Identifier status changed (e.g., marked inactive)
 *
 * This enables:
 * - Historical lookup through deprecated identifiers
 * - Audit trail of all identifier changes
 * - Proper merging of duplicate patient records
 * - HIPAA compliance for identifier lifecycle tracking
 *
 * ★ Insight ─────────────────────────────────────
 * - Immutable events create audit trail of all ID changes
 * - Old identifiers remain searchable via deprecation events
 * - Enables "find patient by old MRN" even after merge
 * - Supports FHIR Patient.link deprecation (old IDs not lost)
 * - Change reason field enables operational intelligence
 * ─────────────────────────────────────────────────
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientIdentifierChangedEvent extends AbstractDomainEvent {

    /**
     * Tenant identifier (multi-tenant isolation)
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * Patient ID whose identifier changed
     */
    @JsonProperty("patient_id")
    private String patientId;

    /**
     * The identifier that changed (added, updated, or deprecated)
     */
    @JsonProperty("identifier")
    private PatientIdentifier identifier;

    /**
     * Type of change: "ADDED", "UPDATED", "DEPRECATED", "REACTIVATED"
     */
    @JsonProperty("change_type")
    private String changeType;

    /**
     * Previous identifier state (for UPDATED or DEPRECATED changes)
     * Null for ADDED and REACTIVATED changes
     */
    @JsonProperty("previous_identifier")
    private PatientIdentifier previousIdentifier;

    /**
     * Reason for the change (for audit and operational intelligence)
     * Examples:
     * - "NEW_INSURANCE_ID": New insurance plan
     * - "MERGE_FROM_PATIENT": Identifier from merged patient record
     * - "DEDUP_INACTIVATED": Inactivated due to deduplication
     * - "CORRECTION": Correction of typo or invalid identifier
     * - "SYSTEM_MIGRATION": Migration from legacy system
     */
    @JsonProperty("change_reason")
    private String changeReason;

    /**
     * User ID who initiated the change (null if system-initiated)
     */
    @JsonProperty("changed_by_user_id")
    private String changedByUserId;

    /**
     * Timestamp when change occurred
     */
    @JsonProperty("changed_at")
    private Instant changedAt;

    /**
     * Source of change: "FHIR_SYNC", "MANUAL", "IMPORT", "SYSTEM"
     */
    @JsonProperty("change_source")
    @Builder.Default
    private String changeSource = "MANUAL";

    /**
     * Generate aggregate ID for the patient
     * @return aggregate ID in format "patient-{tenantId}-{patientId}"
     */
    @Override
    public String getAggregateId() {
        return "patient-" + tenantId + "-" + patientId;
    }

    /**
     * Event type for this event
     * @return "PatientIdentifierChanged"
     */
    @Override
    public String getEventType() {
        return "PatientIdentifierChanged";
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
