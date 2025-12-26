package com.healthdata.quality;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Quality Measure Service - HEDIS Quality Measure Calculation and Reporting
 *
 * Calculates HEDIS quality measures for patients and provides comprehensive
 * quality reporting. Integrates with Patient Service for health data,
 * Care Gap Service for gap tracking, and CQL Engine for measure calculations.
 *
 * Authentication: Uses AuthenticationAutoConfiguration for JWT validation.
 * Gateway-only services (LogoutService, MfaService, etc.) are not loaded
 * because they have no @Service annotation - they're explicitly configured
 * only in the Gateway service.
 *
 * See: /backend/AUTHENTICATION-ARCHITECTURE.md
 *
 * Supported measures:
 * - Comprehensive Diabetes Care (CDC)
 * - Controlling High Blood Pressure (CBP)
 * - Breast Cancer Screening (BCS)
 * - Colorectal Cancer Screening (COL)
 * - Cervical Cancer Screening (CCS)
 * - Immunization measures
 * - And more...
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.quality"
    }
)
@EnableFeignClients
@EnableCaching
@EnableScheduling  // Enable @Scheduled annotations for WebSocket session timeout monitoring (HIPAA §164.312(a)(2)(iii))
@EnableJpaRepositories(basePackages = {
    "com.healthdata.quality.persistence"
    // NOTE: Authentication repositories removed - managed by Gateway service
})
@EntityScan(basePackages = {
    "com.healthdata.quality.persistence"
    // NOTE: Authentication entities removed - managed by Gateway service
})
public class QualityMeasureServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QualityMeasureServiceApplication.class, args);
    }
}
