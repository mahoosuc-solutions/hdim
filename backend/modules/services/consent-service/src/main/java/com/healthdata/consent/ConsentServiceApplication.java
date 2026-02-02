package com.healthdata.consent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Consent Service - HIPAA-compliant consent management
 *
 * Manages patient consent records, consent policies, and consent history
 * in compliance with HIPAA 42 CFR Part 2 and GDPR requirements.
 *
 * ARCHITECTURE:
 * This service uses the authentication-headers module which provides:
 * - TrustedHeaderAuthFilter: Extracts user context from gateway-injected headers
 * - TrustedTenantAccessFilter: Validates tenant access without database lookup
 * - UserContextHolder: Thread-local storage for audit context
 *
 * The authentication-headers module has NO @EntityScan, so it doesn't try to
 * validate the User entity against this service's database. This allows
 * consent-service to use its own database schema (healthdata_consent) without
 * requiring a users table.
 *
 * JPA Configuration:
 * @EntityScan and @EnableJpaRepositories are configured in JpaConfig.java
 * with @ConditionalOnProperty to prevent loading during @WebMvcTest slice tests.
 *
 * Security Flow:
 * 1. Gateway validates JWT and injects X-Auth-* headers
 * 2. TrustedHeaderAuthFilter extracts user context and sets SecurityContext
 * 3. TrustedTenantAccessFilter validates X-Tenant-ID header
 * 4. Controller receives authenticated request
 */
@SpringBootApplication
public class ConsentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsentServiceApplication.class, args);
    }
}
