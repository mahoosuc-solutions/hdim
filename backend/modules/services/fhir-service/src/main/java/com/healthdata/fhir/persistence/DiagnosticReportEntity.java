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
 * JPA Entity for FHIR DiagnosticReport resource.
 * Stores lab reports, imaging reports, and other diagnostic test results.
 *
 * Uses JSONB for the full FHIR resource with denormalized fields for efficient querying.
 */
@Entity
@Table(name = "diagnostic_reports", indexes = {
    @Index(name = "idx_diagrpt_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_diagrpt_tenant_encounter", columnList = "tenant_id, encounter_id"),
    @Index(name = "idx_diagrpt_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_diagrpt_tenant_code", columnList = "tenant_id, code"),
    @Index(name = "idx_diagrpt_tenant_category", columnList = "tenant_id, category_code"),
    @Index(name = "idx_diagrpt_effective_date", columnList = "tenant_id, effective_datetime"),
    @Index(name = "idx_diagrpt_issued_date", columnList = "tenant_id, issued_datetime")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticReportEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /**
     * Full FHIR DiagnosticReport resource as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    /**
     * Reference to the patient this report is about
     */
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /**
     * Reference to the encounter context (optional)
     */
    @Column(name = "encounter_id")
    private UUID encounterId;

    /**
     * Report status: registered | partial | preliminary | final | amended | corrected | appended | cancelled | entered-in-error | unknown
     */
    @Column(name = "status", length = 32)
    private String status;

    /**
     * Report type code (LOINC)
     */
    @Column(name = "code", length = 64)
    private String code;

    /**
     * Code system (typically http://loinc.org)
     */
    @Column(name = "code_system", length = 128)
    private String codeSystem;

    /**
     * Report type display text
     */
    @Column(name = "code_display", length = 256)
    private String codeDisplay;

    /**
     * Category code: LAB, RAD, PATH, CARD, etc.
     */
    @Column(name = "category_code", length = 64)
    private String categoryCode;

    /**
     * Category display text
     */
    @Column(name = "category_display", length = 256)
    private String categoryDisplay;

    /**
     * When the diagnostic procedure was performed
     */
    @Column(name = "effective_datetime")
    private Instant effectiveDatetime;

    /**
     * When the report was issued/released
     */
    @Column(name = "issued_datetime")
    private Instant issuedDatetime;

    /**
     * Reference to the performing organization/practitioner
     */
    @Column(name = "performer_reference", length = 128)
    private String performerReference;

    /**
     * Report conclusion/interpretation
     */
    @Column(name = "conclusion", columnDefinition = "TEXT")
    private String conclusion;

    /**
     * Conclusion codes (comma-separated for multiple)
     */
    @Column(name = "conclusion_codes", length = 512)
    private String conclusionCodes;

    /**
     * Number of results/observations in this report
     */
    @Column(name = "result_count")
    private Integer resultCount;

    /**
     * Reference to the service request that prompted this report
     */
    @Column(name = "based_on_reference", length = 128)
    private String basedOnReference;

    /**
     * Specimen references (comma-separated)
     */
    @Column(name = "specimen_references", length = 512)
    private String specimenReferences;

    /**
     * Presented form (attachment URLs)
     */
    @Column(name = "presented_form_url", length = 512)
    private String presentedFormUrl;

    /**
     * Presented form content type
     */
    @Column(name = "presented_form_content_type", length = 64)
    private String presentedFormContentType;

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
     * Check if report is final
     */
    public boolean isFinal() {
        return "final".equals(status);
    }

    /**
     * Check if report is preliminary
     */
    public boolean isPreliminary() {
        return "preliminary".equals(status) || "partial".equals(status);
    }

    /**
     * Check if this is a lab report
     */
    public boolean isLabReport() {
        return "LAB".equalsIgnoreCase(categoryCode);
    }

    /**
     * Check if this is an imaging report
     */
    public boolean isImagingReport() {
        return "RAD".equalsIgnoreCase(categoryCode) || "imaging".equalsIgnoreCase(categoryCode);
    }
}
