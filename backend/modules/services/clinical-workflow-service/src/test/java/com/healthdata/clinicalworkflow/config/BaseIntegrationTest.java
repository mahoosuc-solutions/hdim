package com.healthdata.clinicalworkflow.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base Integration Test Configuration for Clinical Workflow Service
 *
 * This annotation combines common test configurations needed for integration tests:
 * - Loads full Spring context via @SpringBootTest
 * - Activates "test" profile for test-specific configurations
 * - Uses Testcontainers PostgreSQL for database
 * - Enables transactional rollback for database test isolation
 *
 * Usage:
 * <pre>
 * {@literal @}BaseIntegrationTest
 * class MyIntegrationTest {
 *     // Test methods...
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public @interface BaseIntegrationTest {
}
