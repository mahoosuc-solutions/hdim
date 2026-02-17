package com.healthdata.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Analytics Service - Dashboards, KPIs, Reports, and Alerts
 *
 * Provides analytics capabilities including:
 * - Configurable dashboards with widgets
 * - KPI aggregation from quality-measure, HCC, and care-gap services
 * - Report generation and scheduling
 * - Threshold-based alerting
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.analytics",
    "com.healthdata.common",
    "com.healthdata.authentication",
    "com.healthdata.security",
    "com.healthdata.audit"
})
@EntityScan(basePackages = "com.healthdata.analytics.persistence")
@EnableJpaRepositories(basePackages = "com.healthdata.analytics.repository")
@EnableScheduling
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
