package com.healthdata.hcc.service;

import com.healthdata.hcc.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAF (Risk Adjustment Factor) Calculation Service
 *
 * Calculates patient RAF scores using both V24 and V28 CMS-HCC models,
 * supporting the transition period through 2026.
 *
 * CMS Transition Timeline:
 * - 2024: 67% V24 / 33% V28
 * - 2025: 33% V24 / 67% V28
 * - 2026+: 100% V28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RafCalculationService {

    private final DiagnosisHccMapRepository diagnosisMapRepository;
    private final PatientHccProfileRepository profileRepository;

    @Value("${hcc.model.v24-weight:0.33}")
    private double v24Weight;

    @Value("${hcc.model.v28-weight:0.67}")
    private double v28Weight;

    /**
     * Calculate RAF score for a patient based on their diagnosis codes.
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @param diagnosisCodes List of ICD-10-CM codes
     * @param demographicFactors Demographic factors (age, sex, dual status, etc.)
     * @return RAF calculation result with V24, V28, and blended scores
     */
    @Transactional
    public RafCalculationResult calculateRaf(
            String tenantId,
            UUID patientId,
            List<String> diagnosisCodes,
            DemographicFactors demographicFactors) {

        log.info("Calculating RAF for patient {} with {} diagnosis codes",
            patientId, diagnosisCodes.size());

        // Map diagnosis codes to HCCs
        List<DiagnosisHccMapEntity> mappings = diagnosisMapRepository.findByIcd10Codes(diagnosisCodes);

        // Extract unique HCCs for each model
        Set<String> hccsV24 = mappings.stream()
            .map(DiagnosisHccMapEntity::getHccCodeV24)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<String> hccsV28 = mappings.stream()
            .map(DiagnosisHccMapEntity::getHccCodeV28)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // Apply HCC hierarchies (higher severity HCCs trump lower)
        hccsV24 = applyHierarchiesV24(hccsV24);
        hccsV28 = applyHierarchiesV28(hccsV28);

        // Calculate scores
        BigDecimal scoreV24 = calculateScoreV24(hccsV24, demographicFactors);
        BigDecimal scoreV28 = calculateScoreV28(hccsV28, demographicFactors);

        // Calculate blended score
        BigDecimal blendedScore = scoreV24.multiply(BigDecimal.valueOf(v24Weight))
            .add(scoreV28.multiply(BigDecimal.valueOf(v28Weight)))
            .setScale(5, RoundingMode.HALF_UP);

        // Build result
        RafCalculationResult result = RafCalculationResult.builder()
            .patientId(patientId)
            .profileYear(LocalDate.now().getYear())
            .rafScoreV24(scoreV24)
            .rafScoreV28(scoreV28)
            .rafScoreBlended(blendedScore)
            .hccsV24(new ArrayList<>(hccsV24))
            .hccsV28(new ArrayList<>(hccsV28))
            .diagnosisCount(diagnosisCodes.size())
            .hccCountV24(hccsV24.size())
            .hccCountV28(hccsV28.size())
            .v24Weight(v24Weight)
            .v28Weight(v28Weight)
            .calculatedAt(LocalDateTime.now())
            .build();

        // Persist profile
        saveOrUpdateProfile(tenantId, patientId, diagnosisCodes, result);

        log.info("RAF calculation complete for patient {}: V24={}, V28={}, Blended={}",
            patientId, scoreV24, scoreV28, blendedScore);

        return result;
    }

    /**
     * Get the crosswalk mapping for a diagnosis code.
     */
    @Cacheable(value = "diagnosisHccMap", key = "#icd10Code")
    public Optional<DiagnosisHccMapEntity> getCrosswalk(String icd10Code) {
        return diagnosisMapRepository.findByIcd10Code(icd10Code);
    }

    /**
     * Batch crosswalk lookup for multiple codes.
     */
    public Map<String, DiagnosisHccMapEntity> batchCrosswalk(List<String> icd10Codes) {
        return diagnosisMapRepository.findByIcd10Codes(icd10Codes).stream()
            .collect(Collectors.toMap(
                DiagnosisHccMapEntity::getIcd10Code,
                e -> e,
                (e1, e2) -> e1
            ));
    }

    /**
     * Apply V24 HCC hierarchies.
     * When higher-severity HCCs are present, lower-severity ones are removed.
     */
    private Set<String> applyHierarchiesV24(Set<String> hccs) {
        // V24 hierarchy examples:
        // HCC17 (Diabetes with acute complications) > HCC18 (with chronic complications) > HCC19 (without complications)
        // HCC8 (Metastatic cancer) > HCC9 (Lung cancer) > HCC10 (Lymphoma)

        Set<String> result = new HashSet<>(hccs);

        // Diabetes hierarchy
        if (result.contains("HCC17")) {
            result.remove("HCC18");
            result.remove("HCC19");
        } else if (result.contains("HCC18")) {
            result.remove("HCC19");
        }

        // Cancer hierarchy
        if (result.contains("HCC8")) {
            result.remove("HCC9");
            result.remove("HCC10");
            result.remove("HCC11");
            result.remove("HCC12");
        }

        // Add more hierarchy rules as needed

        return result;
    }

    /**
     * Apply V28 HCC hierarchies.
     */
    private Set<String> applyHierarchiesV28(Set<String> hccs) {
        // V28 has different hierarchy structure
        Set<String> result = new HashSet<>(hccs);

        // V28 diabetes hierarchy (constrained - complications same weight)
        // HCC17 (Diabetes with acute complications) still trumps others
        if (result.contains("HCC17")) {
            result.remove("HCC18");
            result.remove("HCC19");
            result.remove("HCC37"); // Diabetes with peripheral circulatory
        }

        return result;
    }

    /**
     * Calculate V24 RAF score.
     */
    private BigDecimal calculateScoreV24(Set<String> hccs, DemographicFactors factors) {
        // Start with demographic baseline
        BigDecimal score = getDemographicBaselineV24(factors);

        // Add HCC coefficients
        for (String hcc : hccs) {
            BigDecimal coefficient = getHccCoefficientV24(hcc, factors);
            score = score.add(coefficient);
        }

        // Add disease interactions
        score = score.add(calculateInteractionsV24(hccs, factors));

        return score.setScale(5, RoundingMode.HALF_UP);
    }

    /**
     * Calculate V28 RAF score.
     */
    private BigDecimal calculateScoreV28(Set<String> hccs, DemographicFactors factors) {
        // Start with demographic baseline
        BigDecimal score = getDemographicBaselineV28(factors);

        // Add HCC coefficients
        for (String hcc : hccs) {
            BigDecimal coefficient = getHccCoefficientV28(hcc, factors);
            score = score.add(coefficient);
        }

        // Add disease interactions
        score = score.add(calculateInteractionsV28(hccs, factors));

        return score.setScale(5, RoundingMode.HALF_UP);
    }

    // Placeholder methods - actual coefficients from CMS rate tables
    private BigDecimal getDemographicBaselineV24(DemographicFactors factors) {
        // Age-sex baseline from CMS tables
        return BigDecimal.valueOf(0.300); // Placeholder
    }

    private BigDecimal getDemographicBaselineV28(DemographicFactors factors) {
        return BigDecimal.valueOf(0.295); // Placeholder
    }

    private BigDecimal getHccCoefficientV24(String hcc, DemographicFactors factors) {
        // Look up from CMS coefficient tables
        return BigDecimal.valueOf(0.150); // Placeholder
    }

    private BigDecimal getHccCoefficientV28(String hcc, DemographicFactors factors) {
        return BigDecimal.valueOf(0.145); // Placeholder
    }

    private BigDecimal calculateInteractionsV24(Set<String> hccs, DemographicFactors factors) {
        // Disease interaction terms
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateInteractionsV28(Set<String> hccs, DemographicFactors factors) {
        return BigDecimal.ZERO;
    }

    private void saveOrUpdateProfile(String tenantId, UUID patientId,
            List<String> diagnosisCodes, RafCalculationResult result) {

        PatientHccProfileEntity profile = profileRepository
            .findByTenantIdAndPatientIdAndProfileYear(tenantId, patientId, result.getProfileYear())
            .orElse(PatientHccProfileEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .profileYear(result.getProfileYear())
                .build());

        profile.setRafScoreV24(result.getRafScoreV24());
        profile.setRafScoreV28(result.getRafScoreV28());
        profile.setRafScoreBlended(result.getRafScoreBlended());
        profile.setHccsV24(result.getHccsV24());
        profile.setHccsV28(result.getHccsV28());
        profile.setDiagnosisCodes(diagnosisCodes);
        profile.setLastCalculatedAt(LocalDateTime.now());

        profileRepository.save(profile);
    }

    @lombok.Data
    @lombok.Builder
    public static class RafCalculationResult {
        private UUID patientId;
        private Integer profileYear;
        private BigDecimal rafScoreV24;
        private BigDecimal rafScoreV28;
        private BigDecimal rafScoreBlended;
        private List<String> hccsV24;
        private List<String> hccsV28;
        private Integer diagnosisCount;
        private Integer hccCountV24;
        private Integer hccCountV28;
        private Double v24Weight;
        private Double v28Weight;
        private LocalDateTime calculatedAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class DemographicFactors {
        private Integer age;
        private String sex;
        private boolean dualEligible;
        private boolean institutionalized;
        private String medicaidStatus;
        private String originalReasonForEntitlement;
    }
}
