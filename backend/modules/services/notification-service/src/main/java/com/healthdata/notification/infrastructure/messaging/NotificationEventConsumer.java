package com.healthdata.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.notification.api.v1.dto.SendNotificationRequest;
import com.healthdata.notification.application.NotificationService;
import com.healthdata.notification.domain.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer for notification events from other services.
 *
 * Listens to:
 * - clinical-alert: Care gap and clinical alerts
 * - approval-completed: Approval workflow notifications
 * - agent-notification: Agent-initiated notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    /**
     * Handle clinical alert events (care gaps, medication alerts, etc.)
     */
    @KafkaListener(topics = "clinical-alert", groupId = "notification-service")
    public void handleClinicalAlert(String message) {
        log.info("Received clinical alert event");
        try {
            JsonNode event = objectMapper.readTree(message);

            String tenantId = event.path("tenantId").asText();
            String patientId = event.path("patientId").asText();
            String alertType = event.path("alertType").asText();
            String alertMessage = event.path("message").asText();
            String recipientEmail = event.path("recipientEmail").asText(null);

            if (tenantId.isEmpty() || patientId.isEmpty()) {
                log.warn("Invalid clinical alert event: missing tenantId or patientId");
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("alertType", alertType);
            metadata.put("patientId", patientId);
            metadata.put("source", "clinical-alert");

            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId(patientId)
                .recipientEmail(recipientEmail)
                .channel(NotificationChannel.EMAIL)
                .subject("Clinical Alert: " + alertType)
                .body(alertMessage)
                .priority(2) // High priority for clinical alerts
                .metadata(metadata)
                .build();

            notificationService.sendNotification(request, tenantId);
            log.info("Clinical alert notification sent for patient: {}", patientId);

        } catch (Exception e) {
            log.error("Failed to process clinical alert event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle approval workflow completion events.
     */
    @KafkaListener(topics = "approval-completed", groupId = "notification-service")
    public void handleApprovalCompleted(String message) {
        log.info("Received approval completed event");
        try {
            JsonNode event = objectMapper.readTree(message);

            String tenantId = event.path("tenantId").asText();
            String requesterId = event.path("requesterId").asText();
            String approvalType = event.path("approvalType").asText();
            String status = event.path("status").asText();
            String requesterEmail = event.path("requesterEmail").asText(null);

            if (tenantId.isEmpty() || requesterId.isEmpty()) {
                log.warn("Invalid approval completed event: missing tenantId or requesterId");
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("approvalType", approvalType);
            metadata.put("status", status);
            metadata.put("source", "approval-workflow");

            String subject = "Approval Request " + ("APPROVED".equals(status) ? "Approved" : "Rejected");
            String body = String.format("Your %s request has been %s.", approvalType, status.toLowerCase());

            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId(requesterId)
                .recipientEmail(requesterEmail)
                .channel(NotificationChannel.EMAIL)
                .subject(subject)
                .body(body)
                .priority(3)
                .metadata(metadata)
                .build();

            notificationService.sendNotification(request, tenantId);
            log.info("Approval notification sent to requester: {}", requesterId);

        } catch (Exception e) {
            log.error("Failed to process approval completed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle agent-initiated notification events.
     */
    @KafkaListener(topics = "agent-notification", groupId = "notification-service")
    public void handleAgentNotification(String message) {
        log.info("Received agent notification event");
        try {
            JsonNode event = objectMapper.readTree(message);

            String tenantId = event.path("tenantId").asText();
            String recipientId = event.path("recipientId").asText();
            String channel = event.path("channel").asText("EMAIL");
            String subject = event.path("subject").asText();
            String body = event.path("body").asText();
            String recipientEmail = event.path("recipientEmail").asText(null);

            if (tenantId.isEmpty() || recipientId.isEmpty() || body.isEmpty()) {
                log.warn("Invalid agent notification event: missing required fields");
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "agent-runtime");
            if (event.has("agentId")) {
                metadata.put("agentId", event.path("agentId").asText());
            }

            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId(recipientId)
                .recipientEmail(recipientEmail)
                .channel(NotificationChannel.fromValue(channel))
                .subject(subject)
                .body(body)
                .priority(event.path("priority").asInt(5))
                .metadata(metadata)
                .build();

            notificationService.sendNotification(request, tenantId);
            log.info("Agent notification sent to recipient: {}", recipientId);

        } catch (Exception e) {
            log.error("Failed to process agent notification event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle care gap notification events.
     */
    @KafkaListener(topics = "care-gap", groupId = "notification-service")
    public void handleCareGapEvent(String message) {
        log.info("Received care gap event");
        try {
            JsonNode event = objectMapper.readTree(message);

            String tenantId = event.path("tenantId").asText();
            String patientId = event.path("patientId").asText();
            String measureId = event.path("measureId").asText();
            String gapStatus = event.path("status").asText();
            String providerEmail = event.path("providerEmail").asText(null);
            String providerId = event.path("providerId").asText();

            if (tenantId.isEmpty() || patientId.isEmpty()) {
                log.warn("Invalid care gap event: missing tenantId or patientId");
                return;
            }

            // Only notify on new or reopened gaps
            if (!"OPEN".equals(gapStatus) && !"REOPENED".equals(gapStatus)) {
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("measureId", measureId);
            metadata.put("patientId", patientId);
            metadata.put("gapStatus", gapStatus);
            metadata.put("source", "care-gap-service");

            String subject = "Care Gap Alert: " + measureId;
            String body = String.format(
                "A care gap has been identified for patient %s on measure %s. " +
                "Please review and take appropriate action.",
                patientId, measureId
            );

            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId(providerId.isEmpty() ? patientId : providerId)
                .recipientEmail(providerEmail)
                .channel(NotificationChannel.EMAIL)
                .subject(subject)
                .body(body)
                .priority(3)
                .metadata(metadata)
                .build();

            notificationService.sendNotification(request, tenantId);
            log.info("Care gap notification sent for patient: {}, measure: {}", patientId, measureId);

        } catch (Exception e) {
            log.error("Failed to process care gap event: {}", e.getMessage(), e);
        }
    }
}
