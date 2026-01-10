package com.healthdata.cms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CMS Observation Entity
 *
 * Represents a clinical observation/lab result from CMS DPC data.
 * Maps to FHIR R4 Observation resource.
 */
@Entity
@Table(name = "cms_observations", indexes = {
    @Index(name = "idx_cms_observations_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cms_observations_patient", columnList = "patient_id"),
    @Index(name = "idx_cms_observations_code", columnList = "code_system, code_value")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsObservation {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "observation_id")
    private String observationId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "data_source")
    private String dataSource;

    // Observation code (LOINC, SNOMED CT)
    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_value")
    private String codeValue;

    @Column(name = "code_display")
    private String codeDisplay;

    // Status: registered, preliminary, final, amended, corrected, cancelled, entered-in-error, unknown
    @Column(name = "status")
    private String status;

    // Category: vital-signs, laboratory, imaging, procedure, survey, exam, therapy, activity
    @Column(name = "category")
    private String category;

    // Effective date/time when observation was made
    @Column(name = "effective_datetime")
    private LocalDateTime effectiveDatetime;

    // Issued date/time (when result was available)
    @Column(name = "issued")
    private LocalDateTime issued;

    // Value (quantity)
    @Column(name = "value_quantity")
    private Double valueQuantity;

    @Column(name = "value_unit")
    private String valueUnit;

    @Column(name = "value_system")
    private String valueSystem;

    @Column(name = "value_code")
    private String valueCode;

    // Value (string) for non-numeric observations
    @Column(name = "value_string")
    private String valueString;

    // Value (codeable concept)
    @Column(name = "value_codeable_concept")
    private String valueCodeableConcept;

    // Reference range (low)
    @Column(name = "reference_range_low")
    private Double referenceRangeLow;

    // Reference range (high)
    @Column(name = "reference_range_high")
    private Double referenceRangeHigh;

    // Interpretation: normal, abnormal, critical, etc.
    @Column(name = "interpretation")
    private String interpretation;

    // Body site
    @Column(name = "body_site")
    private String bodySite;

    // Method used for observation
    @Column(name = "method")
    private String method;

    // Performer reference
    @Column(name = "performer_reference")
    private String performerReference;

    // Note/comment
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

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
