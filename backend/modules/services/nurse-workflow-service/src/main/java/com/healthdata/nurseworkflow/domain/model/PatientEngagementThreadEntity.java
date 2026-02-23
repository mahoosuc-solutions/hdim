package com.healthdata.nurseworkflow.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Asynchronous patient-clinician engagement thread.
 * Serves as the system of record for secure communication state.
 */
@Entity
@Table(name = "patient_engagement_threads", indexes = {
    @Index(name = "idx_eng_thread_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_eng_thread_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_eng_thread_last_message", columnList = "last_message_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientEngagementThreadEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "subject", nullable = false, length = 300)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    private ThreadPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ThreadStatus status;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "assigned_clinician_id", length = 120)
    private String assignedClinicianId;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.priority == null) {
            this.priority = ThreadPriority.MEDIUM;
        }
        if (this.status == null) {
            this.status = ThreadStatus.OPEN;
        }
        if (this.lastMessageAt == null) {
            this.lastMessageAt = now;
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum ThreadPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    public enum ThreadStatus {
        OPEN,
        PENDING_CLINICIAN,
        PENDING_PATIENT,
        CLOSED
    }
}
