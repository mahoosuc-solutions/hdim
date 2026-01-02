package com.healthdata.ecr.trigger;

import com.healthdata.ecr.persistence.RctcTriggerCodeEntity;
import com.healthdata.ecr.persistence.RctcTriggerCodeEntity.TriggerType;
import com.healthdata.ecr.persistence.RctcTriggerCodeEntity.Urgency;
import com.healthdata.ecr.persistence.RctcTriggerCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RCTC (Reportable Condition Trigger Codes) Rules Engine
 *
 * Evaluates clinical data against CDC RCTC value sets to determine
 * if a reportable condition has been detected.
 *
 * Supported code systems:
 * - ICD-10-CM (2.16.840.1.113883.6.90) - Diagnoses
 * - SNOMED CT (2.16.840.1.113883.6.96) - Diagnoses, Findings
 * - LOINC (2.16.840.1.113883.6.1) - Lab Tests
 * - RXNORM (2.16.840.1.113883.6.88) - Medications
 * - CPT (2.16.840.1.113883.6.12) - Procedures
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RctcRulesEngine {

    private final RctcTriggerCodeRepository triggerCodeRepository;

    // Code system OIDs
    public static final String ICD10CM_OID = "2.16.840.1.113883.6.90";
    public static final String SNOMEDCT_OID = "2.16.840.1.113883.6.96";
    public static final String LOINC_OID = "2.16.840.1.113883.6.1";
    public static final String RXNORM_OID = "2.16.840.1.113883.6.88";
    public static final String CPT_OID = "2.16.840.1.113883.6.12";

    /**
     * Evaluate a diagnosis code against RCTC trigger codes.
     *
     * @param icd10Code ICD-10-CM diagnosis code
     * @return Trigger match result, or empty if not reportable
     */
    public Optional<TriggerMatch> evaluateDiagnosis(String icd10Code) {
        return evaluateCode(icd10Code, ICD10CM_OID, TriggerType.DIAGNOSIS);
    }

    /**
     * Evaluate a SNOMED diagnosis code against RCTC trigger codes.
     */
    public Optional<TriggerMatch> evaluateSnomedDiagnosis(String snomedCode) {
        return evaluateCode(snomedCode, SNOMEDCT_OID, TriggerType.DIAGNOSIS);
    }

    /**
     * Evaluate a lab result LOINC code against RCTC trigger codes.
     */
    public Optional<TriggerMatch> evaluateLabResult(String loincCode) {
        return evaluateCode(loincCode, LOINC_OID, TriggerType.LAB_RESULT);
    }

    /**
     * Evaluate a medication RXNORM code against RCTC trigger codes.
     */
    public Optional<TriggerMatch> evaluateMedication(String rxnormCode) {
        return evaluateCode(rxnormCode, RXNORM_OID, TriggerType.MEDICATION);
    }

    /**
     * Evaluate a procedure CPT code against RCTC trigger codes.
     */
    public Optional<TriggerMatch> evaluateProcedure(String cptCode) {
        return evaluateCode(cptCode, CPT_OID, TriggerType.PROCEDURE);
    }

    /**
     * Batch evaluate multiple codes of the same system.
     *
     * @param codes List of codes to evaluate
     * @param codeSystem Code system OID
     * @return List of trigger matches found
     */
    public List<TriggerMatch> batchEvaluate(List<String> codes, String codeSystem) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }

        List<RctcTriggerCodeEntity> matches = triggerCodeRepository.findMatchingTriggers(codes, codeSystem);

        return matches.stream()
            .map(this::toTriggerMatch)
            .collect(Collectors.toList());
    }

    /**
     * Evaluate a clinical event containing multiple code types.
     *
     * @param event Clinical event with various codes
     * @return All trigger matches found
     */
    public List<TriggerMatch> evaluateClinicalEvent(ClinicalEvent event) {
        List<TriggerMatch> matches = new ArrayList<>();

        // Evaluate diagnoses
        if (event.getDiagnosisCodes() != null) {
            for (String code : event.getDiagnosisCodes()) {
                evaluateDiagnosis(code).ifPresent(matches::add);
            }
        }

        // Evaluate SNOMED diagnoses
        if (event.getSnomedCodes() != null) {
            for (String code : event.getSnomedCodes()) {
                evaluateSnomedDiagnosis(code).ifPresent(matches::add);
            }
        }

        // Evaluate lab results
        if (event.getLabCodes() != null) {
            for (String code : event.getLabCodes()) {
                evaluateLabResult(code).ifPresent(matches::add);
            }
        }

        // Evaluate medications
        if (event.getMedicationCodes() != null) {
            for (String code : event.getMedicationCodes()) {
                evaluateMedication(code).ifPresent(matches::add);
            }
        }

        // Evaluate procedures
        if (event.getProcedureCodes() != null) {
            for (String code : event.getProcedureCodes()) {
                evaluateProcedure(code).ifPresent(matches::add);
            }
        }

        // Sort by urgency (most urgent first)
        matches.sort(Comparator.comparing(m -> m.getUrgency().ordinal()));

        return matches;
    }

    /**
     * Check if a specific code is a reportable trigger.
     */
    @Cacheable(value = "rctcTriggerCheck", key = "#code + ':' + #codeSystem")
    public boolean isReportableTrigger(String code, String codeSystem) {
        String normalizedCode = normalizeCode(code, codeSystem);
        return triggerCodeRepository.isReportableTrigger(normalizedCode, codeSystem);
    }

    /**
     * Get all conditions currently monitored.
     */
    @Cacheable(value = "rctcConditions")
    public Set<String> getMonitoredConditions() {
        return triggerCodeRepository.findAllActiveTriggers().stream()
            .map(RctcTriggerCodeEntity::getConditionName)
            .collect(Collectors.toSet());
    }

    /**
     * Get all immediate-urgency conditions.
     */
    @Cacheable(value = "rctcImmediateConditions")
    public Set<String> getImmediateUrgencyConditions() {
        return triggerCodeRepository.findByUrgencyAndIsActiveTrue(Urgency.IMMEDIATE).stream()
            .map(RctcTriggerCodeEntity::getConditionName)
            .collect(Collectors.toSet());
    }

    private Optional<TriggerMatch> evaluateCode(String code, String codeSystem, TriggerType expectedType) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        // Normalize code (remove dots for ICD-10)
        String normalizedCode = normalizeCode(code, codeSystem);

        return triggerCodeRepository.findByCodeAndCodeSystem(normalizedCode, codeSystem)
            .filter(RctcTriggerCodeEntity::getIsActive)
            .map(this::toTriggerMatch);
    }

    private String normalizeCode(String code, String codeSystem) {
        if (ICD10CM_OID.equals(codeSystem)) {
            // Remove dots from ICD-10 codes
            return code.replace(".", "").toUpperCase();
        }
        return code.trim();
    }

    private TriggerMatch toTriggerMatch(RctcTriggerCodeEntity entity) {
        return TriggerMatch.builder()
            .code(entity.getCode())
            .codeSystem(entity.getCodeSystem())
            .display(entity.getDisplay())
            .triggerType(entity.getTriggerType())
            .conditionName(entity.getConditionName())
            .urgency(entity.getUrgency())
            .valueSetOid(entity.getValueSetOid())
            .valueSetName(entity.getValueSetName())
            .build();
    }

    /**
     * Result of a trigger code match
     */
    @lombok.Data
    @lombok.Builder
    public static class TriggerMatch {
        private String code;
        private String codeSystem;
        private String display;
        private TriggerType triggerType;
        private String conditionName;
        private Urgency urgency;
        private String valueSetOid;
        private String valueSetName;
    }

    /**
     * Clinical event containing codes to evaluate
     */
    @lombok.Data
    @lombok.Builder
    public static class ClinicalEvent {
        private UUID patientId;
        private UUID encounterId;
        private List<String> diagnosisCodes;      // ICD-10-CM
        private List<String> snomedCodes;         // SNOMED CT
        private List<String> labCodes;            // LOINC
        private List<String> medicationCodes;     // RXNORM
        private List<String> procedureCodes;      // CPT
    }
}
