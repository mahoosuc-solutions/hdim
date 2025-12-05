package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Health Score History Entity
 *
 * Stores historical health scores for trend analysis
 */
@Entity
@Table(name = "health_score_history", indexes = {
    @Index(name = "idx_hsh_patient_date", columnList = "patient_id, calculated_at DESC"),
    @Index(name = "idx_hsh_tenant", columnList = "tenant_id, calculated_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "overall_score", nullable = false)
    private Double overallScore;

    @Column(name = "physical_health_score", nullable = false)
    private Double physicalHealthScore;

    @Column(name = "mental_health_score", nullable = false)
    private Double mentalHealthScore;

    @Column(name = "social_determinants_score", nullable = false)
    private Double socialDeterminantsScore;

    @Column(name = "preventive_care_score", nullable = false)
    private Double preventiveCareScore;

    @Column(name = "chronic_disease_score", nullable = false)
    private Double chronicDiseaseScore;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "previous_score")
    private Double previousScore;

    @Column(name = "score_delta")
    private Double scoreDelta;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Create from current health score
     */
    public static HealthScoreHistoryEntity fromHealthScore(HealthScoreEntity healthScore) {
        return HealthScoreHistoryEntity.builder()
            .patientId(healthScore.getPatientId())
            .tenantId(healthScore.getTenantId())
            .overallScore(healthScore.getOverallScore())
            .physicalHealthScore(healthScore.getPhysicalHealthScore())
            .mentalHealthScore(healthScore.getMentalHealthScore())
            .socialDeterminantsScore(healthScore.getSocialDeterminantsScore())
            .preventiveCareScore(healthScore.getPreventiveCareScore())
            .chronicDiseaseScore(healthScore.getChronicDiseaseScore())
            .calculatedAt(healthScore.getCalculatedAt())
            .previousScore(healthScore.getPreviousScore())
            .scoreDelta(healthScore.getScoreDelta())
            .changeReason(healthScore.getChangeReason())
            .build();
    }
}
