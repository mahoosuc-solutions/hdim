package com.healthdata.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Demo Seeding Service - Generates synthetic patient data and manages demo scenarios
 *
 * Purpose: Enable professional video demonstrations of HDIM capabilities
 *
 * Key Features:
 * - Synthetic FHIR R4 patient data generation
 * - Pre-configured demo scenarios (HEDIS, Patient Journey, Risk, Multi-Tenant)
 * - One-command reset capability
 * - Performance-optimized for video recording
 *
 * @version 1.0.0
 * @since 2026-01-03
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.demo",
    "com.healthdata.shared"
})
@EnableCaching
@EnableJpaRepositories
public class DemoSeedingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoSeedingApplication.class, args);
    }
}
