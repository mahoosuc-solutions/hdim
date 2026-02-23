package com.healthdata.nurseworkflow.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Escalation record created from a patient engagement thread.
 */
@Entity
@Table(name = "patient_engagement_escalations", indexes = {
    @Index(name = "idx_eng_esc_tenant_thread", columnList = "tenant_id, thread_id"),
    @Index(name = "idx_eng_esc_status", columnList = "status"),
    @Index(name = "idx_eng_esc_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientEngagementEscalationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private EscalationSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EscalationStatus status;

    @Column(name = "recipient_id", nullable = false, length = 120)
    private String recipientId;

    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;

    @Column(name = "correlation_id", nullable = false, length = 120)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = EscalationStatus.OPEN;
        }
        this.createdAt = Instant.now();
    }

    public enum EscalationSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum EscalationStatus {
        OPEN,
        ACKNOWLEDGED,
        RESOLVED
    }
}
