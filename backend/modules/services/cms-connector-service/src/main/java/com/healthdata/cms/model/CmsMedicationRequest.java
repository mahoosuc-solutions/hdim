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
 * CMS Medication Request Entity
 *
 * Represents a medication prescription from CMS DPC Part D data.
 * Maps to FHIR R4 MedicationRequest resource.
 */
@Entity
@Table(name = "cms_medication_requests", indexes = {
    @Index(name = "idx_cms_med_requests_tenant", columnList = "tenant_id"),
    @Index(name = "idx_cms_med_requests_patient", columnList = "patient_id"),
    @Index(name = "idx_cms_med_requests_code", columnList = "medication_code_system, medication_code_value")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsMedicationRequest {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "medication_request_id")
    private String medicationRequestId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "data_source")
    private String dataSource;

    // Medication code (RxNorm, NDC)
    @Column(name = "medication_code_system")
    private String medicationCodeSystem;

    @Column(name = "medication_code_value")
    private String medicationCodeValue;

    @Column(name = "medication_code_display")
    private String medicationCodeDisplay;

    // Status: active, on-hold, cancelled, completed, entered-in-error, stopped, draft, unknown
    @Column(name = "status")
    private String status;

    // Intent: proposal, plan, order, original-order, reflex-order, filler-order, instance-order, option
    @Column(name = "intent")
    private String intent;

    // Category: inpatient, outpatient, community, discharge
    @Column(name = "category")
    private String category;

    // Priority: routine, urgent, asap, stat
    @Column(name = "priority")
    private String priority;

    // Date when prescription was written
    @Column(name = "authored_on")
    private LocalDate authoredOn;

    // Requester (prescriber)
    @Column(name = "requester_reference")
    private String requesterReference;

    // Reason for prescription
    @Column(name = "reason_code")
    private String reasonCode;

    // Dosage instructions (text)
    @Column(name = "dosage_instruction", columnDefinition = "TEXT")
    private String dosageInstruction;

    // Quantity dispensed
    @Column(name = "dispense_quantity")
    private Double dispenseQuantity;

    @Column(name = "dispense_quantity_unit")
    private String dispenseQuantityUnit;

    // Number of refills
    @Column(name = "number_of_refills")
    private Integer numberOfRefills;

    // Days supply
    @Column(name = "days_supply")
    private Integer daysSupply;

    // Substitution allowed
    @Column(name = "substitution_allowed")
    private Boolean substitutionAllowed;

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
