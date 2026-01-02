package com.healthdata.hcc.service;

import com.healthdata.hcc.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HCC Recapture Tracking Service
 *
 * Tracks chronic condition HCCs from prior years that need to be recaptured
 * (re-documented and re-coded) annually for RAF score credit.
 *
 * CMS Recapture Requirements:
 * - Chronic conditions must be documented each calendar year
 * - MEAT criteria must be met (Monitor, Evaluate, Assess/Address, Treat)
 * - Conditions not recaptured "fall off" the RAF score
 *
 * Key Features:
 * - Identify HCCs from prior year not yet recaptured
 * - Calculate RAF impact of missing recaptures
 * - Generate recapture worklists by provider/practice
 * - Track recapture rates and trends
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecaptureTrackingService {

    private final RecaptureOpportunityRepository recaptureRepository;
    private final PatientHccProfileRepository profileRepository;
    private final DiagnosisHccMapRepository diagnosisMapRepository;

    // Chronic HCCs that require annual recapture
    private static final Set<String> CHRONIC_HCCS = Set.of(
        "HCC17", "HCC18", "HCC19",  // Diabetes
        "HCC85", "HCC86", "HCC87", "HCC88",  // Heart failure
        "HCC111", "HCC112",  // COPD
        "HCC134", "HCC135", "HCC136", "HCC137", "HCC138",  // CKD stages
        "HCC157", "HCC158", "HCC159", "HCC160", "HCC161",  // Pressure ulcers
        "HCC21", "HCC22", "HCC23", "HCC24",  // Protein-calorie malnutrition
        "HCC8", "HCC9", "HCC10", "HCC11", "HCC12",  // Cancer
        "HCC57", "HCC58", "HCC59", "HCC60",  // Vascular disease
        "HCC82", "HCC83", "HCC84"  // Respiratory arrest, cardio-respiratory failure
    );

    /**
     * Identify recapture opportunities by comparing prior year HCCs with current year.
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @param currentYear Current calendar year
     * @return List of recapture opportunities
     */
    @Transactional
    public List<RecaptureOpportunityEntity> identifyRecaptureOpportunities(
            String tenantId,
            UUID patientId,
            int currentYear) {

        int priorYear = currentYear - 1;

        log.info("Identifying recapture opportunities for patient {} (prior year: {})",
            patientId, priorYear);

        // Get prior year HCCs
        Optional<PatientHccProfileEntity> priorProfile = profileRepository
            .findByTenantIdAndPatientIdAndProfileYear(tenantId, patientId, priorYear);

        if (priorProfile.isEmpty() || priorProfile.get().getHccsV28() == null) {
            log.debug("No prior year profile found for patient {}", patientId);
            return Collections.emptyList();
        }

        Set<String> priorYearHccs = new HashSet<>(priorProfile.get().getHccsV28());

        // Filter to chronic HCCs only
        Set<String> chronicHccsFromPriorYear = priorYearHccs.stream()
            .filter(CHRONIC_HCCS::contains)
            .collect(Collectors.toSet());

        if (chronicHccsFromPriorYear.isEmpty()) {
            log.debug("No chronic HCCs to recapture for patient {}", patientId);
            return Collections.emptyList();
        }

        // Get current year HCCs
        Optional<PatientHccProfileEntity> currentProfile = profileRepository
            .findByTenantIdAndPatientIdAndProfileYear(tenantId, patientId, currentYear);

        Set<String> currentYearHccs = currentProfile
            .map(p -> p.getHccsV28() != null ? new HashSet<>(p.getHccsV28()) : new HashSet<String>())
            .orElse(new HashSet<>());

        // Find HCCs not yet recaptured
        Set<String> notRecaptured = chronicHccsFromPriorYear.stream()
            .filter(hcc -> !currentYearHccs.contains(hcc))
            .collect(Collectors.toSet());

        // Create recapture opportunities
        List<RecaptureOpportunityEntity> opportunities = notRecaptured.stream()
            .map(hcc -> createRecaptureOpportunity(tenantId, patientId, priorYear, currentYear, hcc, priorProfile.get()))
            .toList();

        // Save opportunities
        List<RecaptureOpportunityEntity> saved = recaptureRepository.saveAll(opportunities);

        // Update profile with recapture summary
        updateProfileRecaptureSummary(tenantId, patientId, currentYear, saved, currentYearHccs, chronicHccsFromPriorYear);

        log.info("Identified {} recapture opportunities for patient {}", saved.size(), patientId);

        return saved;
    }

    /**
     * Get all open recapture opportunities for a patient.
     */
    public List<RecaptureOpportunityEntity> getOpenOpportunities(
            String tenantId, UUID patientId, int currentYear) {
        return recaptureRepository.findByTenantIdAndPatientIdAndCurrentYear(tenantId, patientId, currentYear)
            .stream()
            .filter(r -> !Boolean.TRUE.equals(r.getIsRecaptured()))
            .toList();
    }

    /**
     * Get high-value recapture opportunities across population.
     */
    public List<RecaptureOpportunityEntity> getHighValueOpportunities(
            String tenantId, int currentYear, BigDecimal minRafValue) {
        return recaptureRepository.findHighValueOpportunities(tenantId, currentYear, minRafValue);
    }

    /**
     * Mark an HCC as recaptured.
     */
    @Transactional
    public RecaptureOpportunityEntity markRecaptured(
            UUID opportunityId,
            String recapturedIcd10) {

        RecaptureOpportunityEntity opportunity = recaptureRepository.findById(opportunityId)
            .orElseThrow(() -> new IllegalArgumentException("Opportunity not found: " + opportunityId));

        opportunity.setIsRecaptured(true);
        opportunity.setRecapturedIcd10(recapturedIcd10);
        opportunity.setRecapturedAt(LocalDateTime.now());

        return recaptureRepository.save(opportunity);
    }

    /**
     * Calculate recapture rate for a tenant.
     */
    public RecaptureRateSummary calculateRecaptureRate(String tenantId, int year) {
        List<Object[]> stats = recaptureRepository.getRecaptureStatsByTenant(tenantId, year);

        long totalOpportunities = 0;
        long recaptured = 0;
        BigDecimal totalRafAtRisk = BigDecimal.ZERO;
        BigDecimal rafSecured = BigDecimal.ZERO;

        for (Object[] row : stats) {
            totalOpportunities += ((Number) row[0]).longValue();
            recaptured += ((Number) row[1]).longValue();
            totalRafAtRisk = totalRafAtRisk.add((BigDecimal) row[2]);
            rafSecured = rafSecured.add((BigDecimal) row[3]);
        }

        double recaptureRate = totalOpportunities > 0 ?
            (double) recaptured / totalOpportunities * 100 : 0;

        return RecaptureRateSummary.builder()
            .tenantId(tenantId)
            .year(year)
            .totalOpportunities(totalOpportunities)
            .recapturedCount(recaptured)
            .pendingCount(totalOpportunities - recaptured)
            .recaptureRate(recaptureRate)
            .totalRafAtRisk(totalRafAtRisk)
            .rafSecured(rafSecured)
            .rafPending(totalRafAtRisk.subtract(rafSecured))
            .build();
    }

    /**
     * Get recapture worklist grouped by priority.
     */
    public Map<String, List<RecaptureOpportunityEntity>> getRecaptureWorklist(
            String tenantId, int currentYear) {

        List<RecaptureOpportunityEntity> opportunities =
            recaptureRepository.findPendingByTenant(tenantId, currentYear);

        return opportunities.stream()
            .collect(Collectors.groupingBy(RecaptureOpportunityEntity::getPriority));
    }

    private RecaptureOpportunityEntity createRecaptureOpportunity(
            String tenantId, UUID patientId, int priorYear, int currentYear,
            String hccCode, PatientHccProfileEntity priorProfile) {

        // Find the diagnosis code used in prior year for this HCC
        String priorIcd10 = findPriorYearDiagnosisForHcc(priorProfile, hccCode);
        String priorIcd10Description = null;

        if (priorIcd10 != null) {
            priorIcd10Description = diagnosisMapRepository.findByIcd10Code(priorIcd10)
                .map(DiagnosisHccMapEntity::getIcd10Description)
                .orElse(null);
        }

        // Estimate RAF value (in production, look up from coefficient tables)
        BigDecimal rafValueV24 = BigDecimal.valueOf(0.15);
        BigDecimal rafValueV28 = BigDecimal.valueOf(0.12);

        return RecaptureOpportunityEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .priorYear(priorYear)
            .currentYear(currentYear)
            .hccCode(hccCode)
            .hccName(getHccName(hccCode))
            .priorYearIcd10(priorIcd10)
            .priorYearIcd10Description(priorIcd10Description)
            .rafValueV24(rafValueV24)
            .rafValueV28(rafValueV28)
            .isRecaptured(false)
            .clinicalGuidance(generateClinicalGuidance(hccCode))
            .priority(calculateRecapturePriority(rafValueV28))
            .build();
    }

    private String findPriorYearDiagnosisForHcc(PatientHccProfileEntity profile, String hccCode) {
        // In production, store the mapping of HCC to source diagnosis
        // For now, return null
        if (profile.getDiagnosisCodes() == null) {
            return null;
        }

        // Find a diagnosis that maps to this HCC
        List<DiagnosisHccMapEntity> mappings = diagnosisMapRepository.findByHccCodeV28(hccCode);
        if (!mappings.isEmpty()) {
            return mappings.get(0).getIcd10Code();
        }

        return null;
    }

    private String getHccName(String hccCode) {
        // HCC code to name mapping (subset)
        return switch (hccCode) {
            case "HCC17" -> "Diabetes with Acute Complications";
            case "HCC18" -> "Diabetes with Chronic Complications";
            case "HCC19" -> "Diabetes without Complication";
            case "HCC85" -> "Congestive Heart Failure";
            case "HCC86" -> "Acute Myocardial Infarction";
            case "HCC111" -> "Chronic Obstructive Pulmonary Disease";
            case "HCC134" -> "Dialysis Status";
            case "HCC135" -> "Acute Renal Failure";
            case "HCC136" -> "Chronic Kidney Disease, Stage 5";
            case "HCC137" -> "Chronic Kidney Disease, Stage 4";
            case "HCC138" -> "Chronic Kidney Disease, Stage 3";
            default -> "HCC " + hccCode;
        };
    }

    private String generateClinicalGuidance(String hccCode) {
        // Generate MEAT-based documentation guidance
        StringBuilder guidance = new StringBuilder();
        guidance.append("To recapture this condition, document:\n\n");
        guidance.append("MONITOR: Current status of the condition\n");
        guidance.append("EVALUATE: Test results, assessments, clinical findings\n");
        guidance.append("ASSESS/ADDRESS: Clinical decision-making, treatment adjustments\n");
        guidance.append("TREAT: Current treatment plan, medications, therapies\n\n");

        // Condition-specific guidance
        if (hccCode.startsWith("HCC17") || hccCode.startsWith("HCC18") || hccCode.startsWith("HCC19")) {
            guidance.append("Diabetes-specific: Document HbA1c, complications, management plan");
        } else if (hccCode.startsWith("HCC85") || hccCode.startsWith("HCC86")) {
            guidance.append("Heart failure-specific: Document EF, NYHA class, medications");
        } else if (hccCode.startsWith("HCC111")) {
            guidance.append("COPD-specific: Document FEV1, oxygen use, exacerbation history");
        } else if (hccCode.startsWith("HCC13")) {
            guidance.append("CKD-specific: Document eGFR, stage, dialysis status");
        }

        return guidance.toString();
    }

    private String calculateRecapturePriority(BigDecimal rafValue) {
        if (rafValue.compareTo(BigDecimal.valueOf(0.3)) > 0) {
            return "HIGH";
        } else if (rafValue.compareTo(BigDecimal.valueOf(0.15)) > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private void updateProfileRecaptureSummary(
            String tenantId, UUID patientId, int currentYear,
            List<RecaptureOpportunityEntity> opportunities,
            Set<String> currentYearHccs, Set<String> chronicHccsFromPriorYear) {

        profileRepository.findByTenantIdAndPatientIdAndProfileYear(tenantId, patientId, currentYear)
            .ifPresent(profile -> {
                long recapturedCount = chronicHccsFromPriorYear.stream()
                    .filter(currentYearHccs::contains)
                    .count();

                profile.setRecaptureOpportunitiesCount(opportunities.size());

                BigDecimal totalRecaptureValue = opportunities.stream()
                    .map(RecaptureOpportunityEntity::getRafValueV28)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                profile.setRecaptureRafValue(totalRecaptureValue);

                profileRepository.save(profile);
            });
    }

    @lombok.Data
    @lombok.Builder
    public static class RecaptureRateSummary {
        private String tenantId;
        private int year;
        private long totalOpportunities;
        private long recapturedCount;
        private long pendingCount;
        private double recaptureRate;
        private BigDecimal totalRafAtRisk;
        private BigDecimal rafSecured;
        private BigDecimal rafPending;
    }
}
