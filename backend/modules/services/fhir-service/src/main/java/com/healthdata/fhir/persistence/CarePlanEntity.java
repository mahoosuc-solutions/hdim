package com.healthdata.fhir.persistence;

import java.time.Instant;
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
 * JPA Entity for FHIR CarePlan resource.
 * Stores comprehensive care coordination plans for patients.
 *
 * Uses JSONB for the full FHIR resource with denormalized fields for efficient querying.
 */
@Entity
@Table(name = "care_plans", indexes = {
    @Index(name = "idx_careplan_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_careplan_tenant_encounter", columnList = "tenant_id, encounter_id"),
    @Index(name = "idx_careplan_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_careplan_tenant_intent", columnList = "tenant_id, intent"),
    @Index(name = "idx_careplan_tenant_category", columnList = "tenant_id, category_code"),
    @Index(name = "idx_careplan_period", columnList = "tenant_id, period_start, period_end")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /**
     * Full FHIR CarePlan resource as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    /**
     * Reference to the patient this care plan is for
     */
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /**
     * Reference to the encounter context (optional)
     */
    @Column(name = "encounter_id")
    private UUID encounterId;

    /**
     * Care plan status: draft | active | on-hold | revoked | completed | entered-in-error | unknown
     */
    @Column(name = "status", length = 32)
    private String status;

    /**
     * Care plan intent: proposal | plan | order | option
     */
    @Column(name = "intent", length = 32)
    private String intent;

    /**
     * Care plan title
     */
    @Column(name = "title", length = 256)
    private String title;

    /**
     * Care plan description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Category code (e.g., assess-plan, longitudinal, encounter-diagnosis)
     */
    @Column(name = "category_code", length = 64)
    private String categoryCode;

    /**
     * Category display text
     */
    @Column(name = "category_display", length = 256)
    private String categoryDisplay;

    /**
     * Care plan period start date
     */
    @Column(name = "period_start")
    private Instant periodStart;

    /**
     * Care plan period end date
     */
    @Column(name = "period_end")
    private Instant periodEnd;

    /**
     * When care plan was created
     */
    @Column(name = "created_date")
    private Instant createdDate;

    /**
     * Primary author reference
     */
    @Column(name = "author_reference", length = 128)
    private String authorReference;

    /**
     * Care team references (comma-separated)
     */
    @Column(name = "care_team_references", length = 512)
    private String careTeamReferences;

    /**
     * Number of activities in the care plan
     */
    @Column(name = "activity_count")
    private Integer activityCount;

    /**
     * Goal references (comma-separated)
     */
    @Column(name = "goal_references", length = 512)
    private String goalReferences;

    /**
     * Addresses condition references (comma-separated)
     */
    @Column(name = "addresses_references", length = 512)
    private String addressesReferences;

    /**
     * Supporting information references (comma-separated)
     */
    @Column(name = "supporting_info_references", length = 512)
    private String supportingInfoReferences;

    /**
     * Replaces care plan reference
     */
    @Column(name = "replaces_reference", length = 128)
    private String replacesReference;

    /**
     * Part of care plan reference (parent plan)
     */
    @Column(name = "part_of_reference", length = 128)
    private String partOfReference;

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
     * Check if care plan is currently active
     */
    public boolean isActive() {
        return "active".equals(status);
    }

    /**
     * Check if care plan is completed
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * Check if care plan is currently in effect based on period
     */
    public boolean isCurrentlyInEffect() {
        if (!isActive()) {
            return false;
        }
        Instant now = Instant.now();
        boolean afterStart = periodStart == null || !now.isBefore(periodStart);
        boolean beforeEnd = periodEnd == null || !now.isAfter(periodEnd);
        return afterStart && beforeEnd;
    }

    /**
     * Check if this is a primary care plan (not part of another)
     */
    public boolean isPrimary() {
        return partOfReference == null || partOfReference.isEmpty();
    }
}
