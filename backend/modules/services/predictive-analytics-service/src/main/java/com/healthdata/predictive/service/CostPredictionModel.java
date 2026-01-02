package com.healthdata.predictive.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ML model for cost prediction
 */
@Component
@Slf4j
public class CostPredictionModel {

    private static final String MODEL_VERSION = "v1.0.0";
    private static final double DEFAULT_CONFIDENCE = 0.75;
    private static final double BASE_MONTHLY_COST = 1200.0;

    public double predictTotalCost(double[] features) {
        return predictInpatientCost(features) + predictOutpatientCost(features) +
               predictPharmacyCost(features) + predictEmergencyCost(features) +
               predictLabCost(features) + predictImagingCost(features) + predictOtherCost(features);
    }

    public double predictInpatientCost(double[] features) {
        double baseCost = 5000.0;
        double cost = baseCost;
        if (features.length > 4 && features[4] > 0) cost += features[4] * 8000.0; // Hospitalizations
        if (features.length > 2 && features[2] > 0) cost += features[2] * 500.0;  // CCI
        return cost;
    }

    public double predictOutpatientCost(double[] features) {
        double baseCost = 2000.0;
        if (features.length > 6 && features[6] > 0) baseCost += features[6] * 200.0; // Outpatient visits
        return baseCost;
    }

    public double predictPharmacyCost(double[] features) {
        double baseCost = 1500.0;
        if (features.length > 7 && features[7] > 0) baseCost += features[7] * 150.0; // Active meds
        return baseCost;
    }

    public double predictEmergencyCost(double[] features) {
        double baseCost = 500.0;
        if (features.length > 5 && features[5] > 0) baseCost += features[5] * 1200.0; // ED visits
        return baseCost;
    }

    public double predictLabCost(double[] features) {
        return 800.0;
    }

    public double predictImagingCost(double[] features) {
        return 600.0;
    }

    public double predictOtherCost(double[] features) {
        return 400.0;
    }

    public double getConfidence() {
        return DEFAULT_CONFIDENCE;
    }

    public String getModelVersion() {
        return MODEL_VERSION;
    }
}
