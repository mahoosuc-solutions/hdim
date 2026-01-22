package com.healthdata.qualitymeasure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration for Quality Measure Event Service
 *
 * Explicitly controls entity and repository scanning to handle package structure:
 * - Handler library: com.healthdata.qualityevent.* (projections, event handlers)
 * - Event service: com.healthdata.qualitymeasure.* (application service, repositories)
 *
 * This configuration ensures both old and new package structures are scanned.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.healthdata.qualitymeasure.persistence",  // Application service repositories
        "com.healthdata.qualityevent.repository"       // Handler library repositories (legacy)
    }
)
@EntityScan(
    basePackages = {
        "com.healthdata.qualityevent.projection",      // Handler library projections
        "com.healthdata.qualitymeasure.projection"     // Event service projections (if any)
    }
)
public class JpaConfig {
    // Configuration through annotations only
}
