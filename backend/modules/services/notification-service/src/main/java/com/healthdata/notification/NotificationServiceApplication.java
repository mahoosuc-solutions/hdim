package com.healthdata.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Notification Service - Multi-channel notification delivery
 *
 * Provides centralized notification management for the HDIM platform including:
 * - Email notifications (SMTP/SES)
 * - SMS notifications (Twilio/AWS SNS)
 * - Push notifications (Firebase/APNs)
 * - In-app notifications
 *
 * Features:
 * - Template-based message generation
 * - User preference management
 * - Quiet hours support
 * - Delivery tracking and retry
 * - HIPAA-compliant audit logging
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.notification",
    "com.healthdata.authentication",
    "com.healthdata.security",
    "com.healthdata.audit",
    "com.healthdata.tracing"
})
@EnableFeignClients
@EnableCaching
@EnableAsync
@EnableJpaRepositories(basePackages = {
    "com.healthdata.notification.domain.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.notification.domain.model"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
