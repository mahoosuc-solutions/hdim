package com.healthdata.authentication.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for authentication controller and user management components.
 *
 * This configuration is CONDITIONALLY enabled only when authentication.controller.enabled=true.
 * It enables component scanning for the authentication controller package.
 *
 * CONDITIONAL LOADING:
 * - When authentication.controller.enabled=true → AuthController loads (Gateway Service)
 * - When property is missing or false → AuthController skipped (Microservices)
 *
 * ARCHITECTURE:
 * - Gateway Service: Sets authentication.controller.enabled=true → Full auth stack
 * - CQL Engine Service: Property not set → Only JWT validation (no controller)
 * - Quality Measure Service: Property not set → Only JWT validation (no controller)
 *
 * This ensures microservices can use JWT validation (from AuthenticationJwtAutoConfiguration)
 * without loading the authentication controller and its dependencies (UserRepository,
 * RefreshTokenService, LogoutService, etc.).
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "authentication.controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
@ComponentScan(basePackages = "com.healthdata.authentication.controller")
@EntityScan(basePackages = "com.healthdata.authentication.domain")
public class AuthenticationControllerAutoConfiguration {
    // Configuration is activated by the @ConditionalOnProperty annotation
    // Component scanning will find and register AuthController when enabled
}
