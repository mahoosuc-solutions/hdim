package com.healthdata.caregap.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration for Care Gap Event Service
 *
 * Explicitly controls entity scanning to prevent conflicts between:
 * - Handler library projections (com.healthdata.caregap.projection)
 * - Event service projections (com.healthdata.caregapevent.projection)
 *
 * Both have entities with table name "care_gap_projections" but different id types,
 * causing Hibernate schema validation errors. This configuration ensures ONLY the
 * event service projections are scanned by JPA.
 *
 * Architecture:
 * - Handler library provides business logic (CareGapEventHandler) with no-op projection store
 * - Event service maintains its own projection tables updated via Kafka listeners
 * - Only event service repositories and entities are registered with JPA
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.healthdata.caregap.persistence",      // NEW package structure
        "com.healthdata.caregapevent.repository"   // OLD package structure (legacy)
    }
)
@EntityScan(
    basePackages = {
        "com.healthdata.caregap.projection",       // Handler library projections
        "com.healthdata.caregapevent.projection"   // Event service projections (legacy)
    }
)
public class JpaConfig {
    // Configuration through annotations only
}
