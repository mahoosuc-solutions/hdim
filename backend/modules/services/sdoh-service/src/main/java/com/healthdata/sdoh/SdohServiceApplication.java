package com.healthdata.sdoh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * SDOH Integration Service - Social Determinants of Health service
 *
 * Implements Gravity Project FHIR Implementation Guide for SDOH screening,
 * Z-code mapping, community resource directory integration, and health equity analytics.
 *
 * Features:
 * - Gravity Project standardized SDOH screening (AHC-HRSN, PRAPARE)
 * - ICD-10-CM Z-code mapping (Z55-Z65)
 * - Community resource directory integration
 * - Health equity analytics and disparity measurement
 * - SDOH risk scoring and impact assessment
 * - Multi-tenant support
 */
@SpringBootApplication
@EnableFeignClients
@EnableCaching
@EnableJpaRepositories(basePackages = {
    "com.healthdata.sdoh.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.sdoh.entity"
})
public class SdohServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdohServiceApplication.class, args);
    }
}
