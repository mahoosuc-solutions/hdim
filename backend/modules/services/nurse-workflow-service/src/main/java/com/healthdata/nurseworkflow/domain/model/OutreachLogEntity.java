package com.healthdata.nurseworkflow.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Patient Outreach Log Entity
 *
 * Tracks patient outreach attempts by nurses including contact method, outcome,
 * and follow-up scheduling. Links to FHIR Task and Communication resources.
 *
 * HIPAA Compliance: Access to all fields requires audit logging.
 */
@Entity
@Table(name = "outreach_logs", indexes = {
    @Index(name = "idx_outreach_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_outreach_nurse", columnList = "nurse_id"),
    @Index(name = "idx_outreach_attempted_at", columnList = "attempted_at"),
    @Index(name = "idx_outreach_outcome", columnList = "outcome_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutreachLogEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "nurse_id", nullable = false)
    private UUID nurseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_type", nullable = false, length = 50)
    private OutcomeType outcomeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_method", nullable = false, length = 50)
    private ContactMethod contactMethod;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason; // post-discharge, medication-reminder, screening-reminder, care-gap-intervention

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "scheduled_follow_up")
    private Instant scheduledFollowUp;

    // FHIR Resource References
    @Column(name = "task_id", length = 128)
    private String taskId; // FHIR Task resource UUID

    @Column(name = "communication_id", length = 128)
    private String communicationId; // FHIR Communication resource UUID

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        this.createdAt = Instant.now();
    }

    /**
     * Outcome types for patient outreach attempts
     */
    public enum OutcomeType {
        SUCCESSFUL_CONTACT,      // Successfully reached patient
        NO_ANSWER,              // Patient did not answer
        LEFT_MESSAGE,           // Left voicemail/message
        WRONG_NUMBER,           // Number is incorrect
        CALL_REFUSED,           // Patient refused to take call
        LANGUAGE_BARRIER,       // Could not communicate due to language
        UNABLE_TO_SCHEDULE,     // Could not schedule needed appointment
        PATIENT_UNAVAILABLE,    // Patient available but inconvenient time
        ESCALATION_NEEDED,      // Contact revealed concerns requiring provider review
        OTHER                   // Other outcome
    }

    /**
     * Contact methods for patient outreach
     */
    public enum ContactMethod {
        PHONE,
        EMAIL,
        SMS,
        IN_PERSON,
        PATIENT_PORTAL,
        FAMILY_MEMBER
    }
}
