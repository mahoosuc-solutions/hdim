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
 * Referral Coordination Entity
 *
 * Tracks the referral process from request through completion, including:
 * - Insurance authorization
 * - Specialist office scheduling
 * - Medical records transmission
 * - Appointment completion
 * - Results follow-up
 *
 * Implements:
 * - PCMH (Patient-Centered Medical Home): Care coordination requirement
 * - Closed-Loop Referrals: Ensure referral completion is tracked
 * - Continuity of Care: Track follow-up with ordering provider
 *
 * HIPAA Compliance: Referral details are PHI - all access requires audit logging.
 */
@Entity
@Table(name = "referral_coordinations", indexes = {
    @Index(name = "idx_ref_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_ref_coordinator", columnList = "coordinator_id"),
    @Index(name = "idx_ref_status", columnList = "status"),
    @Index(name = "idx_ref_specialty", columnList = "specialty_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralCoordinationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "coordinator_id", nullable = false)
    private UUID coordinatorId; // RN coordinating the referral

    @Column(name = "referrer_id", nullable = false, length = 128)
    private String referrerId; // Ordering provider (Practitioner resource UUID)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReferralStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private ReferralPriority priority; // Routine, Urgent, Stat

    @Column(name = "specialty_type", nullable = false, length = 100)
    private String specialtyType; // Cardiology, Podiatry, etc.

    @Column(name = "reason_for_referral", columnDefinition = "TEXT")
    private String reasonForReferral;

    // Specialist information
    @Column(name = "specialist_practice_name")
    private String specialistPracticeName;

    @Column(name = "specialist_provider_name")
    private String specialistProviderName;

    @Column(name = "specialist_contact_phone", length = 20)
    private String specialistContactPhone;

    @Column(name = "specialist_contact_fax", length = 20)
    private String specialistContactFax;

    @Column(name = "specialist_contact_email")
    private String specialistContactEmail;

    // Insurance authorization
    @Column(name = "authorization_required")
    private Boolean authorizationRequired;

    @Column(name = "authorization_number", length = 128)
    private String authorizationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization_status", length = 50)
    private AuthorizationStatus authorizationStatus;

    // Appointment tracking
    @Column(name = "appointment_scheduled")
    private Boolean appointmentScheduled;

    @Column(name = "appointment_date")
    private Instant appointmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_status", length = 50)
    private AppointmentStatus appointmentStatus;

    // Records sent to specialist
    @Column(name = "records_sent")
    private Boolean recordsSent;

    @Column(name = "records_sent_date")
    private Instant recordsSentDate;

    // Follow-up/Results
    @Column(name = "results_received")
    private Boolean resultsReceived;

    @Column(name = "results_received_date")
    private Instant resultsReceivedDate;

    @Column(name = "results_summary", columnDefinition = "TEXT")
    private String resultsSummary;

    @Column(name = "follow_up_completed")
    private Boolean followUpCompleted;

    @Column(name = "follow_up_date")
    private Instant followUpDate;

    // Coordination notes
    @Column(name = "coordination_notes", columnDefinition = "TEXT")
    private String coordinationNotes;

    // FHIR Resource References
    @Column(name = "service_request_id", length = 128)
    private String serviceRequestId; // FHIR ServiceRequest resource UUID

    @Column(name = "appointment_id", length = 128)
    private String appointmentId; // FHIR Appointment resource UUID (when scheduled)

    // Timestamps
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.requestedAt == null) {
            this.requestedAt = Instant.now();
        }
        this.createdAt = Instant.now();
    }

    /**
     * Referral status throughout its lifecycle
     */
    public enum ReferralStatus {
        PENDING_AUTHORIZATION,  // Awaiting insurance authorization
        AUTHORIZED,            // Insurance approved, ready to schedule
        SCHEDULED,             // Appointment scheduled with specialist
        AWAITING_APPOINTMENT,  // Authorization received, no appointment yet
        COMPLETED,             // Patient attended appointment, follow-up done
        NO_SHOW,              // Patient did not attend scheduled appointment
        CANCELLED,            // Referral cancelled
        CLOSED                // Referral process closed
    }

    /**
     * Referral urgency/priority
     */
    public enum ReferralPriority {
        ROUTINE,    // Standard priority, can wait
        URGENT,     // Should be scheduled within 1-2 weeks
        STAT        // Emergency, schedule immediately
    }

    /**
     * Insurance authorization status
     */
    public enum AuthorizationStatus {
        NOT_REQUIRED,           // No authorization needed
        PENDING,               // Awaiting insurance decision
        APPROVED,              // Authorization granted
        APPROVED_LIMITED,      // Approved with limitations (visit count, etc.)
        DENIED,                // Authorization denied
        APPEAL_PENDING         // Appeal submitted
    }

    /**
     * Appointment status
     */
    public enum AppointmentStatus {
        NOT_SCHEDULED,         // No appointment yet
        SCHEDULED,            // Appointment scheduled
        CONFIRMED,            // Patient confirmed attendance
        ATTENDED,             // Patient attended
        CANCELLED,            // Appointment cancelled
        NO_SHOW               // Patient did not attend
    }
}
