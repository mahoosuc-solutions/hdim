package com.healthdata.priorauth.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a Prior Authorization Request.
 *
 * Implements Da Vinci PAS (Prior Authorization Support) specification.
 * Tracks the full lifecycle of PA requests from submission to final determination.
 */
@Entity
@Table(name = "prior_auth_requests", schema = "prior_auth",
    indexes = {
        @Index(name = "idx_pa_tenant_patient", columnList = "tenant_id, patient_id"),
        @Index(name = "idx_pa_status", columnList = "status"),
        @Index(name = "idx_pa_payer", columnList = "payer_id"),
        @Index(name = "idx_pa_request_id", columnList = "pa_request_id"),
        @Index(name = "idx_pa_sla_deadline", columnList = "sla_deadline")
    })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorAuthRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "pa_request_id", unique = true, length = 255)
    private String paRequestId;

    @Column(name = "service_code", length = 128)
    private String serviceCode;

    @Column(name = "service_description", length = 500)
    private String serviceDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", length = 32)
    private Urgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    @Column(name = "payer_id", length = 255)
    private String payerId;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "provider_npi", length = 10)
    private String providerNpi;

    @Column(name = "facility_id", length = 255)
    private String facilityId;

    @Column(name = "diagnosis_codes", length = 500)
    private String diagnosisCodes;

    @Column(name = "procedure_codes", length = 500)
    private String procedureCodes;

    @Column(name = "quantity_requested")
    private Integer quantityRequested;

    @Column(name = "quantity_approved")
    private Integer quantityApproved;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "claim_bundle_json", columnDefinition = "jsonb")
    private Map<String, Object> claimBundleJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "claim_response_json", columnDefinition = "jsonb")
    private Map<String, Object> claimResponseJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supporting_info_json", columnDefinition = "jsonb")
    private Map<String, Object> supportingInfoJson;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "auth_number", length = 100)
    private String authNumber;

    @Column(name = "auth_effective_date")
    private LocalDateTime authEffectiveDate;

    @Column(name = "auth_expiration_date")
    private LocalDateTime authExpirationDate;

    @Column(name = "payer_tracking_id", length = 255)
    private String payerTrackingId;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "requested_by", length = 255)
    private String requestedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Urgency {
        STAT,       // 72 hours SLA
        ROUTINE     // 7 days SLA
    }

    public enum Status {
        DRAFT,
        PENDING_SUBMISSION,
        SUBMITTED,
        PENDING_REVIEW,
        INFO_REQUESTED,
        APPROVED,
        PARTIALLY_APPROVED,
        DENIED,
        CANCELLED,
        EXPIRED,
        ERROR
    }

    /**
     * Calculate SLA deadline based on urgency.
     */
    public void calculateSlaDeadline() {
        if (submittedAt != null && urgency != null) {
            this.slaDeadline = switch (urgency) {
                case STAT -> submittedAt.plusHours(72);
                case ROUTINE -> submittedAt.plusDays(7);
            };
        }
    }

    /**
     * Check if SLA is breached.
     */
    public boolean isSlaBreached() {
        if (slaDeadline == null) return false;
        if (status == Status.APPROVED || status == Status.DENIED ||
            status == Status.PARTIALLY_APPROVED || status == Status.CANCELLED) {
            return decisionAt != null && decisionAt.isAfter(slaDeadline);
        }
        return LocalDateTime.now().isAfter(slaDeadline);
    }
}
