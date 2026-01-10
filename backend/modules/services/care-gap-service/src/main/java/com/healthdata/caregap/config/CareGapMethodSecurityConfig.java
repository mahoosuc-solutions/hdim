package com.healthdata.caregap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Method-level security configuration for Care Gap Service.
 *
 * Disabled in demo profile to allow demo-mode bypass of @PreAuthorize checks.
 */
@Configuration
@Profile("!demo")
@EnableMethodSecurity(prePostEnabled = true)
public class CareGapMethodSecurityConfig {
}
