package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * CDS Acknowledgment Entity
 * Tracks all acknowledgment/action history for CDS recommendations.
 * Provides an audit trail of all provider interactions with recommendations.
 */
@Entity
@Table(name = "cds_acknowledgments", indexes = {
    @Index(name = "idx_cds_ack_recommendation", columnList = "recommendation_id"),
    @Index(name = "idx_cds_ack_patient", columnList = "patient_id"),
    @Index(name = "idx_cds_ack_user", columnList = "user_id"),
    @Index(name = "idx_cds_ack_action", columnList = "action_type"),
    @Index(name = "idx_cds_ack_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsAcknowledgmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "recommendation_id", nullable = false)
    private UUID recommendationId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Column(name = "user_role", length = 100)
    private String userRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private ActionType actionType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "outcome", columnDefinition = "TEXT")
    private String outcome;

    @Column(name = "follow_up_date")
    private Instant followUpDate;

    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    private String followUpNotes;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", length = 30)
    private String newStatus;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Types of actions that can be taken on a CDS recommendation
     */
    public enum ActionType {
        VIEWED,         // User viewed the recommendation
        ACKNOWLEDGED,   // User acknowledged seeing the recommendation
        ACCEPTED,       // User accepted the recommendation for action
        DECLINED,       // User declined to act on the recommendation
        COMPLETED,      // User marked recommendation as completed
        DEFERRED,       // User deferred action to a later time
        REASSIGNED,     // Recommendation was reassigned to another user
        ESCALATED,      // Recommendation was escalated
        SNOOZED,        // User temporarily dismissed the recommendation
        DISMISSED,      // Recommendation was permanently dismissed
        REOPENED        // A closed recommendation was reopened
    }
}
