package com.healthdata.predictive.service;

import com.healthdata.predictive.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Readmission risk prediction service
 * Predicts 30/90-day hospital readmission risk using ML models
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReadmissionRiskPredictor {

    private final FeatureExtractor featureExtractor;
    private final ReadmissionRiskModel riskModel;

    /**
     * Predict 30-day readmission risk
     */
    public ReadmissionRiskScore predict30DayRisk(
        String tenantId,
        String patientId,
        Map<String, Object> patientData
    ) {
        log.info("Predicting 30-day readmission risk for patient: {}, tenant: {}", patientId, tenantId);
        return predictRisk(tenantId, patientId, patientData, 30);
    }

    /**
     * Predict 90-day readmission risk
     */
    public ReadmissionRiskScore predict90DayRisk(
        String tenantId,
        String patientId,
        Map<String, Object> patientData
    ) {
        log.info("Predicting 90-day readmission risk for patient: {}, tenant: {}", patientId, tenantId);
        return predictRisk(tenantId, patientId, patientData, 90);
    }

    /**
     * Internal method to predict readmission risk
     */
    private ReadmissionRiskScore predictRisk(
        String tenantId,
        String patientId,
        Map<String, Object> patientData,
        int predictionPeriodDays
    ) {
        // Validation
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (patientId == null || patientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient ID cannot be null or empty");
        }
        if (patientData == null) {
            throw new IllegalArgumentException("Patient data cannot be null");
        }

        // Extract features
        PatientFeatures features = featureExtractor.extractFeatures(tenantId, patientId, patientData);

        // Normalize features
        double[] normalizedFeatures = featureExtractor.normalizeFeatures(features.getFeatureVector());

        // Predict probability
        double probability = riskModel.predict(normalizedFeatures);

        // Adjust probability for prediction period (90-day has higher probability)
        if (predictionPeriodDays == 90) {
            probability = adjustProbabilityFor90Days(probability);
        }

        // Convert probability to score (0-100 scale)
        double score = probability * 100.0;

        // Determine risk tier
        RiskTier riskTier = RiskTier.fromScore(score);

        // Calculate LACE index
        int laceIndex = calculateLaceIndex(features);

        // Extract risk factors
        ReadmissionRiskFactors riskFactors = extractRiskFactors(features, riskModel.getFeatureImportance());

        // Build risk score
        ReadmissionRiskScore riskScore = ReadmissionRiskScore.builder()
            .patientId(patientId)
            .tenantId(tenantId)
            .score(score)
            .riskTier(riskTier)
            .predictionPeriodDays(predictionPeriodDays)
            .readmissionProbability(probability)
            .laceIndex(laceIndex)
            .riskFactors(riskFactors)
            .confidence(riskModel.getConfidence())
            .modelVersion(riskModel.getModelVersion())
            .predictedAt(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();

        log.info("Predicted {}-day readmission risk for patient {}: score={}, tier={}, LACE={}",
            predictionPeriodDays, patientId, score, riskTier, laceIndex);

        return riskScore;
    }

    /**
     * Calculate LACE index
     * L = Length of stay
     * A = Acuity of admission
     * C = Comorbidities (Charlson index)
     * E = Emergency department visits
     */
    private int calculateLaceIndex(PatientFeatures features) {
        int laceScore = 0;

        // Length of stay (0-7 points)
        Integer los = features.getLengthOfStayLastAdmission();
        if (los != null) {
            if (los < 1) {
                laceScore += 0;
            } else if (los == 1) {
                laceScore += 1;
            } else if (los == 2) {
                laceScore += 2;
            } else if (los == 3) {
                laceScore += 3;
            } else if (los >= 4 && los <= 6) {
                laceScore += 4;
            } else if (los >= 7 && los <= 13) {
                laceScore += 5;
            } else {
                laceScore += 7;
            }
        }

        // Acuity (0-3 points)
        String acuity = features.getLastAdmissionAcuity();
        if (acuity != null) {
            if (acuity.equalsIgnoreCase("emergency") || acuity.equalsIgnoreCase("urgent")) {
                laceScore += 3;
            } else if (acuity.equalsIgnoreCase("elective")) {
                laceScore += 0;
            }
        }

        // Comorbidities using Charlson Index (0-5 points)
        Integer cci = features.getCharlsonComorbidityIndex();
        if (cci != null) {
            if (cci == 0) {
                laceScore += 0;
            } else if (cci == 1) {
                laceScore += 1;
            } else if (cci == 2) {
                laceScore += 2;
            } else if (cci == 3) {
                laceScore += 3;
            } else if (cci >= 4) {
                laceScore += 5;
            }
        }

        // Emergency department visits in past 6 months (0-4 points)
        Integer edVisits = features.getEdVisitsPast6Months();
        if (edVisits != null) {
            if (edVisits == 0) {
                laceScore += 0;
            } else if (edVisits == 1) {
                laceScore += 1;
            } else if (edVisits == 2) {
                laceScore += 2;
            } else if (edVisits == 3) {
                laceScore += 3;
            } else if (edVisits >= 4) {
                laceScore += 4;
            }
        }

        return laceScore;
    }

    /**
     * Extract risk factors with importance scores
     */
    private ReadmissionRiskFactors extractRiskFactors(
        PatientFeatures features,
        Map<String, Double> featureImportance
    ) {
        return ReadmissionRiskFactors.builder()
            .lengthOfStay(features.getLengthOfStayLastAdmission())
            .acuity(features.getLastAdmissionAcuity())
            .charlsonComorbidityIndex(features.getCharlsonComorbidityIndex())
            .edVisitsPast6Months(features.getEdVisitsPast6Months())
            .activeChronicConditions(features.getActiveConditionCount())
            .activeMedications(features.getActiveMedicationCount())
            .recentMedicationChanges(features.getMedicationChangesPast30Days())
            .age(features.getAge())
            .previousReadmissions(features.getHospitalizationsPastYear())
            .socialRiskFactors(features.getSocialRiskScore())
            .featureImportance(featureImportance)
            .build();
    }

    /**
     * Adjust probability for 90-day prediction period
     * 90-day readmission rates are typically higher than 30-day
     */
    private double adjustProbabilityFor90Days(double probability30Day) {
        // Approximate adjustment: 90-day rate is ~1.5x 30-day rate
        double adjustedProbability = probability30Day * 1.35;
        return Math.min(adjustedProbability, 0.95); // Cap at 95%
    }
}
