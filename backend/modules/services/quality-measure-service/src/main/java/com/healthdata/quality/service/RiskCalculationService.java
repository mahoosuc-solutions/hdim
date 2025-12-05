package com.healthdata.quality.service;

import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Risk Calculation Service
 *
 * Provides continuous risk assessment with event-driven recalculation:
 * - Recalculates risk when new conditions are diagnosed
 * - Recalculates risk when lab results are received
 * - Detects risk level changes (LOW → HIGH)
 * - Extracts risk factors from FHIR data
 * - Calculates predicted outcomes
 * - Publishes events on risk changes
 *
 * Part of Phase 4: Continuous Risk Assessment Automation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Recalculate risk assessment when a new condition is added or updated
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param conditionData FHIR Condition resource data
     * @return Updated risk assessment
     */
    @Transactional
    public RiskAssessmentDTO recalculateRiskOnCondition(
        String tenantId,
        String patientId,
        Map<String, Object> conditionData
    ) {
        log.info("Recalculating risk for patient {} on new condition", patientId);

        // Get previous assessment
        Optional<RiskAssessmentEntity> previousOpt = riskAssessmentRepository
            .findLatestByTenantIdAndPatientId(tenantId, patientId);

        RiskAssessmentEntity.RiskLevel previousLevel = previousOpt
            .map(RiskAssessmentEntity::getRiskLevel)
            .orElse(null);

        // Extract risk factor from condition
        RiskFactor conditionRiskFactor = extractRiskFactorFromCondition(conditionData);

        // Calculate new risk assessment
        List<RiskFactor> allRiskFactors = new ArrayList<>();
        allRiskFactors.add(conditionRiskFactor);

        int newChronicConditionCount = previousOpt
            .map(RiskAssessmentEntity::getChronicConditionCount)
            .orElse(0) + 1;

        int totalRiskScore = calculateTotalRiskScore(allRiskFactors);
        RiskAssessmentEntity.RiskLevel newRiskLevel = determineRiskLevel(totalRiskScore);

        // Convert to JSON-friendly format
        List<Map<String, Object>> riskFactorMaps = allRiskFactors.stream()
            .map(rf -> Map.of(
                "factor", (Object) rf.factor,
                "category", rf.category,
                "weight", rf.weight,
                "severity", rf.severity,
                "evidence", rf.evidence
            ))
            .collect(Collectors.toList());

        List<PredictedOutcome> predictedOutcomes = generatePredictedOutcomes(newRiskLevel);
        List<Map<String, Object>> outcomeMaps = predictedOutcomes.stream()
            .map(po -> Map.of(
                "outcome", (Object) po.outcome,
                "probability", po.probability,
                "timeframe", po.timeframe
            ))
            .collect(Collectors.toList());

        List<String> recommendations = generateRecommendations(allRiskFactors, newRiskLevel);

        // Save new assessment
        RiskAssessmentEntity entity = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskScore(totalRiskScore)
            .riskLevel(newRiskLevel)
            .chronicConditionCount(newChronicConditionCount)
            .riskFactors(riskFactorMaps)
            .predictedOutcomes(outcomeMaps)
            .recommendations(recommendations)
            .assessmentDate(Instant.now())
            .build();

        entity = riskAssessmentRepository.save(entity);

        // Publish risk-assessment.updated event
        publishRiskAssessmentUpdatedEvent(tenantId, patientId, entity);

        // Check for risk level change
        if (previousLevel != null && previousLevel != newRiskLevel) {
            publishRiskLevelChangedEvent(tenantId, patientId, previousLevel, newRiskLevel);
        }

        return mapToDTO(entity);
    }

    /**
     * Recalculate risk assessment when a new observation (lab result) is received
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param observationData FHIR Observation resource data
     * @return Updated risk assessment
     */
    @Transactional
    public RiskAssessmentDTO recalculateRiskOnObservation(
        String tenantId,
        String patientId,
        Map<String, Object> observationData
    ) {
        log.info("Recalculating risk for patient {} on new observation", patientId);

        // Get previous assessment
        Optional<RiskAssessmentEntity> previousOpt = riskAssessmentRepository
            .findLatestByTenantIdAndPatientId(tenantId, patientId);

        RiskAssessmentEntity.RiskLevel previousLevel = previousOpt
            .map(RiskAssessmentEntity::getRiskLevel)
            .orElse(null);

        int previousScore = previousOpt
            .map(RiskAssessmentEntity::getRiskScore)
            .orElse(0);

        // Extract risk factor from observation
        RiskFactor observationRiskFactor = extractRiskFactorFromObservation(observationData);

        // Calculate new risk score (add observation risk to previous)
        int totalRiskScore = previousScore + observationRiskFactor.weight;
        totalRiskScore = Math.min(totalRiskScore, 100); // Cap at 100

        RiskAssessmentEntity.RiskLevel newRiskLevel = determineRiskLevel(totalRiskScore);

        List<Map<String, Object>> riskFactorMaps = List.of(Map.of(
            "factor", (Object) observationRiskFactor.factor,
            "category", observationRiskFactor.category,
            "weight", observationRiskFactor.weight,
            "severity", observationRiskFactor.severity,
            "evidence", observationRiskFactor.evidence
        ));

        List<PredictedOutcome> predictedOutcomes = generatePredictedOutcomes(newRiskLevel);
        List<Map<String, Object>> outcomeMaps = predictedOutcomes.stream()
            .map(po -> Map.of(
                "outcome", (Object) po.outcome,
                "probability", po.probability,
                "timeframe", po.timeframe
            ))
            .collect(Collectors.toList());

        List<String> recommendations = generateRecommendations(
            List.of(observationRiskFactor), newRiskLevel);

        int chronicConditionCount = previousOpt
            .map(RiskAssessmentEntity::getChronicConditionCount)
            .orElse(0);

        // Save new assessment
        RiskAssessmentEntity entity = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskScore(totalRiskScore)
            .riskLevel(newRiskLevel)
            .chronicConditionCount(chronicConditionCount)
            .riskFactors(riskFactorMaps)
            .predictedOutcomes(outcomeMaps)
            .recommendations(recommendations)
            .assessmentDate(Instant.now())
            .build();

        entity = riskAssessmentRepository.save(entity);

        // Publish events
        publishRiskAssessmentUpdatedEvent(tenantId, patientId, entity);

        if (previousLevel != null && previousLevel != newRiskLevel) {
            publishRiskLevelChangedEvent(tenantId, patientId, previousLevel, newRiskLevel);
        }

        return mapToDTO(entity);
    }

    /**
     * Extract risk factor from FHIR Condition resource
     */
    private RiskFactor extractRiskFactorFromCondition(Map<String, Object> conditionData) {
        // Extract condition display name
        String conditionName = extractConditionName(conditionData);

        // Extract severity if available
        String severity = extractSeverity(conditionData);

        // Extract onset date if available
        String onsetDate = (String) conditionData.get("onsetDateTime");

        // Determine weight based on condition type and severity
        int weight = calculateConditionWeight(conditionName, severity);

        String evidence = onsetDate != null
            ? String.format("Active diagnosis since %s", onsetDate)
            : "Active diagnosis";

        return new RiskFactor(
            conditionName,
            "chronic-disease",
            weight,
            severity != null ? severity.toLowerCase() : "moderate",
            evidence
        );
    }

    /**
     * Extract risk factor from FHIR Observation resource
     */
    private RiskFactor extractRiskFactorFromObservation(Map<String, Object> observationData) {
        // Extract observation code and display
        Map<String, Object> code = (Map<String, Object>) observationData.get("code");
        Map<String, Object> coding = (Map<String, Object>) ((List<?>) code.get("coding")).get(0);
        String loincCode = (String) coding.get("code");
        String display = (String) coding.get("display");

        // Extract value
        Map<String, Object> valueQuantity = (Map<String, Object>) observationData.get("valueQuantity");
        Double value = ((Number) valueQuantity.get("value")).doubleValue();
        String unit = (String) valueQuantity.get("unit");

        // Determine if value is out of range
        String factor;
        String evidence;
        int weight = 0;
        String severity = "moderate";

        switch (loincCode) {
            case "4548-4": // HbA1c
                if (value > 9.0) {
                    factor = "Uncontrolled Diabetes";
                    evidence = String.format("HbA1c %.1f%% (target <7.0%%)", value);
                    weight = 15;
                    severity = "high";
                } else if (value > 7.0) {
                    factor = "Suboptimal Diabetes Control";
                    evidence = String.format("HbA1c %.1f%% (target <7.0%%)", value);
                    weight = 10;
                    severity = "moderate";
                } else {
                    factor = "Controlled Diabetes";
                    evidence = String.format("HbA1c %.1f%%", value);
                    weight = 0;
                    severity = "low";
                }
                break;

            default:
                factor = display;
                evidence = String.format("%s: %.1f %s", display, value, unit);
                weight = 5;
                break;
        }

        return new RiskFactor(factor, "lab-result", weight, severity, evidence);
    }

    /**
     * Extract condition name from FHIR Condition
     */
    private String extractConditionName(Map<String, Object> conditionData) {
        Map<String, Object> code = (Map<String, Object>) conditionData.get("code");

        // Try text first
        if (code.containsKey("text")) {
            return (String) code.get("text");
        }

        // Fall back to coding display
        if (code.containsKey("coding")) {
            List<Map<String, Object>> codings = (List<Map<String, Object>>) code.get("coding");
            if (!codings.isEmpty()) {
                return (String) codings.get(0).get("display");
            }
        }

        return "Unknown Condition";
    }

    /**
     * Extract severity from FHIR Condition
     */
    private String extractSeverity(Map<String, Object> conditionData) {
        if (!conditionData.containsKey("severity")) {
            return null;
        }

        Map<String, Object> severity = (Map<String, Object>) conditionData.get("severity");
        List<Map<String, Object>> codings = (List<Map<String, Object>>) severity.get("coding");

        if (codings != null && !codings.isEmpty()) {
            return (String) codings.get(0).get("code");
        }

        return null;
    }

    /**
     * Calculate weight for a condition based on type and severity
     */
    private int calculateConditionWeight(String conditionName, String severity) {
        int baseWeight = 15; // Default for chronic conditions

        // Adjust based on severity
        if ("severe".equalsIgnoreCase(severity)) {
            baseWeight += 10;
        } else if ("mild".equalsIgnoreCase(severity)) {
            baseWeight -= 5;
        }

        return Math.max(5, Math.min(baseWeight, 30)); // Keep between 5-30
    }

    /**
     * Calculate total risk score from factors
     */
    private int calculateTotalRiskScore(List<RiskFactor> riskFactors) {
        int total = riskFactors.stream()
            .mapToInt(rf -> rf.weight)
            .sum();
        return Math.min(total, 100);
    }

    /**
     * Determine risk level from score
     */
    private RiskAssessmentEntity.RiskLevel determineRiskLevel(int score) {
        if (score >= 75) return RiskAssessmentEntity.RiskLevel.VERY_HIGH;
        if (score >= 50) return RiskAssessmentEntity.RiskLevel.HIGH;
        if (score >= 25) return RiskAssessmentEntity.RiskLevel.MODERATE;
        return RiskAssessmentEntity.RiskLevel.LOW;
    }

    /**
     * Generate predicted outcomes based on risk level
     */
    private List<PredictedOutcome> generatePredictedOutcomes(RiskAssessmentEntity.RiskLevel riskLevel) {
        List<PredictedOutcome> outcomes = new ArrayList<>();

        switch (riskLevel) {
            case VERY_HIGH -> {
                outcomes.add(new PredictedOutcome("Hospital admission", 0.45, "next 90 days"));
                outcomes.add(new PredictedOutcome("ED visit", 0.65, "next 90 days"));
                outcomes.add(new PredictedOutcome("Disease progression", 0.70, "next 6 months"));
            }
            case HIGH -> {
                outcomes.add(new PredictedOutcome("Hospital admission", 0.25, "next 90 days"));
                outcomes.add(new PredictedOutcome("ED visit", 0.40, "next 90 days"));
                outcomes.add(new PredictedOutcome("Disease progression", 0.50, "next 6 months"));
            }
            case MODERATE -> {
                outcomes.add(new PredictedOutcome("Hospital admission", 0.10, "next 90 days"));
                outcomes.add(new PredictedOutcome("ED visit", 0.20, "next 90 days"));
                outcomes.add(new PredictedOutcome("Disease progression", 0.30, "next 6 months"));
            }
            case LOW -> {
                outcomes.add(new PredictedOutcome("Hospital admission", 0.02, "next 90 days"));
                outcomes.add(new PredictedOutcome("ED visit", 0.05, "next 90 days"));
            }
        }

        return outcomes;
    }

    /**
     * Generate recommendations based on risk factors and level
     */
    private List<String> generateRecommendations(
        List<RiskFactor> riskFactors,
        RiskAssessmentEntity.RiskLevel riskLevel
    ) {
        List<String> recommendations = new ArrayList<>();

        if (riskLevel == RiskAssessmentEntity.RiskLevel.VERY_HIGH ||
            riskLevel == RiskAssessmentEntity.RiskLevel.HIGH) {
            recommendations.add("Enroll in care coordination program");
            recommendations.add("Schedule comprehensive care plan review within 7 days");
            recommendations.add("Implement weekly outreach calls");
        }

        return recommendations;
    }

    /**
     * Publish risk-assessment.updated event
     */
    private void publishRiskAssessmentUpdatedEvent(
        String tenantId,
        String patientId,
        RiskAssessmentEntity assessment
    ) {
        Map<String, Object> event = Map.of(
            "eventType", "risk-assessment.updated",
            "tenantId", tenantId,
            "patientId", patientId,
            "riskScore", assessment.getRiskScore(),
            "riskLevel", assessment.getRiskLevel().name(),
            "assessmentId", assessment.getId().toString(),
            "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send("risk-assessment.updated", event);
        log.info("Published risk-assessment.updated event for patient {}", patientId);
    }

    /**
     * Publish risk-level.changed event
     */
    private void publishRiskLevelChangedEvent(
        String tenantId,
        String patientId,
        RiskAssessmentEntity.RiskLevel previousLevel,
        RiskAssessmentEntity.RiskLevel newLevel
    ) {
        Map<String, Object> event = Map.of(
            "eventType", "risk-level.changed",
            "tenantId", tenantId,
            "patientId", patientId,
            "previousLevel", previousLevel.name(),
            "newLevel", newLevel.name(),
            "timestamp", Instant.now().toString()
        );

        kafkaTemplate.send("risk-level.changed", event);
        log.info("Published risk-level.changed event for patient {}: {} → {}",
            patientId, previousLevel, newLevel);
    }

    /**
     * Map entity to DTO
     */
    private RiskAssessmentDTO mapToDTO(RiskAssessmentEntity entity) {
        List<RiskAssessmentDTO.RiskFactorDTO> riskFactorDTOs = entity.getRiskFactors().stream()
            .map(map -> RiskAssessmentDTO.RiskFactorDTO.builder()
                .factor((String) map.get("factor"))
                .category((String) map.get("category"))
                .weight(((Number) map.get("weight")).intValue())
                .severity((String) map.get("severity"))
                .evidence((String) map.get("evidence"))
                .build())
            .collect(Collectors.toList());

        List<RiskAssessmentDTO.PredictedOutcomeDTO> outcomeDTOs = entity.getPredictedOutcomes().stream()
            .map(map -> RiskAssessmentDTO.PredictedOutcomeDTO.builder()
                .outcome((String) map.get("outcome"))
                .probability(((Number) map.get("probability")).doubleValue())
                .timeframe((String) map.get("timeframe"))
                .build())
            .collect(Collectors.toList());

        return RiskAssessmentDTO.builder()
            .id(entity.getId().toString())
            .patientId(entity.getPatientId())
            .riskScore(entity.getRiskScore())
            .riskLevel(entity.getRiskLevel().name().toLowerCase().replace("_", "-"))
            .riskFactors(riskFactorDTOs)
            .predictedOutcomes(outcomeDTOs)
            .recommendations(entity.getRecommendations())
            .assessmentDate(entity.getAssessmentDate())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    /**
     * Internal risk factor structure
     */
    private record RiskFactor(
        String factor,
        String category,
        int weight,
        String severity,
        String evidence
    ) {}

    /**
     * Internal predicted outcome structure
     */
    private record PredictedOutcome(
        String outcome,
        double probability,
        String timeframe
    ) {}
}
