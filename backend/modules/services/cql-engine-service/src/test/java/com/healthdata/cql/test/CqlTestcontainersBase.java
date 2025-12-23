package com.healthdata.cql.test;

import com.healthdata.cql.config.CqlTestContainersConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for CQL Engine Spring Boot tests that require Kafka Testcontainers.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class CqlTestcontainersBase {

    @DynamicPropertySource
    static void configureKafkaProperties(DynamicPropertyRegistry registry) {
        CqlTestContainersConfiguration.configureKafka(registry);
    }
}
