package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Risk Assessment Entity
 * Stores risk stratification results for patients
 */
@Entity
@Table(name = "risk_assessments", indexes = {
    @Index(name = "idx_ra_patient_date", columnList = "patient_id, assessment_date DESC"),
    @Index(name = "idx_ra_risk_level", columnList = "patient_id, risk_level"),
    @Index(name = "idx_ra_patient_category", columnList = "patient_id, risk_category, assessment_date DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private UUID patientId;

    @Column(name = "risk_category", length = 50)
    private String riskCategory; // CARDIOVASCULAR, DIABETES, RESPIRATORY, MENTAL_HEALTH

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "chronic_condition_count")
    private Integer chronicConditionCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_factors", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> riskFactors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "predicted_outcomes", columnDefinition = "jsonb")
    private List<Map<String, Object>> predictedOutcomes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", columnDefinition = "jsonb")
    private List<String> recommendations;

    @Column(name = "assessment_date", nullable = false)
    private Instant assessmentDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (assessmentDate == null) {
            assessmentDate = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Risk Levels
     */
    public enum RiskLevel {
        LOW,        // 0-24
        MODERATE,   // 25-49
        HIGH,       // 50-74
        VERY_HIGH   // 75-100
    }
}
