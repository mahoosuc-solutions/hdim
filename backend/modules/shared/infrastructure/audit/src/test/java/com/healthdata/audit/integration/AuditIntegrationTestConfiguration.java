package com.healthdata.audit.integration;

import com.healthdata.audit.config.AuditAutoConfiguration;
import com.healthdata.audit.config.AuditClientConfig;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Test configuration for integration tests in the audit module.
 * 
 * Provides necessary Spring Boot configuration for testing library module components.
 * Acts as a minimal Spring Boot application for testing purposes.
 * 
 * Note: AuditAutoConfiguration already handles:
 * - @EnableJpaRepositories (basePackages = "com.healthdata.audit.repository")
 * - Component scanning is handled by @SpringBootApplication
 * - Entity scanning is handled by @SpringBootApplication default behavior
 * - RestTemplate is provided by AuditClientConfig with @ConditionalOnMissingBean
 */
@SpringBootApplication
@Import({AuditAutoConfiguration.class, AuditClientConfig.class})
@EntityScan(basePackages = "com.healthdata.audit.entity")
public class AuditIntegrationTestConfiguration {
    // Minimal configuration - all beans provided by imported configurations
}
