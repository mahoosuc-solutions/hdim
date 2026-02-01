package com.healthdata.featureflags;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Feature Flag Auto-Configuration
 *
 * Auto-configures feature flag infrastructure for all services.
 *
 * To use in a service:
 * 1. Add dependency: implementation(project(":modules:shared:infrastructure:feature-flags"))
 * 2. Add @EnableFeatureFlags to your main application class
 * 3. Use @FeatureFlag annotation on methods
 */
@Configuration
@ComponentScan(basePackages = "com.healthdata.featureflags")
@EntityScan(basePackages = "com.healthdata.featureflags")
@EnableJpaRepositories(basePackages = "com.healthdata.featureflags")
public class FeatureFlagAutoConfiguration {

    @Bean
    public ObjectMapper featureFlagObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules(); // Registers JavaTimeModule for Instant serialization
    }
}
