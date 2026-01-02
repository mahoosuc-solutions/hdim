package com.healthdata.predictive.service;

import com.healthdata.predictive.model.CostBreakdown;
import com.healthdata.predictive.model.PatientFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Cost prediction service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CostPredictor {

    private final FeatureExtractor featureExtractor;
    private final CostPredictionModel costModel;

    public CostBreakdown predictCosts(String tenantId, String patientId, Map<String, Object> patientData, int predictionPeriodMonths) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (patientId == null || patientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient ID cannot be null or empty");
        }
        if (patientData == null) {
            throw new IllegalArgumentException("Patient data cannot be null");
        }
        if (predictionPeriodMonths <= 0) {
            throw new IllegalArgumentException("Prediction period must be positive");
        }

        PatientFeatures features = featureExtractor.extractFeatures(tenantId, patientId, patientData);
        double[] normalizedFeatures = featureExtractor.normalizeFeatures(features.getFeatureVector());

        double inpatientCost = costModel.predictInpatientCost(normalizedFeatures);
        double outpatientCost = costModel.predictOutpatientCost(normalizedFeatures);
        double pharmacyCost = costModel.predictPharmacyCost(normalizedFeatures);
        double emergencyCost = costModel.predictEmergencyCost(normalizedFeatures);
        double labCost = costModel.predictLabCost(normalizedFeatures);
        double imagingCost = costModel.predictImagingCost(normalizedFeatures);
        double otherCost = costModel.predictOtherCost(normalizedFeatures);

        double totalCost = inpatientCost + outpatientCost + pharmacyCost + emergencyCost + labCost + imagingCost + otherCost;
        double adjustedTotal = totalCost * (predictionPeriodMonths / 12.0);

        return CostBreakdown.builder()
            .patientId(patientId)
            .tenantId(tenantId)
            .totalPredictedCost(adjustedTotal)
            .inpatientCost(inpatientCost * (predictionPeriodMonths / 12.0))
            .outpatientCost(outpatientCost * (predictionPeriodMonths / 12.0))
            .pharmacyCost(pharmacyCost * (predictionPeriodMonths / 12.0))
            .emergencyCost(emergencyCost * (predictionPeriodMonths / 12.0))
            .labCost(labCost * (predictionPeriodMonths / 12.0))
            .imagingCost(imagingCost * (predictionPeriodMonths / 12.0))
            .otherCost(otherCost * (predictionPeriodMonths / 12.0))
            .predictionPeriodMonths(predictionPeriodMonths)
            .confidenceLower(adjustedTotal * 0.75)
            .confidenceUpper(adjustedTotal * 1.25)
            .confidence(costModel.getConfidence())
            .modelVersion(costModel.getModelVersion())
            .predictedAt(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();
    }
}
