package com.healthdata.caregap.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base Integration Test Configuration for Care Gap Service
 *
 * This annotation combines common test configurations needed for integration tests:
 * - Loads full Spring context via @SpringBootTest
 * - Activates "test" profile for test-specific configurations
 * - Uses in-memory H2 database (configured in application-test.yml)
 * - Disables Docker dependencies (Kafka, Postgres testcontainers)
 * - Kafka is disabled in tests via spring.autoconfigure.exclude
 * - Enables transactional rollback for database test isolation
 * - Uses in-memory cache for test isolation
 *
 * NOTE: Kafka-specific tests should be marked with @Tag("integration") for selective execution.
 * To run Kafka-dependent tests, use a separate test profile with embedded Kafka configured.
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
@Import({
    TestCacheConfiguration.class,
    TestAuditConfiguration.class
})
public @interface BaseIntegrationTest {
}
