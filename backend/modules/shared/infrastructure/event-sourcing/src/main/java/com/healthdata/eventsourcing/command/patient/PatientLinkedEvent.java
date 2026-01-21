package com.healthdata.eventsourcing.command.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthdata.eventsourcing.event.AbstractDomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event fired when FHIR Patient.link is processed.
 *
 * FHIR Patient.link represents relationships between patient records:
 * - "replaced-by": This patient was superseded by another (merge source)
 * - "seealso": Related/duplicate patient (not definitive merge)
 * - "refer": Alternate patient identity in different system
 * - "seealso": Information for link may not be definitive
 *
 * This event enables:
 * - Processing of patient merge chains from FHIR service
 * - Tracking historical patient record relationships
 * - Supporting FHIR interoperability for patient matching
 * - Cascading updates to dependent aggregates (care gaps, measures)
 *
 * ★ Insight ─────────────────────────────────────
 * - FHIR Patient.link is standardized relationship format
 * - "replaced-by" link type corresponds to our merge operation
 * - Link status (active/superseded) tracks link validity
 * - Enable cascading deduplication across federated systems
 * - Immutable events preserve audit trail of all links
 * ─────────────────────────────────────────────────
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientLinkedEvent extends AbstractDomainEvent {

    /**
     * Tenant identifier (multi-tenant isolation)
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * Source patient ID (patient this link is on)
     */
    @JsonProperty("source_patient_id")
    private String sourcePatientId;

    /**
     * Source FHIR Patient Resource ID
     */
    @JsonProperty("source_fhir_resource_id")
    private UUID sourceFhirResourceId;

    /**
     * Target patient ID (linked-to patient)
     */
    @JsonProperty("target_patient_id")
    private String targetPatientId;

    /**
     * Target FHIR Patient Resource ID
     */
    @JsonProperty("target_fhir_resource_id")
    private UUID targetFhirResourceId;

    /**
     * FHIR Patient.link type (from FHIR specification)
     * Values: "replaced-by", "replaces", "refer", "seealso"
     *
     * - "replaced-by": This patient was replaced (merge target)
     * - "replaces": This patient replaced another (merge source)
     * - "refer": Another patient reference (not definitive)
     * - "seealso": Related patient (informational only)
     */
    @JsonProperty("link_type")
    private String linkType;

    /**
     * Assertion status for the link (from FHIR)
     * Values: "confirmed", "provisional"
     *
     * - "confirmed": Link is definitive (process merge immediately)
     * - "provisional": Link is suspected (may need review)
     */
    @JsonProperty("assertion_status")
    @Builder.Default
    private String assertionStatus = "confirmed";

    /**
     * Confidence score (0.0-1.0) for this link
     * Used especially for "provisional" links
     * Example: 0.95 means 95% confidence this is a valid link
     */
    @JsonProperty("confidence_score")
    private Double confidenceScore;

    /**
     * Context for this link (for audit)
     * Examples: "EHR_SYNC", "HIE_MATCH", "PATIENT_REPORTED", "MANUAL_REVIEW"
     */
    @JsonProperty("link_context")
    private String linkContext;

    /**
     * User ID who created this link (null if system-initiated)
     */
    @JsonProperty("linked_by_user_id")
    private String linkedByUserId;

    /**
     * Timestamp when link was created/processed
     */
    @JsonProperty("linked_at")
    private Instant linkedAt;

    /**
     * Whether this link is currently active
     * Links can be superseded when merges are reversed or corrected
     */
    @JsonProperty("active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Comment/reason for this link (for audit trail)
     */
    @JsonProperty("link_reason")
    private String linkReason;

    /**
     * Generate aggregate ID for the source patient
     * @return aggregate ID in format "patient-{tenantId}-{sourcePatientId}"
     */
    @Override
    public String getAggregateId() {
        return "patient-" + tenantId + "-" + sourcePatientId;
    }

    /**
     * Event type for this event
     * @return "PatientLinked"
     */
    @Override
    public String getEventType() {
        return "PatientLinked";
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
