package com.healthdata.auditquery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Audit Query Service - Centralized HIPAA-compliant audit log query API.
 *
 * <p>Provides REST endpoints for querying audit events across all HDIM services
 * with multi-tenant isolation, role-based access control, and compliance reporting.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Multi-criteria audit log search (user, resource, action, date range)</li>
 *   <li>Daily audit event projections for fast dashboard queries</li>
 *   <li>Compliance report export (CSV, JSON, PDF)</li>
 *   <li>Statistics and aggregations (by user, resource, action)</li>
 *   <li>7-year retention enforcement per HIPAA 45 CFR § 164.312(b)</li>
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>Gateway trust authentication (validates X-Auth-* headers)</li>
 *   <li>Role-based access: AUDITOR and ADMIN roles only</li>
 *   <li>Multi-tenant isolation enforced at query level</li>
 * </ul>
 *
 * @see com.healthdata.auditquery.controller.AuditQueryController
 * @see com.healthdata.audit.entity.AuditEventEntity
 */
@SpringBootApplication(exclude = {
    com.healthdata.authentication.config.AuthenticationAutoConfiguration.class,
    com.healthdata.authentication.config.AuthenticationControllerAutoConfiguration.class,
    com.healthdata.authentication.config.AuthenticationJwtAutoConfiguration.class
})
@EnableKafka
@EnableScheduling
@ComponentScan(
    basePackages = {
        "com.healthdata.auditquery",
        "com.healthdata.audit",
        "com.healthdata.security",
        "com.healthdata.persistence"
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.audit\\.service\\..*"  // Exclude service-specific services
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.audit\\.controller\\..*"  // Exclude service-specific controllers
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.authentication\\.service\\..*"  // Exclude authentication services
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.authentication\\.controller\\..*"  // Exclude authentication controllers
        )
    }
)
@EntityScan(basePackages = {
    "com.healthdata.audit.entity.shared",     // Shared AuditEventEntity
    "com.healthdata.audit.entity.ai",        // AI Agent Decision Events
    "com.healthdata.audit.entity.clinical",  // Clinical Decision Events
    "com.healthdata.audit.entity",           // MPIMergeEntity (top-level package)
    "com.healthdata.auditquery.persistence"   // Projection entities
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.auditquery.repository",    // Projection repositories
    "com.healthdata.audit.repository",         // MPIMergeRepository, QAReviewRepository, DataQualityIssueRepository (top-level)
    "com.healthdata.audit.repository.shared",  // AuditEventRepository
    "com.healthdata.audit.repository.ai",      // AIAgentDecisionEventRepository, ConfigurationEngineEventRepository, UserConfigurationActionEventRepository
    "com.healthdata.audit.repository.clinical" // ClinicalDecisionRepository
})
public class AuditQueryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditQueryServiceApplication.class, args);
    }
}
