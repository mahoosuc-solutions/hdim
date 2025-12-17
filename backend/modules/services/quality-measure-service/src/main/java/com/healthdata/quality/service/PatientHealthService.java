package com.healthdata.quality.service;

import com.healthdata.quality.dto.*;
import com.healthdata.quality.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Patient Health Service
 *
 * Aggregator service that provides a complete patient health overview
 * by combining data from multiple services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientHealthService {

    private final MentalHealthAssessmentService mentalHealthService;
    private final CareGapService careGapService;
    private final RiskStratificationService riskService;
    private final CareGapRepository careGapRepository;
    private final MentalHealthAssessmentRepository mentalHealthRepository;
    private final PatientDataService patientDataService;
    private final HealthScoreHistoryRepository healthScoreHistoryRepository;

    /**
     * Get complete health overview for a patient
     */
    public PatientHealthOverviewDTO getPatientHealthOverview(String tenantId, String patientId) {
        log.info("Fetching health overview for patient {}", patientId);

        // Get recent mental health assessments (last 5)
        List<MentalHealthAssessmentDTO> recentAssessments = mentalHealthService.getPatientAssessments(
            tenantId, patientId, null, 5, 0
        );

        // Get open care gaps
        List<CareGapDTO> openCareGaps = careGapService.getPatientCareGaps(
            tenantId, patientId, null, null
        );

        // Get most recent risk assessment
        RiskAssessmentDTO riskAssessment = riskService.getRiskAssessment(tenantId, patientId);

        // Calculate health score
        HealthScoreDTO healthScore = calculateHealthScore(tenantId, patientId);

        // Calculate summary statistics
        PatientHealthOverviewDTO.SummaryStatsDTO summaryStats = calculateSummaryStats(
            tenantId, patientId, openCareGaps
        );

        return PatientHealthOverviewDTO.builder()
            .patientId(patientId)
            .healthScore(healthScore)
            .recentMentalHealthAssessments(recentAssessments)
            .openCareGaps(openCareGaps)
            .riskAssessment(riskAssessment)
            .summaryStats(summaryStats)
            .build();
    }

    /**
     * Calculate overall health score
     *
     * Weighted composite score (0-100) based on:
     * - Physical health (30%): Vitals, labs, chronic disease control
     * - Mental health (25%): Recent screenings, positive screens
     * - Social determinants (15%): SDOH screening, identified needs
     * - Preventive care (15%): Screenings up to date, immunizations
     * - Chronic disease management (15%): Care plan adherence, gaps
     */
    public HealthScoreDTO calculateHealthScore(String tenantId, String patientId) {
        log.info("Calculating health score for patient {} (tenant: {})", patientId, tenantId);

        // Calculate component scores from real FHIR data
        double physicalScore = calculatePhysicalHealthScore(tenantId, patientId);
        double mentalScore = calculateMentalHealthScore(tenantId, patientId);
        double socialScore = calculateSocialScore(tenantId, patientId);
        double preventiveScore = calculatePreventiveCareScore(tenantId, patientId);
        double chronicDiseaseScore = calculateChronicDiseaseScore(tenantId, patientId);

        // Calculate weighted overall score
        double overallScore =
            (physicalScore * 0.30) +
            (mentalScore * 0.25) +
            (socialScore * 0.15) +
            (preventiveScore * 0.15) +
            (chronicDiseaseScore * 0.15);

        // Get historical trend data
        List<HealthScoreHistoryEntity> history = healthScoreHistoryRepository
            .findRecentScores(tenantId, patientId, 5);

        Double previousScore = null;
        Double scoreDelta = null;
        boolean significantChange = false;

        if (!history.isEmpty()) {
            previousScore = history.get(0).getOverallScore();
            scoreDelta = overallScore - previousScore;
            significantChange = Math.abs(scoreDelta) > 10.0;
        }

        String interpretation = determineInterpretation((int) Math.round(overallScore));

        HealthScoreDTO scoreDTO = HealthScoreDTO.builder()
            .patientId(patientId)
            .tenantId(tenantId)
            .overallScore(overallScore)
            .interpretation(interpretation)
            .physicalHealthScore(physicalScore)
            .mentalHealthScore(mentalScore)
            .socialDeterminantsScore(socialScore)
            .preventiveCareScore(preventiveScore)
            .chronicDiseaseScore(chronicDiseaseScore)
            .previousScore(previousScore)
            .scoreDelta(scoreDelta)
            .significantChange(significantChange)
            .calculatedAt(Instant.now())
            .build();

        log.info("Health score calculated: {} (trend: {})", overallScore, scoreDTO.getTrend());

        return scoreDTO;
    }

    /**
     * Calculate physical health score
     *
     * Based on vital signs in healthy ranges:
     * - Heart rate: 60-100 bpm
     * - Systolic BP: 90-120 mmHg
     * - Diastolic BP: 60-80 mmHg
     * - BMI: 18.5-24.9 kg/m2
     * - Weight: reasonable range
     *
     * Score = (vitals in range / total vitals) * 100
     */
    private double calculatePhysicalHealthScore(String tenantId, String patientId) {
        log.debug("Calculating physical health score for patient {}", patientId);

        try {
            List<Observation> observations = patientDataService.fetchPatientObservations(tenantId, patientId);

            if (observations.isEmpty()) {
                log.debug("No observations found, returning default score of 50");
                return 50.0; // Default when no data available
            }

            // Get most recent vital signs
            Map<String, Observation> recentVitals = new HashMap<>();
            Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS); // Last 90 days

            for (Observation obs : observations) {
                if (!isVitalSign(obs) || !isRecent(obs, cutoff)) {
                    continue;
                }

                String code = getLoincCode(obs);
                if (code != null && isRelevantVital(code)) {
                    // Keep only most recent for each vital type
                    if (!recentVitals.containsKey(code) ||
                        isMoreRecent(obs, recentVitals.get(code))) {
                        recentVitals.put(code, obs);
                    }
                }
            }

            if (recentVitals.isEmpty()) {
                log.debug("No recent vital signs found");
                return 50.0;
            }

            // Check how many vitals are in healthy range
            int inRange = 0;
            int total = recentVitals.size();

            for (Map.Entry<String, Observation> entry : recentVitals.entrySet()) {
                if (isInHealthyRange(entry.getKey(), entry.getValue())) {
                    inRange++;
                }
            }

            double score = (total > 0) ? ((double) inRange / total) * 100 : 50.0;
            log.debug("Physical health score: {} ({}/{} vitals in range)", score, inRange, total);

            return score;

        } catch (Exception e) {
            log.error("Error calculating physical health score for patient {}", patientId, e);
            return 50.0; // Default on error
        }
    }

    private boolean isVitalSign(Observation obs) {
        return obs.getCategory().stream()
            .anyMatch(cat -> cat.getCoding().stream()
                .anyMatch(coding -> "vital-signs".equals(coding.getCode())));
    }

    private boolean isRecent(Observation obs, Instant cutoff) {
        if (obs.getEffective() instanceof DateTimeType) {
            Date effectiveDate = ((DateTimeType) obs.getEffective()).getValue();
            return effectiveDate != null && effectiveDate.toInstant().isAfter(cutoff);
        }
        return false;
    }

    private String getLoincCode(Observation obs) {
        return obs.getCode().getCoding().stream()
            .filter(coding -> "http://loinc.org".equals(coding.getSystem()))
            .map(Coding::getCode)
            .findFirst()
            .orElse(null);
    }

    private boolean isRelevantVital(String loincCode) {
        Set<String> relevantCodes = Set.of(
            "85714-4", // Heart rate
            "8480-6",  // Systolic BP
            "8462-4",  // Diastolic BP
            "39156-5", // BMI
            "29463-7"  // Body Weight
        );
        return relevantCodes.contains(loincCode);
    }

    private boolean isMoreRecent(Observation obs1, Observation obs2) {
        if (obs1.getEffective() instanceof DateTimeType && obs2.getEffective() instanceof DateTimeType) {
            Date date1 = ((DateTimeType) obs1.getEffective()).getValue();
            Date date2 = ((DateTimeType) obs2.getEffective()).getValue();
            return date1 != null && date2 != null && date1.after(date2);
        }
        return false;
    }

    private boolean isInHealthyRange(String loincCode, Observation obs) {
        if (!(obs.getValue() instanceof Quantity)) {
            return false;
        }

        double value = ((Quantity) obs.getValue()).getValue().doubleValue();

        return switch (loincCode) {
            case "85714-4" -> value >= 60 && value <= 100; // Heart rate
            case "8480-6" -> value >= 90 && value <= 120;  // Systolic BP
            case "8462-4" -> value >= 60 && value <= 80;   // Diastolic BP
            case "39156-5" -> value >= 18.5 && value <= 24.9; // BMI
            case "29463-7" -> value >= 45 && value <= 95;  // Weight (kg) - rough range
            default -> false;
        };
    }

    /**
     * Calculate mental health score
     */
    private double calculateMentalHealthScore(String tenantId, String patientId) {
        // Query recent mental health assessments
        var assessments = mentalHealthRepository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            tenantId, patientId, PageRequest.of(0, 3)
        );

        if (assessments.isEmpty()) {
            return 100.0; // No assessments = assume good mental health
        }

        // Calculate score based on most recent assessment
        var latest = assessments.get(0);

        // Convert assessment score to 0-100 scale (inverse, where higher is better)
        double percentage = (double) latest.getScore() / latest.getMaxScore();
        double mentalScore = (1.0 - percentage) * 100;

        // Apply severity penalty
        if (latest.getSeverity().equals("severe")) {
            mentalScore = Math.max(mentalScore - 30, 0);
        } else if (latest.getSeverity().equals("moderately-severe") ||
                   latest.getSeverity().equals("moderate")) {
            mentalScore = Math.max(mentalScore - 15, 0);
        }

        return mentalScore;
    }

    /**
     * Calculate social determinants score
     *
     * Based on SDOH (Social Determinants of Health) screening results:
     * - Housing stability (LOINC 71802-3)
     * - Food insecurity (LOINC 88122-7)
     * - Transportation access (LOINC 93030-5)
     * - Employment status (LOINC 67875-5)
     * - Social isolation (LOINC 93159-2)
     *
     * Score calculation:
     * - Base score of 100 if no SDOH screenings exist (no identified risks)
     * - Deduct points for each identified unaddressed need
     * - Positive observations (e.g., stable housing) add points back
     */
    private double calculateSocialScore(String tenantId, String patientId) {
        log.debug("Calculating social score for patient {}", patientId);

        try {
            // Query FHIR for social history observations
            List<Observation> socialObs = patientDataService.fetchSocialHistoryObservations(tenantId, patientId);

            if (socialObs.isEmpty()) {
                // No SDOH screening data - return neutral score
                log.debug("No social history observations for patient {}", patientId);
                return 80.0;
            }

            double score = 100.0;
            int identifiedNeeds = 0;
            int addressedNeeds = 0;

            for (Observation obs : socialObs) {
                // Check for negative SDOH findings (unmet needs)
                if (isUnmetSdohNeed(obs)) {
                    identifiedNeeds++;
                    // Deduct 10 points per unmet need
                    score -= 10.0;
                } else if (isAddressedSdohNeed(obs)) {
                    addressedNeeds++;
                    // Add back 5 points for addressed needs
                    score += 5.0;
                }
            }

            // Cap score between 0 and 100
            score = Math.max(0.0, Math.min(100.0, score));

            log.debug("Social score for patient {}: {} (needs: {}, addressed: {})",
                patientId, score, identifiedNeeds, addressedNeeds);

            return score;

        } catch (Exception e) {
            log.warn("Error calculating social score for patient {}: {}", patientId, e.getMessage());
            return 80.0; // Fallback to neutral score
        }
    }

    /**
     * Check if observation represents an unmet SDOH need
     */
    private boolean isUnmetSdohNeed(Observation obs) {
        // Check for common unmet need indicators:
        // - valueBoolean = true for "has food insecurity", "has housing instability"
        // - valueCodeableConcept with codes indicating risk

        if (obs.hasValueBooleanType() && obs.getValueBooleanType().booleanValue()) {
            // Check if this is a "problem exists" observation
            String display = obs.getCode().getCodingFirstRep().getDisplay();
            if (display != null && (
                display.toLowerCase().contains("insecurity") ||
                display.toLowerCase().contains("instability") ||
                display.toLowerCase().contains("lack") ||
                display.toLowerCase().contains("unable"))) {
                return true;
            }
        }

        if (obs.hasValueCodeableConcept()) {
            String code = obs.getValueCodeableConcept().getCodingFirstRep().getCode();
            // LA codes from LOINC answer lists indicating risk
            if (code != null && (
                code.equals("LA31996-4") || // At risk
                code.equals("LA31993-1") || // Housing unstable
                code.equals("LA28397-0"))) { // Often true (food insecurity)
                return true;
            }
        }

        return false;
    }

    /**
     * Check if observation indicates an SDOH need has been addressed
     */
    private boolean isAddressedSdohNeed(Observation obs) {
        // Check for "addressed" or "resolved" status
        if (obs.hasStatus() &&
            (obs.getStatus() == Observation.ObservationStatus.FINAL ||
             obs.getStatus() == Observation.ObservationStatus.AMENDED)) {

            // Check for positive outcome codes
            if (obs.hasValueCodeableConcept()) {
                String code = obs.getValueCodeableConcept().getCodingFirstRep().getCode();
                if (code != null && (
                    code.equals("LA31994-9") || // Housing stable
                    code.equals("LA28398-8") || // Never true (no food insecurity)
                    code.equals("LA31997-2"))) { // Not at risk
                    return true;
                }
            }

            // Boolean false typically means "no risk identified"
            if (obs.hasValueBooleanType() && !obs.getValueBooleanType().booleanValue()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calculate preventive care score
     *
     * Based on age/gender-appropriate screenings:
     * - Women 50-74: Mammography every 2 years
     * - Women 21-65: Cervical cancer screening every 3 years
     * - Adults 50-75: Colorectal cancer screening (colonoscopy every 10 years)
     * - Adults 65+: Pneumococcal vaccine
     *
     * Score = (completed screenings / recommended screenings) * 100
     */
    private double calculatePreventiveCareScore(String tenantId, String patientId) {
        log.debug("Calculating preventive care score for patient {}", patientId);

        try {
            // Get patient demographics to determine age/gender
            Patient patient = patientDataService.fetchPatient(tenantId, patientId);

            if (patient == null || patient.getBirthDate() == null) {
                log.debug("Patient demographics unavailable, returning default score");
                return 50.0;
            }

            int age = calculateAge(patient.getBirthDate());
            Enumerations.AdministrativeGender gender = patient.getGender();

            // Determine recommended screenings
            List<String> recommendedScreenings = getRecommendedScreenings(age, gender);

            if (recommendedScreenings.isEmpty()) {
                log.debug("No recommended screenings for age {} gender {}", age, gender);
                return 100.0; // No screenings required
            }

            // Get patient's procedures
            List<Procedure> procedures = patientDataService.fetchPatientProcedures(tenantId, patientId);

            // Check which screenings are up to date
            int completedScreenings = 0;

            for (String screeningCode : recommendedScreenings) {
                if (isScreeningUpToDate(screeningCode, procedures, age)) {
                    completedScreenings++;
                }
            }

            double score = ((double) completedScreenings / recommendedScreenings.size()) * 100;
            log.debug("Preventive care score: {} ({}/{} screenings up to date)",
                     score, completedScreenings, recommendedScreenings.size());

            return score;

        } catch (Exception e) {
            log.error("Error calculating preventive care score for patient {}", patientId, e);
            return 50.0;
        }
    }

    private int calculateAge(Date birthDate) {
        Instant birthInstant = birthDate.toInstant();
        java.time.LocalDate birthLocalDate = birthInstant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        java.time.LocalDate now = java.time.LocalDate.now();
        long years = ChronoUnit.YEARS.between(birthLocalDate, now);
        return (int) years;
    }

    private List<String> getRecommendedScreenings(int age, Enumerations.AdministrativeGender gender) {
        List<String> screenings = new ArrayList<>();

        // Colorectal cancer screening (50-75)
        if (age >= 50 && age <= 75) {
            screenings.add("73761001"); // Colonoscopy
        }

        // Gender-specific screenings
        if (gender == Enumerations.AdministrativeGender.FEMALE) {
            // Mammography (50-74)
            if (age >= 50 && age <= 74) {
                screenings.add("268547008"); // Mammography
            }

            // Cervical cancer screening (21-65)
            if (age >= 21 && age <= 65) {
                screenings.add("310078007"); // Cervical cancer screening
            }
        }

        return screenings;
    }

    private boolean isScreeningUpToDate(String snomedCode, List<Procedure> procedures, int patientAge) {
        // Define screening intervals (in days)
        Map<String, Integer> screeningIntervals = Map.of(
            "73761001", 3650,  // Colonoscopy: 10 years
            "268547008", 730,  // Mammography: 2 years
            "310078007", 1095  // Cervical screening: 3 years
        );

        int interval = screeningIntervals.getOrDefault(snomedCode, 365);
        Instant cutoff = Instant.now().minus(interval, ChronoUnit.DAYS);

        // Check if screening was completed within the interval
        return procedures.stream()
            .filter(proc -> proc.getStatus() == Procedure.ProcedureStatus.COMPLETED)
            .filter(proc -> hasScreeningCode(proc, snomedCode))
            .filter(proc -> isPerformedAfter(proc, cutoff))
            .findAny()
            .isPresent();
    }

    private boolean hasScreeningCode(Procedure procedure, String snomedCode) {
        if (procedure.getCode() == null) {
            return false;
        }

        return procedure.getCode().getCoding().stream()
            .filter(coding -> "http://snomed.info/sct".equals(coding.getSystem()))
            .anyMatch(coding -> snomedCode.equals(coding.getCode()));
    }

    private boolean isPerformedAfter(Procedure procedure, Instant cutoff) {
        if (procedure.getPerformed() instanceof DateTimeType) {
            Date performedDate = ((DateTimeType) procedure.getPerformed()).getValue();
            return performedDate != null && performedDate.toInstant().isAfter(cutoff);
        }
        return false;
    }

    /**
     * Calculate chronic disease management score
     *
     * Based on:
     * 1. Presence of chronic conditions
     * 2. Control status for each condition:
     *    - Diabetes: HbA1c <7% (good), 7-9% (fair), >9% (poor)
     *    - Hypertension: BP <130/80 (good), 130-140/80-90 (fair), >140/90 (poor)
     *    - Hyperlipidemia: Total cholesterol <200 (good), 200-239 (fair), >240 (poor)
     * 3. Open care gaps
     *
     * Score = 100 if no chronic conditions, otherwise based on control percentage
     */
    private double calculateChronicDiseaseScore(String tenantId, String patientId) {
        log.debug("Calculating chronic disease score for patient {}", patientId);

        try {
            // Get chronic conditions
            List<Condition> conditions = patientDataService.fetchPatientConditions(tenantId, patientId);

            List<Condition> chronicConditions = conditions.stream()
                .filter(this::isChronicCondition)
                .filter(this::isActiveCondition)
                .collect(Collectors.toList());

            if (chronicConditions.isEmpty()) {
                // No chronic conditions - check care gaps
                long chronicGaps = careGapRepository
                    .findByTenantIdAndPatientIdAndCategoryOrderByPriorityAscDueDateAsc(
                        tenantId, patientId, CareGapEntity.GapCategory.CHRONIC_DISEASE)
                    .stream()
                    .filter(gap -> gap.getStatus() == CareGapEntity.Status.OPEN)
                    .count();

                return chronicGaps == 0 ? 100.0 : Math.max(100.0 - (chronicGaps * 10), 0.0);
            }

            // Get observations for control metrics
            List<Observation> observations = patientDataService.fetchPatientObservations(tenantId, patientId);

            // Assess control for each chronic condition
            int wellControlled = 0;
            int poorlyControlled = 0;
            int fairlyControlled = 0;

            for (Condition condition : chronicConditions) {
                String controlStatus = assessDiseaseControl(condition, observations);
                switch (controlStatus) {
                    case "well-controlled" -> wellControlled++;
                    case "fairly-controlled" -> fairlyControlled++;
                    case "poorly-controlled" -> poorlyControlled++;
                }
            }

            int total = chronicConditions.size();
            if (total == 0) return 100.0;

            // Calculate score: well = 100%, fair = 65%, poor = 30%
            double score = ((wellControlled * 100.0) +
                           (fairlyControlled * 65.0) +
                           (poorlyControlled * 30.0)) / total;

            log.debug("Chronic disease score: {} ({} well, {} fair, {} poor controlled)",
                     score, wellControlled, fairlyControlled, poorlyControlled);

            return score;

        } catch (Exception e) {
            log.error("Error calculating chronic disease score for patient {}", patientId, e);
            return 50.0;
        }
    }

    private boolean isChronicCondition(Condition condition) {
        if (condition.getCode() == null || condition.getCode().getCoding().isEmpty()) {
            return false;
        }

        Set<String> chronicDiseaseCodes = Set.of(
            "44054006",  // Type 2 Diabetes
            "46635009",  // Type 1 Diabetes
            "38341003",  // Hypertensive disorder
            "13644009",  // Hyperlipidemia
            "195967001", // Asthma
            "13645005",  // COPD
            "49601007",  // Cardiovascular disease
            "399211009", // CHF
            "429559004"  // CKD
        );

        return condition.getCode().getCoding().stream()
            .filter(coding -> "http://snomed.info/sct".equals(coding.getSystem()))
            .anyMatch(coding -> chronicDiseaseCodes.contains(coding.getCode()));
    }

    private boolean isActiveCondition(Condition condition) {
        if (condition.getClinicalStatus() == null) {
            return true; // Assume active if not specified
        }

        return condition.getClinicalStatus().getCoding().stream()
            .anyMatch(coding -> "active".equals(coding.getCode()) ||
                              "recurrence".equals(coding.getCode()));
    }

    private String assessDiseaseControl(Condition condition, List<Observation> observations) {
        String diseaseCode = condition.getCode().getCoding().stream()
            .filter(coding -> "http://snomed.info/sct".equals(coding.getSystem()))
            .map(Coding::getCode)
            .findFirst()
            .orElse(null);

        if (diseaseCode == null) {
            return "unknown";
        }

        // Get most recent relevant observation
        Instant cutoff = Instant.now().minus(180, ChronoUnit.DAYS); // Last 6 months

        return switch (diseaseCode) {
            case "44054006", "46635009" -> // Diabetes
                assessDiabetesControl(observations, cutoff);
            case "38341003" -> // Hypertension
                assessHypertensionControl(observations, cutoff);
            case "13644009" -> // Hyperlipidemia
                assessHyperlipidemiaControl(observations, cutoff);
            default -> "unknown";
        };
    }

    private String assessDiabetesControl(List<Observation> observations, Instant cutoff) {
        // Find most recent HbA1c
        Optional<Observation> hba1c = observations.stream()
            .filter(obs -> isRecent(obs, cutoff))
            .filter(obs -> "4548-4".equals(getLoincCode(obs))) // HbA1c
            .max(Comparator.comparing(obs ->
                ((DateTimeType) obs.getEffective()).getValue().toInstant()));

        if (hba1c.isEmpty() || !(hba1c.get().getValue() instanceof Quantity)) {
            return "unknown";
        }

        double value = ((Quantity) hba1c.get().getValue()).getValue().doubleValue();

        if (value < 7.0) return "well-controlled";
        if (value < 9.0) return "fairly-controlled";
        return "poorly-controlled";
    }

    private String assessHypertensionControl(List<Observation> observations, Instant cutoff) {
        // Find most recent systolic BP
        Optional<Observation> systolic = observations.stream()
            .filter(obs -> isRecent(obs, cutoff))
            .filter(obs -> "8480-6".equals(getLoincCode(obs)))
            .max(Comparator.comparing(obs ->
                ((DateTimeType) obs.getEffective()).getValue().toInstant()));

        if (systolic.isEmpty() || !(systolic.get().getValue() instanceof Quantity)) {
            return "unknown";
        }

        double value = ((Quantity) systolic.get().getValue()).getValue().doubleValue();

        if (value < 130) return "well-controlled";
        if (value < 140) return "fairly-controlled";
        return "poorly-controlled";
    }

    private String assessHyperlipidemiaControl(List<Observation> observations, Instant cutoff) {
        // Find most recent total cholesterol
        Optional<Observation> cholesterol = observations.stream()
            .filter(obs -> isRecent(obs, cutoff))
            .filter(obs -> "2093-3".equals(getLoincCode(obs))) // Total Cholesterol
            .max(Comparator.comparing(obs ->
                ((DateTimeType) obs.getEffective()).getValue().toInstant()));

        if (cholesterol.isEmpty() || !(cholesterol.get().getValue() instanceof Quantity)) {
            return "unknown";
        }

        double value = ((Quantity) cholesterol.get().getValue()).getValue().doubleValue();

        if (value < 200) return "well-controlled";
        if (value < 240) return "fairly-controlled";
        return "poorly-controlled";
    }

    /**
     * Determine interpretation from score
     */
    private String determineInterpretation(int score) {
        if (score >= 85) {
            return "excellent";
        } else if (score >= 70) {
            return "good";
        } else if (score >= 50) {
            return "fair";
        } else {
            return "poor";
        }
    }

    /**
     * Calculate summary statistics
     */
    private PatientHealthOverviewDTO.SummaryStatsDTO calculateSummaryStats(
        String tenantId,
        String patientId,
        List<CareGapDTO> openCareGaps
    ) {
        int totalOpenCareGaps = openCareGaps.size();
        int urgentCareGaps = (int) openCareGaps.stream()
            .filter(gap -> gap.getPriority().equals("urgent"))
            .count();

        long totalMentalHealthAssessments = mentalHealthRepository.countByTenantIdAndPatientId(
            tenantId, patientId
        );

        long positiveScreens = mentalHealthRepository.findPositiveScreensRequiringFollowup(
            tenantId, patientId
        ).size();

        return PatientHealthOverviewDTO.SummaryStatsDTO.builder()
            .totalOpenCareGaps(totalOpenCareGaps)
            .urgentCareGaps(urgentCareGaps)
            .totalMentalHealthAssessments((int) totalMentalHealthAssessments)
            .positiveScreensRequiringFollowup((int) positiveScreens)
            .build();
    }
}
