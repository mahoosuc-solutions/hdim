package com.healthdata.consent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for Consent Service.
 * Separated from main application class to allow @WebMvcTest to work properly.
 *
 * This configuration is loaded conditionally - it will NOT load when:
 * - Running @WebMvcTest (which sets spring.test.mockmvc property)
 * - spring.jpa.enabled=false
 *
 * This separation ensures that @WebMvcTest slice tests don't try to
 * initialize JPA infrastructure (EntityManagerFactory, repositories, etc.).
 */
@Configuration
@EntityScan(basePackages = "com.healthdata.consent.persistence")
@EnableJpaRepositories(basePackages = "com.healthdata.consent.persistence")
@ConditionalOnProperty(name = "spring.datasource.url")
public class JpaConfig {
}
