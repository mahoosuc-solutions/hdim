package com.healthdata.caregapevent.projection;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Care Gap Projection Entity (CQRS Read Model)
 *
 * Denormalized care gap view optimized for fast queries.
 * Updated through Kafka event stream.
 *
 * FIX: Using preConditions in migration to prevent "relation already exists" error
 */
@Entity
@Table(name = "care_gap_projections", indexes = {
    @Index(name = "idx_cgp_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_cgp_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_cgp_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_cgp_tenant_priority", columnList = "tenant_id, priority"),
    @Index(name = "idx_cgp_updated_at", columnList = "last_updated_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareGapProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tenant Isolation (CRITICAL)
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    // Care Gap Identifiers
    @Column(name = "care_gap_id", nullable = false, length = 100)
    private UUID careGapId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    @Column(name = "measure_id", length = 100)
    private String measureId;

    // Care Gap Details
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 50, nullable = false)
    private String status;  // OPEN, CLOSED, WAIVED

    @Column(name = "priority", length = 50)
    private String priority;  // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "days_overdue", nullable = false)
    @lombok.Builder.Default
    private Integer daysOverdue = 0;

    // Closure Information
    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closure_date")
    private LocalDate closureDate;  // Date gap was closed (from database schema)

    @Column(name = "closed_reason", length = 255)
    private String closedReason;

    @Column(name = "closure_method", length = 50)
    private String closureMethod;  // AUTO_CLOSED, MANUAL_CLOSED, WAIVED

    // Associated Data
    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Column(name = "event_version", nullable = false)
    @lombok.Builder.Default
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
