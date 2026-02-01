package com.healthdata.consent.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for Consent Service.
 * Separated from main application class to allow @WebMvcTest to work properly.
 *
 * IMPORTANT: This service uses a separate database (healthdata_consent) and should
 * NOT validate entities from other databases (like the User entity from patient_db).
 *
 * Strategy: Set Hibernate's ddl-auto to 'none' (in application.yml) to skip ALL
 * automatic schema management and validation. Liquibase handles schema management.
 */
@Configuration
@EntityScan(basePackages = "com.healthdata.consent.persistence")
@EnableJpaRepositories(basePackages = "com.healthdata.consent.persistence")
public class JpaConfig {
}
