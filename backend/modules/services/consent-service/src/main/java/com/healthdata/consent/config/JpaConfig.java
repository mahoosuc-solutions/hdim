package com.healthdata.consent.config;

import org.springframework.context.annotation.Configuration;

/**
 * JPA configuration for Consent Service.
 * Separated from main application class to allow @WebMvcTest to work properly.
 *
 * @EntityScan and @EnableJpaRepositories are now on ConsentServiceApplication
 * to ensure they take precedence over any auto-scanned configurations.
 * This configuration class remains for backward compatibility with @WebMvcTest.
 */
@Configuration
public class JpaConfig {
}
