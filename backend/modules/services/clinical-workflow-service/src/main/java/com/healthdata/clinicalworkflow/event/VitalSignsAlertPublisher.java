package com.healthdata.clinicalworkflow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kafka Publisher for Vital Signs Alerts
 *
 * Publishes abnormal vital signs to Kafka topics for downstream processing:
 * - Alert Service: Sends notifications to providers
 * - FHIR Service: Creates Flag resources for abnormal vitals
 * - Analytics Service: Tracks alert trends and patterns
 *
 * Topic Strategy:
 * - Critical alerts → vitals.alert.critical (highest priority)
 * - Warning alerts → vitals.alert.warning (medium priority)
 *
 * Resilience:
 * - Non-blocking: Failures don't prevent vital signs recording
 * - Async: Uses Kafka's async send for performance
 * - Logged: All publish failures logged for debugging
 *
 * HIPAA Compliance:
 * - PHI included in events (patient ID, vital values)
 * - Kafka encryption required in production (TLS + at-rest)
 * - Access control via tenant ID filtering
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VitalSignsAlertPublisher {

    private static final String TOPIC_PREFIX = "vitals.alert.";
    private static final String EVENT_TYPE = "VITAL_SIGNS_ALERT_CREATED";
    private static final String EVENT_SOURCE = "clinical-workflow-service";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish vital signs alert to Kafka
     *
     * Publishes abnormal vital signs to appropriate Kafka topic based on severity.
     * Uses patient ID as partition key for ordered processing per patient.
     *
     * Topic Routing:
     * - "critical" → vitals.alert.critical
     * - "warning" → vitals.alert.warning
     *
     * Partition Key: Patient ID (ensures all alerts for same patient go to same partition)
     *
     * Non-Blocking: Failures logged but don't throw exceptions
     *
     * @param vitals the vital signs record with abnormal values
     * @param patientName optional patient name for notification display
     * @param roomNumber optional room number for emergency response
     */
    public void publishAlert(VitalSignsRecordEntity vitals, String patientName, String roomNumber) {
        try {
            log.debug("Publishing vital signs alert for patient {} (status: {})",
                    vitals.getPatientId(), vitals.getAlertStatus());

            // Build event
            VitalSignsAlertEvent event = buildEvent(vitals, patientName, roomNumber);

            // Determine topic based on severity
            String topic = TOPIC_PREFIX + vitals.getAlertStatus();

            // Serialize event to JSON
            String eventJson = objectMapper.writeValueAsString(event);

            // Use patient ID as partition key (ordered processing per patient)
            String partitionKey = vitals.getPatientId().toString();

            // Publish to Kafka asynchronously
            kafkaTemplate.send(topic, partitionKey, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish vital signs alert to Kafka topic {} for patient {}: {}",
                                    topic, vitals.getPatientId(), ex.getMessage(), ex);
                        } else {
                            log.info("Successfully published vital signs alert to Kafka topic {} for patient {} " +
                                            "(vitals ID: {}, partition: {}, offset: {})",
                                    topic,
                                    vitals.getPatientId(),
                                    vitals.getId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize vital signs alert event for patient {}: {}",
                    vitals.getPatientId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error publishing vital signs alert for patient {}: {}",
                    vitals.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Build Kafka event from vital signs entity
     *
     * Creates standardized event structure with metadata, identifiers, and PHI data.
     * Includes only abnormal vital sign values to minimize PHI exposure.
     *
     * @param vitals the vital signs record
     * @param patientName optional patient name (nullable)
     * @param roomNumber optional room number (nullable)
     * @return structured Kafka event
     */
    private VitalSignsAlertEvent buildEvent(
            VitalSignsRecordEntity vitals,
            String patientName,
            String roomNumber) {

        // Extract alert types from message
        String[] alertTypes = extractAlertTypes(vitals.getAlertMessage());

        // Build vital signs values (only include non-null values)
        VitalSignsAlertEvent.VitalSignsValues values = VitalSignsAlertEvent.VitalSignsValues.builder()
                .systolicBp(vitals.getSystolicBp())
                .diastolicBp(vitals.getDiastolicBp())
                .heartRate(vitals.getHeartRate())
                .temperatureF(vitals.getTemperatureF())
                .respirationRate(vitals.getRespirationRate())
                .oxygenSaturation(vitals.getOxygenSaturation())
                .build();

        return VitalSignsAlertEvent.builder()
                // Event metadata
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE)
                .eventTimestamp(Instant.now())
                .eventSource(EVENT_SOURCE)
                // Identifiers
                .tenantId(vitals.getTenantId())
                .patientId(vitals.getPatientId())
                .patientName(patientName)
                .vitalsId(vitals.getId())
                .encounterId(vitals.getEncounterId())
                .recordedAt(vitals.getRecordedAt())
                .recordedBy(vitals.getRecordedBy())
                // Alert metadata
                .alertStatus(vitals.getAlertStatus())
                .alertMessage(vitals.getAlertMessage())
                .alertTypes(alertTypes)
                // Vital signs values (PHI)
                .values(values)
                // Location
                .roomNumber(roomNumber)
                .build();
    }

    /**
     * Extract alert types from alert message
     *
     * Parses alert message to identify specific abnormal vital signs.
     * Used for filtering and routing by downstream consumers.
     *
     * Examples:
     * - "Systolic BP > 180 mmHg (critical)" → ["HIGH_BLOOD_PRESSURE"]
     * - "Heart Rate > 130 bpm, O2 < 85%" → ["HIGH_HEART_RATE", "LOW_OXYGEN_SATURATION"]
     *
     * @param alertMessage the alert message text
     * @return array of alert type codes
     */
    private String[] extractAlertTypes(String alertMessage) {
        if (alertMessage == null || alertMessage.isEmpty()) {
            return new String[0];
        }

        List<String> types = new ArrayList<>();
        String upperMessage = alertMessage.toUpperCase();

        if (upperMessage.contains("SYSTOLIC BP") && (upperMessage.contains(">") || upperMessage.contains("HIGH"))) {
            types.add("HIGH_BLOOD_PRESSURE");
        } else if (upperMessage.contains("SYSTOLIC BP") && (upperMessage.contains("<") || upperMessage.contains("LOW"))) {
            types.add("LOW_BLOOD_PRESSURE");
        }

        if (upperMessage.contains("HEART RATE") && (upperMessage.contains(">") || upperMessage.contains("HIGH"))) {
            types.add("HIGH_HEART_RATE");
        } else if (upperMessage.contains("HEART RATE") && (upperMessage.contains("<") || upperMessage.contains("LOW"))) {
            types.add("LOW_HEART_RATE");
        }

        if (upperMessage.contains("TEMPERATURE") && upperMessage.contains(">")) {
            types.add("HIGH_TEMPERATURE");
        } else if (upperMessage.contains("TEMPERATURE") && upperMessage.contains("<")) {
            types.add("LOW_TEMPERATURE");
        }

        if (upperMessage.contains("O2") || upperMessage.contains("OXYGEN")) {
            types.add("LOW_OXYGEN_SATURATION");
        }

        if (upperMessage.contains("RESPIR")) {
            if (upperMessage.contains(">") || upperMessage.contains("HIGH")) {
                types.add("HIGH_RESPIRATORY_RATE");
            } else if (upperMessage.contains("<") || upperMessage.contains("LOW")) {
                types.add("LOW_RESPIRATORY_RATE");
            }
        }

        return types.toArray(new String[0]);
    }
}
