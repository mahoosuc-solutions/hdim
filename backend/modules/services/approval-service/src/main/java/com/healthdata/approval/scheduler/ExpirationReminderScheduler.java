package com.healthdata.approval.scheduler;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.notification.EmailNotificationService;
import com.healthdata.approval.repository.ApprovalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled job to send expiration reminder emails for approval requests.
 *
 * Features:
 * - Runs hourly to check for requests expiring soon
 * - Sends reminder emails at 4h, 2h, and 1h before expiration
 * - Uses Redis to track which reminders have been sent (to avoid duplicates)
 * - Graceful fallback if Redis is unavailable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpirationReminderScheduler {

    private final ApprovalRequestRepository requestRepository;
    private final EmailNotificationService emailNotificationService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${hdim.approval.reminder.enabled:true}")
    private boolean reminderEnabled;

    @Value("${hdim.approval.reminder.hours-before:4}")
    private int reminderHoursBefore;

    private static final String REMINDER_KEY_PREFIX = "hdim:approval:reminder:";
    private static final Duration REMINDER_KEY_TTL = Duration.ofHours(24);

    /**
     * Scheduled job to send expiration reminders.
     * Runs every hour.
     */
    @Scheduled(fixedRateString = "${hdim.approval.reminder.check-interval-ms:3600000}")
    public void sendExpirationReminders() {
        if (!reminderEnabled) {
            log.debug("Expiration reminders disabled");
            return;
        }

        log.info("Starting expiration reminder check");

        try {
            // Find requests expiring within the configured time window
            Instant threshold = Instant.now().plus(Duration.ofHours(reminderHoursBefore));
            List<ApprovalRequest> expiringRequests = requestRepository.findExpiringSoonAllTenants(threshold);

            log.info("Found {} requests expiring within {} hours", expiringRequests.size(), reminderHoursBefore);

            for (ApprovalRequest request : expiringRequests) {
                sendReminderIfNeeded(request);
            }

        } catch (Exception e) {
            log.error("Failed to process expiration reminders", e);
        }
    }

    private void sendReminderIfNeeded(ApprovalRequest request) {
        if (request.getAssignedTo() == null) {
            log.debug("Request {} has no assignee, skipping reminder", request.getId());
            return;
        }

        // Determine which reminder level (4h, 2h, 1h)
        Duration timeUntilExpiration = Duration.between(Instant.now(), request.getExpiresAt());
        String reminderLevel = getReminderLevel(timeUntilExpiration);

        if (reminderLevel == null) {
            return; // Not yet time for a reminder
        }

        // Check if we've already sent this reminder
        String reminderKey = REMINDER_KEY_PREFIX + request.getId() + ":" + reminderLevel;
        if (hasReminderBeenSent(reminderKey)) {
            log.debug("Reminder {} already sent for request {}", reminderLevel, request.getId());
            return;
        }

        // Send the reminder
        try {
            // In production, we'd look up the email from a user service
            // For now, use assignedTo as the email (or could be a username)
            String recipientEmail = lookupEmail(request.getAssignedTo());
            String recipientName = request.getAssignedTo();

            if (recipientEmail != null) {
                emailNotificationService.sendExpirationReminderNotification(
                    request,
                    recipientEmail,
                    recipientName
                );

                // Mark reminder as sent
                markReminderSent(reminderKey);

                log.info("Sent {} expiration reminder for request {} to {}",
                    reminderLevel, request.getId(), recipientEmail);
            }

        } catch (Exception e) {
            log.error("Failed to send expiration reminder for request {}: {}",
                request.getId(), e.getMessage(), e);
        }
    }

    /**
     * Determine reminder level based on time until expiration.
     * Returns null if no reminder should be sent at this time.
     */
    private String getReminderLevel(Duration timeUntilExpiration) {
        long hours = timeUntilExpiration.toHours();

        if (hours <= 1) {
            return "1h";
        } else if (hours <= 2) {
            return "2h";
        } else if (hours <= 4) {
            return "4h";
        }

        return null; // Not time for a reminder yet
    }

    private boolean hasReminderBeenSent(String reminderKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(reminderKey));
        } catch (Exception e) {
            log.warn("Redis unavailable, cannot check reminder status: {}", e.getMessage());
            return false; // In case of Redis failure, try to send (better duplicate than no reminder)
        }
    }

    private void markReminderSent(String reminderKey) {
        try {
            redisTemplate.opsForValue().set(reminderKey, "sent", REMINDER_KEY_TTL.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis unavailable, cannot mark reminder as sent: {}", e.getMessage());
        }
    }

    /**
     * Look up email address for a user.
     * In production, this would call a user service.
     * For now, if the assignedTo looks like an email, use it directly.
     */
    private String lookupEmail(String userId) {
        if (userId == null) {
            return null;
        }

        // If it looks like an email, use it directly
        if (userId.contains("@")) {
            return userId;
        }

        // Otherwise, construct a default email (in production, query user service)
        // For demo purposes, return null to skip
        log.debug("Cannot determine email for user {}, skipping reminder", userId);
        return null;
    }
}
