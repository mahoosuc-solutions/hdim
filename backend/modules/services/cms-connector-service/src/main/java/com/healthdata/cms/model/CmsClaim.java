package com.healthdata.cms.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CMS Claim Entity
 * 
 * Represents a Medicare claim imported from CMS APIs (BCDA, DPC, AB2D).
 * Stores the raw FHIR ExplanationOfBenefit resource and metadata.
 */
@Entity
@Table(name = "cms_claims", indexes = {
    @Index(name = "idx_claim_id", columnList = "claim_id", unique = true),
    @Index(name = "idx_beneficiary_id", columnList = "beneficiary_id"),
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_imported_at", columnList = "imported_at"),
    @Index(name = "idx_data_source", columnList = "data_source")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsClaim {

    @Id
    private UUID id;

    // Beneficiary identification
    @Column(nullable = false)
    private String beneficiaryId;

    // Unique claim identifier from CMS
    @Column(nullable = false, unique = true)
    private String claimId;

    // Data source (BCDA, DPC, AB2D)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimSource dataSource;

    // Raw FHIR ExplanationOfBenefit resource
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode fhirResource;

    // Claim metadata
    @Column(nullable = false)
    private LocalDateTime claimDate;

    @Column
    private LocalDateTime serviceStartDate;

    @Column
    private LocalDateTime serviceEndDate;

    // Claim type (Part A, Part B, Part D)
    @Enumerated(EnumType.STRING)
    @Column
    private ClaimType claimType;

    // Claim amount
    @Column
    private Double claimAmount;

    @Column
    private Double allowedAmount;

    @Column
    private Double paidAmount;

    // Import metadata
    @Column(nullable = false)
    private LocalDateTime importedAt;

    @Column
    private LocalDateTime lastUpdated;

    // Data lineage
    @Column
    private String importBatchId;

    @Column
    private String importJobId;

    // Multi-tenancy
    @Column(nullable = false)
    private UUID tenantId;

    // Hash for deduplication
    @Column
    private String contentHash;

    // Data quality flags
    @Column
    private Boolean hasValidationErrors;

    @Column
    private String validationErrors;

    @Column
    private Boolean isProcessed;

    /**
     * Claim source enumeration
     */
    public enum ClaimSource {
        BCDA("Beneficiary Claims Data API"),
        DPC("Data at Point of Care"),
        AB2D("Medicare Part D Claims API"),
        BLUE_BUTTON("Blue Button 2.0");

        private final String displayName;

        ClaimSource(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Medicare claim type
     */
    public enum ClaimType {
        PART_A("Medicare Part A"),
        PART_B("Medicare Part B"),
        PART_D("Medicare Part D"),
        INPATIENT("Inpatient Hospital"),
        OUTPATIENT("Outpatient"),
        CARRIER("Carrier/Professional"),
        PHARMACY("Pharmacy");

        private final String displayName;

        ClaimType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
