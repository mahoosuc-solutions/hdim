package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Care Team Member Entity
 *
 * Represents a member of a patient's care team.
 * Used to determine who should receive notifications about a patient.
 */
@Entity
@Table(name = "care_team_members", indexes = {
    @Index(name = "idx_care_team_patient", columnList = "patient_id"),
    @Index(name = "idx_care_team_user", columnList = "user_id"),
    @Index(name = "idx_care_team_tenant", columnList = "tenant_id"),
    @Index(name = "idx_care_team_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTeamMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private CareTeamRole role;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startDate == null) {
            startDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CareTeamRole {
        PRIMARY_CARE_PHYSICIAN,
        SPECIALIST,
        NURSE_PRACTITIONER,
        REGISTERED_NURSE,
        CASE_MANAGER,
        CARE_COORDINATOR,
        SOCIAL_WORKER,
        MENTAL_HEALTH_COUNSELOR,
        PHARMACIST,
        OTHER
    }

    /**
     * Check if care team member is currently active
     */
    public boolean isCurrentlyActive() {
        if (!Boolean.TRUE.equals(active)) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (startDate != null && startDate.isAfter(now)) {
            return false;
        }

        if (endDate != null && endDate.isBefore(now)) {
            return false;
        }

        return true;
    }
}
