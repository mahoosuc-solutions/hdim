package com.healthdata.migration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test JPA Configuration placeholder for H2 compatibility.
 *
 * The vladmihalcea hibernate-types and hypersistence-utils libraries are excluded
 * from the test classpath via build.gradle.kts configurations to prevent
 * PostgreSQL-specific array type registration issues with H2.
 */
@TestConfiguration
@Profile("test")
public class TestJpaConfiguration {
    // Configuration is handled via build.gradle.kts exclusions and application-test.yml
}
