package com.healthdata.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CMS Connector Service Application
 * 
 * Integrates with Centers for Medicare & Medicaid Services (CMS) APIs including:
 * - BCDA (Beneficiary Claims Data API) - Bulk weekly claims exports
 * - DPC (Data at Point of Care) - Real-time claim queries during patient encounters
 * - Blue Button 2.0 - Beneficiary-initiated data sharing
 * - AB2D - Medicare Part D claims for PDP sponsors
 * 
 * Features:
 * - OAuth2 client credentials flow for API authentication
 * - FHIR data parsing and normalization
 * - Redis caching for performance optimization
 * - Resilience patterns (circuit breaker, retry) for reliability
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class CmsConnectorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmsConnectorServiceApplication.class, args);
    }
}
