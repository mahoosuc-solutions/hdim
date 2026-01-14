package com.healthdata.test.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Base annotation for audit-specific tests.
 * 
 * Characteristics:
 * - Focuses on audit event verification
 * - May use mocked or real Kafka
 * - Verifies event structure and content
 * - Tests audit pipeline components
 * 
 * Usage:
 * <pre>
 * {@code
 * @BaseAuditTest
 * class MyAuditIntegrationTest {
 *     @Autowired
 *     private AIAuditEventPublisher publisher;
 *     
 *     @Test
 *     void shouldPublishAuditEvent() {
 *         // Test audit event publishing
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
public @interface BaseAuditTest {
}

