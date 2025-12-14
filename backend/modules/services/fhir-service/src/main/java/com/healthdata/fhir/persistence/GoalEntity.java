package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA Entity for FHIR Goal resource.
 * Stores patient health objectives and targets.
 *
 * Uses JSONB for the full FHIR resource with denormalized fields for efficient querying.
 */
@Entity
@Table(name = "goals", indexes = {
    @Index(name = "idx_goals_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_goals_tenant_lifecycle", columnList = "tenant_id, lifecycle_status"),
    @Index(name = "idx_goals_tenant_achievement", columnList = "tenant_id, achievement_status"),
    @Index(name = "idx_goals_tenant_category", columnList = "tenant_id, category_code"),
    @Index(name = "idx_goals_target_date", columnList = "tenant_id, target_date")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoalEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /**
     * Full FHIR Goal resource as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    /**
     * Reference to the patient this goal is for
     */
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /**
     * Lifecycle status: proposed | planned | accepted | active | on-hold | completed | cancelled | entered-in-error | rejected
     */
    @Column(name = "lifecycle_status", length = 32)
    private String lifecycleStatus;

    /**
     * Achievement status: in-progress | improving | worsening | no-change | achieved | sustaining | not-achieved | no-progress | not-attainable
     */
    @Column(name = "achievement_status", length = 32)
    private String achievementStatus;

    /**
     * Priority: high-priority | medium-priority | low-priority
     */
    @Column(name = "priority_code", length = 32)
    private String priorityCode;

    /**
     * Human readable description of the goal
     */
    @Column(name = "description_text", columnDefinition = "TEXT")
    private String descriptionText;

    /**
     * Category code: dietary | safety | behavioral | nursing | physiotherapy | etc.
     */
    @Column(name = "category_code", length = 64)
    private String categoryCode;

    /**
     * Category display text
     */
    @Column(name = "category_display", length = 256)
    private String categoryDisplay;

    /**
     * When goal started
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * Target date for achieving the goal
     */
    @Column(name = "target_date")
    private LocalDate targetDate;

    /**
     * Date when status last changed
     */
    @Column(name = "status_date")
    private LocalDate statusDate;

    /**
     * Reference to condition(s) this goal addresses
     */
    @Column(name = "addresses_condition_id")
    private UUID addressesConditionId;

    /**
     * Who expressed the goal (patient, practitioner, related person)
     */
    @Column(name = "expressed_by_reference", length = 128)
    private String expressedByReference;

    // Audit fields

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "last_modified_by", length = 128)
    private String lastModifiedBy;

    @Version
    private Integer version;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        lastModifiedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        lastModifiedAt = Instant.now();
    }

    /**
     * Check if goal is currently active
     */
    public boolean isActive() {
        return "active".equals(lifecycleStatus) || "accepted".equals(lifecycleStatus);
    }

    /**
     * Check if goal is achieved
     */
    public boolean isAchieved() {
        return "achieved".equals(achievementStatus);
    }

    /**
     * Check if goal is overdue
     */
    public boolean isOverdue() {
        return targetDate != null &&
               LocalDate.now().isAfter(targetDate) &&
               !"achieved".equals(achievementStatus) &&
               !"completed".equals(lifecycleStatus);
    }
}
