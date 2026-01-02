package com.healthdata.consent.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for Consent Service.
 * Separated from main application class to allow @WebMvcTest to work properly.
 */
@Configuration
@EntityScan(basePackages = "com.healthdata.consent.persistence")
@EnableJpaRepositories(basePackages = "com.healthdata.consent.persistence")
public class JpaConfig {
}
