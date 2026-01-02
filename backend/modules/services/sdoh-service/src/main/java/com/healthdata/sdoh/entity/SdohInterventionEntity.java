package com.healthdata.sdoh.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking SDOH interventions and referrals.
 *
 * When a patient screens positive for an HRSN domain, interventions
 * should be documented. This entity tracks the intervention workflow
 * from referral to completion.
 *
 * Used for:
 * - SDOH-2 care gap closure
 * - Quality improvement tracking
 * - Community resource utilization
 */
@Entity
@Table(name = "sdoh_interventions", schema = "sdoh",
    indexes = {
        @Index(name = "idx_sdoh_intervention_session", columnList = "screening_session_id"),
        @Index(name = "idx_sdoh_intervention_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_sdoh_intervention_status", columnList = "tenant_id, status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SdohInterventionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "screening_session_id")
    private UUID screeningSessionId;

    @Column(name = "domain", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private HrsnDomain domain;

    @Column(name = "intervention_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private InterventionType interventionType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "resource_name", length = 255)
    private String resourceName;

    @Column(name = "resource_organization", length = 255)
    private String resourceOrganization;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private InterventionStatus status;

    @Column(name = "referred_by", length = 100)
    private String referredBy;

    @Column(name = "referred_at")
    private LocalDateTime referredAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "outcome", length = 100)
    private String outcome;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (referredAt == null) {
            referredAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * HRSN domains that can have interventions.
     */
    public enum HrsnDomain {
        FOOD_INSECURITY,
        HOUSING_INSTABILITY,
        TRANSPORTATION,
        UTILITIES,
        INTERPERSONAL_SAFETY
    }

    /**
     * Types of SDOH interventions.
     */
    public enum InterventionType {
        COMMUNITY_REFERRAL,     // Referral to community organization
        CASE_MANAGEMENT,        // Assigned to case manager
        SOCIAL_WORK,            // Social work consult
        RESOURCE_PROVIDED,      // Direct resource provision (e.g., food box)
        INFORMATION_PROVIDED,   // Educational materials provided
        NAVIGATION_ASSISTANCE,  // Help navigating benefits/services
        FOLLOW_UP_SCHEDULED     // Follow-up visit scheduled
    }

    /**
     * Intervention workflow status.
     */
    public enum InterventionStatus {
        PENDING,            // Referral created, not yet processed
        IN_PROGRESS,        // Being worked on
        SCHEDULED,          // Appointment scheduled
        COMPLETED,          // Successfully completed
        DECLINED,           // Patient declined intervention
        CANCELLED,          // Cancelled by provider/patient
        FAILED              // Intervention attempt failed
    }
}
