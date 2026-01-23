package com.healthdata.notification;

import com.healthdata.featureflags.EnableFeatureFlags;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Service Application.
 *
 * Provides notification capabilities for the HDIM platform:
 * - Email notifications (SMTP/SES)
 * - SMS notifications (future)
 * - Push notifications (future)
 * - In-app notifications
 *
 * Features:
 * - Template-based notifications with variable substitution
 * - User notification preferences
 * - Quiet hours support
 * - Delivery tracking and retry
 * - Bulk notification support
 * - Kafka event consumption for async notifications
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.notification",
    "com.healthdata.authentication",
    "com.healthdata.security",
    "com.healthdata.audit",
    "com.healthdata.common",
    "com.healthdata.featureflags"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.notification.domain.repository",
    "com.healthdata.featureflags"
})
@EntityScan(basePackages = {
    "com.healthdata.notification.domain.model",
    "com.healthdata.audit.entity",
    "com.healthdata.authentication.domain",
    "com.healthdata.featureflags"
})
@EnableAsync
@EnableScheduling
@EnableFeatureFlags
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
