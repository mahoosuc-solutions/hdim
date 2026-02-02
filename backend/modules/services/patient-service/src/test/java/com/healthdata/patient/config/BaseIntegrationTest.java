package com.healthdata.patient.config;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base Integration Test Configuration for Patient Service
 *
 * This annotation combines common test configurations needed for integration tests:
 * - Loads full Spring context via @SpringBootTest
 * - Activates "test" profile for test-specific configurations
 *   (Security already permits all via PatientSecurityConfig's test profile bean)
 * - Uses Testcontainers PostgreSQL for database (configured in build.gradle.kts)
 * - Enables transactional rollback for database test isolation
 * - Uses in-memory cache for test isolation
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
@Tag("integration")
@Tag("slow")
@Import({TestCacheConfiguration.class})
public @interface BaseIntegrationTest {
}
