package com.healthdata.eventsourcing.projection.careplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * CarePlan Projection - CQRS Read Model for Care Coordination
 * Care plan tracking and coordination queries
 */
@Entity
@Table(
    name = "careplan_projections",
    indexes = {
        @Index(name = "idx_careplan_projections_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_careplan_projections_coordinator", columnList = "coordinator_id"),
        @Index(name = "idx_careplan_projections_status", columnList = "status"),
        @Index(name = "idx_careplan_projections_tenant_coordinator", columnList = "tenant_id, coordinator_id"),
        @Index(name = "idx_careplan_projections_tenant_patient_status", columnList = "tenant_id, patient_id, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarePlanProjection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "status")
    private String status;

    @Column(name = "coordinator_id")
    private String coordinatorId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "goal_count")
    private Integer goalCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
