package com.healthdata.clinicalworkflowevent.projection;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Workflow Projection Entity (CQRS Read Model)
 *
 * Denormalized clinical workflow view optimized for fast queries.
 * Updated through Kafka event stream.
 */
@Entity
@Table(name = "workflow_projections", indexes = {
    @Index(name = "idx_wp_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_wp_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_wp_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_wp_tenant_assigned", columnList = "tenant_id, assigned_to"),
    @Index(name = "idx_wp_updated_at", columnList = "last_updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowProjection {

    @Id
    @Column(name = "id", length = 255)
    private String id;  // Composite key: tenant_id + workflow_id

    // Tenant Isolation (CRITICAL)
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // Workflow Identifiers
    @Column(name = "workflow_id", nullable = false, length = 100)
    private UUID workflowId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    // Workflow Details
    @Column(name = "workflow_type", nullable = false, length = 100)
    private String workflowType;  // e.g., APPOINTMENT_SCHEDULING, MEDICATION_REVIEW, etc.

    @Column(name = "status", nullable = false, length = 50)
    private String status;  // PENDING, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "priority", length = 50)
    private String priority;  // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Assignment Information
    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "previous_assignee", length = 255)
    private String previousAssignee;

    // Timing Information
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "days_pending", nullable = false)
    @Builder.Default
    private Integer daysPending = 0;

    @Column(name = "is_overdue", nullable = false)
    @Builder.Default
    private Boolean isOverdue = false;

    // Progress Tracking
    @Column(name = "progress_percentage", nullable = false)
    @Builder.Default
    private Integer progressPercentage = 0;

    @Column(name = "steps_completed", nullable = false)
    @Builder.Default
    private Integer stepsCompleted = 0;

    @Column(name = "total_steps", nullable = false)
    @Builder.Default
    private Integer totalSteps = 0;

    // Related Information
    @Column(name = "related_measure_id", length = 100)
    private String relatedMeasureId;

    @Column(name = "related_care_gap_id", length = 100)
    private UUID relatedCareGapId;

    // Flags
    @Column(name = "requires_review", nullable = false)
    @Builder.Default
    private Boolean requiresReview = false;

    @Column(name = "has_blocking_issue", nullable = false)
    @Builder.Default
    private Boolean hasBlockingIssue = false;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "event_version", nullable = false)
    @Builder.Default
    private Long eventVersion = 0L;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastUpdatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}
