package com.healthdata.quality.service;

import com.healthdata.quality.persistence.ChronicDiseaseMonitoringEntity;
import com.healthdata.quality.persistence.ChronicDiseaseMonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Chronic Disease Monitoring Service
 *
 * Provides continuous monitoring of chronic diseases with:
 * - Lab result processing and trend analysis
 * - Deterioration detection (HbA1c, BP, LDL, etc.)
 * - Automated alert triggering
 * - Improvement detection
 * - Monitoring schedule management
 *
 * Part of Phase 4.2: Chronic Disease Deterioration Detection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChronicDiseaseMonitoringService {

    private final ChronicDiseaseMonitoringRepository monitoringRepository;
    private final DiseaseDeteriorationDetector diseaseDeteriorationDetector;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Disease code mappings
    private static final String DIABETES_CODE = "44054006"; // Type 2 Diabetes Mellitus
    private static final String HYPERTENSION_CODE = "38341003"; // Hypertensive disorder
    private static final String HYPERLIPIDEMIA_CODE = "13644009"; // Hyperlipidemia

    /**
     * Process lab result observation and update monitoring
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param observationData FHIR Observation resource
     * @return Updated monitoring record
     */
    @Transactional
    public ChronicDiseaseMonitoringEntity processLabResult(
        String tenantId,
        UUID patientId,
        Map<String, Object> observationData
    ) {
        log.info("Processing lab result for patient {}", patientId);

        // Extract lab type and value
        LabResult labResult = extractLabResult(observationData);

        if (labResult == null) {
            log.warn("Unable to extract lab result from observation");
            return null;
        }

        // Get or create monitoring record
        Optional<ChronicDiseaseMonitoringEntity> existingOpt = monitoringRepository
            .findByTenantIdAndPatientIdAndDiseaseCode(tenantId, patientId, labResult.diseaseCode);

        ChronicDiseaseMonitoringEntity monitoring;

        if (existingOpt.isPresent()) {
            monitoring = existingOpt.get();

            // Store current value as previous
            monitoring.setPreviousValue(monitoring.getLatestValue());

            // Update with new value
            monitoring.setLatestValue(labResult.value);
            monitoring.setMonitoredAt(Instant.now());
        } else {
            // Create new monitoring record
            monitoring = ChronicDiseaseMonitoringEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .diseaseCode(labResult.diseaseCode)
                .diseaseName(labResult.diseaseName)
                .latestValue(labResult.value)
                .previousValue(null)
                .trend(ChronicDiseaseMonitoringEntity.Trend.STABLE)
                .alertTriggered(false)
                .monitoredAt(Instant.now())
                .build();
        }

        // Analyze trend
        ChronicDiseaseMonitoringEntity.Trend trend = diseaseDeteriorationDetector.analyzeTrend(
            labResult.metric,
            monitoring.getPreviousValue(),
            monitoring.getLatestValue()
        );
        monitoring.setTrend(trend);

        // Check if alert should trigger
        boolean shouldAlert = diseaseDeteriorationDetector.shouldTriggerAlert(
            labResult.metric,
            monitoring.getLatestValue(),
            monitoring.getPreviousValue()
        );
        monitoring.setAlertTriggered(shouldAlert);

        // Set next monitoring due date
        monitoring.setNextMonitoringDue(calculateNextMonitoringDue(labResult.metric, trend));

        // Save monitoring record
        monitoring = monitoringRepository.save(monitoring);

        // Publish events
        if (trend == ChronicDiseaseMonitoringEntity.Trend.DETERIORATING) {
            publishDeteriorationEvent(tenantId, patientId, monitoring, labResult);
        }

        log.info("Updated monitoring for patient {}: {} trend={}, alert={}, value={}",
            patientId, labResult.metric, trend, shouldAlert, labResult.value);

        return monitoring;
    }

    /**
     * Get all monitoring records for a patient
     */
    public List<ChronicDiseaseMonitoringEntity> getPatientMonitoring(String tenantId, UUID patientId) {
        return monitoringRepository.findByTenantIdAndPatientIdOrderByMonitoredAtDesc(tenantId, patientId);
    }

    /**
     * Get all patients with deteriorating trends
     */
    public List<ChronicDiseaseMonitoringEntity> getDeterioratingPatients(String tenantId) {
        return monitoringRepository.findByTenantIdAndTrendOrderByMonitoredAtDesc(
            tenantId,
            ChronicDiseaseMonitoringEntity.Trend.DETERIORATING
        );
    }

    /**
     * Get all patients with active alerts
     */
    public List<ChronicDiseaseMonitoringEntity> getPatientsWithAlerts(String tenantId) {
        return monitoringRepository.findByTenantIdAndAlertTriggeredTrueOrderByMonitoredAtDesc(tenantId);
    }

    /**
     * Get patients due for monitoring
     */
    public List<ChronicDiseaseMonitoringEntity> getPatientsDueForMonitoring(String tenantId) {
        return monitoringRepository.findDueForMonitoring(tenantId, Instant.now());
    }

    /**
     * Extract lab result from FHIR Observation
     */
    private LabResult extractLabResult(Map<String, Object> observationData) {
        try {
            // Extract LOINC code
            Map<String, Object> code = (Map<String, Object>) observationData.get("code");
            Map<String, Object> coding = (Map<String, Object>) ((List<?>) code.get("coding")).get(0);
            String loincCode = (String) coding.get("code");
            String display = (String) coding.get("display");

            // Extract value
            Map<String, Object> valueQuantity = (Map<String, Object>) observationData.get("valueQuantity");
            Double value = ((Number) valueQuantity.get("value")).doubleValue();

            // Map to disease and metric
            return switch (loincCode) {
                case "4548-4" -> // HbA1c
                    new LabResult(DIABETES_CODE, "Type 2 Diabetes Mellitus", "HbA1c", value, display);

                case "8480-6" -> // Systolic BP
                    new LabResult(HYPERTENSION_CODE, "Hypertensive disorder", "BP_SYSTOLIC", value, display);

                case "18262-6" -> // LDL Cholesterol
                    new LabResult(HYPERLIPIDEMIA_CODE, "Hyperlipidemia", "LDL", value, display);

                default -> {
                    log.warn("Unknown LOINC code: {}", loincCode);
                    yield null;
                }
            };

        } catch (Exception e) {
            log.error("Error extracting lab result", e);
            return null;
        }
    }

    /**
     * Calculate next monitoring due date based on metric and trend
     */
    private Instant calculateNextMonitoringDue(String metric, ChronicDiseaseMonitoringEntity.Trend trend) {
        int daysUntilNext;

        if (trend == ChronicDiseaseMonitoringEntity.Trend.DETERIORATING) {
            // More frequent monitoring for deteriorating conditions
            daysUntilNext = switch (metric) {
                case "HbA1c" -> 60;  // 2 months for deteriorating diabetes
                case "BP_SYSTOLIC" -> 14;  // 2 weeks for deteriorating hypertension
                case "LDL" -> 90;  // 3 months for deteriorating hyperlipidemia
                default -> 30;
            };
        } else {
            // Standard monitoring intervals
            daysUntilNext = switch (metric) {
                case "HbA1c" -> 90;  // 3 months for stable diabetes
                case "BP_SYSTOLIC" -> 30;  // 1 month for stable hypertension
                case "LDL" -> 180;  // 6 months for stable hyperlipidemia
                default -> 90;
            };
        }

        return Instant.now().plus(daysUntilNext, ChronoUnit.DAYS);
    }

    /**
     * Publish chronic-disease.deterioration event
     */
    private void publishDeteriorationEvent(
        String tenantId,
        UUID patientId,
        ChronicDiseaseMonitoringEntity monitoring,
        LabResult labResult
    ) {
        String severity = diseaseDeteriorationDetector.getDeteriorationSeverity(
            labResult.metric,
            monitoring.getLatestValue()
        );

        Map<String, Object> event = new java.util.HashMap<>();
        event.put("eventType", "chronic-disease.deterioration");
        event.put("tenantId", tenantId);
        event.put("patientId", patientId);
        event.put("diseaseCode", monitoring.getDiseaseCode());
        event.put("diseaseName", monitoring.getDiseaseName());
        event.put("metric", labResult.metric);
        event.put("previousValue", monitoring.getPreviousValue() != null ? monitoring.getPreviousValue() : 0.0);
        event.put("newValue", monitoring.getLatestValue());
        event.put("trend", monitoring.getTrend().name());
        event.put("alertLevel", severity);
        event.put("monitoringId", monitoring.getId().toString());
        event.put("timestamp", Instant.now().toString());

        kafkaTemplate.send("chronic-disease.deterioration", event);

        log.info("Published chronic-disease.deterioration event for patient {}: {} {} → {}",
            patientId, labResult.metric, monitoring.getPreviousValue(), monitoring.getLatestValue());
    }

    /**
     * Internal lab result structure
     */
    private record LabResult(
        String diseaseCode,
        String diseaseName,
        String metric,
        Double value,
        String display
    ) {}
}
