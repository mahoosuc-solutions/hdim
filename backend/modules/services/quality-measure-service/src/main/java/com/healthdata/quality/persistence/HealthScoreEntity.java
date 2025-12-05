package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Health Score Entity
 *
 * Tracks comprehensive health scores calculated from multiple components:
 * - Physical health (30% weight)
 * - Mental health (25% weight)
 * - Social determinants (15% weight)
 * - Preventive care (15% weight)
 * - Chronic disease management (15% weight)
 */
@Entity
@Table(name = "health_scores", indexes = {
    @Index(name = "idx_hs_patient_calc", columnList = "patient_id, calculated_at DESC"),
    @Index(name = "idx_hs_tenant_patient", columnList = "tenant_id, patient_id"),
    @Index(name = "idx_hs_significant_change", columnList = "significant_change, calculated_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    /**
     * Overall health score (0-100)
     * Weighted average of all component scores
     */
    @Column(name = "overall_score", nullable = false)
    private Double overallScore;

    /**
     * Physical health score (0-100) - 30% weight
     * Based on: vitals, labs, chronic conditions
     */
    @Column(name = "physical_health_score", nullable = false)
    private Double physicalHealthScore;

    /**
     * Mental health score (0-100) - 25% weight
     * Based on: PHQ-9, GAD-7, mental health assessments
     */
    @Column(name = "mental_health_score", nullable = false)
    private Double mentalHealthScore;

    /**
     * Social determinants score (0-100) - 15% weight
     * Based on: SDOH screening results
     */
    @Column(name = "social_determinants_score", nullable = false)
    private Double socialDeterminantsScore;

    /**
     * Preventive care score (0-100) - 15% weight
     * Based on: screening compliance
     */
    @Column(name = "preventive_care_score", nullable = false)
    private Double preventiveCareScore;

    /**
     * Chronic disease management score (0-100) - 15% weight
     * Based on: care plan adherence, gap closure
     */
    @Column(name = "chronic_disease_score", nullable = false)
    private Double chronicDiseaseScore;

    /**
     * When this score was calculated
     */
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    /**
     * Previous overall score (for delta calculation)
     */
    @Column(name = "previous_score")
    private Double previousScore;

    /**
     * Indicates change >10 points from previous score
     */
    @Column(name = "significant_change", nullable = false)
    @Builder.Default
    private boolean significantChange = false;

    /**
     * Explanation of what caused the change
     */
    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (calculatedAt == null) {
            calculatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Calculate if this is a significant change
     */
    public void evaluateSignificantChange() {
        if (previousScore != null) {
            double delta = Math.abs(overallScore - previousScore);
            this.significantChange = delta >= 10.0;

            if (significantChange) {
                String direction = overallScore > previousScore ? "improvement" : "decline";
                this.changeReason = String.format(
                    "Significant %s in health score: %.1f points (%.1f → %.1f)",
                    direction, delta, previousScore, overallScore
                );
            }
        }
    }

    /**
     * Get the score delta
     */
    public Double getScoreDelta() {
        if (previousScore != null) {
            return overallScore - previousScore;
        }
        return null;
    }
}
