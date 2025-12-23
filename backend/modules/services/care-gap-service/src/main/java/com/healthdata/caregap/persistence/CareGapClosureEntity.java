package com.healthdata.caregap.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Care Gap Closure Entity
 *
 * Tracks how a care gap was closed.
 */
@Entity
@Table(name = "care_gap_closures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGapClosureEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "care_gap_id", nullable = false)
    private UUID careGapId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "closure_method", nullable = false, length = 50)
    private String closureMethod;

    @Column(name = "closure_date", nullable = false)
    private Instant closureDate;

    @Column(name = "closed_by_provider_id", nullable = false, length = 64)
    private String closedByProviderId;

    @Column(name = "closed_by_provider_name", length = 255)
    private String closedByProviderName;

    @Column(name = "supporting_evidence_type", length = 50)
    private String supportingEvidenceType;

    @Column(name = "supporting_evidence_id", length = 128)
    private String supportingEvidenceId;

    @Column(name = "service_date")
    private LocalDate serviceDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "documentation_url", length = 512)
    private String documentationUrl;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by", length = 128)
    private String verifiedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (closureDate == null) {
            closureDate = Instant.now();
        }
    }
}
