package com.healthdata.documentation.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * CDA Document Entity
 *
 * Stores CDA (Clinical Document Architecture) and C-CDA documents.
 * Includes raw XML, parsed data, and rendered HTML.
 */
@Entity
@Table(name = "cda_documents",
       indexes = {
           @Index(name = "idx_cda_doc_clinical", columnList = "clinical_document_id"),
           @Index(name = "idx_cda_doc_tenant", columnList = "tenant_id"),
           @Index(name = "idx_cda_doc_type", columnList = "cda_type"),
           @Index(name = "idx_cda_doc_validation", columnList = "validation_status")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdaDocumentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "clinical_document_id")
    private UUID clinicalDocumentId; // Reference to parent ClinicalDocumentEntity

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "cda_type", length = 50)
    private String cdaType; // CCD, C-CDA, CCDA-R2, etc.

    @Column(name = "template_id", length = 100)
    private String templateId; // e.g., "2.16.840.1.113883.10.20.22.1.1"

    @Column(name = "raw_xml", nullable = false, columnDefinition = "TEXT")
    private String rawXml;

    @Type(JsonBinaryType.class)
    @Column(name = "parsed_data", columnDefinition = "jsonb")
    private Map<String, Object> parsedData; // Structured data extracted from CDA

    @Column(name = "rendered_html", columnDefinition = "TEXT")
    private String renderedHtml; // Human-readable HTML rendering

    @Column(name = "validation_status", length = 50)
    private String validationStatus; // VALID, INVALID, WARNINGS, NOT_VALIDATED

    @Type(JsonBinaryType.class)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private Map<String, Object> validationErrors;

    @Column(name = "document_id", length = 100)
    private String documentId; // CDA document ID

    @Column(name = "set_id", length = 100)
    private String setId; // CDA set ID for versioning

    @Column(name = "version_number")
    private Integer versionNumber; // CDA version number

    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        if (validationStatus == null) {
            validationStatus = "NOT_VALIDATED";
        }
    }

    public boolean isValid() {
        return "VALID".equals(validationStatus);
    }

    public boolean hasWarnings() {
        return "WARNINGS".equals(validationStatus);
    }
}
