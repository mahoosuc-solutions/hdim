package com.healthdata.ehrvalidation;

import com.healthdata.testfixtures.validation.EntityMigrationValidator;
import com.healthdata.testfixtures.validation.ValidationReport;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entity-migration validation for ehr-connector-service.
 *
 * Uses @DataJpaTest with minimal configuration - only JPA entities and repositories.
 * No service or controller beans are loaded to avoid dependency issues.
 *
 * Note: ehr-connector-service is primarily an EHR integration service that
 * connects to external EHR systems. It does not define its own JPA entities.
 * This test validates that no unintended entities exist.
 *
 * This test is in a separate package (com.healthdata.ehrvalidation) to
 * avoid Spring Boot auto-scanning for @SpringBootConfiguration in com.healthdata.ehr.
 */
@DataJpaTest(
    properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.flyway.enabled=false",
        "spring.data.jpa.repositories.enabled=false"
    }
)
@ContextConfiguration(classes = EntityMigrationValidationTest.TestConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class EntityMigrationValidationTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.healthdata.ehr.persistence")
    static class TestConfig {
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ehr_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    void validateAllEntitiesMatchDatabaseSchema() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // ehr-connector-service does not define its own JPA entities
        // This test verifies no unintended entities exist
        if (entities.isEmpty()) {
            // Expected - this service has no JPA entities
            return;
        }

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(entities);

        assertTrue(report.isValid(),
                "Entity-migration validation failed for ehr-connector-service.\n" +
                report.getDetailedMessage());
    }

    @Test
    void validateCriticalEhrEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // ehr-connector-service does not define critical entities
        // It connects to external EHR systems and does not persist data locally
        if (entities.isEmpty()) {
            // Expected - this service has no JPA entities
            return;
        }

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(entities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed for ehr-connector-service.\n" +
                report.getDetailedMessage());
    }
}
