package com.healthdata.payer;

import com.healthdata.authentication.config.JwtConfig;
import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.service.JwtTokenService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payer-Specific Workflows Service - Medicare Advantage Star Ratings and Medicaid Compliance
 *
 * Provides comprehensive payer-specific workflows including:
 * - Medicare Advantage Star Ratings calculation (based on HEDIS measures)
 * - Star Rating measure mapping and domain scoring
 * - Medicaid state-specific compliance reporting
 * - Payer dashboard aggregation and analytics
 * - Plan-level and MCO performance metrics
 * - Provider attribution and performance tracking
 *
 * Integrates with Quality Measure Service for HEDIS measure data.
 *
 * Key Features:
 * - CMS 2024 Star Rating methodology implementation
 * - Support for 50+ HEDIS measures mapped to Star Rating categories
 * - State-specific Medicaid compliance (NY, CA, TX, FL, and more)
 * - Multi-tenant architecture (payer as tenant)
 * - Real-time dashboard metrics
 * - Year-over-year improvement tracking
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.payer"
})
@Import({
    // Bring in only the JWT pieces from the authentication module; gateway owns the rest
    JwtAuthenticationFilter.class,
    JwtTokenService.class,
    JwtConfig.class
})
@EnableFeignClients
@EnableCaching
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "com.healthdata.payer.persistence"
})
@EntityScan(basePackages = {
    "com.healthdata.payer.domain"
})
public class PayerWorkflowsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayerWorkflowsServiceApplication.class, args);
    }
}
