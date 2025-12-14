package com.healthdata.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Minimal Spring Boot Application for testing HIPAA compliance features.
 * This is only used in tests to provide a Spring Boot context.
 *
 * Excludes:
 * - Encryption package: Contains JPA AttributeConverters that require jakarta.persistence
 *   (which is compileOnly, not available at test runtime)
 * - SecurityAutoConfiguration: Conflicts with test web context setup
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan(
    basePackages = "com.healthdata.security",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.healthdata\\.security\\.encryption\\..*"
    )
)
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
