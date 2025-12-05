package com.healthdata.quality.service;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.dto.notification.GenericNotificationRequest;
import com.healthdata.quality.persistence.ClinicalAlertEntity;
import com.healthdata.quality.persistence.ClinicalAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Alert Escalation Service (Phase 5)
 *
 * Automatically escalates unacknowledged clinical alerts based on severity and time thresholds.
 *
 * Escalation Thresholds:
 * - CRITICAL: 15 minutes
 * - HIGH: 30 minutes
 * - MEDIUM: 2 hours
 * - LOW: No escalation
 *
 * Escalation Actions:
 * - Mark alert as escalated
 * - Send notifications to escalation recipients (higher in hierarchy)
 * - Publish escalation event to Kafka
 * - Log escalation for audit trail
 *
 * Runs every 5 minutes via @Scheduled task
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEscalationService {

    private final ClinicalAlertRepository alertRepository;
    private final AlertRoutingService alertRoutingService;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Escalation time thresholds (in minutes)
    private static final int CRITICAL_ESCALATION_THRESHOLD_MINUTES = 15;
    private static final int HIGH_ESCALATION_THRESHOLD_MINUTES = 30;
    private static final int MEDIUM_ESCALATION_THRESHOLD_MINUTES = 120; // 2 hours

    /**
     * Scheduled task to process alert escalations
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public int processEscalations() {
        log.debug("Starting alert escalation processing");

        try {
            // Find all unacknowledged, non-escalated alerts
            Instant now = Instant.now();
            List<ClinicalAlertEntity> unacknowledgedAlerts = alertRepository.findUnacknowledgedAlerts(now);

            int escalatedCount = 0;

            for (ClinicalAlertEntity alert : unacknowledgedAlerts) {
                // Skip if already escalated
                if (alert.isEscalated()) {
                    continue;
                }

                // Check if alert should be escalated based on severity and time
                if (shouldEscalate(alert, now)) {
                    escalateAlert(alert);
                    escalatedCount++;
                }
            }

            if (escalatedCount > 0) {
                log.info("Escalated {} clinical alerts", escalatedCount);
            } else {
                log.debug("No alerts require escalation at this time");
            }

            return escalatedCount;

        } catch (Exception e) {
            log.error("Error processing alert escalations: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Determine if alert should be escalated based on severity and time elapsed
     */
    private boolean shouldEscalate(ClinicalAlertEntity alert, Instant now) {
        // Don't escalate LOW severity alerts
        if (alert.getSeverity() == ClinicalAlertEntity.AlertSeverity.LOW) {
            return false;
        }

        // Calculate time elapsed since trigger
        long minutesElapsed = ChronoUnit.MINUTES.between(alert.getTriggeredAt(), now);

        // Check against severity-specific thresholds
        return switch (alert.getSeverity()) {
            case CRITICAL -> minutesElapsed >= CRITICAL_ESCALATION_THRESHOLD_MINUTES;
            case HIGH -> minutesElapsed >= HIGH_ESCALATION_THRESHOLD_MINUTES;
            case MEDIUM -> minutesElapsed >= MEDIUM_ESCALATION_THRESHOLD_MINUTES;
            case LOW -> false;
        };
    }

    /**
     * Escalate a specific alert
     */
    @Transactional
    public void escalateAlert(ClinicalAlertEntity alert) {
        log.warn("Escalating alert {} (patient: {}, severity: {}, triggered: {})",
            alert.getId(), alert.getPatientId(), alert.getSeverity(),
            alert.getTriggeredAt());

        // Mark alert as escalated
        alert.setEscalated(true);
        alert.setEscalatedAt(Instant.now());
        alertRepository.save(alert);

        // Convert to DTO
        ClinicalAlertDTO alertDTO = mapToDTO(alert);

        // Get escalation recipients
        List<String> escalationRecipients = alertRoutingService.getEscalationRecipients(
            alert.getTenantId(),
            alertDTO
        );

        // Send escalation notifications
        sendEscalationNotifications(alert.getTenantId(), alertDTO, escalationRecipients);

        // Publish escalation event
        publishEscalationEvent(alertDTO);
    }

    /**
     * Send notifications to escalation recipients
     */
    private void sendEscalationNotifications(
        String tenantId,
        ClinicalAlertDTO alert,
        List<String> recipients
    ) {
        try {
            Map<String, String> recipientMap = new HashMap<>();
            // Convert recipient list to map format expected by notification service
            recipientMap.put("EMAIL", String.join(",", recipients));

            GenericNotificationRequest request = GenericNotificationRequest.builder()
                .notificationType("CLINICAL_ALERT_ESCALATED")
                .templateId("alert-escalation")
                .tenantId(tenantId)
                .patientId(alert.getPatientId())
                .title(String.format("ESCALATED: %s", alert.getTitle()))
                .message(String.format(
                    "Alert has been escalated due to no acknowledgment. " +
                    "Immediate attention required. Original alert: %s",
                    alert.getMessage()
                ))
                .severity("HIGH")
                .sendWebSocket(true)
                .sendEmail(true)
                .sendSms("CRITICAL".equals(alert.getSeverity()))
                .recipients(recipientMap)
                .templateVariables(buildEscalationTemplateVariables(alert))
                .metadata(buildEscalationMetadata(alert))
                .build();

            notificationService.sendNotification(request);

            log.info("Escalation notifications sent for alert {} to {} recipients",
                alert.getId(), recipients.size());

        } catch (Exception e) {
            log.error("Failed to send escalation notifications for alert {}: {}",
                alert.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publish escalation event to Kafka
     */
    private void publishEscalationEvent(ClinicalAlertDTO alert) {
        Map<String, Object> event = new HashMap<>();
        event.put("alertId", alert.getId());
        event.put("patientId", alert.getPatientId());
        event.put("tenantId", alert.getTenantId());
        event.put("alertType", alert.getAlertType());
        event.put("severity", alert.getSeverity());
        event.put("escalatedAt", alert.getEscalatedAt().toString());
        event.put("originalTriggeredAt", alert.getTriggeredAt().toString());

        kafkaTemplate.send("clinical-alert.escalated", event);

        log.info("Published clinical-alert.escalated event for alert {}", alert.getId());
    }

    /**
     * Build template variables for escalation notification
     */
    private Map<String, Object> buildEscalationTemplateVariables(ClinicalAlertDTO alert) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("alertId", alert.getId());
        variables.put("patientId", alert.getPatientId());
        variables.put("alertType", alert.getAlertType());
        variables.put("severity", alert.getSeverity());
        variables.put("title", alert.getTitle());
        variables.put("message", alert.getMessage());
        variables.put("triggeredAt", alert.getTriggeredAt().toString());
        variables.put("escalatedAt", alert.getEscalatedAt().toString());

        // Calculate time elapsed
        long minutesElapsed = ChronoUnit.MINUTES.between(
            alert.getTriggeredAt(),
            alert.getEscalatedAt()
        );
        variables.put("minutesElapsed", minutesElapsed);

        return variables;
    }

    /**
     * Build metadata for escalation tracking
     */
    private Map<String, Object> buildEscalationMetadata(ClinicalAlertDTO alert) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alertId", alert.getId());
        metadata.put("escalated", true);
        metadata.put("escalatedAt", alert.getEscalatedAt().toString());
        return metadata;
    }

    /**
     * Map entity to DTO
     */
    private ClinicalAlertDTO mapToDTO(ClinicalAlertEntity entity) {
        return ClinicalAlertDTO.builder()
            .id(entity.getId().toString())
            .patientId(entity.getPatientId())
            .tenantId(entity.getTenantId())
            .alertType(entity.getAlertType().name())
            .severity(entity.getSeverity().name())
            .title(entity.getTitle())
            .message(entity.getMessage())
            .sourceEventType(entity.getSourceEventType())
            .sourceEventId(entity.getSourceEventId())
            .triggeredAt(entity.getTriggeredAt())
            .acknowledgedAt(entity.getAcknowledgedAt())
            .acknowledgedBy(entity.getAcknowledgedBy())
            .escalated(entity.isEscalated())
            .escalatedAt(entity.getEscalatedAt())
            .status(entity.getStatus().name())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
