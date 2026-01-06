package com.healthdata.predictive.service;

import com.healthdata.predictive.model.PredictedCareGap;
import com.healthdata.predictive.model.PredictionFactor;
import com.healthdata.predictive.model.RiskTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Predicted Care Gap Service
 *
 * Implements predictive analytics for care gap detection using a weighted
 * multi-factor model to identify gaps before they occur.
 *
 * Prediction Factors (Issue #157):
 * - Historical Pattern (40%): Previous gaps, compliance history
 * - Appointment Adherence (25%): No-shows, cancellations
 * - Medication Refills (20%): Prescription adherence
 * - Similar Patient Behavior (15%): Cohort analysis
 *
 * Generates "Predicted Gap" alerts 30 days before likely occurrence.
 *
 * Issue #157: Implement Predictive Care Gap Detection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictedCareGapService {

    private static final String MODEL_VERSION = "1.0.0";
    private static final int PREDICTION_WINDOW_DAYS = 30;
    private static final double HIGH_RISK_THRESHOLD = 70.0;
    private static final double MODERATE_RISK_THRESHOLD = 50.0;

    /**
     * Get predicted care gaps for a provider's patient panel
     */
    public List<PredictedCareGap> getPredictedGapsForProvider(
        String tenantId,
        String providerId,
        Map<String, Object> providerData
    ) {
        log.info("Generating predicted care gaps for provider: {}, tenant: {}", providerId, tenantId);

        // Extract patient list from provider data
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> patients = (List<Map<String, Object>>) providerData.getOrDefault("patients", Collections.emptyList());

        List<PredictedCareGap> allPredictions = new ArrayList<>();

        for (Map<String, Object> patientData : patients) {
            String patientId = (String) patientData.get("patientId");
            String patientName = (String) patientData.getOrDefault("patientName", "Unknown");

            // Get predictions for each measure
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> measures = (List<Map<String, Object>>) patientData.getOrDefault("measures", Collections.emptyList());

            for (Map<String, Object> measureData : measures) {
                PredictedCareGap prediction = predictGapForMeasure(tenantId, patientId, patientName, measureData, patientData);
                if (prediction != null && prediction.getRiskScore() >= MODERATE_RISK_THRESHOLD) {
                    allPredictions.add(prediction);
                }
            }
        }

        // Sort by risk score descending
        allPredictions.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));

        log.info("Generated {} predicted care gaps for provider {}", allPredictions.size(), providerId);
        return allPredictions;
    }

    /**
     * Get predicted care gaps for a specific patient
     */
    public List<PredictedCareGap> getPredictedGapsForPatient(
        String tenantId,
        String patientId,
        Map<String, Object> patientData
    ) {
        log.info("Generating predicted care gaps for patient: {}, tenant: {}", patientId, tenantId);

        String patientName = (String) patientData.getOrDefault("patientName", "Unknown");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> measures = (List<Map<String, Object>>) patientData.getOrDefault("measures", Collections.emptyList());

        List<PredictedCareGap> predictions = new ArrayList<>();

        for (Map<String, Object> measureData : measures) {
            PredictedCareGap prediction = predictGapForMeasure(tenantId, patientId, patientName, measureData, patientData);
            if (prediction != null) {
                predictions.add(prediction);
            }
        }

        predictions.sort((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()));
        return predictions;
    }

    /**
     * Predict care gap for a specific measure
     */
    private PredictedCareGap predictGapForMeasure(
        String tenantId,
        String patientId,
        String patientName,
        Map<String, Object> measureData,
        Map<String, Object> patientData
    ) {
        String measureId = (String) measureData.get("measureId");
        String measureName = (String) measureData.getOrDefault("measureName", measureId);
        String measureCategory = (String) measureData.getOrDefault("category", "HEDIS");

        // Calculate prediction factors
        List<PredictionFactor> factors = new ArrayList<>();

        // 1. Historical Pattern (40%)
        PredictionFactor historicalFactor = calculateHistoricalPatternFactor(measureData, patientData);
        factors.add(historicalFactor);

        // 2. Appointment Adherence (25%)
        PredictionFactor appointmentFactor = calculateAppointmentAdherenceFactor(patientData);
        factors.add(appointmentFactor);

        // 3. Medication Refills (20%)
        PredictionFactor medicationFactor = calculateMedicationRefillFactor(patientData);
        factors.add(medicationFactor);

        // 4. Similar Patient Behavior (15%)
        PredictionFactor similarPatientFactor = calculateSimilarPatientFactor(measureData, patientData);
        factors.add(similarPatientFactor);

        // Calculate overall risk score
        double riskScore = factors.stream()
            .mapToDouble(PredictionFactor::getContribution)
            .sum() * 100; // Convert to 0-100 scale

        // Determine risk tier
        RiskTier riskTier = RiskTier.fromScore(riskScore);

        // Calculate confidence based on data completeness
        double confidence = calculateConfidence(patientData);

        // Determine predicted gap date
        LocalDate predictedGapDate = calculatePredictedGapDate(measureData, patientData);
        int daysUntilGap = (int) ChronoUnit.DAYS.between(LocalDate.now(), predictedGapDate);

        // Generate recommended interventions
        List<String> interventions = generateInterventions(riskScore, measureCategory, factors);
        String priorityIntervention = interventions.isEmpty() ? null : interventions.get(0);
        double interventionSuccessRate = calculateInterventionSuccessRate(measureCategory, riskTier);

        // Extract historical context
        int previousGaps = ((Number) measureData.getOrDefault("previousGaps", 0)).intValue();
        LocalDate lastComplianceDate = parseDate(measureData.get("lastComplianceDate"));
        int daysSinceLastCompliance = lastComplianceDate != null
            ? (int) ChronoUnit.DAYS.between(lastComplianceDate, LocalDate.now())
            : 365;

        // Similar patient metrics
        int similarPatientPoolSize = ((Number) patientData.getOrDefault("cohortSize", 100)).intValue();
        double similarPatientGapRate = ((Number) patientData.getOrDefault("cohortGapRate", 0.25)).doubleValue();

        return PredictedCareGap.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .patientId(patientId)
            .patientName(patientName)
            .measureId(measureId)
            .measureName(measureName)
            .measureCategory(measureCategory)
            .riskScore(Math.min(riskScore, 100.0))
            .riskTier(riskTier)
            .confidence(confidence)
            .predictedGapDate(predictedGapDate)
            .daysUntilGap(Math.max(daysUntilGap, 0))
            .predictionFactors(factors)
            .recommendedInterventions(interventions)
            .priorityIntervention(priorityIntervention)
            .interventionSuccessRate(interventionSuccessRate)
            .previousGapsForMeasure(previousGaps)
            .lastComplianceDate(lastComplianceDate)
            .daysSinceLastCompliance(daysSinceLastCompliance)
            .similarPatientPoolSize(similarPatientPoolSize)
            .similarPatientGapRate(similarPatientGapRate)
            .predictedAt(LocalDateTime.now())
            .modelVersion(MODEL_VERSION)
            .metadata(new HashMap<>())
            .build();
    }

    /**
     * Calculate Historical Pattern factor (40% weight)
     */
    private PredictionFactor calculateHistoricalPatternFactor(
        Map<String, Object> measureData,
        Map<String, Object> patientData
    ) {
        int previousGaps = ((Number) measureData.getOrDefault("previousGaps", 0)).intValue();
        int daysSinceLastCompliance = ((Number) measureData.getOrDefault("daysSinceLastCompliance", 180)).intValue();
        int measureFrequencyDays = ((Number) measureData.getOrDefault("frequencyDays", 365)).intValue();

        // Calculate score based on:
        // - Number of previous gaps (more gaps = higher risk)
        // - Time since last compliance (closer to due date = higher risk)

        double gapHistoryScore = Math.min(previousGaps * 0.2, 0.5); // Max 0.5 from gap history
        double timeScore = (double) daysSinceLastCompliance / measureFrequencyDays;
        timeScore = Math.min(timeScore, 1.0);

        double rawScore = (gapHistoryScore * 0.4) + (timeScore * 0.6);
        rawScore = Math.min(rawScore, 1.0);

        return PredictionFactor.historicalPattern(rawScore, previousGaps, daysSinceLastCompliance);
    }

    /**
     * Calculate Appointment Adherence factor (25% weight)
     */
    private PredictionFactor calculateAppointmentAdherenceFactor(Map<String, Object> patientData) {
        int noShows = ((Number) patientData.getOrDefault("noShowsPast12Months", 0)).intValue();
        int cancellations = ((Number) patientData.getOrDefault("cancellationsPast12Months", 0)).intValue();
        int totalAppointments = ((Number) patientData.getOrDefault("totalAppointmentsPast12Months", 10)).intValue();

        if (totalAppointments == 0) {
            // No appointment history, use moderate risk
            return PredictionFactor.appointmentAdherence(0.5, 0, 0);
        }

        double missedRate = (double) (noShows + cancellations) / totalAppointments;
        double rawScore = Math.min(missedRate * 2, 1.0); // Scale up missed rate

        return PredictionFactor.appointmentAdherence(rawScore, noShows, cancellations);
    }

    /**
     * Calculate Medication Refill factor (20% weight)
     */
    private PredictionFactor calculateMedicationRefillFactor(Map<String, Object> patientData) {
        double medicationAdherence = ((Number) patientData.getOrDefault("medicationAdherenceRate", 0.8)).doubleValue();
        int missedRefills = ((Number) patientData.getOrDefault("missedRefillsPast6Months", 0)).intValue();

        // Lower adherence = higher risk
        double adherenceRisk = 1.0 - medicationAdherence;
        double refillRisk = Math.min(missedRefills * 0.15, 0.5);

        double rawScore = (adherenceRisk * 0.6) + (refillRisk * 0.4);
        rawScore = Math.min(rawScore, 1.0);

        return PredictionFactor.medicationRefills(rawScore, medicationAdherence, missedRefills);
    }

    /**
     * Calculate Similar Patient Behavior factor (15% weight)
     */
    private PredictionFactor calculateSimilarPatientFactor(
        Map<String, Object> measureData,
        Map<String, Object> patientData
    ) {
        int cohortSize = ((Number) patientData.getOrDefault("cohortSize", 100)).intValue();
        double cohortGapRate = ((Number) patientData.getOrDefault("cohortGapRate", 0.25)).doubleValue();

        // Higher cohort gap rate = higher individual risk
        double rawScore = cohortGapRate * 1.5; // Scale up cohort rate
        rawScore = Math.min(rawScore, 1.0);

        return PredictionFactor.similarPatientBehavior(rawScore, cohortSize, cohortGapRate);
    }

    /**
     * Calculate prediction confidence based on data completeness
     */
    private double calculateConfidence(Map<String, Object> patientData) {
        int dataPoints = 0;
        int totalExpectedPoints = 8;

        // Check for presence of key data points
        if (patientData.containsKey("noShowsPast12Months")) dataPoints++;
        if (patientData.containsKey("cancellationsPast12Months")) dataPoints++;
        if (patientData.containsKey("totalAppointmentsPast12Months")) dataPoints++;
        if (patientData.containsKey("medicationAdherenceRate")) dataPoints++;
        if (patientData.containsKey("missedRefillsPast6Months")) dataPoints++;
        if (patientData.containsKey("cohortSize")) dataPoints++;
        if (patientData.containsKey("cohortGapRate")) dataPoints++;
        if (patientData.containsKey("measures")) dataPoints++;

        // Base confidence is 0.6, can go up to 0.95 with full data
        double baseConfidence = 0.6;
        double dataBonus = ((double) dataPoints / totalExpectedPoints) * 0.35;

        return Math.min(baseConfidence + dataBonus, 0.95);
    }

    /**
     * Calculate predicted gap date based on measure frequency and last compliance
     */
    private LocalDate calculatePredictedGapDate(
        Map<String, Object> measureData,
        Map<String, Object> patientData
    ) {
        LocalDate lastCompliance = parseDate(measureData.get("lastComplianceDate"));
        int frequencyDays = ((Number) measureData.getOrDefault("frequencyDays", 365)).intValue();

        if (lastCompliance == null) {
            // No compliance date, predict gap in 30 days
            return LocalDate.now().plusDays(PREDICTION_WINDOW_DAYS);
        }

        LocalDate expectedDue = lastCompliance.plusDays(frequencyDays);

        // If already past due, return today
        if (expectedDue.isBefore(LocalDate.now())) {
            return LocalDate.now();
        }

        return expectedDue;
    }

    /**
     * Generate recommended interventions based on risk factors
     */
    private List<String> generateInterventions(
        double riskScore,
        String measureCategory,
        List<PredictionFactor> factors
    ) {
        List<String> interventions = new ArrayList<>();

        // Get the most concerning factor
        Optional<PredictionFactor> primaryConcern = factors.stream()
            .filter(PredictionFactor::isConcerning)
            .max(Comparator.comparing(PredictionFactor::getContribution));

        // Add measure-specific interventions
        switch (measureCategory.toUpperCase()) {
            case "HEDIS":
            case "PREVENTIVE":
                interventions.add("Schedule preventive care visit");
                interventions.add("Send appointment reminder");
                break;
            case "CHRONIC":
                interventions.add("Schedule chronic care follow-up");
                interventions.add("Review medication adherence");
                break;
            case "CMS":
                interventions.add("Schedule quality measure assessment");
                interventions.add("Contact care coordinator");
                break;
        }

        // Add factor-specific interventions
        if (primaryConcern.isPresent()) {
            PredictionFactor factor = primaryConcern.get();
            switch (factor.getFactorType()) {
                case HISTORICAL_PATTERN:
                    interventions.add(0, "Proactive outreach - patient has history of gaps");
                    break;
                case APPOINTMENT_ADHERENCE:
                    interventions.add(0, "Implement multiple reminder strategy (call + SMS + email)");
                    break;
                case MEDICATION_REFILLS:
                    interventions.add(0, "Pharmacy coordination for medication sync");
                    break;
                case SIMILAR_PATIENT_BEHAVIOR:
                    interventions.add(0, "Assign to high-touch care management program");
                    break;
            }
        }

        // High risk patients get additional interventions
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            interventions.add("Escalate to care management team");
            interventions.add("Consider home health assessment");
        }

        return interventions.stream().distinct().limit(5).collect(Collectors.toList());
    }

    /**
     * Calculate expected intervention success rate
     */
    private double calculateInterventionSuccessRate(String measureCategory, RiskTier riskTier) {
        // Base success rates by measure category
        double baseRate = switch (measureCategory.toUpperCase()) {
            case "PREVENTIVE" -> 0.75;
            case "HEDIS" -> 0.70;
            case "CHRONIC" -> 0.60;
            case "CMS" -> 0.65;
            default -> 0.65;
        };

        // Adjust by risk tier (higher risk = lower success rate)
        double tierAdjustment = switch (riskTier) {
            case LOW -> 0.10;
            case MODERATE -> 0.0;
            case HIGH -> -0.10;
            case VERY_HIGH -> -0.20;
        };

        return Math.max(0.30, Math.min(0.90, baseRate + tierAdjustment));
    }

    /**
     * Parse date from various formats
     */
    private LocalDate parseDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }
        if (dateObj instanceof LocalDate) {
            return (LocalDate) dateObj;
        }
        if (dateObj instanceof String) {
            try {
                return LocalDate.parse((String) dateObj);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
