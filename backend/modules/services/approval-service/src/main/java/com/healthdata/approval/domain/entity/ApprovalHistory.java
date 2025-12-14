package com.healthdata.approval.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Approval history entity for audit trail.
 * Records all state changes and actions taken on approval requests.
 */
@Entity
@Table(name = "approval_history", indexes = {
    @Index(name = "idx_history_request", columnList = "approval_request_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private HistoryAction action;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Factory method to create a history entry.
     */
    public static ApprovalHistory of(ApprovalRequest request, HistoryAction action, String actor, Map<String, Object> details) {
        return ApprovalHistory.builder()
            .approvalRequest(request)
            .action(action)
            .actor(actor)
            .details(details)
            .createdAt(Instant.now())
            .build();
    }

    public enum HistoryAction {
        CREATED,        // Request created
        ASSIGNED,       // Assigned to reviewer
        REASSIGNED,     // Reassigned to different reviewer
        APPROVED,       // Approved
        REJECTED,       // Rejected
        ESCALATED,      // Escalated to higher authority
        EXPIRED,        // Expired without decision
        VIEWED,         // Viewed by reviewer
        COMMENTED,      // Comment added
        REMINDER_SENT,  // Reminder notification sent
        EXECUTED        // Original action executed after approval
    }
}
