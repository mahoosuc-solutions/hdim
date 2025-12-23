package com.healthdata.quality.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Risk Stratification Service
 *
 * Calculates patient risk scores based on multiple health factors:
 * - Chronic conditions
 * - Mental health status
 * - Social determinants
 * - Medication adherence
 * - Healthcare utilization
 * - Preventive care gaps
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskStratificationService {

    private final RiskAssessmentRepository repository;
    private final IGenericClient fhirClient;
    private final CareGapRepository careGapRepository;
    private final MentalHealthAssessmentRepository mentalHealthAssessmentRepository;
    private final ChronicDiseaseMonitoringRepository chronicDiseaseMonitoringRepository;

    /**
     * Calculate risk assessment for a patient
     *
     * In a real implementation, this would fetch data from:
     * - FHIR Condition resources (chronic diseases)
     * - MentalHealthAssessmentEntity (recent screenings)
     * - Observation resources (vitals, labs)
     * - MedicationStatement resources (adherence)
     * - Encounter resources (utilization patterns)
     * - CareGapEntity (open care gaps)
     */
    @Transactional
    public RiskAssessmentDTO calculateRiskAssessment(String tenantId, UUID patientId) {
        log.info("Calculating risk assessment for patient {}", patientId);

        // Fetch and analyze real patient data from FHIR and database
        List<RiskFactor> riskFactors = analyzeRiskFactors(tenantId, patientId);

        if (riskFactors.isEmpty()) {
            log.info("No risk factors identified for patient {}", patientId);
        }

        int totalRiskScore = calculateTotalRiskScore(riskFactors);
        RiskAssessmentEntity.RiskLevel riskLevel = determineRiskLevel(totalRiskScore);
        List<PredictedOutcome> predictedOutcomes = generatePredictedOutcomes(riskFactors, riskLevel);
        List<String> recommendations = generateRecommendations(riskFactors, riskLevel);

        // Convert risk factors to JSON-friendly maps
        List<Map<String, Object>> riskFactorMaps = riskFactors.stream()
            .map(rf -> Map.of(
                "factor", (Object) rf.factor,
                "category", rf.category,
                "weight", rf.weight,
                "severity", rf.severity,
                "evidence", rf.evidence
            ))
            .collect(Collectors.toList());

        // Convert predicted outcomes to JSON-friendly maps
        List<Map<String, Object>> outcomeMaps = predictedOutcomes.stream()
            .map(po -> Map.of(
                "outcome", (Object) po.outcome,
                "probability", po.probability,
                "timeframe", po.timeframe
            ))
            .collect(Collectors.toList());

        // Save assessment
        RiskAssessmentEntity entity = RiskAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .riskScore(totalRiskScore)
            .riskLevel(riskLevel)
            .riskFactors(riskFactorMaps)
            .predictedOutcomes(outcomeMaps)
            .recommendations(recommendations)
            .assessmentDate(Instant.now())
            .build();

        entity = repository.save(entity);

        log.info("Risk assessment calculated: Patient {} - Score: {}, Level: {}, Factors: {}",
            patientId, totalRiskScore, riskLevel, riskFactors.size());

        return mapToDTO(entity);
    }

    /**
     * Get most recent risk assessment for a patient
     */
    public RiskAssessmentDTO getRiskAssessment(String tenantId, UUID patientId) {
        return repository.findMostRecent(tenantId, patientId)
            .map(this::mapToDTO)
            .orElse(null);
    }

    /**
     * Analyze risk factors for a patient by querying FHIR and database
     *
     * Analyzes:
     * 1. FHIR Condition resources for chronic diseases
     * 2. MentalHealthAssessmentEntity for recent positive screens
     * 3. FHIR Observation resources for uncontrolled vitals/labs
     * 4. FHIR MedicationStatement for non-adherence
     * 5. CareGapEntity for open urgent gaps
     * 6. FHIR Encounter resources for high utilization (ED visits, hospitalizations)
     * 7. ChronicDiseaseMonitoringEntity for deteriorating trends
     */
    private List<RiskFactor> analyzeRiskFactors(String tenantId, UUID patientId) {
        List<RiskFactor> factors = new ArrayList<>();

        try {
            // 1. Query FHIR Condition resources for chronic diseases
            factors.addAll(analyzeChronicConditions(patientId));

            // 2. Query MentalHealthAssessmentEntity for recent positive screens
            factors.addAll(analyzeMentalHealthScreenings(tenantId, patientId));

            // 3. Query Observation resources for uncontrolled vitals/labs
            factors.addAll(analyzeUncontrolledVitals(patientId));

            // 4. Query MedicationStatement for non-adherence indicators
            factors.addAll(analyzeMedicationAdherence(patientId));

            // 5. Query CareGapEntity for open urgent gaps
            factors.addAll(analyzeOpenCareGaps(tenantId, patientId));

            // 6. Query Encounter resources for high utilization
            factors.addAll(analyzeHealthcareUtilization(patientId));

            // 7. Query ChronicDiseaseMonitoringEntity for deteriorating trends
            factors.addAll(analyzeDeterioratingConditions(tenantId, patientId));

        } catch (Exception e) {
            log.error("Error analyzing risk factors for patient {}: {}", patientId, e.getMessage(), e);
            // Return partial results rather than failing completely
        }

        log.info("Analyzed {} risk factors for patient {}", factors.size(), patientId);
        return factors;
    }

    /**
     * Analyze chronic conditions from FHIR Condition resources
     */
    private List<RiskFactor> analyzeChronicConditions(UUID patientId) {
        List<RiskFactor> factors = new ArrayList<>();

        try {
            Bundle conditionBundle = fhirClient.search()
                .forResource(Condition.class)
                .where(Condition.PATIENT.hasId(patientId.toString()))
                .where(Condition.CLINICAL_STATUS.exactly().code("active"))
                .returnBundle(Bundle.class)
                .execute();

            for (Bundle.BundleEntryComponent entry : conditionBundle.getEntry()) {
                if (entry.getResource() instanceof Condition) {
                    Condition condition = (Condition) entry.getResource();
                    RiskFactor riskFactor = extractRiskFactorFromCondition(condition);
                    if (riskFactor != null) {
                        factors.add(riskFactor);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch conditions for patient {}: {}", patientId, e.getMessage());
        }

        return factors;
    }

    /**
     * Extract risk factor from FHIR Condition
     */
    private RiskFactor extractRiskFactorFromCondition(Condition condition) {
        if (!condition.hasCode() || !condition.getCode().hasCoding()) {
            return null;
        }

        String conditionName = condition.getCode().hasText()
            ? condition.getCode().getText()
            : condition.getCode().getCodingFirstRep().getDisplay();

        if (conditionName == null) {
            return null;
        }

        // Determine weight based on condition type
        int weight = calculateConditionWeight(conditionName, condition);
        String severity = extractSeverityFromCondition(condition);
        String evidence = condition.hasOnsetDateTimeType()
            ? "Active since " + condition.getOnsetDateTimeType().toHumanDisplay()
            : "Active diagnosis";

        return new RiskFactor(
            conditionName,
            "chronic-disease",
            weight,
            severity,
            evidence
        );
    }

    /**
     * Calculate weight for a condition based on type and severity
     */
    private int calculateConditionWeight(String conditionName, Condition condition) {
        String lowerName = conditionName.toLowerCase();
        int baseWeight = 10;

        // High-risk chronic conditions
        if (lowerName.contains("diabetes") || lowerName.contains("heart failure") ||
            lowerName.contains("copd") || lowerName.contains("chronic kidney")) {
            baseWeight = 20;
        } else if (lowerName.contains("hypertension") || lowerName.contains("asthma") ||
                   lowerName.contains("hyperlipidemia")) {
            baseWeight = 15;
        }

        // Adjust based on severity
        if (condition.hasSeverity()) {
            String severityCode = condition.getSeverity().getCodingFirstRep().getCode();
            if ("severe".equalsIgnoreCase(severityCode)) {
                baseWeight += 10;
            } else if ("moderate".equalsIgnoreCase(severityCode)) {
                baseWeight += 5;
            }
        }

        return Math.min(baseWeight, 30);
    }

    /**
     * Extract severity from FHIR Condition
     */
    private String extractSeverityFromCondition(Condition condition) {
        if (condition.hasSeverity() && condition.getSeverity().hasCoding()) {
            String code = condition.getSeverity().getCodingFirstRep().getCode();
            if (code != null) {
                return code.toLowerCase();
            }
        }
        return "moderate";
    }

    /**
     * Analyze mental health screenings from database
     */
    private List<RiskFactor> analyzeMentalHealthScreenings(String tenantId, UUID patientId) {
        List<RiskFactor> factors = new ArrayList<>();

        try {
            // Look for positive screens in last 6 months
            List<MentalHealthAssessmentEntity> positiveScreens =
                mentalHealthAssessmentRepository.findPositiveScreensRequiringFollowup(tenantId, patientId);

            for (MentalHealthAssessmentEntity assessment : positiveScreens) {
                // Only include recent assessments (within 6 months)
                if (assessment.getAssessmentDate().isAfter(Instant.now().minus(180, ChronoUnit.DAYS))) {
                    int weight = calculateMentalHealthWeight(assessment);
                    factors.add(new RiskFactor(
                        assessment.getType().name().replace("_", "-") + " - " + assessment.getSeverity(),
                        "mental-health",
                        weight,
                        assessment.getSeverity().toLowerCase(),
                        String.format("%s score: %d/%d", assessment.getType(), assessment.getScore(), assessment.getMaxScore())
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch mental health assessments for patient {}: {}", patientId, e.getMessage());
        }

        return factors;
    }

    /**
     * Calculate weight for mental health risk
     */
    private int calculateMentalHealthWeight(MentalHealthAssessmentEntity assessment) {
        String severity = assessment.getSeverity().toLowerCase();
        if (severity.contains("severe")) {
            return 20;
        } else if (severity.contains("moderate")) {
            return 15;
        } else {
            return 10;
        }
    }

    /**
     * Analyze uncontrolled vitals from FHIR Observation resources
     */
    private List<RiskFactor> analyzeUncontrolledVitals(UUID patientId) {
        List<RiskFactor> factors = new ArrayList<>();

        try {
            // Query recent observations (last 6 months)
            Bundle observationBundle = fhirClient.search()
                .forResource(Observation.class)
                .where(Observation.PATIENT.hasId(patientId.toString()))
                .returnBundle(Bundle.class)
                .execute();

            for (Bundle.BundleEntryComponent entry : observationBundle.getEntry()) {
                if (entry.getResource() instanceof Observation) {
                    Observation obs = (Observation) entry.getResource();
                    RiskFactor riskFactor = extractRiskFactorFromObservation(obs);
                    if (riskFactor != null) {
                        factors.add(riskFactor);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch observations for patient {}: {}", patientId, e.getMessage());
        }

        return factors;
    }

    /**
     * Extract risk factor from FHIR Observation
     */
    private RiskFactor extractRiskFactorFromObservation(Observation obs) {
        if (!obs.hasCode() || !obs.hasValue() || !obs.getCode().hasCoding()) {
            return null;
        }

        // Only consider recent observations (within 6 months)
        if (obs.hasEffectiveDateTimeType() &&
            obs.getEffectiveDateTimeType().getValue().toInstant()
                .isBefore(Instant.now().minus(180, ChronoUnit.DAYS))) {
            return null;
        }

        String loincCode = obs.getCode().getCoding().stream()
            .filter(c -> "http://loinc.org".equals(c.getSystem()))
            .map(Coding::getCode)
            .findFirst()
            .orElse(null);

        if (loincCode == null || !(obs.getValue() instanceof Quantity)) {
            return null;
        }

        Quantity value = (Quantity) obs.getValue();
        double numericValue = value.getValue().doubleValue();
        String display = obs.getCode().hasText() ? obs.getCode().getText() : obs.getCode().getCodingFirstRep().getDisplay();

        // Check for uncontrolled values based on LOINC code
        return evaluateObservationRisk(loincCode, numericValue, value.getUnit(), display);
    }

    /**
     * Evaluate risk from observation value
     */
    private RiskFactor evaluateObservationRisk(String loincCode, double value, String unit, String display) {
        switch (loincCode) {
            case "4548-4": // HbA1c
                if (value > 9.0) {
                    return new RiskFactor(
                        "Uncontrolled Diabetes",
                        "lab-result",
                        20,
                        "high",
                        String.format("HbA1c %.1f%% (target <7.0%%)", value)
                    );
                } else if (value > 7.0) {
                    return new RiskFactor(
                        "Suboptimal Diabetes Control",
                        "lab-result",
                        10,
                        "moderate",
                        String.format("HbA1c %.1f%% (target <7.0%%)", value)
                    );
                }
                break;

            case "8480-6": // Systolic Blood Pressure
                if (value > 180) {
                    return new RiskFactor(
                        "Severe Hypertension",
                        "lab-result",
                        20,
                        "high",
                        String.format("Systolic BP %.0f mmHg (target <140 mmHg)", value)
                    );
                } else if (value > 140) {
                    return new RiskFactor(
                        "Uncontrolled Hypertension",
                        "lab-result",
                        15,
                        "moderate",
                        String.format("Systolic BP %.0f mmHg (target <140 mmHg)", value)
                    );
                }
                break;

            case "2093-3": // Total Cholesterol
                if (value > 240) {
                    return new RiskFactor(
                        "High Cholesterol",
                        "lab-result",
                        10,
                        "moderate",
                        String.format("Total Cholesterol %.0f mg/dL (target <200 mg/dL)", value)
                    );
                }
                break;

            case "18262-6": // LDL Cholesterol
                if (value > 190) {
                    return new RiskFactor(
                        "Very High LDL",
                        "lab-result",
                        15,
                        "high",
                        String.format("LDL %.0f mg/dL (target <100 mg/dL)", value)
                    );
                } else if (value > 130) {
                    return new RiskFactor(
                        "Elevated LDL",
                        "lab-result",
                        10,
                        "moderate",
                        String.format("LDL %.0f mg/dL (target <100 mg/dL)", value)
                    );
                }
                break;
        }

        return null;
    }

    /**
     * Analyze medication adherence from FHIR MedicationStatement
     */
    private List<RiskFactor> analyzeMedicationAdherence(UUID patientId) {
        List<RiskFactor> factors = new ArrayList<>();

        try {
            Bundle medBundle = fhirClient.search()
                .forResource(MedicationStatement.class)
                .where(MedicationStatement.PATIENT.hasId(patientId.toString()))
                .where(MedicationStatement.STATUS.exactly().code("not-taken"))
                .returnBundle(Bundle.class)
                .execute();

            int nonAdherentMeds = medBundle.getEntry().size();
            if (nonAdherentMeds > 0) {
                factors.add(new RiskFactor(
                    "Medication Non-Adherence",
                    "medication",
                    Math.min(nonAdherentMeds * 5, 25),
                    nonAdherentMeds >= 3 ? "high" : "moderate",
                    String.format("%d medications marked as not-taken", nonAdherentMeds)
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch medication statements for patient {}: {}", patientId, e.getMessage());
        }

        return factors;
    }

    /**
     * Analyze open care gaps from database
     */
    private List<RiskFactor> analyzeOpenCareGaps(String tenantId, UUID patientId) {
        List<RiskFactor> factors = new ArrayList<>();

        try {
            // Count urgent care gaps
            Long urgentGaps = careGapRepository.countUrgentCareGaps(tenantId, patientId);
            if (urgentGaps != null && urgentGaps > 0) {
                factors.add(new RiskFactor(
                    "Urgent Care Gaps",
                    "care-gaps",
                    Math.min(urgentGaps.intValue() * 10, 30),
                    "high",
                    String.format("%d urgent care gaps requiring immediate attention", urgentGaps)
                ));
            }

            // Count all open care gaps
            Long openGaps = careGapRepository.countOpenCareGaps(tenantId, patientId);
            if (openGaps != null && openGaps > urgentGaps) {
                long nonUrgentGaps = openGaps - urgentGaps;
                if (nonUrgentGaps >= 3) {
                    factors.add(new RiskFactor(
                        "Multiple Open Care Gaps",
                        "care-gaps",
                        Math.min((int) nonUrgentGaps * 3, 15),
                        "moderate",
                        String.format("%d open care gaps", nonUrgentGaps)
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch care gaps for patient {}: {}", patientId, e.getMessage());
        }

        return factors;
    }

    /**
     * Analyze healthcare utilization from FHIR Encounter resources
     */
    private List<RiskFactor> analyzeHealthcareUtilization(UUID patientId) {
        List<RiskFactor> factors = new ArrayList<>();

        try {
            // Query encounters in last 90 days
            Bundle encounterBundle = fhirClient.search()
                .forResource(Encounter.class)
                .where(Encounter.PATIENT.hasId(patientId.toString()))
                .returnBundle(Bundle.class)
                .execute();

            int edVisits = 0;
            int hospitalizations = 0;
            Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);

            for (Bundle.BundleEntryComponent entry : encounterBundle.getEntry()) {
                if (entry.getResource() instanceof Encounter) {
                    Encounter encounter = (Encounter) entry.getResource();

                    // Only count recent encounters
                    if (encounter.hasPeriod() && encounter.getPeriod().hasStart() &&
                        encounter.getPeriod().getStart().toInstant().isAfter(ninetyDaysAgo)) {

                        if (encounter.hasClass_()) {
                            String encounterClass = encounter.getClass_().getCode();
                            if ("EMER".equals(encounterClass) || "emergency".equalsIgnoreCase(encounterClass)) {
                                edVisits++;
                            } else if ("IMP".equals(encounterClass) || "inpatient".equalsIgnoreCase(encounterClass)) {
                                hospitalizations++;
                            }
                        }
                    }
                }
            }

            if (edVisits >= 2) {
                factors.add(new RiskFactor(
                    "Frequent ED Visits",
                    "utilization",
                    Math.min(edVisits * 10, 25),
                    edVisits >= 3 ? "high" : "moderate",
                    String.format("%d ED visits in past 90 days", edVisits)
                ));
            }

            if (hospitalizations >= 1) {
                factors.add(new RiskFactor(
                    "Recent Hospitalization",
                    "utilization",
                    hospitalizations * 15,
                    "high",
                    String.format("%d hospitalization(s) in past 90 days", hospitalizations)
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch encounters for patient {}: {}", patientId, e.getMessage());
        }

        return factors;
    }

    /**
     * Analyze deteriorating chronic conditions from database
     */
    private List<RiskFactor> analyzeDeterioratingConditions(String tenantId, UUID patientId) {
        List<RiskFactor> factors = new ArrayList<>();

        try {
            List<ChronicDiseaseMonitoringEntity> monitoring =
                chronicDiseaseMonitoringRepository.findByTenantIdAndPatientIdOrderByMonitoredAtDesc(tenantId, patientId);

            for (ChronicDiseaseMonitoringEntity entity : monitoring) {
                if (entity.getTrend() == ChronicDiseaseMonitoringEntity.Trend.DETERIORATING) {
                    factors.add(new RiskFactor(
                        "Deteriorating " + entity.getDiseaseName(),
                        "chronic-disease-trend",
                        20,
                        "high",
                        String.format("Trend: %s → %s", entity.getPreviousValue(), entity.getLatestValue())
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch chronic disease monitoring for patient {}: {}", patientId, e.getMessage());
        }

        return factors;
    }

    /**
     * Calculate total risk score from factors
     */
    private int calculateTotalRiskScore(List<RiskFactor> riskFactors) {
        int total = riskFactors.stream()
            .mapToInt(rf -> rf.weight)
            .sum();

        // Cap at 100
        return Math.min(total, 100);
    }

    /**
     * Determine risk level from score
     */
    private RiskAssessmentEntity.RiskLevel determineRiskLevel(int score) {
        if (score >= 75) {
            return RiskAssessmentEntity.RiskLevel.VERY_HIGH;
        } else if (score >= 50) {
            return RiskAssessmentEntity.RiskLevel.HIGH;
        } else if (score >= 25) {
            return RiskAssessmentEntity.RiskLevel.MODERATE;
        } else {
            return RiskAssessmentEntity.RiskLevel.LOW;
        }
    }

    /**
     * Generate predicted outcomes based on risk factors
     */
    private List<PredictedOutcome> generatePredictedOutcomes(
        List<RiskFactor> riskFactors,
        RiskAssessmentEntity.RiskLevel riskLevel
    ) {
        List<PredictedOutcome> outcomes = new ArrayList<>();

        // Base probabilities on risk level
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
     * Generate recommendations based on risk profile
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
            recommendations.add("Review all medications for adherence and interactions");
        }

        if (riskLevel == RiskAssessmentEntity.RiskLevel.MODERATE) {
            recommendations.add("Schedule care plan review within 30 days");
            recommendations.add("Implement monthly check-ins");
            recommendations.add("Address all urgent and high-priority care gaps");
        }

        // Add category-specific recommendations based on risk factors
        Map<String, Long> categoryCount = riskFactors.stream()
            .collect(Collectors.groupingBy(rf -> rf.category, Collectors.counting()));

        if (categoryCount.getOrDefault("mental-health", 0L) > 0) {
            recommendations.add("Refer to behavioral health specialist");
            recommendations.add("Implement mental health monitoring protocol");
        }

        if (categoryCount.getOrDefault("chronic-disease", 0L) >= 2) {
            recommendations.add("Refer to chronic disease management program");
        }

        if (categoryCount.getOrDefault("social-determinants", 0L) > 0) {
            recommendations.add("Connect with community health worker");
            recommendations.add("Screen for additional social needs");
        }

        return recommendations;
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
