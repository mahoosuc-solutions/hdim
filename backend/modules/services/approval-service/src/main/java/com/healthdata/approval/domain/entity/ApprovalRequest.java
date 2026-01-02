package com.healthdata.approval.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Human-in-the-Loop approval request entity.
 * Captures requests that require human review before execution.
 */
@Entity
@Table(name = "approval_requests", indexes = {
    @Index(name = "idx_approval_tenant_status", columnList = "tenant_id, status, created_at"),
    @Index(name = "idx_approval_assigned", columnList = "assigned_to, status"),
    @Index(name = "idx_approval_type", columnList = "request_type, status"),
    @Index(name = "idx_approval_expires", columnList = "expires_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    // What needs approval
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private RequestType requestType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 255)
    private String entityId;

    // Request details
    @Column(name = "action_requested", nullable = false, length = 100)
    private String actionRequested;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    // Requester info
    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    // Assignment
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "assigned_role", length = 100)
    private String assignedRole;

    // Status tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApprovalStatus status;

    // Decision
    @Column(name = "decision_by", length = 100)
    private String decisionBy;

    @Column(name = "decision_at")
    private Instant decisionAt;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    // Escalation
    @Column(name = "escalation_count")
    @Builder.Default
    private Integer escalationCount = 0;

    @Column(name = "escalated_to", length = 100)
    private String escalatedTo;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    // Expiration
    @Column(name = "expires_at")
    private Instant expiresAt;

    // Audit
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Relationships
    @OneToMany(mappedBy = "approvalRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApprovalHistory> history = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        requestedAt = now;
        if (status == null) {
            status = ApprovalStatus.PENDING;
        }
        if (payload == null) {
            payload = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Approve this request.
     */
    public void approve(String approvedBy, String reason) {
        this.status = ApprovalStatus.APPROVED;
        this.decisionBy = approvedBy;
        this.decisionAt = Instant.now();
        this.decisionReason = reason;
    }

    /**
     * Reject this request.
     */
    public void reject(String rejectedBy, String reason) {
        this.status = ApprovalStatus.REJECTED;
        this.decisionBy = rejectedBy;
        this.decisionAt = Instant.now();
        this.decisionReason = reason;
    }

    /**
     * Escalate this request to another reviewer.
     */
    public void escalate(String escalatedTo, String reason) {
        this.status = ApprovalStatus.ESCALATED;
        this.escalatedTo = escalatedTo;
        this.escalatedAt = Instant.now();
        this.escalationCount = (this.escalationCount == null ? 0 : this.escalationCount) + 1;
        this.decisionReason = reason;
    }

    /**
     * Check if this request has expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Assign this request to a reviewer.
     */
    public void assign(String assignedTo) {
        this.assignedTo = assignedTo;
        this.assignedAt = Instant.now();
        this.status = ApprovalStatus.ASSIGNED;
    }

    public enum RequestType {
        AGENT_ACTION,       // AI agent tool execution
        GUARDRAIL_REVIEW,   // Flagged guardrail content
        DATA_MUTATION,      // FHIR data create/update/delete
        EXPORT,             // Bulk data export
        WORKFLOW_DEPLOY,    // n8n workflow deployment
        DLQ_REPROCESS,      // Dead letter queue reprocessing
        CONSENT_CHANGE,     // Consent modification
        EMERGENCY_ACCESS    // Break-glass access review
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ApprovalStatus {
        PENDING,    // Awaiting assignment
        ASSIGNED,   // Assigned to reviewer
        APPROVED,   // Approved by reviewer
        REJECTED,   // Rejected by reviewer
        EXPIRED,    // Timed out without decision
        ESCALATED   // Escalated to higher authority
    }
}
