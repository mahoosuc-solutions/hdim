package com.healthdata.test.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

/**
 * Base annotation for heavyweight integration tests with Testcontainers.
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
public @interface BaseHeavyweightTest {
}

