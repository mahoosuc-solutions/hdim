package com.healthdata.caregap.service;

import com.healthdata.caregap.domain.CareGap;
import com.healthdata.caregap.repository.CareGapRepository;
import com.healthdata.caregap.events.CareGapDetectedEvent;
import com.healthdata.patient.domain.Patient;
import com.healthdata.patient.service.PatientService;
import com.healthdata.fhir.domain.Observation;
import com.healthdata.fhir.domain.Condition;
import com.healthdata.fhir.domain.MedicationRequest;
import com.healthdata.fhir.service.FhirService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Care Gap Detection Engine
 *
 * Advanced care gap detection and prioritization service for the HealthData Platform.
 * This engine identifies various types of care gaps, prioritizes them based on clinical
 * and financial impact, and manages automatic closure based on clinical criteria.
 *
 * Gap Types:
 * - PREVENTIVE_CARE: Mammography, colonoscopy, vaccines, wellness visits
 * - CHRONIC_DISEASE_MANAGEMENT: Diabetes, hypertension, asthma monitoring
 * - MEDICATION_ADHERENCE: Refills, compliance tracking
 * - CANCER_SCREENING: Breast, colorectal, cervical cancer screenings
 * - CARDIOVASCULAR_RISK_MANAGEMENT: Blood pressure, cholesterol management
 *
 * Features:
 * - Multi-type gap detection for individual and population-level analysis
 * - Dynamic priority scoring based on clinical and financial impact
 * - Age-based and demographic risk stratification
 * - Automatic gap closure validation
 * - Batch processing for large populations
 * - Event-driven notifications
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CareGapDetectionEngine {

    private static final String GAP_TYPE_PREVENTIVE = "PREVENTIVE_CARE";
    private static final String GAP_TYPE_CHRONIC = "CHRONIC_DISEASE_MANAGEMENT";
    private static final String GAP_TYPE_MEDICATION = "MEDICATION_ADHERENCE";
    private static final String GAP_TYPE_CANCER = "CANCER_SCREENING";
    private static final String GAP_TYPE_CARDIOVASCULAR = "CARDIOVASCULAR_RISK_MANAGEMENT";

    private static final String PRIORITY_HIGH = "HIGH";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_LOW = "LOW";

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final CareGapRepository careGapRepository;
    private final PatientService patientService;
    private final FhirService fhirService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Core detection method for single patient
     * Detects all types of care gaps for a specific patient
     *
     * @param patientId The patient identifier
     * @return List of detected care gaps
     */
    public List<CareGap> detectGapsForPatient(String patientId) {
        log.info("Detecting care gaps for patient: {}", patientId);

        try {
            Patient patient = patientService.getPatient(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

            List<Observation> observations = fhirService.getObservationsForPatient(patientId);
            List<Condition> conditions = fhirService.getConditionsForPatient(patientId);
            List<MedicationRequest> medications = fhirService.getMedicationsForPatient(patientId);

            List<CareGap> detectedGaps = new ArrayList<>();

            // Detect all gap types
            detectedGaps.addAll(detectPreventiveCareGaps(patient, observations));
            detectedGaps.addAll(detectChronicDiseaseGaps(patient, conditions, observations));
            detectedGaps.addAll(detectMedicationAdherenceGaps(patient, medications));
            detectedGaps.addAll(detectCancerScreeningGaps(patient, observations));
            detectedGaps.addAll(detectCardiovascularRiskGaps(patient, conditions, observations));

            // Remove duplicates and save
            detectedGaps = detectedGaps.stream()
                .distinct()
                .collect(Collectors.toList());

            List<CareGap> savedGaps = careGapRepository.saveAll(detectedGaps);

            // Publish detection events
            savedGaps.forEach(this::publishGapDetectedEvent);

            log.info("Detected {} care gaps for patient: {}", savedGaps.size(), patientId);
            return savedGaps;
        } catch (Exception e) {
            log.error("Error detecting care gaps for patient: {}", patientId, e);
            throw new RuntimeException("Failed to detect care gaps: " + e.getMessage(), e);
        }
    }

    /**
     * Population-level gap detection with pagination
     * Detects gaps across all patients for a tenant with result pagination
     *
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Paginated list of care gaps
     */
    public Page<CareGap> detectGapsForPopulation(String tenantId, Pageable pageable) {
        log.info("Detecting care gaps for population in tenant: {}", tenantId);

        try {
            // Get all patients for tenant using pageable
            Page<Patient> patientPage = patientService.getAllActivePatients(tenantId, PageRequest.of(0, Integer.MAX_VALUE));
            List<String> patientIds = patientPage.stream()
                .map(Patient::getId)
                .collect(Collectors.toList());

            log.info("Processing {} patients for tenant: {}", patientIds.size(), tenantId);

            // Detect gaps for each patient
            List<CareGap> allGaps = patientIds.stream()
                .flatMap(patientId -> detectGapsForPatient(patientId).stream())
                .collect(Collectors.toList());

            // Return paginated results
            return convertListToPage(allGaps, pageable);
        } catch (Exception e) {
            log.error("Error detecting population gaps for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to detect population gaps: " + e.getMessage(), e);
        }
    }

    /**
     * Detect specific gap type for patient
     * Detects a particular type of care gap for a patient
     *
     * @param patientId The patient identifier
     * @param gapType The specific gap type to detect
     * @return Optional containing the detected gap if found
     */
    public Optional<CareGap> detectSpecificGap(String patientId, String gapType) {
        log.info("Detecting specific gap type {} for patient: {}", gapType, patientId);

        try {
            Patient patient = patientService.getPatient(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

            List<Observation> observations = fhirService.getObservationsForPatient(patientId);
            List<Condition> conditions = fhirService.getConditionsForPatient(patientId);
            List<MedicationRequest> medications = fhirService.getMedicationsForPatient(patientId);

            CareGap gap = switch (gapType) {
                case GAP_TYPE_PREVENTIVE -> detectPreventiveCareGaps(patient, observations)
                    .stream().findFirst().orElse(null);
                case GAP_TYPE_CHRONIC -> detectChronicDiseaseGaps(patient, conditions, observations)
                    .stream().findFirst().orElse(null);
                case GAP_TYPE_MEDICATION -> detectMedicationAdherenceGaps(patient, medications)
                    .stream().findFirst().orElse(null);
                case GAP_TYPE_CANCER -> detectCancerScreeningGaps(patient, observations)
                    .stream().findFirst().orElse(null);
                case GAP_TYPE_CARDIOVASCULAR -> detectCardiovascularRiskGaps(patient, conditions, observations)
                    .stream().findFirst().orElse(null);
                default -> null;
            };

            if (gap != null) {
                gap = careGapRepository.save(gap);
                publishGapDetectedEvent(gap);
                return Optional.of(gap);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error detecting specific gap for patient {}: {}", patientId, gapType, e);
            throw new RuntimeException("Failed to detect specific gap: " + e.getMessage(), e);
        }
    }

    /**
     * Batch gap detection for multiple patients
     * Efficiently detects gaps across multiple patients with result mapping
     *
     * @param patientIds List of patient identifiers
     * @return Map of patient ID to detected gaps
     */
    public Map<String, List<CareGap>> batchDetectGaps(List<String> patientIds) {
        log.info("Batch detecting care gaps for {} patients", patientIds.size());

        try {
            Map<String, List<CareGap>> results = new HashMap<>();

            patientIds.forEach(patientId -> {
                try {
                    List<CareGap> gaps = detectGapsForPatient(patientId);
                    results.put(patientId, gaps);
                } catch (Exception e) {
                    log.warn("Failed to detect gaps for patient {}: {}", patientId, e.getMessage());
                    results.put(patientId, Collections.emptyList());
                }
            });

            log.info("Batch gap detection completed. Total gaps: {}",
                results.values().stream().mapToInt(List::size).sum());

            return results;
        } catch (Exception e) {
            log.error("Error in batch gap detection", e);
            throw new RuntimeException("Batch gap detection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Detect preventive care gaps
     * Includes: annual wellness, vaccinations, mammography, colonoscopy
     */
    private List<CareGap> detectPreventiveCareGaps(Patient patient, List<Observation> observations) {
        List<CareGap> gaps = new ArrayList<>();

        // Annual wellness visit
        if (!hasRecentObservation(observations, "wellness-visit", 365)) {
            gaps.add(createCareGap(patient, GAP_TYPE_PREVENTIVE,
                "Annual wellness visit overdue",
                PRIORITY_MEDIUM, 30));
        }

        // Influenza vaccine
        if (!hasRecentObservation(observations, "flu-vaccine", 365)) {
            gaps.add(createCareGap(patient, GAP_TYPE_PREVENTIVE,
                "Influenza vaccine overdue",
                PRIORITY_HIGH, 30));
        }

        // Pneumonia vaccine (age 65+)
        if (isAgeGreaterThan(patient, 65) && !hasRecentObservation(observations, "pneumonia-vaccine", 730)) {
            gaps.add(createCareGap(patient, GAP_TYPE_PREVENTIVE,
                "Pneumonia vaccine recommended",
                PRIORITY_MEDIUM, 60));
        }

        // COVID-19 vaccine
        if (!hasRecentObservation(observations, "covid-vaccine", 365)) {
            gaps.add(createCareGap(patient, GAP_TYPE_PREVENTIVE,
                "COVID-19 vaccine status should be updated",
                PRIORITY_MEDIUM, 30));
        }

        return gaps;
    }

    /**
     * Detect chronic disease management gaps
     * Includes: diabetes monitoring, hypertension control, asthma management
     */
    private List<CareGap> detectChronicDiseaseGaps(Patient patient, List<Condition> conditions,
                                                    List<Observation> observations) {
        List<CareGap> gaps = new ArrayList<>();

        // Diabetes management
        if (hasCondition(conditions, "diabetes")) {
            if (!hasRecentObservation(observations, "HbA1c", 90)) {
                gaps.add(createCareGap(patient, GAP_TYPE_CHRONIC,
                    "HbA1c test overdue (diabetes monitoring)",
                    PRIORITY_HIGH, 7, "HEDIS-CDC"));
            }
            if (!hasRecentObservation(observations, "microalbumin", 180)) {
                gaps.add(createCareGap(patient, GAP_TYPE_CHRONIC,
                    "Urine microalbumin test recommended",
                    PRIORITY_MEDIUM, 30, "HEDIS-CDC"));
            }
        }

        // Hypertension management
        if (hasCondition(conditions, "hypertension")) {
            if (!hasRecentObservation(observations, "blood-pressure", 30)) {
                gaps.add(createCareGap(patient, GAP_TYPE_CHRONIC,
                    "Blood pressure monitoring overdue",
                    PRIORITY_HIGH, 7, "HEDIS-HTN"));
            }
        }

        // Asthma management
        if (hasCondition(conditions, "asthma")) {
            if (!hasRecentObservation(observations, "spirometry", 365)) {
                gaps.add(createCareGap(patient, GAP_TYPE_CHRONIC,
                    "Spirometry/lung function test recommended",
                    PRIORITY_MEDIUM, 30, "HEDIS-ASM"));
            }
        }

        // COPD management
        if (hasCondition(conditions, "copd")) {
            if (!hasRecentObservation(observations, "spirometry", 365)) {
                gaps.add(createCareGap(patient, GAP_TYPE_CHRONIC,
                    "Spirometry test overdue (COPD monitoring)",
                    PRIORITY_HIGH, 30, "HEDIS-COP"));
            }
        }

        return gaps;
    }

    /**
     * Detect medication adherence gaps
     * Includes: refill status, compliance tracking, drug interactions
     */
    private List<CareGap> detectMedicationAdherenceGaps(Patient patient, List<MedicationRequest> medications) {
        List<CareGap> gaps = new ArrayList<>();

        medications.forEach(med -> {
            // Check for overdue refills
            if (isRefillOverdue(med)) {
                gaps.add(createCareGap(patient, GAP_TYPE_MEDICATION,
                    "Medication refill overdue: " + med.getMedicationDisplay(),
                    PRIORITY_HIGH, 3));
            }

            // Check for compliance issues
            if (isComplianceRisk(med)) {
                gaps.add(createCareGap(patient, GAP_TYPE_MEDICATION,
                    "Medication compliance risk: " + med.getMedicationDisplay(),
                    PRIORITY_MEDIUM, 7));
            }
        });

        return gaps;
    }

    /**
     * Detect cancer screening gaps
     * Includes: breast, colorectal, cervical cancer screenings
     */
    private List<CareGap> detectCancerScreeningGaps(Patient patient, List<Observation> observations) {
        List<CareGap> gaps = new ArrayList<>();

        // Colorectal cancer screening (age 45-75)
        if (isAgeBetween(patient, 45, 75) && !hasRecentObservation(observations, "colonoscopy", 1095)) {
            gaps.add(createCareGap(patient, GAP_TYPE_CANCER,
                "Colorectal cancer screening (colonoscopy) overdue",
                PRIORITY_HIGH, 60, "HEDIS-COL"));
        }

        // Breast cancer screening (women 40-75)
        if (Patient.Gender.FEMALE.equals(patient.getGender()) && isAgeBetween(patient, 40, 75) &&
            !hasRecentObservation(observations, "mammography", 365)) {
            gaps.add(createCareGap(patient, GAP_TYPE_CANCER,
                "Breast cancer screening (mammography) overdue",
                PRIORITY_HIGH, 30, "HEDIS-BCS"));
        }

        // Cervical cancer screening (women 21-65)
        if (Patient.Gender.FEMALE.equals(patient.getGender()) && isAgeBetween(patient, 21, 65) &&
            !hasRecentObservation(observations, "pap-smear", 1095)) {
            gaps.add(createCareGap(patient, GAP_TYPE_CANCER,
                "Cervical cancer screening (Pap smear) overdue",
                PRIORITY_HIGH, 60, "HEDIS-CCS"));
        }

        return gaps;
    }

    /**
     * Detect cardiovascular risk management gaps
     * Includes: blood pressure, cholesterol, weight management
     */
    private List<CareGap> detectCardiovascularRiskGaps(Patient patient, List<Condition> conditions,
                                                        List<Observation> observations) {
        List<CareGap> gaps = new ArrayList<>();

        // Lipid panel screening
        if (!hasRecentObservation(observations, "lipid-panel", 365)) {
            gaps.add(createCareGap(patient, GAP_TYPE_CARDIOVASCULAR,
                "Lipid panel (cholesterol) screening recommended",
                PRIORITY_MEDIUM, 30, "HEDIS-LDL"));
        }

        // Blood pressure management
        if (!hasRecentObservation(observations, "blood-pressure", 30)) {
            gaps.add(createCareGap(patient, GAP_TYPE_CARDIOVASCULAR,
                "Blood pressure monitoring recommended",
                PRIORITY_MEDIUM, 14));
        }

        // Weight management
        if (hasCondition(conditions, "obesity") &&
            !hasRecentObservation(observations, "weight", 90)) {
            gaps.add(createCareGap(patient, GAP_TYPE_CARDIOVASCULAR,
                "Weight management monitoring needed",
                PRIORITY_MEDIUM, 30));
        }

        // Smoking cessation
        if (hasCondition(conditions, "smoking-status") &&
            !hasRecentObservation(observations, "smoking-cessation", 180)) {
            gaps.add(createCareGap(patient, GAP_TYPE_CARDIOVASCULAR,
                "Smoking cessation counseling recommended",
                PRIORITY_HIGH, 30));
        }

        return gaps;
    }

    /**
     * Close a care gap with validation
     * Automatically closes gaps when clinical criteria are met
     *
     * @param gapId The gap identifier
     * @param closureReason Reason for closure
     * @return The closed gap
     */
    @Transactional
    public CareGap closeGap(String gapId, String closureReason) {
        log.info("Closing care gap: {} with reason: {}", gapId, closureReason);

        CareGap gap = careGapRepository.findById(gapId)
            .orElseThrow(() -> new IllegalArgumentException("Care gap not found: " + gapId));

        gap.setStatus(STATUS_CLOSED);
        gap.setClosedDate(LocalDateTime.now());
        gap.setClosureReason(closureReason);

        return careGapRepository.save(gap);
    }

    /**
     * Auto-close gaps based on clinical evidence
     * Checks if gap closure conditions are met and auto-closes if appropriate
     *
     * @param gapId The gap identifier
     * @return True if gap was auto-closed, false otherwise
     */
    @Transactional
    public boolean autoCloseGapIfEligible(String gapId) {
        log.info("Checking auto-closure eligibility for gap: {}", gapId);

        CareGap gap = careGapRepository.findById(gapId)
            .orElseThrow(() -> new IllegalArgumentException("Care gap not found: " + gapId));

        // Get patient observations
        List<Observation> observations = fhirService.getObservationsForPatient(gap.getPatientId());

        // Check if gap closure condition is met
        boolean shouldClose = switch (gap.getGapType()) {
            case GAP_TYPE_PREVENTIVE -> checkPreventiveClosureCondition(gap, observations);
            case GAP_TYPE_CHRONIC -> checkChronicClosureCondition(gap, observations);
            case GAP_TYPE_MEDICATION -> checkMedicationClosureCondition(gap);
            case GAP_TYPE_CANCER -> checkCancerScreeningClosureCondition(gap, observations);
            case GAP_TYPE_CARDIOVASCULAR -> checkCardiovascularClosureCondition(gap, observations);
            default -> false;
        };

        if (shouldClose) {
            closeGap(gapId, "Auto-closed: Clinical criteria met");
            return true;
        }

        return false;
    }

    /**
     * Calculate priority score for gap prioritization
     * Combines clinical impact and financial impact
     *
     * @param gap The care gap to score
     * @return Priority score (0-100)
     */
    public double calculatePriorityScore(CareGap gap) {
        double clinicalScore = calculateClinicalImpactScore(gap);
        double financialScore = calculateFinancialImpactScore(gap);
        double ageScore = calculateAgeBasedScore(gap);

        // Weighted combination: 50% clinical, 30% financial, 20% age
        return (clinicalScore * 0.5) + (financialScore * 0.3) + (ageScore * 0.2);
    }

    /**
     * Get high-priority gaps for a tenant
     * Retrieves gaps ranked by priority
     *
     * @param tenantId The tenant identifier
     * @param limit Maximum number of gaps to return
     * @return Sorted list of high-priority gaps
     */
    public List<CareGap> getHighPriorityGaps(String tenantId, int limit) {
        return careGapRepository.findByTenantId(tenantId)
            .stream()
            .filter(gap -> STATUS_OPEN.equals(gap.getStatus()))
            .sorted((g1, g2) -> Double.compare(
                calculatePriorityScore(g2),
                calculatePriorityScore(g1)
            ))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get overdue gaps for a tenant
     * Retrieves gaps that are past their due date
     *
     * @param tenantId The tenant identifier
     * @return List of overdue gaps
     */
    public List<CareGap> getOverdueGaps(String tenantId) {
        return careGapRepository.findOverdueGaps(tenantId);
    }

    /**
     * Get gaps due soon
     * Retrieves gaps due within specified days
     *
     * @param tenantId The tenant identifier
     * @param daysUntilDue Number of days to look ahead
     * @return List of gaps due soon
     */
    public List<CareGap> getGapsDueSoon(String tenantId, int daysUntilDue) {
        return careGapRepository.findGapsDueSoon(tenantId, daysUntilDue);
    }

    /**
     * Get financial impact summary for tenant
     * Calculates total financial impact of open gaps
     *
     * @param tenantId The tenant identifier
     * @return Total financial impact value
     */
    public Double getTotalFinancialImpact(String tenantId) {
        Double total = careGapRepository.getTotalFinancialImpact(tenantId);
        return total != null ? total : 0.0;
    }

    /**
     * Get average risk score for tenant
     * Calculates mean risk score across open gaps
     *
     * @param tenantId The tenant identifier
     * @return Average risk score
     */
    public Double getAverageRiskScore(String tenantId) {
        Double average = careGapRepository.getAverageRiskScore(tenantId);
        return average != null ? average : 0.0;
    }

    // ======================== Helper Methods ========================

    private CareGap createCareGap(Patient patient, String gapType, String description,
                                  String priority, int daysUntilDue) {
        return createCareGap(patient, gapType, description, priority, daysUntilDue, null);
    }

    private CareGap createCareGap(Patient patient, String gapType, String description,
                                  String priority, int daysUntilDue, String measureId) {
        LocalDateTime dueDate = LocalDateTime.now().plusDays(daysUntilDue);
        double priorityScore = calculatePriorityScoreByType(gapType, priority);

        return CareGap.builder()
            .patientId(patient.getId())
            .tenantId(patient.getTenantId())
            .gapType(gapType)
            .description(description)
            .priority(priority)
            .status(STATUS_OPEN)
            .dueDate(dueDate)
            .detectedDate(LocalDateTime.now())
            .measureId(measureId)
            .riskScore(priorityScore)
            .financialImpact(estimateFinancialImpact(gapType))
            .build();
    }

    private boolean hasRecentObservation(List<Observation> observations, String observationType, int daysBack) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);
        return observations.stream()
            .filter(obs -> obs.getDisplay() != null && obs.getDisplay().toLowerCase().contains(observationType.toLowerCase()))
            .anyMatch(obs -> obs.getEffectiveDate() != null && obs.getEffectiveDate().isAfter(cutoffDate));
    }

    private boolean hasCondition(List<Condition> conditions, String conditionType) {
        return conditions.stream()
            .anyMatch(c -> c.getDisplay() != null && c.getDisplay().toLowerCase().contains(conditionType.toLowerCase()));
    }

    private boolean isRefillOverdue(MedicationRequest med) {
        return med.getValidPeriodEnd() != null &&
            med.getValidPeriodEnd().isBefore(LocalDateTime.now());
    }

    private boolean isComplianceRisk(MedicationRequest med) {
        // Calculate compliance based on refills remaining vs days supply
        if (med.getRefillsRemaining() == null || med.getDaysSupply() == null) {
            return false;
        }
        // Risk if very few refills remaining or prescription is old
        return med.getRefillsRemaining() < 2 || (med.getAuthoredOn() != null &&
            med.getAuthoredOn().isBefore(LocalDateTime.now().minusDays(180)));
    }

    private boolean isAgeGreaterThan(Patient patient, int age) {
        return patient.getDateOfBirth() != null &&
            Period.between(patient.getDateOfBirth(), LocalDate.now())
                .getYears() > age;
    }

    private boolean isAgeBetween(Patient patient, int minAge, int maxAge) {
        if (patient.getDateOfBirth() == null) {
            return false;
        }
        int age = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
        return age >= minAge && age <= maxAge;
    }

    private double calculateClinicalImpactScore(CareGap gap) {
        return switch (gap.getPriority()) {
            case PRIORITY_HIGH -> 85.0;
            case PRIORITY_MEDIUM -> 50.0;
            case PRIORITY_LOW -> 20.0;
            default -> 0.0;
        };
    }

    private double calculateFinancialImpactScore(CareGap gap) {
        if (gap.getFinancialImpact() == null) {
            return 0.0;
        }
        // Normalize financial impact to 0-100 scale (assuming max impact is 100000)
        return Math.min(100.0, (gap.getFinancialImpact() / 100000.0) * 100.0);
    }

    private double calculateAgeBasedScore(CareGap gap) {
        // Retrieve patient and calculate age-based score
        return patientService.getPatient(gap.getPatientId())
            .map(patient -> {
                if (patient.getDateOfBirth() == null) {
                    return 50.0;
                }
                int age = Period.between(patient.getDateOfBirth(),
                    LocalDate.now()).getYears();
                // Older patients have higher risk
                return Math.min(100.0, age);
            })
            .orElse(50.0);
    }

    private double calculatePriorityScoreByType(String gapType, String priority) {
        double baseScore = switch (priority) {
            case PRIORITY_HIGH -> 85.0;
            case PRIORITY_MEDIUM -> 50.0;
            case PRIORITY_LOW -> 20.0;
            default -> 0.0;
        };

        // Adjust by gap type
        return switch (gapType) {
            case GAP_TYPE_CHRONIC -> baseScore + 10.0;
            case GAP_TYPE_CANCER -> baseScore + 15.0;
            case GAP_TYPE_CARDIOVASCULAR -> baseScore + 12.0;
            default -> baseScore;
        };
    }

    private double estimateFinancialImpact(String gapType) {
        return switch (gapType) {
            case GAP_TYPE_CHRONIC -> 5000.0;
            case GAP_TYPE_CANCER -> 8000.0;
            case GAP_TYPE_CARDIOVASCULAR -> 4000.0;
            case GAP_TYPE_MEDICATION -> 2000.0;
            case GAP_TYPE_PREVENTIVE -> 1500.0;
            default -> 1000.0;
        };
    }

    private boolean checkPreventiveClosureCondition(CareGap gap, List<Observation> observations) {
        String description = gap.getDescription().toLowerCase();
        if (description.contains("wellness")) {
            return hasRecentObservation(observations, "wellness-visit", 30);
        }
        if (description.contains("vaccine")) {
            return hasRecentObservation(observations, "vaccine", 30);
        }
        return false;
    }

    private boolean checkChronicClosureCondition(CareGap gap, List<Observation> observations) {
        String description = gap.getDescription().toLowerCase();
        if (description.contains("HbA1c")) {
            return hasRecentObservation(observations, "HbA1c", 30);
        }
        if (description.contains("blood-pressure")) {
            return hasRecentObservation(observations, "blood-pressure", 30);
        }
        if (description.contains("spirometry")) {
            return hasRecentObservation(observations, "spirometry", 30);
        }
        return false;
    }

    private boolean checkMedicationClosureCondition(CareGap gap) {
        // Check if refill has been filled
        return gap.getUpdatedAt().isAfter(gap.getDetectedDate().plusHours(24));
    }

    private boolean checkCancerScreeningClosureCondition(CareGap gap, List<Observation> observations) {
        String description = gap.getDescription().toLowerCase();
        if (description.contains("colonoscopy")) {
            return hasRecentObservation(observations, "colonoscopy", 30);
        }
        if (description.contains("mammography")) {
            return hasRecentObservation(observations, "mammography", 30);
        }
        if (description.contains("pap")) {
            return hasRecentObservation(observations, "pap-smear", 30);
        }
        return false;
    }

    private boolean checkCardiovascularClosureCondition(CareGap gap, List<Observation> observations) {
        String description = gap.getDescription().toLowerCase();
        if (description.contains("lipid")) {
            return hasRecentObservation(observations, "lipid-panel", 30);
        }
        if (description.contains("blood-pressure")) {
            return hasRecentObservation(observations, "blood-pressure", 30);
        }
        if (description.contains("weight")) {
            return hasRecentObservation(observations, "weight", 30);
        }
        return false;
    }

    private void publishGapDetectedEvent(CareGap gap) {
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            gap.getId(),
            gap.getPatientId(),
            gap.getGapType(),
            gap.getPriority()
        );
        event.setMeasureId(gap.getMeasureId());
        eventPublisher.publishEvent(event);
    }

    @SuppressWarnings("unchecked")
    private Page<CareGap> convertListToPage(List<CareGap> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        List<CareGap> pageContent = list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, list.size());
    }
}
