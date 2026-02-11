package com.healthdata.nurseworkflow.integration;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using TestContainers
 *
 * Provides PostgreSQL container setup for integration testing with real database.
 * All integration tests should extend this class to ensure proper container lifecycle.
 *
 * Configuration:
 * - PostgreSQL 16-alpine
 * - Database: nurse_workflow_test
 * - User: healthdata
 * - Password: test-password
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
})
@Tag("integration")
public class NurseWorkflowIntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("nurse_workflow_test")
        .withUsername("healthdata")
        .withPassword("test-password")
        .withExposedPorts(5432);

    static {
        postgres.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }
}
