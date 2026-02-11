package com.healthdata.test.base;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

/**
 * Base annotation for heavyweight integration tests with Testcontainers.
 *
 * JUnit 5 tags applied:
 * - integration: Marks as integration test
 * - slow: Marks as slow-running test
 * - heavyweight: Marks as requiring significant resources (Testcontainers)
 * 
 * Characteristics:
 * - Full Spring context with real infrastructure
 * - Uses Testcontainers (Kafka, PostgreSQL, Redis)
 * - End-to-end verification
 * - Slower execution (requires Docker)
 * - Run in CI/CD or before releases
 * 
 * Usage:
 * <pre>
 * {@code
 * @BaseHeavyweightTest
 * class MyServiceHeavyweightTest {
 *     @Container
 *     static KafkaContainer kafka = SharedKafkaContainer.getInstance();
 *     
 *     @DynamicPropertySource
 *     static void configureKafka(DynamicPropertyRegistry registry) {
 *         registry.add("spring.kafka.bootstrap-servers", 
 *             SharedKafkaContainer::getBootstrapServers);
 *     }
 *     
 *     @Test
 *     void shouldPublishToRealKafka() {
 *         // Test with real Kafka
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
@Transactional
@Testcontainers
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
public @interface BaseHeavyweightTest {
}

