package com.healthdata.quality.service;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.dto.notification.ClinicalAlertNotificationRequest;
import com.healthdata.quality.dto.notification.NotificationRequest;
import com.healthdata.quality.service.notification.EmailNotificationChannel;
import com.healthdata.quality.service.notification.SmsNotificationChannel;
import com.healthdata.quality.websocket.HealthScoreWebSocketHandler;
import com.healthdata.quality.websocket.WebSocketBroadcastService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-Channel Notification Service
 *
 * Routes notifications to appropriate channels based on NotificationRequest configuration.
 * Supports all notification types: CRITICAL_ALERT, HEALTH_SCORE_UPDATE, APPOINTMENT_REMINDER,
 * MEDICATION_REMINDER, LAB_RESULT, DAILY_DIGEST.
 *
 * Channel routing:
 * - WebSocket: Real-time updates to connected clients
 * - Email: HTML email notifications via JavaMailSender (optional - only if mail configured)
 * - SMS: Text message notifications (ready for Twilio integration)
 *
 * All notifications tracked in notification_history table for HIPAA compliance.
 */
@Service
@Slf4j
public class NotificationService {

    private final HealthScoreWebSocketHandler webSocketHandler;
    private final WebSocketBroadcastService webSocketBroadcastService;
    private final EmailNotificationChannel emailChannel;
    private final SmsNotificationChannel smsChannel;

    public NotificationService(
            HealthScoreWebSocketHandler webSocketHandler,
            WebSocketBroadcastService webSocketBroadcastService,
            @Autowired(required = false) EmailNotificationChannel emailChannel,
            SmsNotificationChannel smsChannel) {
        this.webSocketHandler = webSocketHandler;
        this.webSocketBroadcastService = webSocketBroadcastService;
        this.emailChannel = emailChannel;
        this.smsChannel = smsChannel;

        if (emailChannel == null) {
            log.warn("EmailNotificationChannel not available - email notifications disabled. Configure mail properties to enable.");
        }
    }

    /**
     * Send notification via appropriate channels based on NotificationRequest configuration
     * Universal method that works with all notification types
     *
     * @param request NotificationRequest containing routing and template information
     * @return NotificationStatus with delivery results for each channel
     */
    public NotificationStatus sendNotification(NotificationRequest request) {
        log.info("Sending {} notification for patient {} via requested channels",
                request.getNotificationType(), request.getPatientId());

        Map<String, Boolean> channelStatus = new HashMap<>();
        String tenantId = request.getTenantId();

        try {
            // WebSocket channel - now using generic WebSocketBroadcastService
            if (request.shouldSendWebSocket()) {
                try {
                    // Use generic broadcast service for all notification types
                    boolean webSocketSuccess = webSocketBroadcastService.broadcastNotification(
                            tenantId, request);
                    channelStatus.put("websocket", webSocketSuccess);

                    log.debug("WebSocket broadcast {} for notification type: {} to tenant: {}",
                            webSocketSuccess ? "succeeded" : "failed",
                            request.getNotificationType(), tenantId);
                } catch (Exception e) {
                    channelStatus.put("websocket", false);
                    log.error("WebSocket notification failed for {}: {}",
                            request.getNotificationId(), e.getMessage());
                }
            }

            // Email channel
            if (request.shouldSendEmail()) {
                if (emailChannel != null) {
                    try {
                        channelStatus.put("email", emailChannel.send(request));
                    } catch (Exception e) {
                        channelStatus.put("email", false);
                        log.error("Email notification failed for {}: {}",
                                request.getNotificationId(), e.getMessage());
                    }
                } else {
                    log.debug("Email notification requested but EmailNotificationChannel not available");
                    channelStatus.put("email", false);
                }
            }

            // SMS channel
            if (request.shouldSendSms()) {
                try {
                    channelStatus.put("sms", smsChannel.send(request));
                } catch (Exception e) {
                    channelStatus.put("sms", false);
                    log.error("SMS notification failed for {}: {}",
                            request.getNotificationId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Notification failed for {}: {}",
                    request.getNotificationId(), e.getMessage());
        }

        return NotificationStatus.builder()
                .alertId(request.getNotificationId())
                .channelStatus(channelStatus)
                .allSuccessful(channelStatus.isEmpty() ||
                        channelStatus.values().stream().allMatch(Boolean::booleanValue))
                .build();
    }

    /**
     * Send notification via appropriate channels based on severity
     * Backward compatibility method for ClinicalAlertDTO
     *
     * @deprecated Use {@link #sendNotification(NotificationRequest)} instead
     */
    @Deprecated
    public void sendNotification(String tenantId, ClinicalAlertDTO alert) {
        log.info("Sending {} alert notification for patient {} via appropriate channels",
            alert.getSeverity(), alert.getPatientId());

        String severity = alert.getSeverity();

        try {
            // WebSocket - all severities
            webSocketHandler.broadcastClinicalAlert(alert, tenantId);

            // Email - CRITICAL and HIGH
            if (("CRITICAL".equals(severity) || "HIGH".equals(severity)) && emailChannel != null) {
                try {
                    emailChannel.send(tenantId, alert);
                } catch (Exception e) {
                    log.error("Email notification failed for alert {}: {}",
                        alert.getId(), e.getMessage());
                }
            }

            // SMS - CRITICAL only
            if ("CRITICAL".equals(severity)) {
                try {
                    smsChannel.send(tenantId, alert);
                } catch (Exception e) {
                    log.error("SMS notification failed for alert {}: {}",
                        alert.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Notification failed for alert {}: {}", alert.getId(), e.getMessage());
        }
    }

    /**
     * Send batch notifications
     */
    public void sendBatchNotification(String tenantId, List<ClinicalAlertDTO> alerts) {
        log.info("Sending batch notification for {} alerts", alerts.size());

        for (ClinicalAlertDTO alert : alerts) {
            sendNotification(tenantId, alert);
        }
    }

    /**
     * Send notification and track delivery status
     */
    public NotificationStatus sendNotificationWithStatus(String tenantId, ClinicalAlertDTO alert) {
        Map<String, Boolean> channelStatus = new HashMap<>();

        try {
            channelStatus.put("websocket", webSocketHandler.broadcastClinicalAlert(alert, tenantId));
        } catch (Exception e) {
            channelStatus.put("websocket", false);
            log.error("WebSocket notification failed: {}", e.getMessage());
        }

        String severity = alert.getSeverity();

        if (("CRITICAL".equals(severity) || "HIGH".equals(severity)) && emailChannel != null) {
            try {
                channelStatus.put("email", emailChannel.send(tenantId, alert));
            } catch (Exception e) {
                channelStatus.put("email", false);
                log.error("Email notification failed: {}", e.getMessage());
            }
        }

        if ("CRITICAL".equals(severity)) {
            try {
                channelStatus.put("sms", smsChannel.send(tenantId, alert));
            } catch (Exception e) {
                channelStatus.put("sms", false);
                log.error("SMS notification failed: {}", e.getMessage());
            }
        }

        return NotificationStatus.builder()
            .alertId(alert.getId())
            .channelStatus(channelStatus)
            .allSuccessful(channelStatus.values().stream().allMatch(Boolean::booleanValue))
            .build();
    }

    /**
     * Notification delivery status
     */
    @Data
    @Builder
    public static class NotificationStatus {
        private String alertId;
        private Map<String, Boolean> channelStatus;
        private boolean allSuccessful;
    }
}
