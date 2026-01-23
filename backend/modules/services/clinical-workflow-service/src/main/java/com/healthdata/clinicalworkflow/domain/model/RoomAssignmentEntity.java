package com.healthdata.clinicalworkflow.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Room Assignment Entity
 *
 * Tracks exam room allocation and status:
 * - Room assignment to patients/appointments
 * - Room status (available, occupied, cleaning, reserved, out-of-service)
 * - Timing (assigned, ready, discharged, cleaning completed)
 *
 * Enables room management dashboard for MAs
 * Multi-tenant isolation required
 */
@Entity
@Table(
    name = "room_assignments",
    indexes = {
        @Index(name = "idx_room_assignments_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_room_assignments_room_status", columnList = "tenant_id, room_number, status"),
        @Index(name = "idx_room_assignments_patient_id", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_room_assignments_assigned_at", columnList = "tenant_id, assigned_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id", length = 255)
    private String appointmentId;

    @Column(name = "encounter_id", length = 255)
    private String encounterId;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(name = "location", length = 100, columnDefinition = "VARCHAR(100) COMMENT 'e.g., Building A, Floor 2'")
    private String location;

    @Column(name = "room_type", nullable = false, length = 50, columnDefinition = "VARCHAR(50) COMMENT 'standard, isolation, trauma, etc.'")
    @Builder.Default
    private String roomType = "standard";

    @Column(name = "status", nullable = false, length = 50, columnDefinition = "VARCHAR(50) COMMENT 'available, occupied, cleaning, reserved, out-of-service'")
    private String status;  // available, occupied, cleaning, reserved, out-of-service

    @Column(name = "assigned_by", nullable = false, length = 255)
    private String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "room_ready_at")
    private Instant roomReadyAt;

    @Column(name = "discharged_at")
    private Instant dischargedAt;

    @Column(name = "cleaning_started_at")
    private Instant cleaningStartedAt;

    @Column(name = "cleaning_completed_at")
    private Instant cleaningCompletedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        assignedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Calculate occupancy duration in minutes
     */
    public Integer getOccupancyDurationMinutes() {
        if (assignedAt == null) {
            return null;
        }
        Instant endTime = dischargedAt != null ? dischargedAt : Instant.now();
        return (int) Math.round((endTime.getEpochSecond() - assignedAt.getEpochSecond()) / 60.0);
    }
}
