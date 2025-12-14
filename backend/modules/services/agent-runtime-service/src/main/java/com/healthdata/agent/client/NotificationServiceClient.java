package com.healthdata.agent.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for Notification Service communication.
 */
@FeignClient(
    name = "notification-service",
    url = "${hdim.services.notification.url:http://notification-service:8080}"
)
public interface NotificationServiceClient {

    /**
     * Send a notification.
     */
    @PostMapping("/api/v1/notifications/{tenantId}")
    Map<String, Object> sendNotification(
        @PathVariable("tenantId") String tenantId,
        @RequestBody Map<String, Object> notification
    );

    /**
     * Send batch notifications.
     */
    @PostMapping("/api/v1/notifications/{tenantId}/batch")
    Map<String, Object> sendBatchNotifications(
        @PathVariable("tenantId") String tenantId,
        @RequestBody List<Map<String, Object>> notifications
    );

    /**
     * Get notification status.
     */
    @GetMapping("/api/v1/notifications/{tenantId}/{notificationId}/status")
    Map<String, Object> getNotificationStatus(
        @PathVariable("tenantId") String tenantId,
        @PathVariable("notificationId") String notificationId
    );

    /**
     * Get notification templates.
     */
    @GetMapping("/api/v1/notifications/{tenantId}/templates")
    List<Map<String, Object>> getTemplates(
        @PathVariable("tenantId") String tenantId
    );

    /**
     * Verify patient notification consent.
     */
    @GetMapping("/api/v1/notifications/{tenantId}/consent/{patientId}")
    Map<String, Object> verifyConsent(
        @PathVariable("tenantId") String tenantId,
        @PathVariable("patientId") String patientId,
        @RequestParam("channel") String channel
    );
}
