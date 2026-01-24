package com.healthdata.notification.application;

import com.healthdata.featureflags.TenantFeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Appointment Reminder Scheduler
 *
 * Runs daily to send SMS reminders for upcoming appointments.
 *
 * Schedule:
 * - Runs every day at 9:00 AM server time
 * - Processes reminders for 1, 3, and 7 days before appointments
 * - Only runs for tenants with twilio-sms-reminders feature enabled
 *
 * Configuration:
 * - Enable/disable via: notification.reminders.enabled=true
 * - Default: enabled
 *
 * Multi-Tenant:
 * - Finds all tenants with feature enabled
 * - Processes each tenant independently
 * - Tenant-specific configuration respected (reminder_days)
 */
@Component
@ConditionalOnProperty(
    prefix = "notification.reminders",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final TenantFeatureFlagService featureFlagService;
    private final AppointmentReminderService appointmentReminderService;

    private static final String FEATURE_KEY = "twilio-sms-reminders";
    private static final List<Integer> REMINDER_DAYS = List.of(1, 3, 7);

    /**
     * Send appointment reminders
     *
     * Cron: 0 0 9 * * * = Every day at 9:00 AM
     *
     * Processing:
     * 1. Find all tenants with twilio-sms-reminders enabled
     * 2. For each tenant, process reminders for 1, 3, and 7 days before
     * 3. Tenant-specific config determines which reminder days are active
     */
    @Scheduled(cron = "${notification.reminders.cron:0 0 9 * * *}")
    public void sendAppointmentReminders() {
        log.info("Starting appointment reminder scheduler job");

        try {
            // Find all tenants with Twilio SMS reminders enabled
            List<String> tenants = featureFlagService.findTenantsWithFeatureEnabled(FEATURE_KEY);
            log.info("Found {} tenants with {} feature enabled", tenants.size(), FEATURE_KEY);

            if (tenants.isEmpty()) {
                log.info("No tenants have Twilio SMS reminders enabled, skipping");
                return;
            }

            // Process each tenant
            for (String tenantId : tenants) {
                try {
                    processTenantReminders(tenantId);
                } catch (Exception e) {
                    log.error("Failed to process reminders for tenant {}: {}",
                            tenantId, e.getMessage(), e);
                    // Continue with next tenant - don't let one failure stop others
                }
            }

            log.info("Appointment reminder scheduler job complete");

        } catch (Exception e) {
            log.error("Fatal error in appointment reminder scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Process reminders for a single tenant
     *
     * Sends reminders for 1, 3, and 7 days before appointments
     * (based on tenant configuration).
     */
    private void processTenantReminders(String tenantId) {
        log.info("Processing reminders for tenant {}", tenantId);

        for (int daysBefore : REMINDER_DAYS) {
            try {
                appointmentReminderService.processReminders(tenantId, daysBefore);
            } catch (Exception e) {
                log.error("Failed to process {}-day reminders for tenant {}: {}",
                        daysBefore, tenantId, e.getMessage(), e);
                // Continue with next reminder interval
            }
        }
    }
}
