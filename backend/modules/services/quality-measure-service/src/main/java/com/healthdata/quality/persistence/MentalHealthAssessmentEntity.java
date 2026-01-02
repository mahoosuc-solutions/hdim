package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Mental Health Assessment Entity
 *
 * Stores mental health screening results (PHQ-9, GAD-7, PHQ-2, etc.)
 */
@Entity
@Table(name = "mental_health_assessments", indexes = {
    @Index(name = "idx_mha_patient_date", columnList = "patient_id, assessment_date DESC"),
    @Index(name = "idx_mha_patient_type", columnList = "patient_id, type"),
    @Index(name = "idx_mha_positive_screen", columnList = "patient_id, positive_screen")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentalHealthAssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private AssessmentType type;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "interpretation", nullable = false, length = 1000)
    private String interpretation;

    @Column(name = "positive_screen", nullable = false)
    private Boolean positiveScreen;

    @Column(name = "threshold_score", nullable = false)
    private Integer thresholdScore;

    @Column(name = "requires_followup", nullable = false)
    private Boolean requiresFollowup;

    @Column(name = "assessed_by", nullable = false, length = 100)
    private String assessedBy;

    @Column(name = "assessment_date", nullable = false)
    private Instant assessmentDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responses", columnDefinition = "jsonb", nullable = false)
    private Map<String, Integer> responses;

    @Column(name = "clinical_notes", length = 2000)
    private String clinicalNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Assessment Types
     */
    public enum AssessmentType {
        PHQ_9,      // Patient Health Questionnaire-9 (Depression)
        GAD_7,      // Generalized Anxiety Disorder-7
        PHQ_2,      // Brief Depression Screen
        PHQ_A,      // Adolescent Depression
        AUDIT_C,    // Alcohol Use
        DAST_10,    // Drug Abuse Screening
        PCL_5,      // PTSD Checklist
        MDQ,        // Mood Disorder Questionnaire (Bipolar)
        CAGE_AID    // Substance Abuse
    }
}
