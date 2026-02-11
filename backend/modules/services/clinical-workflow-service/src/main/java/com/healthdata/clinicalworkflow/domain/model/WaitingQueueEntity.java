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
 * Waiting Queue Entity
 *
 * Manages waiting room queue with priority-based triage:
 * - Queue position and status tracking
 * - Priority levels (urgent, high, normal, low)
 * - Wait time calculation and estimation
 * - Real-time queue updates via WebSocket
 *
 * Core to MA waiting room workflow
 * Multi-tenant isolation required
 */
@Entity
@Table(
    name = "waiting_queue",
    indexes = {
        @Index(name = "idx_waiting_queue_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_waiting_queue_priority", columnList = "tenant_id, priority, queue_position"),
        @Index(name = "idx_waiting_queue_patient_id", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_waiting_queue_entered_at", columnList = "tenant_id, entered_queue_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitingQueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "appointment_id", length = 255)
    private String appointmentId;

    @Column(name = "check_in_id")
    private UUID checkInId;

    @Column(name = "queue_position", nullable = false)
    private Integer queuePosition;

    @Column(name = "priority", nullable = false, length = 50)
    private String priority;  // urgent, high, normal, low

    @Column(name = "status", nullable = false, length = 50)
    private String status;  // waiting, called, in-room, completed, cancelled

    @Column(name = "entered_queue_at", nullable = false)
    private Instant enteredQueueAt;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "exited_queue_at")
    private Instant exitedQueueAt;

    @Column(name = "wait_time_minutes")
    private Integer waitTimeMinutes;

    @Column(name = "estimated_wait_minutes")
    private Integer estimatedWaitMinutes;

    @Column(name = "provider_assigned", length = 255)
    private String providerAssigned;

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
        enteredQueueAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        // Update wait time on each update
        if (enteredQueueAt != null) {
            Instant endTime = exitedQueueAt != null ? exitedQueueAt : Instant.now();
            waitTimeMinutes = (int) Math.round((endTime.getEpochSecond() - enteredQueueAt.getEpochSecond()) / 60.0);
        }
    }

    /**
     * Get current wait time in minutes (if still waiting)
     */
    public Integer getCurrentWaitTimeMinutes() {
        if (enteredQueueAt == null || exitedQueueAt != null) {
            return waitTimeMinutes;
        }
        return (int) Math.round((Instant.now().getEpochSecond() - enteredQueueAt.getEpochSecond()) / 60.0);
    }

    /**
     * Convert priority string to numeric priority for sorting
     */
    public Integer getPriorityLevel() {
        return switch (priority != null ? priority.toLowerCase() : "normal") {
            case "urgent" -> 1;
            case "high" -> 2;
            case "normal" -> 3;
            case "low" -> 4;
            default -> 3;
        };
    }
}
