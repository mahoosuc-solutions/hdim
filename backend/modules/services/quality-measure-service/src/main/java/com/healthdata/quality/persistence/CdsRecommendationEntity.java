package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * CDS Recommendation Entity
 * Represents an active recommendation generated from a CDS rule evaluation
 * for a specific patient.
 */
@Entity
@Table(name = "cds_recommendations", indexes = {
    @Index(name = "idx_cds_rec_patient_status", columnList = "patient_id, status"),
    @Index(name = "idx_cds_rec_patient_urgency", columnList = "patient_id, urgency"),
    @Index(name = "idx_cds_rec_patient_category", columnList = "patient_id, category"),
    @Index(name = "idx_cds_rec_rule", columnList = "rule_id"),
    @Index(name = "idx_cds_rec_due_date", columnList = "due_date"),
    @Index(name = "idx_cds_rec_related_care_gap", columnList = "related_care_gap_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsRecommendationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private CdsRuleEntity.CdsCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 20)
    private CdsRuleEntity.CdsUrgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CdsStatus status = CdsStatus.ACTIVE;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 5;

    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;

    @Column(name = "suggested_intervention", columnDefinition = "TEXT")
    private String suggestedIntervention;

    @Column(name = "evidence_source", columnDefinition = "TEXT")
    private String evidenceSource;

    @Column(name = "clinical_guideline", length = 255)
    private String clinicalGuideline;

    @Column(name = "related_care_gap_id")
    private UUID relatedCareGapId;

    @Column(name = "related_measure_id", length = 50)
    private String relatedMeasureId;

    @Column(name = "related_measure_name", length = 255)
    private String relatedMeasureName;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by", length = 255)
    private String acknowledgedBy;

    @Column(name = "acknowledgment_notes", columnDefinition = "TEXT")
    private String acknowledgmentNotes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by", length = 255)
    private String completedBy;

    @Column(name = "completion_outcome", columnDefinition = "TEXT")
    private String completionOutcome;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "declined_by", length = 255)
    private String declinedBy;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "cql_evaluation_result", columnDefinition = "TEXT")
    private String cqlEvaluationResult;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * CDS Recommendation Status
     */
    public enum CdsStatus {
        ACTIVE,         // Recommendation is active and pending action
        ACKNOWLEDGED,   // Provider has acknowledged but not yet acted
        IN_PROGRESS,    // Action is being taken
        COMPLETED,      // Recommendation has been addressed
        DECLINED,       // Provider declined the recommendation
        DISMISSED,      // Recommendation dismissed (no longer applicable)
        EXPIRED         // Recommendation has expired
    }
}
