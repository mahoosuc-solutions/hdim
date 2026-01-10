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
 * CMS Procedure Entity
 *
 * Represents a clinical procedure from CMS DPC data.
 * Maps to FHIR R4 Procedure resource.
 */
@Entity
@Table(name = "cms_procedures", indexes = {
    @Index(name = "idx_cms_procedures_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cms_procedures_patient", columnList = "patient_id"),
    @Index(name = "idx_cms_procedures_code", columnList = "code_system, code_value")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsProcedure {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "procedure_id")
    private String procedureId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "data_source")
    private String dataSource;

    // Procedure code (CPT, HCPCS, ICD-10-PCS, SNOMED CT)
    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_value")
    private String codeValue;

    @Column(name = "code_display")
    private String codeDisplay;

    // Status: preparation, in-progress, not-done, on-hold, stopped, completed, entered-in-error, unknown
    @Column(name = "status")
    private String status;

    // Category: surgical-procedure, diagnostic-procedure, etc.
    @Column(name = "category")
    private String category;

    // Performed date
    @Column(name = "performed_date")
    private LocalDate performedDate;

    // Performed date (end for period)
    @Column(name = "performed_date_end")
    private LocalDate performedDateEnd;

    // Location where procedure was performed
    @Column(name = "location")
    private String location;

    // Performer reference (practitioner)
    @Column(name = "performer_reference")
    private String performerReference;

    // Body site
    @Column(name = "body_site")
    private String bodySite;

    // Outcome
    @Column(name = "outcome")
    private String outcome;

    // Reason code (why procedure was performed)
    @Column(name = "reason_code")
    private String reasonCode;

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
