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
 * Clinical Document Entity
 *
 * Represents a FHIR DocumentReference resource for clinical documents.
 * Stores metadata about clinical documents such as discharge summaries,
 * progress notes, consultation notes, etc.
 */
@Entity
@Table(name = "clinical_documents",
       indexes = {
           @Index(name = "idx_clinical_doc_tenant", columnList = "tenant_id"),
           @Index(name = "idx_clinical_doc_patient", columnList = "patient_id"),
           @Index(name = "idx_clinical_doc_status", columnList = "status"),
           @Index(name = "idx_clinical_doc_type", columnList = "document_type"),
           @Index(name = "idx_clinical_doc_date", columnList = "document_date")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalDocumentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType; // e.g., "Discharge Summary", "Progress Note"

    @Column(name = "document_type_code", length = 50)
    private String documentTypeCode; // LOINC code

    @Column(name = "document_type_system", length = 255)
    private String documentTypeSystem; // e.g., "http://loinc.org"

    @Column(name = "status", nullable = false, length = 50)
    private String status; // current, superseded, entered-in-error

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "author_reference", length = 255)
    private String authorReference; // FHIR reference to Practitioner

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "custodian_reference", length = 255)
    private String custodianReference; // FHIR reference to Organization

    @Column(name = "document_date")
    private LocalDateTime documentDate;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "encounter_reference", length = 255)
    private String encounterReference;

    @Column(name = "facility_reference", length = 255)
    private String facilityReference;

    @Type(JsonBinaryType.class)
    @Column(name = "fhir_resource", columnDefinition = "jsonb")
    private Map<String, Object> fhirResource; // Full FHIR DocumentReference

    @Type(JsonBinaryType.class)
    @Column(name = "category_codes", columnDefinition = "jsonb")
    private Map<String, Object> categoryCodes; // Category coding

    @Type(JsonBinaryType.class)
    @Column(name = "security_labels", columnDefinition = "jsonb")
    private Map<String, Object> securityLabels; // Security/privacy labels

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = "current";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isCurrent() {
        return "current".equals(status);
    }

    public boolean isSuperseded() {
        return "superseded".equals(status);
    }
}
