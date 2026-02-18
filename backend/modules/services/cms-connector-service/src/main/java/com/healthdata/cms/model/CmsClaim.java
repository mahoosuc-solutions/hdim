package com.healthdata.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cms_claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmsClaim {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Column(name = "beneficiary_id", nullable = false)
    private String beneficiaryId;

    // DB: varchar(50) NOT NULL
    @Column(name = "data_source", nullable = false, length = 50)
    private String dataSource;

    @Column(name = "claim_type", length = 100)
    private String claimType;

    @Column(name = "claim_date")
    private LocalDate claimDate;

    @Column(name = "processing_date")
    private LocalDate processingDate;

    // DB: numeric(15,2)
    @Column(name = "total_charge_amount", precision = 15, scale = 2)
    private BigDecimal totalChargeAmount;

    // DB: numeric(15,2)
    @Column(name = "total_allowed_amount", precision = 15, scale = 2)
    private BigDecimal totalAllowedAmount;

    // DB: jsonb NOT NULL — mapped as TEXT in JPA (jsonb stored as string)
    @Column(name = "fhir_resource", nullable = false, columnDefinition = "jsonb")
    private String fhirResource;

    @Column(name = "is_processed")
    private Boolean isProcessed;

    @Column(name = "has_validation_errors")
    private Boolean hasValidationErrors;

    // DB: varchar(50) default 'NEW'
    @Column(name = "deduplication_status", length = 50)
    private String deduplicationStatus;

    @Column(name = "deduplication_confidence", precision = 3, scale = 2)
    private BigDecimal deduplicationConfidence;

    @Column(name = "matched_claim_id")
    private UUID matchedClaimId;

    // DB: varchar(64) NOT NULL
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    // DB: timestamp with time zone NOT NULL
    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "imported_by")
    private String importedBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "validation_run_count")
    private Integer validationRunCount;

    // DB: timestamp with time zone (nullable)
    @Column(name = "last_validation_at")
    private Instant lastValidationAt;
}
