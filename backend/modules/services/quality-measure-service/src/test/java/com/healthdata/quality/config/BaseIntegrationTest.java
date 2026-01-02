package com.healthdata.quality.config;

import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Base Integration Test Configuration for Quality Measure Service
 *
 * This annotation combines common test configurations needed for integration tests:
 * - Loads full Spring context via @SpringBootTest
 * - Activates "test" profile for test-specific configurations
 * - Imports test security and cache configurations
 * - Enables transactional rollback for database tests
 *
 * Usage:
 * <pre>
 * {@literal @}BaseIntegrationTest
 * class MyIntegrationTest {
 *     // Test methods...
 * }
 * </pre>
 *
 * Benefits:
 * - Consistent test configuration across all integration tests
 * - Automatic cleanup of test data via @Transactional
 * - Mock beans for external dependencies (JWT, Kafka, etc.)
 * - Reduced boilerplate in test classes
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@Import({
    TestSecurityConfiguration.class,
    TestWebSocketConfiguration.class,
    TestCacheConfiguration.class,
    TestMessagingConfiguration.class
})
public @interface BaseIntegrationTest {
}
