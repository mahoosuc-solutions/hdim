package com.healthdata.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CMS Condition Entity
 *
 * Represents a clinical condition/diagnosis from CMS DPC data.
 * Maps to FHIR R4 Condition resource.
 */
@Entity
@Table(name = "cms_conditions", indexes = {
    @Index(name = "idx_cms_conditions_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cms_conditions_patient", columnList = "patient_id"),
    @Index(name = "idx_cms_conditions_code", columnList = "code_system, code_value")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsCondition {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "condition_id")
    private String conditionId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "data_source")
    private String dataSource;

    // Condition code (ICD-10, SNOMED CT, etc.)
    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_value")
    private String codeValue;

    @Column(name = "code_display")
    private String codeDisplay;

    // Clinical status: active, recurrence, relapse, inactive, remission, resolved
    @Column(name = "clinical_status")
    private String clinicalStatus;

    // Verification status: unconfirmed, provisional, differential, confirmed, refuted, entered-in-error
    @Column(name = "verification_status")
    private String verificationStatus;

    // Category: problem-list-item, encounter-diagnosis, health-concern
    @Column(name = "category")
    private String category;

    // Severity: severe, moderate, mild
    @Column(name = "severity")
    private String severity;

    // Onset date (when the condition started)
    @Column(name = "onset_date")
    private LocalDate onsetDate;

    // Abatement date (when the condition resolved/ended)
    @Column(name = "abatement_date")
    private LocalDate abatementDate;

    // Recorded date (when the condition was documented)
    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    // Body site
    @Column(name = "body_site")
    private String bodySite;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "is_processed")
    private Boolean isProcessed;

    @Column(name = "has_validation_errors")
    private Boolean hasValidationErrors;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (importedAt == null) {
            importedAt = LocalDateTime.now();
        }
        if (isProcessed == null) {
            isProcessed = false;
        }
        if (hasValidationErrors == null) {
            hasValidationErrors = false;
        }
    }
}
