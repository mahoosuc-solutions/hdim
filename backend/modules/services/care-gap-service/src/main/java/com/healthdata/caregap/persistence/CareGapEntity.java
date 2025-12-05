package com.healthdata.caregap.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Care Gap Entity
 *
 * Represents identified care gaps for quality measures (HEDIS, CMS, etc.).
 * Tracks gap status, recommendations, and closure activities.
 */
@Entity
@Table(name = "care_gaps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareGapEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    // Quality measure information
    @Column(name = "measure_id", nullable = false, length = 100)
    private String measureId;

    @Column(name = "measure_name", nullable = false)
    private String measureName;

    @Column(name = "measure_category", length = 50)
    private String measureCategory; // HEDIS, CMS, custom

    @Column(name = "measure_year")
    private Integer measureYear;

    // Gap details
    @Column(name = "gap_type", nullable = false, length = 100)
    private String gapType; // care-gap, quality-gap, preventive-care, chronic-care

    @Column(name = "gap_status", nullable = false, length = 50)
    private String gapStatus; // open, in-progress, closed, cancelled

    @Column(name = "gap_description", columnDefinition = "TEXT")
    private String gapDescription;

    @Column(name = "gap_reason", columnDefinition = "TEXT")
    private String gapReason;

    // Priority and risk
    @Column(name = "priority", length = 20)
    private String priority; // high, medium, low

    @Column(name = "risk_score")
    private Double riskScore; // 0.0 - 1.0

    // Dates
    @Column(name = "identified_date", nullable = false)
    private LocalDate identifiedDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "closed_date")
    private LocalDate closedDate;

    // Recommendations
    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "recommendation_type", length = 100)
    private String recommendationType; // screening, medication, immunization, procedure

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    // CQL evaluation details
    @Column(name = "cql_library", length = 200)
    private String cqlLibrary;

    @Column(name = "cql_expression", length = 200)
    private String cqlExpression;

    @Column(name = "cql_result", columnDefinition = "JSONB")
    private String cqlResult;

    // FHIR references
    @Column(name = "related_encounter_id")
    private UUID relatedEncounterId;

    @Column(name = "related_condition_id")
    private UUID relatedConditionId;

    @Column(name = "related_procedure_id")
    private UUID relatedProcedureId;

    // Closure information
    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "closure_reason", columnDefinition = "TEXT")
    private String closureReason;

    @Column(name = "closure_action", columnDefinition = "TEXT")
    private String closureAction;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Integer version;

    // Lifecycle hooks
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
