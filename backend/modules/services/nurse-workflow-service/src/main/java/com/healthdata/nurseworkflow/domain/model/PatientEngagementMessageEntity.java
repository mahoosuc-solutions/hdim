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
 * Message within a patient engagement thread.
 */
@Entity
@Table(name = "patient_engagement_messages", indexes = {
    @Index(name = "idx_eng_msg_tenant_thread", columnList = "tenant_id, thread_id"),
    @Index(name = "idx_eng_msg_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientEngagementMessageEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 30)
    private SenderType senderType;

    @Column(name = "sender_id", nullable = false, length = 120)
    private String senderId;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "contains_phi", nullable = false)
    private Boolean containsPhi;

    @Column(name = "escalation_flag", nullable = false)
    private Boolean escalationFlag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.containsPhi == null) {
            this.containsPhi = Boolean.FALSE;
        }
        if (this.escalationFlag == null) {
            this.escalationFlag = Boolean.FALSE;
        }
        this.createdAt = Instant.now();
    }

    public enum SenderType {
        PATIENT,
        CLINICIAN,
        SYSTEM
    }
}
