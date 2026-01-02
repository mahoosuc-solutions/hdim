package com.healthdata.predictive.service;

import com.healthdata.predictive.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiseaseProgressionPredictor {

    private final FeatureExtractor featureExtractor;

    public ProgressionRisk predictProgression(String tenantId, String patientId, Map<String, Object> patientData, String condition) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        if (patientId == null || patientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient ID cannot be null or empty");
        }
        if (condition == null || condition.trim().isEmpty()) {
            throw new IllegalArgumentException("Condition cannot be null or empty");
        }

        PatientFeatures features = featureExtractor.extractFeatures(tenantId, patientId, patientData);

        double probability = calculateProgressionProbability(features, condition);
        double score = probability * 100.0;
        RiskTier riskTier = RiskTier.fromScore(score);

        TimeToEvent timeToEvent = calculateTimeToEvent(features, condition, probability);
        Map<String, Double> riskFactors = extractRiskFactors(features, condition);

        String currentStage = determineCurrentStage(features, condition);
        String predictedStage = determinePredictedStage(currentStage, probability);

        return ProgressionRisk.builder()
            .patientId(patientId)
            .tenantId(tenantId)
            .condition(condition)
            .currentStage(currentStage)
            .predictedStage(predictedStage)
            .progressionProbability(probability)
            .riskScore(score)
            .riskTier(riskTier)
            .timeToEvent(timeToEvent)
            .riskFactors(riskFactors)
            .confidence(0.78)
            .modelVersion("v1.0.0")
            .predictedAt(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();
    }

    private double calculateProgressionProbability(PatientFeatures features, String condition) {
        double baseProb = 0.20;

        if ("diabetes".equalsIgnoreCase(condition)) {
            if (features.getHemoglobinA1c() != null && features.getHemoglobinA1c() > 9.0) baseProb += 0.25;
            else if (features.getHemoglobinA1c() != null && features.getHemoglobinA1c() > 7.5) baseProb += 0.15;
            if (features.getCharlsonComorbidityIndex() != null && features.getCharlsonComorbidityIndex() > 5) baseProb += 0.15;
        } else if ("chronic-kidney-disease".equalsIgnoreCase(condition)) {
            if (features.getEgfr() != null && features.getEgfr() < 30) baseProb += 0.35;
            else if (features.getEgfr() != null && features.getEgfr() < 60) baseProb += 0.20;
        } else if ("heart-failure".equalsIgnoreCase(condition)) {
            if (features.getHospitalizationsPastYear() != null && features.getHospitalizationsPastYear() > 2) baseProb += 0.30;
        }

        if (features.getAge() != null && features.getAge() > 65) baseProb += 0.10;

        return Math.min(baseProb, 0.95);
    }

    private TimeToEvent calculateTimeToEvent(PatientFeatures features, String condition, double probability) {
        int baseDays = 730; // 2 years default
        int predictedDays = (int)(baseDays * (1.0 - probability));

        return TimeToEvent.builder()
            .eventType("progression")
            .predictedDays(predictedDays)
            .predictedMonths(predictedDays / 30)
            .predictedEventDate(LocalDate.now().plusDays(predictedDays))
            .confidenceLowerDays((int)(predictedDays * 0.7))
            .confidenceUpperDays((int)(predictedDays * 1.3))
            .eventProbability(probability)
            .build();
    }

    private Map<String, Double> extractRiskFactors(PatientFeatures features, String condition) {
        Map<String, Double> factors = new HashMap<>();
        factors.put("age", features.getAge() != null ? features.getAge() / 100.0 : 0.0);
        factors.put("comorbidity_index", features.getCharlsonComorbidityIndex() != null ? features.getCharlsonComorbidityIndex() / 15.0 : 0.0);

        if ("diabetes".equalsIgnoreCase(condition) && features.getHemoglobinA1c() != null) {
            factors.put("hba1c", features.getHemoglobinA1c() / 15.0);
        }
        if ("chronic-kidney-disease".equalsIgnoreCase(condition) && features.getEgfr() != null) {
            factors.put("egfr", 1.0 - (features.getEgfr() / 120.0));
        }

        return factors;
    }

    private String determineCurrentStage(PatientFeatures features, String condition) {
        if ("diabetes".equalsIgnoreCase(condition)) {
            if (features.getHemoglobinA1c() == null) return "uncontrolled";
            if (features.getHemoglobinA1c() < 7.0) return "controlled";
            if (features.getHemoglobinA1c() < 9.0) return "moderate";
            return "severe";
        } else if ("chronic-kidney-disease".equalsIgnoreCase(condition)) {
            if (features.getEgfr() == null) return "unknown";
            if (features.getEgfr() >= 60) return "stage-1-2";
            if (features.getEgfr() >= 30) return "stage-3";
            return "stage-4-5";
        }
        return "active";
    }

    private String determinePredictedStage(String currentStage, double probability) {
        if (probability > 0.6) return "advanced";
        if (probability > 0.4) return "worsening";
        return currentStage;
    }
}
