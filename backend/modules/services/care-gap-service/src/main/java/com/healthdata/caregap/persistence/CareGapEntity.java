package com.healthdata.caregap.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    // Quality measure information
    @Column(name = "measure_id", nullable = false, length = 128)
    private String measureId;

    @Column(name = "measure_name", nullable = false)
    private String measureName;

    @Column(name = "gap_category", length = 50)
    private String gapCategory; // HEDIS, CMS, custom

    @Column(name = "measure_year")
    private Integer measureYear;

    // Gap details
    @Column(name = "gap_type", nullable = false, length = 100)
    private String gapType; // care-gap, quality-gap, preventive-care, chronic-care

    @Column(name = "status", nullable = false, length = 20)
    private String gapStatus; // OPEN, IN_PROGRESS, CLOSED, CANCELLED

    @Column(name = "description", columnDefinition = "TEXT")
    private String gapDescription;

    @Column(name = "gap_reason", columnDefinition = "TEXT")
    private String gapReason;

    // Priority and risk
    @Column(name = "priority", length = 20)
    private String priority; // high, medium, low

    @Column(name = "severity", length = 20)
    private String severity; // high, medium, low

    @Column(name = "star_impact", precision = 3, scale = 2)
    private BigDecimal starImpact;

    @Column(name = "risk_score")
    private Double riskScore; // 0.0 - 1.0

    // Dates
    @Column(name = "identified_date", nullable = false)
    private Instant identifiedDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "closed_date")
    private Instant closedDate;

    @Column(name = "assigned_to_provider_id", length = 64)
    private String assignedToProviderId;

    @Column(name = "assigned_to_care_team_id", length = 64)
    private String assignedToCareTeamId;

    @Column(name = "clinical_data", columnDefinition = "TEXT")
    private String clinicalData;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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

    @JdbcTypeCode(SqlTypes.JSON)
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
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

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
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (identifiedDate == null) {
            identifiedDate = now;
        }
        if (gapStatus == null) {
            gapStatus = "OPEN";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @JsonProperty("status")
    public String getStatus() {
        return gapStatus;
    }

    @JsonProperty("status")
    public void setStatus(String status) {
        this.gapStatus = status;
    }
}
