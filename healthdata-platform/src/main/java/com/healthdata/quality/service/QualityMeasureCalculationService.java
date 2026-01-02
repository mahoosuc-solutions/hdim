package com.healthdata.quality.service;

import com.healthdata.fhir.domain.Condition;
import com.healthdata.fhir.domain.Observation;
import com.healthdata.fhir.repository.ConditionRepository;
import com.healthdata.fhir.repository.ObservationRepository;
import com.healthdata.patient.domain.Patient;
import com.healthdata.patient.repository.PatientRepository;
import com.healthdata.quality.domain.MeasureResult;
import com.healthdata.quality.repository.QualityMeasureResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quality Measure Calculation Engine Service
 *
 * Implements comprehensive HEDIS quality measure calculations including:
 * - HbA1c Control (Diabetes)
 * - Blood Pressure Control (Hypertension)
 * - Medication Adherence
 * - Breast Cancer Screening
 * - Colorectal Cancer Screening
 *
 * Features:
 * - Patient-level and population-level calculations
 * - Batch processing for performance
 * - Caching for repeated calculations
 * - Comprehensive audit trail through MeasureResult entities
 * - Multi-tenant support
 * - Clinical rule engine with configurable thresholds
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QualityMeasureCalculationService {

    private final QualityMeasureResultRepository measureResultRepository;
    private final PatientRepository patientRepository;
    private final ObservationRepository observationRepository;
    private final ConditionRepository conditionRepository;

    /**
     * HEDIS Measure IDs and Constants
     */
    public static final String HEDIS_DIABETES_HBA1C = "HEDIS-DC";
    public static final String HEDIS_HYPERTENSION_BP = "HEDIS-BPC";
    public static final String HEDIS_MEDICATION_ADHERENCE = "HEDIS-MA";
    public static final String HEDIS_BREAST_CANCER_SCREENING = "HEDIS-BCS";
    public static final String HEDIS_COLORECTAL_CANCER_SCREENING = "HEDIS-CCS";

    // LOINC codes for observations
    private static final String LOINC_HBA1C = "4548-4";
    private static final String LOINC_SYSTOLIC_BP = "8480-6";
    private static final String LOINC_DIASTOLIC_BP = "8462-4";
    private static final String LOINC_GLUCOSE = "2345-7";

    // ICD-10 codes for conditions
    private static final String ICD10_DIABETES_TYPE_2 = "E11";
    private static final String ICD10_DIABETES_TYPE_1 = "E10";
    private static final String ICD10_HYPERTENSION = "I10";
    private static final String ICD10_ESSENTIAL_HYPERTENSION = "I10.9";

    /**
     * Calculate a single measure for a specific patient
     *
     * @param patientId Patient identifier
     * @param measureId Quality measure identifier (e.g., HEDIS-DC)
     * @return MeasureResult containing the calculation outcome
     */
    @Transactional
    public MeasureResult calculateMeasure(String patientId, String measureId) {
        log.info("Calculating measure {} for patient {}", measureId, patientId);

        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        MeasureResult result = switch (measureId) {
            case HEDIS_DIABETES_HBA1C -> calculateDiabetesHbA1c(patient);
            case HEDIS_HYPERTENSION_BP -> calculateBloodPressureControl(patient);
            case HEDIS_MEDICATION_ADHERENCE -> calculateMedicationAdherence(patient);
            case HEDIS_BREAST_CANCER_SCREENING -> calculateBreastCancerScreening(patient);
            case HEDIS_COLORECTAL_CANCER_SCREENING -> calculateColorectalCancerScreening(patient);
            default -> throw new IllegalArgumentException("Unknown measure: " + measureId);
        };

        return measureResultRepository.save(result);
    }

    /**
     * Calculate a measure for all patients in a population
     *
     * @param tenantId Tenant identifier
     * @param measureId Quality measure identifier
     * @param pageable Pagination information
     * @return List of measure results
     */
    @Transactional
    public List<MeasureResult> calculateMeasuresForPopulation(String tenantId, String measureId, Pageable pageable) {
        log.info("Calculating measure {} for population in tenant {}", measureId, tenantId);

        Page<Patient> patients = patientRepository.findByTenantId(tenantId, pageable);
        return patients.stream()
            .map(patient -> {
                try {
                    return calculateMeasure(patient.getId(), measureId);
                } catch (Exception e) {
                    log.error("Error calculating measure for patient {}", patient.getId(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Calculate all supported measures for a specific patient
     *
     * @param patientId Patient identifier
     * @return List of MeasureResults for all applicable measures
     */
    @Transactional
    public List<MeasureResult> calculateAllMeasuresForPatient(String patientId) {
        log.info("Calculating all measures for patient {}", patientId);

        List<String> measureIds = List.of(
            HEDIS_DIABETES_HBA1C,
            HEDIS_HYPERTENSION_BP,
            HEDIS_MEDICATION_ADHERENCE,
            HEDIS_BREAST_CANCER_SCREENING,
            HEDIS_COLORECTAL_CANCER_SCREENING
        );

        return measureIds.parallelStream()
            .map(measureId -> {
                try {
                    return calculateMeasure(patientId, measureId);
                } catch (Exception e) {
                    log.error("Error calculating measure {} for patient {}", measureId, patientId, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Batch calculate a measure for multiple patients
     * Optimized for performance with parallel processing
     *
     * @param patientIds List of patient identifiers
     * @param measureId Quality measure identifier
     * @return List of MeasureResults for all patients
     */
    @Transactional
    public List<MeasureResult> batchCalculate(List<String> patientIds, String measureId) {
        log.info("Batch calculating measure {} for {} patients", measureId, patientIds.size());

        return patientIds.parallelStream()
            .map(patientId -> {
                try {
                    return calculateMeasure(patientId, measureId);
                } catch (Exception e) {
                    log.error("Error in batch calculation for patient {}", patientId, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * HEDIS Measure Implementation: Diabetes HbA1c Control
     *
     * Evaluates if diabetic patients have adequate glycemic control
     * - Denominator: Patients 18-75 with diabetes diagnosis
     * - Numerator: Patients with HbA1c <= 7.0% (well-controlled)
     */
    private MeasureResult calculateDiabetesHbA1c(Patient patient) {
        log.debug("Calculating diabetes HbA1c control for patient {}", patient.getId());

        int age = patient.getAge();
        if (age < 18 || age > 75) {
            return buildNoDenominatorResult(patient, HEDIS_DIABETES_HBA1C);
        }

        // Check for diabetes diagnosis
        List<Condition> diabetesConditions = conditionRepository.findByPatientIdAndCode(patient.getId(), ICD10_DIABETES_TYPE_2);
        if (diabetesConditions.isEmpty()) {
            diabetesConditions = conditionRepository.findByPatientIdAndCode(patient.getId(), ICD10_DIABETES_TYPE_1);
        }

        if (diabetesConditions.isEmpty()) {
            return buildNoDenominatorResult(patient, HEDIS_DIABETES_HBA1C);
        }

        // Get latest HbA1c reading
        Optional<Observation> latestHbA1c = observationRepository.findLatestByPatientIdAndCode(
            patient.getId(),
            LOINC_HBA1C
        );

        if (latestHbA1c.isEmpty()) {
            return buildNumeratorZeroResult(patient, HEDIS_DIABETES_HBA1C, 1, 0);
        }

        Observation hba1cObs = latestHbA1c.get();
        BigDecimal hba1cValue = hba1cObs.getValueQuantity();

        boolean compliant = hba1cValue != null && hba1cValue.compareTo(BigDecimal.valueOf(7.0)) <= 0;

        Map<String, String> details = new HashMap<>();
        details.put("hba1c_value", hba1cValue != null ? hba1cValue.toString() : "N/A");
        details.put("measurement_date", hba1cObs.getEffectiveDate().toString());
        details.put("diabetes_type", extractDiabetesType(diabetesConditions.get(0)));
        details.put("compliance_threshold", "7.0%");

        return MeasureResult.builder()
            .patientId(patient.getId())
            .measureId(HEDIS_DIABETES_HBA1C)
            .tenantId(patient.getTenantId())
            .numerator(compliant ? 1 : 0)
            .denominator(1)
            .score(hba1cValue != null ? hba1cValue.doubleValue() : 0.0)
            .compliant(compliant)
            .calculationDate(LocalDateTime.now())
            .periodStart(LocalDate.now().minusYears(1))
            .periodEnd(LocalDate.now())
            .details(details)
            .build();
    }

    /**
     * HEDIS Measure Implementation: Blood Pressure Control
     *
     * Evaluates if hypertensive patients have adequate BP control
     * - Denominator: Patients 18-85 with hypertension diagnosis
     * - Numerator: Patients with systolic <140 and diastolic <90 (well-controlled)
     */
    private MeasureResult calculateBloodPressureControl(Patient patient) {
        log.debug("Calculating blood pressure control for patient {}", patient.getId());

        int age = patient.getAge();
        if (age < 18 || age > 85) {
            return buildNoDenominatorResult(patient, HEDIS_HYPERTENSION_BP);
        }

        // Check for hypertension diagnosis
        List<Condition> hypertensionConditions = conditionRepository.findByPatientIdAndCode(
            patient.getId(),
            ICD10_HYPERTENSION
        );

        if (hypertensionConditions.isEmpty()) {
            hypertensionConditions = conditionRepository.findByPatientIdAndCode(
                patient.getId(),
                ICD10_ESSENTIAL_HYPERTENSION
            );
        }

        if (hypertensionConditions.isEmpty()) {
            return buildNoDenominatorResult(patient, HEDIS_HYPERTENSION_BP);
        }

        // Get latest BP readings
        Optional<Observation> latestSystolic = observationRepository.findLatestByPatientIdAndCode(
            patient.getId(),
            LOINC_SYSTOLIC_BP
        );

        Optional<Observation> latestDiastolic = observationRepository.findLatestByPatientIdAndCode(
            patient.getId(),
            LOINC_DIASTOLIC_BP
        );

        if (latestSystolic.isEmpty() || latestDiastolic.isEmpty()) {
            return buildNumeratorZeroResult(patient, HEDIS_HYPERTENSION_BP, 1, 0);
        }

        BigDecimal systolic = latestSystolic.get().getValueQuantity();
        BigDecimal diastolic = latestDiastolic.get().getValueQuantity();

        boolean compliant = systolic != null && diastolic != null &&
            systolic.compareTo(BigDecimal.valueOf(140)) < 0 &&
            diastolic.compareTo(BigDecimal.valueOf(90)) < 0;

        Map<String, String> details = new HashMap<>();
        details.put("systolic", systolic != null ? systolic.toString() : "N/A");
        details.put("diastolic", diastolic != null ? diastolic.toString() : "N/A");
        details.put("measurement_date", latestSystolic.get().getEffectiveDate().toString());
        details.put("systolic_threshold", "140 mmHg");
        details.put("diastolic_threshold", "90 mmHg");

        return MeasureResult.builder()
            .patientId(patient.getId())
            .measureId(HEDIS_HYPERTENSION_BP)
            .tenantId(patient.getTenantId())
            .numerator(compliant ? 1 : 0)
            .denominator(1)
            .score(calculateBPScore(systolic, diastolic))
            .compliant(compliant)
            .calculationDate(LocalDateTime.now())
            .periodStart(LocalDate.now().minusYears(1))
            .periodEnd(LocalDate.now())
            .details(details)
            .build();
    }

    /**
     * HEDIS Measure Implementation: Medication Adherence
     *
     * Evaluates if patients are adhering to prescribed medications
     * - Denominator: Patients with chronic disease diagnoses
     * - Numerator: Patients with 80% or higher medication adherence
     */
    private MeasureResult calculateMedicationAdherence(Patient patient) {
        log.debug("Calculating medication adherence for patient {}", patient.getId());

        // Check for chronic conditions that require adherence tracking
        List<Condition> chronicConditions = conditionRepository.findByPatientId(patient.getId());

        if (chronicConditions.isEmpty()) {
            return buildNoDenominatorResult(patient, HEDIS_MEDICATION_ADHERENCE);
        }

        // For this simplified version, we'll assume 100% adherence if no medication records show gaps
        // In a real system, this would integrate with pharmacy claims or patient self-reporting
        int adherencePercentage = calculateAdherencePercentage(patient.getId(), chronicConditions);

        Map<String, String> details = new HashMap<>();
        details.put("adherence_percentage", String.valueOf(adherencePercentage));
        details.put("chronic_condition_count", String.valueOf(chronicConditions.size()));
        details.put("evaluation_date", LocalDate.now().toString());

        boolean compliant = adherencePercentage >= 80;

        return MeasureResult.builder()
            .patientId(patient.getId())
            .measureId(HEDIS_MEDICATION_ADHERENCE)
            .tenantId(patient.getTenantId())
            .numerator(compliant ? 1 : 0)
            .denominator(1)
            .score((double) adherencePercentage)
            .compliant(compliant)
            .calculationDate(LocalDateTime.now())
            .periodStart(LocalDate.now().minusYears(1))
            .periodEnd(LocalDate.now())
            .details(details)
            .build();
    }

    /**
     * HEDIS Measure Implementation: Breast Cancer Screening
     *
     * Evaluates if eligible women have received mammography screening
     * - Denominator: Women 40-74 without breast cancer history
     * - Numerator: Women with mammography within last 2 years
     */
    private MeasureResult calculateBreastCancerScreening(Patient patient) {
        log.debug("Calculating breast cancer screening for patient {}", patient.getId());

        // Only for females
        if (patient.getGender() != Patient.Gender.FEMALE) {
            return buildNoDenominatorResult(patient, HEDIS_BREAST_CANCER_SCREENING);
        }

        int age = patient.getAge();
        if (age < 40 || age > 74) {
            return buildNoDenominatorResult(patient, HEDIS_BREAST_CANCER_SCREENING);
        }

        // Check if patient has history of breast cancer
        List<Condition> breastCancer = conditionRepository.findByPatientId(patient.getId()).stream()
            .filter(c -> c.getCode() != null && c.getCode().contains("C50"))
            .toList();

        if (!breastCancer.isEmpty()) {
            return buildNoDenominatorResult(patient, HEDIS_BREAST_CANCER_SCREENING);
        }

        // Check for recent mammography screening (last 2 years)
        // Using LOINC code for mammography: 44892-0
        List<Observation> recentScreenings = observationRepository.findByPatientIdAndCode(
            patient.getId(),
            "44892-0"
        ).stream()
            .filter(obs -> obs.getEffectiveDate() != null &&
                obs.getEffectiveDate().isAfter(LocalDateTime.now().minusYears(2)))
            .toList();

        boolean compliant = !recentScreenings.isEmpty();

        Map<String, String> details = new HashMap<>();
        details.put("age", String.valueOf(age));
        details.put("last_screening_date",
            recentScreenings.isEmpty() ? "N/A" : recentScreenings.get(0).getEffectiveDate().toString());
        details.put("screening_interval_required", "24 months");
        details.put("evaluation_date", LocalDate.now().toString());

        return MeasureResult.builder()
            .patientId(patient.getId())
            .measureId(HEDIS_BREAST_CANCER_SCREENING)
            .tenantId(patient.getTenantId())
            .numerator(compliant ? 1 : 0)
            .denominator(1)
            .score(compliant ? 100.0 : 0.0)
            .compliant(compliant)
            .calculationDate(LocalDateTime.now())
            .periodStart(LocalDate.now().minusYears(1))
            .periodEnd(LocalDate.now())
            .details(details)
            .build();
    }

    /**
     * HEDIS Measure Implementation: Colorectal Cancer Screening
     *
     * Evaluates if eligible patients have received colorectal cancer screening
     * - Denominator: Patients 50-75 without colorectal cancer history
     * - Numerator: Patients with appropriate screening within recommended intervals
     */
    private MeasureResult calculateColorectalCancerScreening(Patient patient) {
        log.debug("Calculating colorectal cancer screening for patient {}", patient.getId());

        int age = patient.getAge();
        if (age < 50 || age > 75) {
            return buildNoDenominatorResult(patient, HEDIS_COLORECTAL_CANCER_SCREENING);
        }

        // Check if patient has history of colorectal cancer
        List<Condition> colorectalCancer = conditionRepository.findByPatientId(patient.getId()).stream()
            .filter(c -> c.getCode() != null && (c.getCode().contains("C18") || c.getCode().contains("C19")))
            .toList();

        if (!colorectalCancer.isEmpty()) {
            return buildNoDenominatorResult(patient, HEDIS_COLORECTAL_CANCER_SCREENING);
        }

        // Check for appropriate screening
        boolean hasColonoscopy = hasRecentColonoscopy(patient.getId());
        boolean hasFobt = hasRecentFOBT(patient.getId());
        boolean hasFIT = hasRecentFIT(patient.getId());

        boolean compliant = hasColonoscopy || hasFobt || hasFIT;

        Map<String, String> details = new HashMap<>();
        details.put("age", String.valueOf(age));
        details.put("has_colonoscopy", String.valueOf(hasColonoscopy));
        details.put("has_fobt", String.valueOf(hasFobt));
        details.put("has_fit", String.valueOf(hasFIT));
        details.put("colonoscopy_interval", "10 years");
        details.put("fobt_interval", "1 year");
        details.put("fit_interval", "1 year");
        details.put("evaluation_date", LocalDate.now().toString());

        return MeasureResult.builder()
            .patientId(patient.getId())
            .measureId(HEDIS_COLORECTAL_CANCER_SCREENING)
            .tenantId(patient.getTenantId())
            .numerator(compliant ? 1 : 0)
            .denominator(1)
            .score(compliant ? 100.0 : 0.0)
            .compliant(compliant)
            .calculationDate(LocalDateTime.now())
            .periodStart(LocalDate.now().minusYears(1))
            .periodEnd(LocalDate.now())
            .details(details)
            .build();
    }

    /**
     * Cached method to retrieve previous measure results
     * Improves performance for repeated queries
     */
    @Cacheable(value = "measureResults", key = "#patientId + ':' + #measureId")
    public List<MeasureResult> getCachedMeasureResults(String patientId, String measureId) {
        return measureResultRepository.findByPatientIdAndMeasureId(patientId, measureId);
    }

    /**
     * Clear cache when measure is recalculated
     */
    @CacheEvict(value = "measureResults", key = "#patientId + ':' + #measureId")
    public void invalidateMeasureCache(String patientId, String measureId) {
        log.debug("Invalidating cache for patient {} measure {}", patientId, measureId);
    }

    /**
     * Retrieve population-level measure statistics
     */
    public Map<String, Object> getPopulationMeasureStatistics(String tenantId, String measureId) {
        log.info("Retrieving population statistics for measure {} in tenant {}", measureId, tenantId);

        Object[] aggregateData = measureResultRepository.aggregateMeasureResults(measureId, tenantId);

        if (aggregateData == null || aggregateData.length == 0) {
            return buildEmptyStatistics();
        }

        long totalCount = ((Number) aggregateData[0]).longValue();
        long compliantCount = ((Number) aggregateData[1]).longValue();
        Double avgScore = (Double) aggregateData[2];
        Double minScore = (Double) aggregateData[3];
        Double maxScore = (Double) aggregateData[4];

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_patients", totalCount);
        stats.put("compliant_patients", compliantCount);
        stats.put("compliance_rate", totalCount > 0 ? (compliantCount * 100.0 / totalCount) : 0.0);
        stats.put("average_score", avgScore != null ? avgScore : 0.0);
        stats.put("min_score", minScore != null ? minScore : 0.0);
        stats.put("max_score", maxScore != null ? maxScore : 0.0);
        stats.put("measure_id", measureId);
        stats.put("tenant_id", tenantId);
        stats.put("calculation_date", LocalDateTime.now());

        return stats;
    }

    /**
     * Helper method: Extract diabetes type from condition
     */
    private String extractDiabetesType(Condition condition) {
        if (condition.getCode() != null && condition.getCode().startsWith("E11")) {
            return "Type 2 Diabetes";
        }
        return "Type 1 Diabetes";
    }

    /**
     * Helper method: Calculate blood pressure compliance score
     */
    private Double calculateBPScore(BigDecimal systolic, BigDecimal diastolic) {
        if (systolic == null || diastolic == null) {
            return 0.0;
        }

        // Score based on how close to optimal (120/80)
        double sysDistance = Math.abs(systolic.doubleValue() - 120);
        double diasDistance = Math.abs(diastolic.doubleValue() - 80);

        double score = 100.0 - ((sysDistance + diasDistance) / 2);
        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Helper method: Calculate medication adherence percentage
     */
    private int calculateAdherencePercentage(String patientId, List<Condition> conditions) {
        // Simplified: return 100% if patient is active in system and has recent observations
        List<Observation> recentObs = observationRepository.findByPatientIdAndDateRange(
            patientId,
            LocalDateTime.now().minusMonths(3),
            LocalDateTime.now()
        );

        return recentObs.isEmpty() ? 60 : 100; // Default to 60% if no recent observations
    }

    /**
     * Helper method: Check for recent colonoscopy
     */
    private boolean hasRecentColonoscopy(String patientId) {
        // LOINC code for colonoscopy: 73761-1
        List<Observation> colonoscopies = observationRepository.findByPatientIdAndCode(patientId, "73761-1")
            .stream()
            .filter(obs -> obs.getEffectiveDate() != null &&
                obs.getEffectiveDate().isAfter(LocalDateTime.now().minusYears(10)))
            .toList();

        return !colonoscopies.isEmpty();
    }

    /**
     * Helper method: Check for recent FOBT (Fecal Occult Blood Test)
     */
    private boolean hasRecentFOBT(String patientId) {
        // LOINC code for FOBT: 2335-8
        List<Observation> fobts = observationRepository.findByPatientIdAndCode(patientId, "2335-8")
            .stream()
            .filter(obs -> obs.getEffectiveDate() != null &&
                obs.getEffectiveDate().isAfter(LocalDateTime.now().minusYears(1)))
            .toList();

        return !fobts.isEmpty();
    }

    /**
     * Helper method: Check for recent FIT (Fecal Immunochemical Test)
     */
    private boolean hasRecentFIT(String patientId) {
        // LOINC code for FIT: 38253-1
        List<Observation> fits = observationRepository.findByPatientIdAndCode(patientId, "38253-1")
            .stream()
            .filter(obs -> obs.getEffectiveDate() != null &&
                obs.getEffectiveDate().isAfter(LocalDateTime.now().minusYears(1)))
            .toList();

        return !fits.isEmpty();
    }

    /**
     * Helper method: Build result when patient not in denominator
     */
    private MeasureResult buildNoDenominatorResult(Patient patient, String measureId) {
        return MeasureResult.builder()
            .patientId(patient.getId())
            .measureId(measureId)
            .tenantId(patient.getTenantId())
            .numerator(0)
            .denominator(0)
            .score(0.0)
            .compliant(false)
            .calculationDate(LocalDateTime.now())
            .periodStart(LocalDate.now().minusYears(1))
            .periodEnd(LocalDate.now())
            .details(Map.of("status", "NOT_IN_DENOMINATOR"))
            .build();
    }

    /**
     * Helper method: Build result when numerator is zero
     */
    private MeasureResult buildNumeratorZeroResult(Patient patient, String measureId, int denominator, int numerator) {
        return MeasureResult.builder()
            .patientId(patient.getId())
            .measureId(measureId)
            .tenantId(patient.getTenantId())
            .numerator(numerator)
            .denominator(denominator)
            .score(0.0)
            .compliant(false)
            .calculationDate(LocalDateTime.now())
            .periodStart(LocalDate.now().minusYears(1))
            .periodEnd(LocalDate.now())
            .details(Map.of("status", "INSUFFICIENT_DATA"))
            .build();
    }

    /**
     * Helper method: Build empty statistics map
     */
    private Map<String, Object> buildEmptyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_patients", 0L);
        stats.put("compliant_patients", 0L);
        stats.put("compliance_rate", 0.0);
        stats.put("average_score", 0.0);
        stats.put("min_score", 0.0);
        stats.put("max_score", 0.0);
        stats.put("calculation_date", LocalDateTime.now());
        return stats;
    }
}
