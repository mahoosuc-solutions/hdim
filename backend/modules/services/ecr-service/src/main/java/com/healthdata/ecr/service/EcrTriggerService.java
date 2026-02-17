package com.healthdata.ecr.service;

import com.healthdata.ecr.persistence.ElectronicCaseReportEntity;
import com.healthdata.ecr.persistence.ElectronicCaseReportEntity.*;
import com.healthdata.ecr.persistence.ElectronicCaseReportRepository;
import com.healthdata.ecr.trigger.RctcRulesEngine;
import com.healthdata.ecr.trigger.RctcRulesEngine.TriggerMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * eCR Trigger Service
 *
 * Listens for clinical events via Kafka and evaluates them against
 * RCTC rules to detect reportable conditions. When a trigger is detected,
 * creates an ElectronicCaseReport entity to initiate the eCR workflow.
 *
 * Event Types Monitored:
 * - condition.created - New diagnosis/condition
 * - observation.created - Lab result
 * - medicationadministration.created - Medication given
 * - procedure.created - Procedure performed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcrTriggerService {

    private final RctcRulesEngine rctcRulesEngine;
    private final ElectronicCaseReportRepository ecrRepository;
    private final EcrProcessingService processingService;

    // Duplicate detection window (don't create duplicate eCRs within this period)
    private static final int DUPLICATE_WINDOW_HOURS = 24;

    /**
     * Listen for new Condition (diagnosis) events
     */
    @KafkaListener(topics = "clinical.condition.created", groupId = "ecr-service")
    @Transactional(rollbackFor = Exception.class)
    public void handleConditionCreated(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        UUID patientId = UUID.fromString((String) event.get("patientId"));
        String icd10Code = (String) event.get("code");
        String codeSystem = (String) event.getOrDefault("codeSystem", RctcRulesEngine.ICD10CM_OID);
        String display = (String) event.get("display");
        UUID encounterId = event.containsKey("encounterId")
            ? UUID.fromString((String) event.get("encounterId")) : null;

        log.debug("Processing condition event: tenant={}, patient={}, code={}",
            tenantId, patientId, icd10Code);

        evaluateAndCreateEcr(tenantId, patientId, encounterId, icd10Code, codeSystem,
            display, TriggerCategory.DIAGNOSIS);
    }

    /**
     * Listen for lab result events
     */
    @KafkaListener(topics = "clinical.observation.created", groupId = "ecr-service")
    @Transactional(rollbackFor = Exception.class)
    public void handleObservationCreated(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        UUID patientId = UUID.fromString((String) event.get("patientId"));
        String loincCode = (String) event.get("code");
        String display = (String) event.get("display");
        UUID encounterId = event.containsKey("encounterId")
            ? UUID.fromString((String) event.get("encounterId")) : null;

        // Only process lab observations
        String category = (String) event.get("category");
        if (!"laboratory".equals(category)) {
            return;
        }

        log.debug("Processing lab observation event: tenant={}, patient={}, code={}",
            tenantId, patientId, loincCode);

        evaluateAndCreateEcr(tenantId, patientId, encounterId, loincCode,
            RctcRulesEngine.LOINC_OID, display, TriggerCategory.LAB_RESULT);
    }

    /**
     * Listen for medication administration events
     */
    @KafkaListener(topics = "clinical.medicationadministration.created", groupId = "ecr-service")
    @Transactional(rollbackFor = Exception.class)
    public void handleMedicationAdministered(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        UUID patientId = UUID.fromString((String) event.get("patientId"));
        String rxnormCode = (String) event.get("code");
        String display = (String) event.get("display");
        UUID encounterId = event.containsKey("encounterId")
            ? UUID.fromString((String) event.get("encounterId")) : null;

        log.debug("Processing medication event: tenant={}, patient={}, code={}",
            tenantId, patientId, rxnormCode);

        evaluateAndCreateEcr(tenantId, patientId, encounterId, rxnormCode,
            RctcRulesEngine.RXNORM_OID, display, TriggerCategory.MEDICATION);
    }

    /**
     * Listen for procedure events
     */
    @KafkaListener(topics = "clinical.procedure.created", groupId = "ecr-service")
    @Transactional(rollbackFor = Exception.class)
    public void handleProcedureCreated(Map<String, Object> event) {
        String tenantId = (String) event.get("tenantId");
        UUID patientId = UUID.fromString((String) event.get("patientId"));
        String cptCode = (String) event.get("code");
        String display = (String) event.get("display");
        UUID encounterId = event.containsKey("encounterId")
            ? UUID.fromString((String) event.get("encounterId")) : null;

        log.debug("Processing procedure event: tenant={}, patient={}, code={}",
            tenantId, patientId, cptCode);

        evaluateAndCreateEcr(tenantId, patientId, encounterId, cptCode,
            RctcRulesEngine.CPT_OID, display, TriggerCategory.PROCEDURE);
    }

    /**
     * Evaluate a clinical code against RCTC rules and create eCR if reportable.
     */
    private void evaluateAndCreateEcr(
            String tenantId,
            UUID patientId,
            UUID encounterId,
            String code,
            String codeSystem,
            String display,
            TriggerCategory category) {

        // Check if code triggers a reportable condition
        if (!rctcRulesEngine.isReportableTrigger(code, codeSystem)) {
            log.debug("Code {} not a reportable trigger", code);
            return;
        }

        // Get full trigger match details
        TriggerMatch match = getTriggerMatch(code, codeSystem, category);
        if (match == null) {
            log.warn("Code {} matched as trigger but details not found", code);
            return;
        }

        // Check for duplicate eCR within window
        if (isDuplicate(tenantId, patientId, code)) {
            log.info("Duplicate eCR detected for patient {} with code {}, skipping",
                patientId, code);
            return;
        }

        // Create eCR entity
        ElectronicCaseReportEntity ecr = ElectronicCaseReportEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .encounterId(encounterId)
            .triggerCode(code)
            .triggerCodeSystem(codeSystem)
            .triggerDisplay(display != null ? display : match.getDisplay())
            .triggerCategory(category)
            .conditionName(match.getConditionName())
            .status(EcrStatus.PENDING)
            .urgency(mapUrgency(match.getUrgency()))
            .triggerDetectedAt(LocalDateTime.now())
            .retryCount(0)
            .build();

        ecr = ecrRepository.save(ecr);

        log.info("Created eCR {} for reportable condition '{}' (urgency: {})",
            ecr.getId(), match.getConditionName(), ecr.getUrgency());

        // Queue for immediate processing if urgent
        if (ecr.getUrgency() == EcrUrgency.IMMEDIATE) {
            processingService.processImmediately(ecr.getId());
        }
    }

    private TriggerMatch getTriggerMatch(String code, String codeSystem, TriggerCategory category) {
        return switch (category) {
            case DIAGNOSIS -> {
                if (RctcRulesEngine.ICD10CM_OID.equals(codeSystem)) {
                    yield rctcRulesEngine.evaluateDiagnosis(code).orElse(null);
                } else {
                    yield rctcRulesEngine.evaluateSnomedDiagnosis(code).orElse(null);
                }
            }
            case LAB_RESULT -> rctcRulesEngine.evaluateLabResult(code).orElse(null);
            case MEDICATION -> rctcRulesEngine.evaluateMedication(code).orElse(null);
            case PROCEDURE -> rctcRulesEngine.evaluateProcedure(code).orElse(null);
        };
    }

    private boolean isDuplicate(String tenantId, UUID patientId, String triggerCode) {
        LocalDateTime since = LocalDateTime.now().minusHours(DUPLICATE_WINDOW_HOURS);
        return ecrRepository.existsPendingForTrigger(tenantId, patientId, triggerCode, since);
    }

    private EcrUrgency mapUrgency(com.healthdata.ecr.persistence.RctcTriggerCodeEntity.Urgency urgency) {
        if (urgency == null) {
            return EcrUrgency.ROUTINE;
        }
        return switch (urgency) {
            case IMMEDIATE -> EcrUrgency.IMMEDIATE;
            case WITHIN_24_HOURS -> EcrUrgency.WITHIN_24_HOURS;
            case WITHIN_72_HOURS -> EcrUrgency.WITHIN_72_HOURS;
            case ROUTINE -> EcrUrgency.ROUTINE;
        };
    }

    /**
     * Manually trigger eCR evaluation for a patient's codes.
     * Useful for batch processing or re-evaluation.
     */
    @Transactional
    public List<ElectronicCaseReportEntity> evaluatePatientCodes(
            String tenantId,
            UUID patientId,
            List<String> diagnosisCodes,
            List<String> labCodes) {

        log.info("Manual eCR evaluation for patient {} with {} diagnoses and {} labs",
            patientId, diagnosisCodes.size(), labCodes.size());

        RctcRulesEngine.ClinicalEvent event = RctcRulesEngine.ClinicalEvent.builder()
            .patientId(patientId)
            .diagnosisCodes(diagnosisCodes)
            .labCodes(labCodes)
            .build();

        List<TriggerMatch> matches = rctcRulesEngine.evaluateClinicalEvent(event);

        return matches.stream()
            .map(match -> {
                TriggerCategory category = mapTriggerType(match.getTriggerType());
                ElectronicCaseReportEntity ecr = ElectronicCaseReportEntity.builder()
                    .tenantId(tenantId)
                    .patientId(patientId)
                    .triggerCode(match.getCode())
                    .triggerCodeSystem(match.getCodeSystem())
                    .triggerDisplay(match.getDisplay())
                    .triggerCategory(category)
                    .conditionName(match.getConditionName())
                    .status(EcrStatus.PENDING)
                    .urgency(mapUrgency(match.getUrgency()))
                    .triggerDetectedAt(LocalDateTime.now())
                    .retryCount(0)
                    .build();
                return ecrRepository.save(ecr);
            })
            .toList();
    }

    private TriggerCategory mapTriggerType(
            com.healthdata.ecr.persistence.RctcTriggerCodeEntity.TriggerType type) {
        return switch (type) {
            case DIAGNOSIS -> TriggerCategory.DIAGNOSIS;
            case LAB_ORDER, LAB_RESULT -> TriggerCategory.LAB_RESULT;
            case MEDICATION -> TriggerCategory.MEDICATION;
            case PROCEDURE -> TriggerCategory.PROCEDURE;
        };
    }
}
