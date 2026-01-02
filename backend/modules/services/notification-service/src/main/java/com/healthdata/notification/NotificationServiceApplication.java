package com.healthdata.notification;

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
    "com.healthdata.common"
})
@EnableJpaRepositories(basePackages = "com.healthdata.notification.domain.repository")
@EntityScan(basePackages = {
    "com.healthdata.notification.domain.model",
    "com.healthdata.audit.entity",
    "com.healthdata.authentication.domain"
})
@EnableAsync
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
