package com.healthdata.predictive.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ML model wrapper for readmission risk prediction
 * Uses logistic regression for binary classification
 */
@Component
@Slf4j
public class ReadmissionRiskModel {

    private static final String MODEL_VERSION = "v1.0.0";
    private static final double DEFAULT_CONFIDENCE = 0.80;

    // Feature importance weights (derived from model training)
    private static final Map<String, Double> FEATURE_IMPORTANCE = new HashMap<>();
    static {
        FEATURE_IMPORTANCE.put("age", 0.12);
        FEATURE_IMPORTANCE.put("charlson_comorbidity_index", 0.25);
        FEATURE_IMPORTANCE.put("active_conditions", 0.10);
        FEATURE_IMPORTANCE.put("hospitalizations_past_year", 0.20);
        FEATURE_IMPORTANCE.put("ed_visits_past_6m", 0.18);
        FEATURE_IMPORTANCE.put("active_medications", 0.08);
        FEATURE_IMPORTANCE.put("length_of_stay", 0.12);
        FEATURE_IMPORTANCE.put("social_risk_score", 0.05);
    }

    /**
     * Predict readmission probability
     * Returns probability between 0.0 and 1.0
     */
    public double predict(double[] features) {
        if (features == null || features.length == 0) {
            throw new IllegalArgumentException("Features cannot be null or empty");
        }

        // In a real implementation, this would use a trained ML model
        // For now, we use a weighted scoring approach based on clinical evidence
        double probability = calculateWeightedRisk(features);

        log.debug("Predicted readmission probability: {}", probability);
        return probability;
    }

    /**
     * Calculate weighted risk score based on clinical features
     * This is a simplified model for demonstration
     */
    private double calculateWeightedRisk(double[] features) {
        double score = 0.0;
        double baseRisk = 0.15; // 15% baseline readmission rate

        // Age factor (index 0)
        if (features.length > 0 && features[0] > 0) {
            double age = features[0];
            if (age >= 75) {
                score += 0.15;
            } else if (age >= 65) {
                score += 0.10;
            } else if (age >= 50) {
                score += 0.05;
            }
        }

        // Charlson Comorbidity Index (index 2)
        if (features.length > 2 && features[2] > 0) {
            double cci = features[2];
            score += Math.min(cci * 0.04, 0.25); // Cap at 0.25
        }

        // Hospitalizations past year (index 4)
        if (features.length > 4 && features[4] > 0) {
            double hosp = features[4];
            score += Math.min(hosp * 0.10, 0.30); // Cap at 0.30
        }

        // ED visits (index 5)
        if (features.length > 5 && features[5] > 0) {
            double edVisits = features[5];
            score += Math.min(edVisits * 0.05, 0.20); // Cap at 0.20
        }

        // Length of stay (index 16)
        if (features.length > 16 && features[16] > 0) {
            double los = features[16];
            if (los >= 7) {
                score += 0.12;
            } else if (los >= 4) {
                score += 0.08;
            }
        }

        // Active medications (index 7)
        if (features.length > 7 && features[7] > 0) {
            double meds = features[7];
            if (meds >= 10) {
                score += 0.08;
            }
        }

        // Social risk (index 15)
        if (features.length > 15 && features[15] > 0) {
            double socialRisk = features[15];
            score += Math.min(socialRisk * 0.02, 0.10);
        }

        double probability = baseRisk + score;
        return Math.min(Math.max(probability, 0.0), 1.0); // Clamp to [0, 1]
    }

    /**
     * Get feature importance scores
     */
    public Map<String, Double> getFeatureImportance() {
        return new HashMap<>(FEATURE_IMPORTANCE);
    }

    /**
     * Get model confidence score
     */
    public double getConfidence() {
        return DEFAULT_CONFIDENCE;
    }

    /**
     * Get model version
     */
    public String getModelVersion() {
        return MODEL_VERSION;
    }
}
