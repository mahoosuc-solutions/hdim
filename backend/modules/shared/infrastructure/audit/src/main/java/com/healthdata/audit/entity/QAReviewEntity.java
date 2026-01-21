package com.healthdata.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * QA Review Entity
 * 
 * Represents a quality assurance review of an AI decision.
 * Stores approval, rejection, or flagging information.
 */
@Entity
@Table(name = "qa_reviews", indexes = {
        @Index(name = "idx_qa_decision_id", columnList = "decision_id"),
        @Index(name = "idx_qa_tenant_status", columnList = "tenant_id, review_status"),
        @Index(name = "idx_qa_reviewed_at", columnList = "reviewed_at"),
        @Index(name = "idx_qa_flag_type", columnList = "flag_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "decision_id", nullable = false, unique = true)
    private String decisionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_status")
    private String reviewStatus; // PENDING, APPROVED, REJECTED, FLAGGED

    @Column(name = "review_outcome")
    private String reviewOutcome; // APPROVED, REJECTED, NEEDS_REVIEW

    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;

    @Column(name = "is_false_positive")
    private Boolean isFalsePositive;

    @Column(name = "is_false_negative")
    private Boolean isFalseNegative;

    @Column(name = "correct_decision_type")
    private String correctDecisionType;

    @Column(name = "flag_type")
    private String flagType; // NEEDS_ESCALATION, DATA_QUALITY_ISSUE, ALGORITHM_ERROR, CLINICAL_REVIEW_NEEDED

    @Column(name = "flag_reason", length = 2000)
    private String flagReason;

    @Column(name = "flag_priority")
    private String flagPriority; // HIGH, MEDIUM, LOW

    @Column(name = "resolution_status")
    private String resolutionStatus; // PENDING, RESOLVED, ESCALATED

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
