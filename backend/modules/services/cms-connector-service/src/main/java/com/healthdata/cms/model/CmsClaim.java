package com.healthdata.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cms_claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmsClaim {
    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "claim_id")
    private String claimId;

    @Column(name = "beneficiary_id")
    private String beneficiaryId;

    @Column(name = "data_source")
    private String dataSource;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "is_processed")
    private Boolean isProcessed;

    @Column(name = "has_validation_errors")
    private Boolean hasValidationErrors;

    @Column(name = "deduplication_status")
    private String deduplicationStatus;

    @Column(name = "matched_claim_id")
    private UUID matchedClaimId;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "total_charge_amount")
    private Double totalChargeAmount;

    @Column(name = "total_allowed_amount")
    private Double totalAllowedAmount;

    @Column(name = "last_validation_at")
    private LocalDateTime lastValidationAt;
}
