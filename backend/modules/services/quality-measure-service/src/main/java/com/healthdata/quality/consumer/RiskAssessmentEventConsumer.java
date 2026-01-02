package com.healthdata.quality.consumer;

import com.healthdata.quality.service.RiskCalculationService;
import com.healthdata.quality.service.ChronicDiseaseMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Risk Assessment Event Consumer
 *
 * Listens to FHIR events and triggers continuous risk assessment:
 * - fhir.conditions.created/updated → Recalculate risk
 * - fhir.observations.created → Process lab results and recalculate risk
 *
 * Publishes events:
 * - risk-assessment.updated
 * - risk-level.changed
 * - chronic-disease.deterioration
 *
 * Part of Phase 4.1: Continuous Risk Assessment Event Listeners
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentEventConsumer {

    private final RiskCalculationService riskCalculationService;
    private final ChronicDiseaseMonitoringService chronicDiseaseMonitoringService;

    /**
     * Listen to condition created events
     *
     * When a new condition is diagnosed, recalculate patient risk
     */
    @KafkaListener(
        topics = "fhir.conditions.created",
        groupId = "risk-assessment-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onConditionCreated(Map<String, Object> event) {
        try {
            log.info("Received fhir.conditions.created event");

            String tenantId = (String) event.get("tenantId");
            UUID patientId = extractPatientId(event);
            Map<String, Object> conditionData = (Map<String, Object>) event.get("resource");

            if (tenantId == null || patientId == null || conditionData == null) {
                log.warn("Missing required fields in condition created event");
                return;
            }

            // Only process encounter-diagnosis conditions (chronic diseases)
            if (!isChronicCondition(conditionData)) {
                log.debug("Condition is not a chronic disease, skipping risk recalculation");
                return;
            }

            // Recalculate risk assessment
            riskCalculationService.recalculateRiskOnCondition(tenantId, patientId, conditionData);

            log.info("Successfully processed condition created event for patient {}", patientId);

        } catch (Exception e) {
            log.error("Error processing condition created event", e);
        }
    }

    /**
     * Listen to condition updated events
     *
     * When a condition status changes (e.g., active → resolved), recalculate risk
     */
    @KafkaListener(
        topics = "fhir.conditions.updated",
        groupId = "risk-assessment-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onConditionUpdated(Map<String, Object> event) {
        try {
            log.info("Received fhir.conditions.updated event");

            String tenantId = (String) event.get("tenantId");
            UUID patientId = extractPatientId(event);
            Map<String, Object> conditionData = (Map<String, Object>) event.get("resource");

            if (tenantId == null || patientId == null || conditionData == null) {
                log.warn("Missing required fields in condition updated event");
                return;
            }

            if (!isChronicCondition(conditionData)) {
                log.debug("Condition is not a chronic disease, skipping risk recalculation");
                return;
            }

            // Recalculate risk assessment
            riskCalculationService.recalculateRiskOnCondition(tenantId, patientId, conditionData);

            log.info("Successfully processed condition updated event for patient {}", patientId);

        } catch (Exception e) {
            log.error("Error processing condition updated event", e);
        }
    }

    /**
     * Listen to observation created events
     *
     * When new lab results are received, process for:
     * 1. Chronic disease monitoring and deterioration detection
     * 2. Risk assessment recalculation
     */
    @KafkaListener(
        topics = "fhir.observations.created",
        groupId = "risk-assessment-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onObservationCreated(Map<String, Object> event) {
        try {
            log.info("Received fhir.observations.created event");

            String tenantId = (String) event.get("tenantId");
            UUID patientId = extractPatientId(event);
            Map<String, Object> observationData = (Map<String, Object>) event.get("resource");

            if (tenantId == null || patientId == null || observationData == null) {
                log.warn("Missing required fields in observation created event");
                return;
            }

            // Check if this is a relevant lab result
            if (!isMonitoredLabResult(observationData)) {
                log.debug("Observation is not a monitored lab result, skipping processing");
                return;
            }

            // Process for chronic disease monitoring
            chronicDiseaseMonitoringService.processLabResult(tenantId, patientId, observationData);

            // Recalculate risk assessment
            riskCalculationService.recalculateRiskOnObservation(tenantId, patientId, observationData);

            log.info("Successfully processed observation created event for patient {}", patientId);

        } catch (Exception e) {
            log.error("Error processing observation created event", e);
        }
    }

    /**
     * Extract patient ID from event
     */
    private UUID extractPatientId(Map<String, Object> event) {
        // Try direct patientId field
        if (event.containsKey("patientId")) {
            return parsePatientIdValue(event.get("patientId"));
        }

        // Try resource.subject.reference
        Map<String, Object> resource = (Map<String, Object>) event.get("resource");
        if (resource != null && resource.containsKey("subject")) {
            Map<String, Object> subject = (Map<String, Object>) resource.get("subject");
            if (subject != null && subject.containsKey("reference")) {
                String reference = (String) subject.get("reference");
                // Extract ID from "Patient/123"
                return parsePatientIdString(reference.replace("Patient/", ""));
            }
        }

        return null;
    }

    private UUID parsePatientIdValue(Object patientId) {
        if (patientId instanceof UUID) {
            return (UUID) patientId;
        }
        return parsePatientIdString(patientId != null ? patientId.toString() : null);
    }

    private UUID parsePatientIdString(String patientId) {
        if (patientId == null) {
            return null;
        }
        try {
            return UUID.fromString(patientId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Check if condition is a chronic disease (encounter-diagnosis)
     */
    private boolean isChronicCondition(Map<String, Object> conditionData) {
        try {
            // Check clinical status - must be active
            Map<String, Object> clinicalStatus = (Map<String, Object>) conditionData.get("clinicalStatus");
            if (clinicalStatus != null) {
                List<Map<String, Object>> codings = (List<Map<String, Object>>) clinicalStatus.get("coding");
                if (codings != null && !codings.isEmpty()) {
                    String statusCode = (String) codings.get(0).get("code");
                    if (!"active".equals(statusCode)) {
                        return false;
                    }
                }
            }

            // Check category - must be encounter-diagnosis
            List<Map<String, Object>> categories = (List<Map<String, Object>>) conditionData.get("category");
            if (categories != null) {
                for (Map<String, Object> category : categories) {
                    List<Map<String, Object>> codings = (List<Map<String, Object>>) category.get("coding");
                    if (codings != null) {
                        for (Map<String, Object> coding : codings) {
                            String code = (String) coding.get("code");
                            if ("encounter-diagnosis".equals(code)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;

        } catch (Exception e) {
            log.warn("Error checking if condition is chronic", e);
            return false;
        }
    }

    /**
     * Check if observation is a monitored lab result
     */
    private boolean isMonitoredLabResult(Map<String, Object> observationData) {
        try {
            Map<String, Object> code = (Map<String, Object>) observationData.get("code");
            List<Map<String, Object>> codings = (List<Map<String, Object>>) code.get("coding");

            if (codings == null || codings.isEmpty()) {
                return false;
            }

            String loincCode = (String) codings.get(0).get("code");

            // Monitored LOINC codes
            return "4548-4".equals(loincCode) ||   // HbA1c
                   "8480-6".equals(loincCode) ||   // Systolic BP
                   "18262-6".equals(loincCode);    // LDL Cholesterol

        } catch (Exception e) {
            log.warn("Error checking if observation is monitored lab result", e);
            return false;
        }
    }
}
