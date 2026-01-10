package com.healthdata.quality;

import com.healthdata.quality.service.MeasureDefinitionSeedingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

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
@SpringBootApplication
@ComponentScan(
    basePackages = {
        "com.healthdata.quality",
        "com.healthdata.authentication"  // For Tier 1: JwtAuthenticationFilter, JwtTokenService, CookieService
    },
    excludeFilters = {
        // Exclude Tier 2 authentication controllers (Gateway-only)
        // These require database repositories (UserRepository, ApiKeyRepository, etc.)
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.authentication\\.controller\\..*"
        )
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
@RequiredArgsConstructor
@Slf4j
public class QualityMeasureServiceApplication {

    private final MeasureDefinitionSeedingService measureSeedingService;

    @Value("${healthdata.quality.auto-seed-measures:true}")
    private boolean autoSeedMeasures;

    @Value("${healthdata.quality.default-tenant:acme-health}")
    private String defaultTenant;

    public static void main(String[] args) {
        SpringApplication.run(QualityMeasureServiceApplication.class, args);
    }

    /**
     * Automatically seed HEDIS measure definitions on application startup.
     *
     * This ensures that the quality measure service has all necessary measure
     * definitions available immediately after startup, preventing evaluation
     * failures due to missing measure metadata.
     *
     * Configuration:
     * - healthdata.quality.auto-seed-measures: Enable/disable auto-seeding (default: true)
     * - healthdata.quality.default-tenant: Tenant ID to seed measures for (default: acme-health)
     *
     * The seeding service uses an idempotent approach - existing measures are not duplicated.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedMeasuresOnStartup() {
        if (!autoSeedMeasures) {
            log.info("Auto-seeding of HEDIS measures is disabled via configuration");
            return;
        }

        try {
            log.info("Auto-seeding HEDIS measure definitions for tenant: {}", defaultTenant);
            int seededCount = measureSeedingService.seedHedisMeasures(defaultTenant);
            long totalCount = measureSeedingService.getMeasureCount(defaultTenant);

            log.info("HEDIS measure seeding complete: {} new measures seeded, {} total measures available for tenant: {}",
                seededCount, totalCount, defaultTenant);
        } catch (Exception e) {
            log.error("Failed to auto-seed HEDIS measures for tenant: {}. Evaluation may fail without measure definitions.",
                defaultTenant, e);
            // Don't throw exception - allow service to start even if seeding fails
            // This prevents startup failures in environments where database is read-only
        }
    }
}
