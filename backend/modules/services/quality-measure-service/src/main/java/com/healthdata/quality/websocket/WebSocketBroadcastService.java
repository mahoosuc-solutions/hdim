package com.healthdata.quality.websocket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.dto.notification.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic WebSocket Notification Broadcasting Service
 *
 * Provides centralized service for broadcasting notifications via WebSocket.
 * Supports all notification types: CRITICAL_ALERT, HEALTH_SCORE_UPDATE,
 * APPOINTMENT_REMINDER, MEDICATION_REMINDER, LAB_RESULT, DAILY_DIGEST, CARE_GAP.
 *
 * Features:
 * - Generic NotificationRequest broadcasting (not limited to specific alert types)
 * - Tenant-based isolation for multi-tenancy
 * - User-specific broadcasting
 * - Role-based broadcasting (e.g., all doctors in a tenant)
 * - JSON message formatting for client consumption
 * - Support for different message types and priorities
 *
 * Usage:
 * <pre>
 * // Broadcast to all users in a tenant
 * broadcastService.broadcastNotification("TENANT001", notificationRequest);
 *
 * // Broadcast to specific user
 * broadcastService.broadcastToUser("user-123", notificationRequest);
 *
 * // Broadcast to role
 * broadcastService.broadcastToRole("TENANT001", "DOCTOR", notificationRequest);
 * </pre>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketBroadcastService {

    private final HealthScoreWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * Broadcast notification to all sessions for a specific tenant
     *
     * @param tenantId Tenant ID for filtering
     * @param notification Notification request containing all notification data
     * @return true if message was sent successfully to at least one client
     */
    public boolean broadcastNotification(String tenantId, NotificationRequest notification) {
        log.debug("Broadcasting {} notification to tenant: {}",
                notification.getNotificationType(), tenantId);

        try {
            Map<String, Object> message = buildNotificationMessage(notification);
            return webSocketHandler.broadcastGenericNotification(tenantId, message);
        } catch (Exception e) {
            log.error("Failed to broadcast notification {} to tenant {}: {}",
                    notification.getNotificationId(), tenantId, e.getMessage());
            return false;
        }
    }

    /**
     * Broadcast notification to a specific user
     *
     * @param userId User ID to send notification to
     * @param notification Notification request containing all notification data
     * @return true if message was sent successfully to the user
     */
    public boolean broadcastToUser(String userId, NotificationRequest notification) {
        log.debug("Broadcasting {} notification to user: {}",
                notification.getNotificationType(), userId);

        try {
            Map<String, Object> message = buildNotificationMessage(notification);
            return webSocketHandler.broadcastToUser(userId, notification.getTenantId(), message);
        } catch (Exception e) {
            log.error("Failed to broadcast notification {} to user {}: {}",
                    notification.getNotificationId(), userId, e.getMessage());
            return false;
        }
    }

    /**
     * Broadcast notification to all users with a specific role within a tenant
     *
     * @param tenantId Tenant ID for filtering
     * @param role Role to broadcast to (e.g., "DOCTOR", "NURSE", "CARE_COORDINATOR")
     * @param notification Notification request containing all notification data
     * @return true if message was sent successfully to at least one user with the role
     */
    public boolean broadcastToRole(String tenantId, String role, NotificationRequest notification) {
        log.debug("Broadcasting {} notification to role {} in tenant: {}",
                notification.getNotificationType(), role, tenantId);

        try {
            Map<String, Object> message = buildNotificationMessage(notification);
            return webSocketHandler.broadcastToRole(tenantId, role, message);
        } catch (Exception e) {
            log.error("Failed to broadcast notification {} to role {} in tenant {}: {}",
                    notification.getNotificationId(), role, tenantId, e.getMessage());
            return false;
        }
    }

    /**
     * Format notification as JSON string for client consumption
     *
     * @param notification Notification request to format
     * @return JSON string representation of the notification
     * @throws JsonProcessingException if JSON serialization fails
     */
    public String formatNotificationMessage(NotificationRequest notification) throws JsonProcessingException {
        Map<String, Object> message = buildNotificationMessage(notification);
        return objectMapper.writeValueAsString(message);
    }

    /**
     * Build notification message structure
     *
     * Message format:
     * {
     *   "type": "NOTIFICATION",
     *   "notificationType": "CRITICAL_ALERT" | "HEALTH_SCORE_UPDATE" | ...,
     *   "title": "Notification title",
     *   "message": "Notification message",
     *   "severity": "CRITICAL" | "HIGH" | "MEDIUM" | "LOW" | null,
     *   "patientId": "patient-123" | null,
     *   "tenantId": "TENANT001",
     *   "timestamp": 1234567890123,
     *   "notificationId": "notif-123",
     *   "relatedEntityId": "entity-456",
     *   "data": { ... template variables ... },
     *   "metadata": { ... additional metadata ... }
     * }
     *
     * @param notification Notification request
     * @return Map containing message structure
     */
    private Map<String, Object> buildNotificationMessage(NotificationRequest notification) {
        Map<String, Object> message = new HashMap<>();

        // Message type - always "NOTIFICATION" for generic notifications
        message.put("type", "NOTIFICATION");

        // Notification type (CRITICAL_ALERT, HEALTH_SCORE_UPDATE, etc.)
        message.put("notificationType", notification.getNotificationType());

        // Core notification fields
        message.put("title", notification.getTitle());
        message.put("message", notification.getMessage());
        message.put("severity", notification.getSeverity());
        message.put("patientId", notification.getPatientId());
        message.put("tenantId", notification.getTenantId());

        // Timestamp (convert Instant to epoch millis for JSON)
        if (notification.getTimestamp() != null) {
            message.put("timestamp", notification.getTimestamp().toEpochMilli());
        } else {
            message.put("timestamp", System.currentTimeMillis());
        }

        // Notification identifiers
        message.put("notificationId", notification.getNotificationId());
        message.put("relatedEntityId", notification.getRelatedEntityId());

        // Template variables (for client-side rendering)
        message.put("data", notification.getTemplateVariables());

        // Additional metadata
        message.put("metadata", notification.getMetadata());

        return message;
    }
}
