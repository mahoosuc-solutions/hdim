package com.healthdata.investor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Investor Dashboard Service Application.
 *
 * Provides a production-grade investor launch tracking dashboard for the HDIM platform:
 * - Task management (23 pre-defined launch tasks with dependencies)
 * - Contact management (investors, partners, advisors)
 * - Outreach activity tracking (LinkedIn, email, calls, meetings)
 * - Progress visualization and reporting
 * - LinkedIn integration for automated outreach tracking
 *
 * Security:
 * - JWT-based authentication (standalone, not gateway-dependent)
 * - BCrypt password hashing
 * - Stateless sessions
 *
 * This is an internal admin tool, separate from the clinical HIPAA-compliant services.
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.investor"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.investor.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.investor.entity"
})
@EnableAsync
public class InvestorDashboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestorDashboardServiceApplication.class, args);
    }
}
