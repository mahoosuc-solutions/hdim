package com.healthdata.eventsourcing.command.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthdata.eventsourcing.event.AbstractDomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event fired when two patient records are merged.
 *
 * This event represents the consolidation of duplicate or related patient records.
 * When a source patient is merged into a target patient:
 * - Source patient record becomes inactive
 * - Target patient receives combined identifiers
 * - Merge chain is recorded for historical lookup
 * - Dependent aggregates (care gaps, quality measures) must cascade update
 *
 * ★ Insight ─────────────────────────────────────
 * - Master patient index deduplication via events
 * - Merge chain enables following old IDs to current patient
 * - Combined identifiers support historical searches
 * - Confidence score indicates merge certainty (for audit)
 * - Immutable event allows safe replays and audit trails
 * ─────────────────────────────────────────────────
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMergedEvent extends AbstractDomainEvent {

    /**
     * Tenant identifier (multi-tenant isolation)
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * Source patient ID being merged (deprecated/duplicate)
     * This patient becomes inactive after merge
     */
    @JsonProperty("source_patient_id")
    private String sourcePatientId;

    /**
     * Target patient ID receiving the merge (canonical/surviving)
     * This patient continues with combined identifiers
     */
    @JsonProperty("target_patient_id")
    private String targetPatientId;

    /**
     * FHIR Patient Resource ID of source (if linked)
     */
    @JsonProperty("source_fhir_resource_id")
    private UUID sourceFhirResourceId;

    /**
     * FHIR Patient Resource ID of target (if linked)
     */
    @JsonProperty("target_fhir_resource_id")
    private UUID targetFhirResourceId;

    /**
     * Combined identifiers from both source and target patients
     * Includes all MRNs, SSNs, Enterprise IDs from both records
     * Supports historical lookups through old identifiers
     */
    @JsonProperty("combined_identifiers")
    private List<PatientIdentifier> combinedIdentifiers;

    /**
     * Confidence score (0.0-1.0) indicating merge certainty
     * 1.0 = deterministic (manual merge or exact match)
     * < 1.0 = probabilistic (based on name/DOB similarity)
     * Used for audit and potential future reversal
     */
    @JsonProperty("confidence_score")
    @Builder.Default
    private Double confidenceScore = 1.0;

    /**
     * Reason for merge (for audit and operational intelligence)
     * Examples: "DUPLICATE_MRN", "EXACT_MATCH", "MANUAL_MERGE", "PROBABILISTIC_MATCH"
     */
    @JsonProperty("merge_reason")
    private String mergeReason;

    /**
     * User ID who initiated the merge (null if automated)
     */
    @JsonProperty("merged_by_user_id")
    private String mergedByUserId;

    /**
     * Timestamp when merge occurred
     */
    @JsonProperty("merged_at")
    private Instant mergedAt;

    /**
     * Generate aggregate ID for tracking merge as a domain event
     * Uses target patient ID as the primary aggregate
     * @return aggregate ID in format "patient-{tenantId}-{targetPatientId}"
     */
    @Override
    public String getAggregateId() {
        return "patient-" + tenantId + "-" + targetPatientId;
    }

    /**
     * Event type for this event
     * @return "PatientMerged"
     */
    @Override
    public String getEventType() {
        return "PatientMerged";
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
