package com.healthdata.notification.infrastructure.messaging;

import com.healthdata.notification.application.NotificationService;
import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPriority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka consumer for notification events from other services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    /**
     * Process clinical alert events.
     */
    @KafkaListener(
        topics = "${kafka.topics.clinical-alerts:clinical-alerts}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleClinicalAlert(ClinicalAlertEvent event) {
        log.info("Received clinical alert event: {}", event);

        try {
            NotificationService.SendNotificationRequest request = 
                NotificationService.SendNotificationRequest.builder()
                    .tenantId(event.getTenantId())
                    .recipientId(event.getRecipientId())
                    .recipientEmail(event.getRecipientEmail())
                    .channel(NotificationChannel.EMAIL)
                    .templateCode("clinical-alert")
                    .priority(NotificationPriority.HIGH)
                    .variables(Map.of(
                        "patientName", event.getPatientName() != null ? event.getPatientName() : "Unknown",
                        "alertType", event.getAlertType() != null ? event.getAlertType() : "Clinical Alert",
                        "alertMessage", event.getMessage() != null ? event.getMessage() : "",
                        "severity", event.getSeverity() != null ? event.getSeverity() : "MEDIUM"
                    ))
                    .correlationId(event.getCorrelationId())
                    .createdBy("system")
                    .build();

            notificationService.sendNotification(request);
            log.info("Clinical alert notification sent for event: {}", event.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to process clinical alert event: {}", e.getMessage(), e);
        }
    }

    /**
     * Process care gap events.
     */
    @KafkaListener(
        topics = "${kafka.topics.care-gaps:care-gap-events}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCareGapEvent(CareGapEvent event) {
        log.info("Received care gap event: {}", event);

        try {
            NotificationService.SendNotificationRequest request = 
                NotificationService.SendNotificationRequest.builder()
                    .tenantId(event.getTenantId())
                    .recipientId(event.getRecipientId())
                    .recipientEmail(event.getRecipientEmail())
                    .channel(NotificationChannel.EMAIL)
                    .templateCode("care-gap-alert")
                    .priority(NotificationPriority.NORMAL)
                    .variables(Map.of(
                        "patientName", event.getPatientName() != null ? event.getPatientName() : "Unknown",
                        "measureName", event.getMeasureName() != null ? event.getMeasureName() : "Quality Measure",
                        "gapCount", event.getGapCount() != null ? event.getGapCount() : 0,
                        "dueDate", event.getDueDate() != null ? event.getDueDate() : "N/A"
                    ))
                    .correlationId(event.getCorrelationId())
                    .createdBy("system")
                    .build();

            notificationService.sendNotification(request);
            log.info("Care gap notification sent for event: {}", event.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to process care gap event: {}", e.getMessage(), e);
        }
    }

    /**
     * Process approval workflow events.
     */
    @KafkaListener(
        topics = "${kafka.topics.approvals:approval-events}",
        groupId = "${spring.kafka.consumer.group-id:notification-service}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleApprovalEvent(ApprovalEvent event) {
        log.info("Received approval event: {}", event);

        try {
            NotificationService.SendNotificationRequest request = 
                NotificationService.SendNotificationRequest.builder()
                    .tenantId(event.getTenantId())
                    .recipientId(event.getRecipientId())
                    .recipientEmail(event.getRecipientEmail())
                    .channel(NotificationChannel.EMAIL)
                    .templateCode("approval-" + event.getEventType().toLowerCase())
                    .priority(NotificationPriority.NORMAL)
                    .variables(Map.of(
                        "requestType", event.getRequestType() != null ? event.getRequestType() : "Request",
                        "requestId", event.getRequestId() != null ? event.getRequestId() : "",
                        "status", event.getStatus() != null ? event.getStatus() : "",
                        "requesterName", event.getRequesterName() != null ? event.getRequesterName() : "Unknown"
                    ))
                    .correlationId(event.getCorrelationId())
                    .createdBy("system")
                    .build();

            notificationService.sendNotification(request);
            log.info("Approval notification sent for event: {}", event.getCorrelationId());

        } catch (Exception e) {
            log.error("Failed to process approval event: {}", e.getMessage(), e);
        }
    }

    // Event DTOs
    @lombok.Data
    public static class ClinicalAlertEvent {
        private String tenantId;
        private String recipientId;
        private String recipientEmail;
        private String patientName;
        private String alertType;
        private String message;
        private String severity;
        private String correlationId;
    }

    @lombok.Data
    public static class CareGapEvent {
        private String tenantId;
        private String recipientId;
        private String recipientEmail;
        private String patientName;
        private String measureName;
        private Integer gapCount;
        private String dueDate;
        private String correlationId;
    }

    @lombok.Data
    public static class ApprovalEvent {
        private String tenantId;
        private String recipientId;
        private String recipientEmail;
        private String eventType;
        private String requestType;
        private String requestId;
        private String status;
        private String requesterName;
        private String correlationId;
    }
}
