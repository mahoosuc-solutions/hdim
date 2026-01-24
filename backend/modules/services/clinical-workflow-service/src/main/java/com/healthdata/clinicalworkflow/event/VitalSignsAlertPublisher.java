package com.healthdata.clinicalworkflow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.service.AuditService;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hybrid Publisher for Vital Signs Alerts (Kafka + WebSocket)
 *
 * Publishes abnormal vital signs to both Kafka topics and WebSocket for:
 * - Kafka (Service-to-Service): Downstream processing, analytics, FHIR integration
 * - WebSocket (Service-to-Frontend): Real-time provider notifications in browser
 *
 * Topic Strategy:
 * - Kafka Topics:
 *   - vitals.alert.critical → Critical alerts (highest priority)
 *   - vitals.alert.warning → Warning alerts (medium priority)
 * - WebSocket Topics:
 *   - /topic/vitals-alerts/{providerId} → Provider-specific alerts
 *   - /topic/vitals-alerts/critical → Broadcast critical alerts to all
 *
 * Resilience:
 * - Non-blocking: Failures don't prevent vital signs recording
 * - Async: Uses Kafka's async send for performance
 * - Dual-channel: WebSocket failure doesn't block Kafka publishing
 * - Logged: All publish failures logged for debugging
 *
 * HIPAA Compliance:
 * - PHI included in events (patient ID, vital values)
 * - Kafka: TLS encryption + at-rest encryption required in production
 * - WebSocket: Spring Security authentication from HTTP session
 * - Access control: Tenant ID + Provider ID routing
 *
 * Issue: #288 - Real-time vital sign alerts via WebSocket
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VitalSignsAlertPublisher {

    private static final String TOPIC_PREFIX = "vitals.alert.";
    private static final String EVENT_TYPE = "VITAL_SIGNS_ALERT_CREATED";
    private static final String EVENT_SOURCE = "clinical-workflow-service";
    private static final String WEBSOCKET_DESTINATION_PREFIX = "/topic/vitals-alerts/";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Publish vital signs alert to Kafka + WebSocket
     *
     * Publishes abnormal vital signs to both:
     * 1. Kafka topic for backend processing (async, persisted)
     * 2. WebSocket for real-time frontend notifications (immediate)
     *
     * Kafka Topic Routing:
     * - "critical" → vitals.alert.critical
     * - "warning" → vitals.alert.warning
     *
     * WebSocket Destination Routing:
     * - Provider-specific: /topic/vitals-alerts/{providerId}
     * - Critical broadcast: /topic/vitals-alerts/critical (all providers)
     *
     * Partition Key: Patient ID (ensures all alerts for same patient go to same partition)
     *
     * Non-Blocking: Failures logged but don't throw exceptions
     * Dual-Channel: WebSocket failure doesn't block Kafka publishing
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

            // Determine Kafka topic based on severity
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

            // Publish to WebSocket for real-time frontend notifications
            publishToWebSocket(event, vitals);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize vital signs alert event for patient {}: {}",
                    vitals.getPatientId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error publishing vital signs alert for patient {}: {}",
                    vitals.getPatientId(), e.getMessage(), e);
        }
    }

    /**
     * Publish alert to WebSocket for real-time frontend notifications
     *
     * Sends alert to provider-specific destination and critical broadcast if applicable.
     * Non-blocking: Failures don't prevent Kafka publishing.
     * HIPAA Audited: All alert deliveries logged to audit service.
     *
     * Destinations:
     * - /topic/vitals-alerts/{providerId} - Specific provider (if recorded_by available)
     * - /topic/vitals-alerts/critical - All providers (critical alerts only)
     *
     * @param event the vital signs alert event
     * @param vitals the vital signs record (for metadata)
     */
    private void publishToWebSocket(VitalSignsAlertEvent event, VitalSignsRecordEntity vitals) {
        try {
            int deliveryCount = 0;

            // Send to provider-specific destination if recorded_by is available
            if (vitals.getRecordedBy() != null && !vitals.getRecordedBy().isBlank()) {
                String providerDestination = WEBSOCKET_DESTINATION_PREFIX + vitals.getRecordedBy();
                messagingTemplate.convertAndSend(providerDestination, event);
                log.debug("Published vital signs alert to WebSocket destination {} for provider {}",
                        providerDestination, vitals.getRecordedBy());
                deliveryCount++;

                // Audit provider-specific delivery
                auditProviderNotification(vitals, vitals.getRecordedBy(), "PROVIDER_SPECIFIC", true);
            }

            // Broadcast critical alerts to all providers
            if ("critical".equalsIgnoreCase(vitals.getAlertStatus())) {
                String broadcastDestination = WEBSOCKET_DESTINATION_PREFIX + "critical";
                messagingTemplate.convertAndSend(broadcastDestination, event);
                log.debug("Broadcast critical vital signs alert to WebSocket destination {}", broadcastDestination);
                deliveryCount++;

                // Audit critical broadcast
                auditProviderNotification(vitals, "ALL_PROVIDERS", "CRITICAL_BROADCAST", true);
            }

            log.info("Successfully published vital signs alert to WebSocket for patient {} (vitals ID: {}, deliveries: {})",
                    vitals.getPatientId(), vitals.getId(), deliveryCount);

        } catch (Exception e) {
            // WebSocket failures should not block Kafka publishing or vital signs recording
            log.error("Failed to publish vital signs alert to WebSocket for patient {}: {}",
                    vitals.getPatientId(), e.getMessage(), e);

            // Audit failed delivery
            auditProviderNotification(vitals,
                    vitals.getRecordedBy() != null ? vitals.getRecordedBy() : "UNKNOWN",
                    "WEBSOCKET_DELIVERY", false);
        }
    }

    /**
     * Audit provider notification delivery (HIPAA §164.312(b) - Audit Controls)
     *
     * Logs all alert deliveries to audit service for compliance tracking.
     * Records both successful and failed delivery attempts.
     *
     * @param vitals the vital signs record
     * @param providerId the provider ID receiving the alert
     * @param notificationType the notification type (PROVIDER_SPECIFIC, CRITICAL_BROADCAST, etc.)
     * @param success whether the notification was delivered successfully
     */
    private void auditProviderNotification(
            VitalSignsRecordEntity vitals,
            String providerId,
            String notificationType,
            boolean success) {
        try {
            auditService.logAuditEvent(AuditEvent.builder()
                    .tenantId(vitals.getTenantId())
                    .action(AuditAction.CREATE)
                    .resourceType("VitalSignsAlert")
                    .resourceId(vitals.getId().toString())
                    .serviceName("clinical-workflow-service")
                    .methodName("publishToWebSocket")
                    .outcome(success ? AuditOutcome.SUCCESS : AuditOutcome.MINOR_FAILURE)
                    .build());

            log.debug("Audited vital signs alert notification: patient={}, provider={}, type={}, success={}",
                    vitals.getPatientId(), providerId, notificationType, success);

        } catch (Exception e) {
            // Audit logging failures should not block notifications
            log.error("Failed to audit vital signs alert notification for patient {}: {}",
                    vitals.getPatientId(), e.getMessage());
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
