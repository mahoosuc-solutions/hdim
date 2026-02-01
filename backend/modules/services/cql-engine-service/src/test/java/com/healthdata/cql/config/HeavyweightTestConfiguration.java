package com.healthdata.cql.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test configuration specifically for heavyweight integration tests.
 * 
 * This configuration is used by heavyweight tests that require full Spring Boot context
 * with real infrastructure (Kafka, PostgreSQL) via Testcontainers.
 * 
 * Unlike TestCqlEngineApplication, this does NOT exclude Kafka or other infrastructure
 * components, allowing heavyweight tests to verify end-to-end functionality.
 */
@TestConfiguration
@ComponentScan(basePackages = {
    "com.healthdata.cql",
    "com.healthdata.audit.service.ai"
})
public class HeavyweightTestConfiguration {
}
