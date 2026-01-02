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
 * JPA Entity for FHIR DocumentReference resource.
 * Stores references to clinical documents (discharge summaries, notes, reports, etc.).
 *
 * Uses JSONB for the full FHIR resource with denormalized fields for efficient querying.
 */
@Entity
@Table(name = "document_references", indexes = {
    @Index(name = "idx_docref_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_docref_tenant_encounter", columnList = "tenant_id, encounter_id"),
    @Index(name = "idx_docref_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_docref_tenant_type", columnList = "tenant_id, type_code"),
    @Index(name = "idx_docref_tenant_category", columnList = "tenant_id, category_code"),
    @Index(name = "idx_docref_created_date", columnList = "tenant_id, created_date")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReferenceEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /**
     * Full FHIR DocumentReference resource as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    /**
     * Reference to the patient this document is about
     */
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /**
     * Reference to the encounter context (optional)
     */
    @Column(name = "encounter_id")
    private UUID encounterId;

    /**
     * Document status: current | superseded | entered-in-error
     */
    @Column(name = "status", length = 32)
    private String status;

    /**
     * Document composition status: preliminary | final | amended | entered-in-error
     */
    @Column(name = "doc_status", length = 32)
    private String docStatus;

    /**
     * Document type code (LOINC)
     */
    @Column(name = "type_code", length = 64)
    private String typeCode;

    /**
     * Document type code system
     */
    @Column(name = "type_system", length = 128)
    private String typeSystem;

    /**
     * Document type display text
     */
    @Column(name = "type_display", length = 256)
    private String typeDisplay;

    /**
     * Category code (clinical category)
     */
    @Column(name = "category_code", length = 64)
    private String categoryCode;

    /**
     * Category display text
     */
    @Column(name = "category_display", length = 256)
    private String categoryDisplay;

    /**
     * Document creation date (when clinically relevant)
     */
    @Column(name = "created_date")
    private Instant createdDate;

    /**
     * When indexed in the system
     */
    @Column(name = "indexed_date")
    private Instant indexedDate;

    /**
     * Human-readable description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Security label codes (comma-separated)
     */
    @Column(name = "security_labels", length = 256)
    private String securityLabels;

    /**
     * Content MIME type
     */
    @Column(name = "content_type", length = 64)
    private String contentType;

    /**
     * Content URL (attachment reference)
     */
    @Column(name = "content_url", length = 512)
    private String contentUrl;

    /**
     * Content size in bytes
     */
    @Column(name = "content_size")
    private Long contentSize;

    /**
     * Content hash for integrity verification
     */
    @Column(name = "content_hash", length = 128)
    private String contentHash;

    /**
     * Author reference
     */
    @Column(name = "author_reference", length = 128)
    private String authorReference;

    /**
     * Custodian organization reference
     */
    @Column(name = "custodian_reference", length = 128)
    private String custodianReference;

    /**
     * Related document references (for replacements, transforms)
     */
    @Column(name = "relates_to_code", length = 32)
    private String relatesToCode;

    @Column(name = "relates_to_target", length = 128)
    private String relatesToTarget;

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
        if (indexedDate == null) {
            indexedDate = now;
        }
        lastModifiedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        lastModifiedAt = Instant.now();
    }

    /**
     * Check if document is current/active
     */
    public boolean isCurrent() {
        return "current".equals(status);
    }

    /**
     * Check if document is final
     */
    public boolean isFinal() {
        return "final".equals(docStatus);
    }
}
