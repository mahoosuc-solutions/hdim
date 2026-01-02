package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.notification.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock NotificationService for testing and validation.
 *
 * Records all notification requests instead of actually sending them,
 * allowing full end-to-end validation without external provider dependencies.
 *
 * Features:
 * - Records all notification requests with timestamps
 * - Tracks channel-specific delivery attempts
 * - Provides query methods for test assertions
 * - Thread-safe for concurrent testing
 * - Can simulate failures for error handling tests
 *
 * Usage:
 * <pre>
 * // In test
 * mockNotificationService.clear(); // Reset before test
 *
 * // Trigger notification
 * careGapService.createCareGap(...);
 *
 * // Assert notification was sent
 * List<NotificationRecord> records = mockNotificationService.getRecordsForPatient(patientId);
 * assertThat(records).hasSize(1);
 * assertThat(records.get(0).getSendEmail()).isTrue();
 * </pre>
 */
@Service
@Profile("test")
@Primary
@Slf4j
public class MockNotificationService extends com.healthdata.quality.service.NotificationService {

    /**
     * Thread-safe storage for notification records
     */
    private final Map<java.util.UUID, List<NotificationRecord>> notificationsByPatient = new ConcurrentHashMap<>();
    private final List<NotificationRecord> allNotifications = new ArrayList<>();

    /**
     * Configuration for simulating failures
     */
    private boolean simulateWebSocketFailure = false;
    private boolean simulateEmailFailure = false;
    private boolean simulateSmsFailure = false;

    /**
     * No-arg constructor for test mode - passes null dependencies to parent
     * since we override all methods anyway
     */
    public MockNotificationService() {
        super(null, null, null, null);
    }

    @Override
    public NotificationStatus sendNotification(NotificationRequest request) {
        log.info("📧 MOCK NOTIFICATION SENT:");
        log.info("   Patient: {}", request.getPatientId());
        log.info("   Type: {}", request.getNotificationType());
        log.info("   Template: {}", request.getTemplateId());
        log.info("   Channels: WebSocket={}, Email={}, SMS={}",
                request.shouldSendWebSocket(), request.shouldSendEmail(), request.shouldSendSms());

        // Record the notification
        NotificationRecord record = NotificationRecord.builder()
                .request(request)
                .timestamp(System.currentTimeMillis())
                .build();

        synchronized (allNotifications) {
            allNotifications.add(record);
        }

        notificationsByPatient.computeIfAbsent(request.getPatientId(), k -> new ArrayList<>())
                .add(record);

        // Simulate channel delivery
        Map<String, Boolean> channelStatus = new HashMap<>();

        if (request.shouldSendWebSocket()) {
            boolean success = !simulateWebSocketFailure;
            channelStatus.put("WEBSOCKET", success);
            log.info("   ✓ WebSocket: {}", success ? "SENT" : "FAILED");
        }

        if (request.shouldSendEmail()) {
            boolean success = !simulateEmailFailure;
            channelStatus.put("EMAIL", success);
            log.info("   ✓ Email: {}", success ? "SENT" : "FAILED");
        }

        if (request.shouldSendSms()) {
            boolean success = !simulateSmsFailure;
            channelStatus.put("SMS", success);
            log.info("   ✓ SMS: {}", success ? "SENT" : "FAILED");
        }

        boolean allSuccessful = channelStatus.values().stream().allMatch(Boolean::booleanValue);

        return NotificationStatus.builder()
                .allSuccessful(allSuccessful)
                .channelStatus(channelStatus)
                .build();
    }

    /**
     * Get all notification records for a specific patient
     */
    public List<NotificationRecord> getRecordsForPatient(java.util.UUID patientId) {
        return new ArrayList<>(notificationsByPatient.getOrDefault(patientId, List.of()));
    }

    /**
     * Get all notification records
     */
    public List<NotificationRecord> getAllRecords() {
        synchronized (allNotifications) {
            return new ArrayList<>(allNotifications);
        }
    }

    /**
     * Get notification records by type
     */
    public List<NotificationRecord> getRecordsByType(String notificationType) {
        synchronized (allNotifications) {
            return allNotifications.stream()
                    .filter(r -> r.getRequest().getNotificationType().equals(notificationType))
                    .toList();
        }
    }

    /**
     * Get notification records by template
     */
    public List<NotificationRecord> getRecordsByTemplate(String templateId) {
        synchronized (allNotifications) {
            return allNotifications.stream()
                    .filter(r -> r.getRequest().getTemplateId().equals(templateId))
                    .toList();
        }
    }


    /**
     * Count notifications sent via specific channel
     */
    public long countByChannel(String channel) {
        synchronized (allNotifications) {
            return allNotifications.stream()
                    .filter(r -> {
                        NotificationRequest req = r.getRequest();
                        return switch (channel.toUpperCase()) {
                            case "WEBSOCKET" -> req.shouldSendWebSocket();
                            case "EMAIL" -> req.shouldSendEmail();
                            case "SMS" -> req.shouldSendSms();
                            default -> false;
                        };
                    })
                    .count();
        }
    }

    /**
     * Clear all recorded notifications (call before each test)
     */
    public void clear() {
        synchronized (allNotifications) {
            allNotifications.clear();
        }
        notificationsByPatient.clear();
        resetFailureSimulation();
        log.info("🧹 Mock notification service cleared");
    }

    /**
     * Get total notification count
     */
    public int getNotificationCount() {
        synchronized (allNotifications) {
            return allNotifications.size();
        }
    }

    /**
     * Simulate WebSocket failure for error handling tests
     */
    public void simulateWebSocketFailure(boolean enabled) {
        this.simulateWebSocketFailure = enabled;
        log.info("WebSocket failure simulation: {}", enabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Simulate Email failure for error handling tests
     */
    public void simulateEmailFailure(boolean enabled) {
        this.simulateEmailFailure = enabled;
        log.info("Email failure simulation: {}", enabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Simulate SMS failure for error handling tests
     */
    public void simulateSmsFailure(boolean enabled) {
        this.simulateSmsFailure = enabled;
        log.info("SMS failure simulation: {}", enabled ? "ENABLED" : "DISABLED");
    }

    /**
     * Reset all failure simulations
     */
    public void resetFailureSimulation() {
        this.simulateWebSocketFailure = false;
        this.simulateEmailFailure = false;
        this.simulateSmsFailure = false;
    }

    /**
     * Record of a sent notification
     */
    @lombok.Data
    @lombok.Builder
    public static class NotificationRecord {
        private NotificationRequest request;
        private long timestamp;

        public boolean wasSentViaWebSocket() {
            return request.shouldSendWebSocket();
        }

        public boolean wasSentViaEmail() {
            return request.shouldSendEmail();
        }

        public boolean wasSentViaSms() {
            return request.shouldSendSms();
        }

        public String getNotificationType() {
            return request.getNotificationType();
        }

        public java.util.UUID getPatientId() {
            return request.getPatientId();
        }

        public String getTemplateId() {
            return request.getTemplateId();
        }
    }
}
