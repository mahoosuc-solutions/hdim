package com.healthdata.authentication.config;

import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for header-based authentication.
 *
 * IMPORTANT: This configuration has NO @EntityScan.
 * It can be safely used by services that don't have a User table.
 *
 * This module provides:
 * - TrustedHeaderAuthFilter: Extracts user context from gateway-injected headers
 * - TrustedTenantAccessFilter: Validates tenant access without database lookup
 * - UserContextHolder: Thread-local storage for current user context
 *
 * Services that use this module:
 * - consent-service
 * - event-processing-service
 * - Any service that uses gateway-trust pattern without local User entity
 *
 * Usage:
 * Simply add this module as a dependency:
 * <pre>
 * implementation(project(":modules:shared:infrastructure:authentication-headers"))
 * </pre>
 *
 * The filters will be auto-registered via Spring Boot auto-configuration.
 * To disable, set: hdim.auth.headers.enabled=false
 */
@AutoConfiguration
@ConditionalOnProperty(
    prefix = "hdim.auth.headers",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class HeaderAuthAutoConfiguration {

    @Value("${hdim.auth.headers.development-mode:true}")
    private boolean developmentMode;

    @Value("${hdim.auth.headers.strict-mode:false}")
    private boolean strictMode;

    @Value("${hdim.auth.headers.shared-secret:}")
    private String sharedSecret;

    @Value("${hdim.auth.headers.signature-validity-seconds:300}")
    private long signatureValiditySeconds;

    /**
     * Creates the TrustedHeaderAuthFilter configuration.
     */
    @Bean
    @ConditionalOnMissingBean
    public TrustedHeaderAuthFilter.TrustedHeaderAuthConfig trustedHeaderAuthConfig() {
        TrustedHeaderAuthFilter.TrustedHeaderAuthConfig config =
            new TrustedHeaderAuthFilter.TrustedHeaderAuthConfig();
        config.setDevelopmentMode(developmentMode);
        config.setStrictMode(strictMode);
        config.setSharedSecret(sharedSecret);
        config.setSignatureValiditySeconds(signatureValiditySeconds);
        return config;
    }

    /**
     * Creates the TrustedHeaderAuthFilter bean.
     *
     * This filter:
     * - Extracts user context from X-Auth-* headers
     * - Sets Spring Security authentication
     * - Populates UserContextHolder for audit
     */
    @Bean
    @ConditionalOnMissingBean
    public TrustedHeaderAuthFilter trustedHeaderAuthFilter(
            TrustedHeaderAuthFilter.TrustedHeaderAuthConfig config,
            MeterRegistry meterRegistry) {
        return new TrustedHeaderAuthFilter(config, meterRegistry);
    }

    /**
     * Creates the TrustedTenantAccessFilter bean.
     *
     * This filter:
     * - Validates X-Tenant-ID against user's allowed tenants
     * - Uses request attributes (no database lookup)
     * - Must run after TrustedHeaderAuthFilter
     */
    @Bean
    @ConditionalOnMissingBean
    public TrustedTenantAccessFilter trustedTenantAccessFilter(MeterRegistry meterRegistry) {
        return new TrustedTenantAccessFilter(meterRegistry);
    }
}
