package com.healthdata.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sales Automation Service
 *
 * Provides CRM functionality for HDIM including:
 * - Lead capture and management
 * - Contact and account management
 * - Opportunity pipeline tracking
 * - Email sequence automation
 * - Zoho CRM integration
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.sales",
    "com.healthdata.shared.infrastructure.authentication",
    "com.healthdata.shared.infrastructure.security",
    "com.healthdata.shared.infrastructure.audit",
    "com.healthdata.shared.infrastructure.persistence",
    "com.healthdata.shared.infrastructure.tracing"
})
@EnableFeignClients
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class SalesAutomationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesAutomationServiceApplication.class, args);
    }
}
