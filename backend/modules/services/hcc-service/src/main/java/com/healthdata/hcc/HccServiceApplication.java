package com.healthdata.hcc;

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
 * HCC Risk Adjustment Service Application
 *
 * Provides HCC (Hierarchical Condition Category) risk adjustment capabilities
 * for Medicare Advantage and other value-based care programs.
 *
 * Key Features:
 * - Dual-model RAF calculation (V24 and V28)
 * - V24 to V28 crosswalk and transition analysis
 * - Documentation gap identification
 * - HCC recapture opportunity tracking
 * - Coding accuracy analysis
 *
 * CMS Timeline:
 * - 2024: 67% V24 / 33% V28
 * - 2025: 33% V24 / 67% V28
 * - 2026: 100% V28
 *
 * @see <a href="https://www.cms.gov/medicare/health-plans/medicareadvtgspecratestats/risk-adjustors">CMS HCC Risk Adjustment</a>
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.hcc",
    "com.healthdata.authentication",
    "com.healthdata.audit"
})
@Import({JwtAuthenticationFilter.class, JwtTokenService.class, JwtConfig.class})
@EnableFeignClients
@EnableCaching
@EnableScheduling
@EnableJpaRepositories(basePackages = {"com.healthdata.hcc.persistence"})
@EntityScan(basePackages = {"com.healthdata.hcc.persistence"})
public class HccServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HccServiceApplication.class, args);
    }
}
