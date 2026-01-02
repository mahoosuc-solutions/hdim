package com.healthdata.notification.config;

import com.healthdata.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for notification-related background tasks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final NotificationService notificationService;

    /**
     * Process scheduled notifications every minute.
     */
    @Scheduled(fixedRate = 60000)
    public void processScheduledNotifications() {
        log.debug("Processing scheduled notifications...");
        notificationService.processScheduledNotifications();
    }

    /**
     * Retry failed notifications every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    public void retryFailedNotifications() {
        log.debug("Retrying failed notifications...");
        notificationService.retryFailedNotifications();
    }
}
