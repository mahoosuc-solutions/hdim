package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA Entity for FHIR Coverage resource.
 * Stores insurance/coverage information for patients.
 *
 * Uses JSONB for the full FHIR resource with denormalized fields for efficient querying.
 */
@Entity
@Table(name = "coverages", indexes = {
    @Index(name = "idx_coverages_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_coverages_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_coverages_tenant_subscriber", columnList = "tenant_id, subscriber_id"),
    @Index(name = "idx_coverages_tenant_payor", columnList = "tenant_id, payor_reference")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CoverageEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /**
     * Full FHIR Coverage resource as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    /**
     * Reference to the patient (beneficiary) this coverage applies to
     */
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /**
     * Coverage status: active | cancelled | draft | entered-in-error
     */
    @Column(name = "status", length = 32)
    private String status;

    /**
     * Coverage type code (e.g., health, auto, workers-comp)
     */
    @Column(name = "type_code", length = 64)
    private String typeCode;

    /**
     * Coverage type display text
     */
    @Column(name = "type_display", length = 256)
    private String typeDisplay;

    /**
     * Subscriber ID / Member number / Policy number
     */
    @Column(name = "subscriber_id", length = 128)
    private String subscriberId;

    /**
     * Group number for employer-based coverage
     */
    @Column(name = "group_number", length = 64)
    private String groupNumber;

    /**
     * Reference to the insurance organization (payor)
     */
    @Column(name = "payor_reference", length = 128)
    private String payorReference;

    /**
     * Payor organization display name
     */
    @Column(name = "payor_display", length = 256)
    private String payorDisplay;

    /**
     * Coverage period start date
     */
    @Column(name = "period_start")
    private Instant periodStart;

    /**
     * Coverage period end date
     */
    @Column(name = "period_end")
    private Instant periodEnd;

    /**
     * Dependent number (for family coverage)
     */
    @Column(name = "dependent", length = 32)
    private String dependent;

    /**
     * Relationship to subscriber
     */
    @Column(name = "relationship_code", length = 32)
    private String relationshipCode;

    /**
     * Order of coverage (primary, secondary, etc.)
     */
    @Column(name = "coverage_order")
    private Integer coverageOrder;

    /**
     * Network type (in-network, out-of-network)
     */
    @Column(name = "network", length = 128)
    private String network;

    // Audit fields

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "last_modified_by", length = 128)
    private String lastModifiedBy;

    @Version
    private Integer version;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        lastModifiedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        lastModifiedAt = Instant.now();
    }

    /**
     * Check if coverage is currently active based on period
     */
    public boolean isCurrentlyActive() {
        if (!"active".equals(status)) {
            return false;
        }
        Instant now = Instant.now();
        boolean afterStart = periodStart == null || !now.isBefore(periodStart);
        boolean beforeEnd = periodEnd == null || !now.isAfter(periodEnd);
        return afterStart && beforeEnd;
    }
}
