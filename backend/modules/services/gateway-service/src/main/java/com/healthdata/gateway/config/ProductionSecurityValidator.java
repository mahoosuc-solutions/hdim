package com.healthdata.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Production Security Validator - Fails application startup if security requirements not met.
 *
 * CRITICAL SECURITY: This class ensures that production deployments cannot start
 * with insecure configurations. Unlike dev/test modes, production must have:
 * - Authentication enforced (no demo mode bypass)
 * - Valid header signing secret (min 32 characters)
 * - External auth headers stripped
 * - JWT secret properly configured
 *
 * Activation: This validator only runs when 'prod' or 'production' profile is active.
 */
@Slf4j
@Configuration
@Profile({"prod", "production", "kubernetes"})
@RequiredArgsConstructor
public class ProductionSecurityValidator {

    private final GatewayAuthProperties authProperties;
    private final Environment environment;

    @PostConstruct
    public void validateProductionSecurity() {
        log.info("Running production security validation...");

        List<String> criticalErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // CRITICAL: Authentication must be enforced in production
        if (!authProperties.getEnforced()) {
            criticalErrors.add(
                "SECURITY VIOLATION: gateway.auth.enforced=false is NOT allowed in production. " +
                "This would allow unauthenticated access to protected resources."
            );
        }

        // CRITICAL: Header signing secret must be configured and strong
        String signingSecret = authProperties.getHeaderSigningSecret();
        if (signingSecret == null || signingSecret.isBlank()) {
            criticalErrors.add(
                "SECURITY VIOLATION: gateway.auth.header-signing-secret is not configured. " +
                "Generate with: openssl rand -base64 32"
            );
        } else if (signingSecret.length() < 32) {
            criticalErrors.add(
                "SECURITY VIOLATION: gateway.auth.header-signing-secret is too short (" +
                signingSecret.length() + " chars). Minimum 32 characters required."
            );
        } else if (signingSecret.contains("dev") || signingSecret.contains("test") ||
                   signingSecret.contains("change") || signingSecret.contains("default")) {
            criticalErrors.add(
                "SECURITY VIOLATION: gateway.auth.header-signing-secret appears to be a default/dev value. " +
                "Generate a secure secret with: openssl rand -base64 32"
            );
        }

        // CRITICAL: External auth headers must be stripped
        if (!authProperties.getStripExternalAuthHeaders()) {
            criticalErrors.add(
                "SECURITY VIOLATION: gateway.auth.strip-external-auth-headers=false is NOT allowed. " +
                "This would enable header injection attacks."
            );
        }

        // CRITICAL: JWT secret must be configured
        String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.isBlank()) {
            criticalErrors.add(
                "SECURITY VIOLATION: jwt.secret is not configured. " +
                "Generate with: openssl rand -base64 32"
            );
        } else if (jwtSecret.length() < 32) {
            criticalErrors.add(
                "SECURITY VIOLATION: jwt.secret is too short. Minimum 32 characters required."
            );
        }

        // WARNING: Audit logging should be enabled
        if (!authProperties.getAuditLogging()) {
            warnings.add(
                "HIPAA WARNING: gateway.auth.audit-logging=false. " +
                "Authentication events should be logged for compliance."
            );
        }

        // WARNING: Rate limiting should be enabled
        if (authProperties.getRateLimit() == null || !authProperties.getRateLimit().getEnabled()) {
            warnings.add(
                "SECURITY WARNING: gateway.auth.rate-limit.enabled=false. " +
                "Rate limiting protects against brute force attacks."
            );
        }

        // Log warnings
        warnings.forEach(warning -> log.warn(warning));

        // Fail startup on critical errors
        if (!criticalErrors.isEmpty()) {
            log.error("=".repeat(80));
            log.error("PRODUCTION SECURITY VALIDATION FAILED");
            log.error("=".repeat(80));
            criticalErrors.forEach(error -> log.error("  - {}", error));
            log.error("=".repeat(80));
            log.error("Application startup aborted. Fix security configuration before deploying.");
            log.error("=".repeat(80));

            throw new SecurityConfigurationException(
                "Production security validation failed with " + criticalErrors.size() + " critical error(s). " +
                "See logs for details."
            );
        }

        log.info("Production security validation passed. {} warning(s).", warnings.size());
    }

    /**
     * Exception thrown when security configuration is invalid for production.
     */
    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message) {
            super(message);
        }
    }
}
