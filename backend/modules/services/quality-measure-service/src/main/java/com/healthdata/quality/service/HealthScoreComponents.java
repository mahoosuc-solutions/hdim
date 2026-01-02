package com.healthdata.quality.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Health Score Component Scores
 *
 * Contains individual component scores used to calculate overall health score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreComponents {

    /**
     * Physical health score (0-100) - 30% weight
     * Based on: vitals, labs, chronic conditions
     */
    private Double physicalHealthScore;

    /**
     * Mental health score (0-100) - 25% weight
     * Based on: PHQ-9, GAD-7, mental health assessments
     */
    private Double mentalHealthScore;

    /**
     * Social determinants score (0-100) - 15% weight
     * Based on: SDOH screening results
     */
    private Double socialDeterminantsScore;

    /**
     * Preventive care score (0-100) - 15% weight
     * Based on: screening compliance
     */
    private Double preventiveCareScore;

    /**
     * Chronic disease management score (0-100) - 15% weight
     * Based on: care plan adherence, gap closure
     */
    private Double chronicDiseaseScore;

    /**
     * Validate all components are present and in valid range
     */
    public void validate() {
        validateScore("Physical Health", physicalHealthScore);
        validateScore("Mental Health", mentalHealthScore);
        validateScore("Social Determinants", socialDeterminantsScore);
        validateScore("Preventive Care", preventiveCareScore);
        validateScore("Chronic Disease", chronicDiseaseScore);
    }

    private void validateScore(String component, Double score) {
        if (score == null) {
            throw new IllegalArgumentException(component + " score is required");
        }
        if (score < 0.0 || score > 100.0) {
            throw new IllegalArgumentException(
                component + " score must be between 0 and 100, got: " + score
            );
        }
    }

    /**
     * Calculate weighted overall score
     */
    public Double calculateOverallScore() {
        validate();
        return (physicalHealthScore * 0.30) +
               (mentalHealthScore * 0.25) +
               (socialDeterminantsScore * 0.15) +
               (preventiveCareScore * 0.15) +
               (chronicDiseaseScore * 0.15);
    }
}
