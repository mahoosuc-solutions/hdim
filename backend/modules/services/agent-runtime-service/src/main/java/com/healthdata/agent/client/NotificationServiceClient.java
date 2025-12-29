package com.healthdata.agent.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for Notification Service communication.
 *
 * Note: The notification-service uses context-path /notification and
 * X-Tenant-ID header for multi-tenancy.
 */
@FeignClient(
    name = "notification-service",
    url = "${hdim.services.notification.url:http://notification-service:8107}"
)
public interface NotificationServiceClient {

    /**
     * Send a notification.
     *
     * @param tenantId Tenant identifier (passed via X-Tenant-ID header)
     * @param notification Notification details including recipientId, channel, subject, body, etc.
     * @return Created notification with ID and status
     */
    @PostMapping("/notification/api/v1/notifications")
    Map<String, Object> sendNotification(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestBody Map<String, Object> notification
    );

    /**
     * Send bulk notifications.
     *
     * @param tenantId Tenant identifier
     * @param request Bulk notification request with list of notifications
     * @return Bulk response with success/failure counts
     */
    @PostMapping("/notification/api/v1/notifications/bulk")
    Map<String, Object> sendBulkNotifications(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestBody Map<String, Object> request
    );

    /**
     * Get notification by ID.
     *
     * @param tenantId Tenant identifier
     * @param notificationId Notification UUID
     * @return Notification details including status
     */
    @GetMapping("/notification/api/v1/notifications/{notificationId}")
    Map<String, Object> getNotification(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("notificationId") String notificationId
    );

    /**
     * Get notification templates.
     *
     * @param tenantId Tenant identifier
     * @return List of available templates
     */
    @GetMapping("/notification/api/v1/templates")
    Map<String, Object> getTemplates(
        @RequestHeader("X-Tenant-ID") String tenantId
    );

    /**
     * Get template by code.
     *
     * @param tenantId Tenant identifier
     * @param code Template code (e.g., "clinical-alert", "care-gap-alert")
     * @return Template details
     */
    @GetMapping("/notification/api/v1/templates/code/{code}")
    Map<String, Object> getTemplateByCode(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("code") String code
    );

    /**
     * Get user notification preferences.
     *
     * @param tenantId Tenant identifier
     * @param userId User identifier
     * @return User's notification preferences per channel
     */
    @GetMapping("/notification/api/v1/preferences/{userId}")
    List<Map<String, Object>> getUserPreferences(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("userId") String userId
    );
}
