package com.healthdata.caregap.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Care Gap entity representing gaps in patient care
 */
@Entity
@Table(name = "care_gaps", schema = "caregap")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareGap {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "gap_type", nullable = false)
    private String gapType; // PREVENTIVE_CARE, CHRONIC_DISEASE_MONITORING, MEDICATION_ADHERENCE, CANCER_SCREENING

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority", nullable = false)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(name = "status", nullable = false)
    private String status = "OPEN"; // OPEN, IN_PROGRESS, CLOSED

    @Column(name = "measure_id")
    private String measureId; // Associated quality measure ID (e.g., HEDIS-CDC)

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "detected_date")
    private LocalDateTime detectedDate;

    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    @Column(name = "closure_reason")
    private String closureReason;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "care_team_id")
    private String careTeamId;

    @Column(name = "intervention_type")
    private String interventionType; // OUTREACH, APPOINTMENT, MEDICATION_REFILL, LAB_ORDER

    @Column(name = "intervention_notes", columnDefinition = "TEXT")
    private String interventionNotes;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "financial_impact")
    private Double financialImpact;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        detectedDate = LocalDateTime.now();
        if (status == null) {
            status = "OPEN";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}