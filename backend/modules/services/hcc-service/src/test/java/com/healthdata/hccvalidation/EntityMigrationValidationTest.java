package com.healthdata.hccvalidation;

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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entity-migration validation for hcc-service.
 *
 * Uses @DataJpaTest with minimal configuration - only JPA entities and repositories.
 * No service or controller beans are loaded to avoid dependency issues.
 *
 * This test is in a separate package (com.healthdata.hccvalidation) to
 * avoid Spring Boot auto-scanning for @SpringBootConfiguration in com.healthdata.hcc.
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
@Tag("entity-migration-validation")
class EntityMigrationValidationTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.healthdata.hcc.persistence")
    static class TestConfig {
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("hcc_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("db/init-schema.sql")
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

        if (entities.isEmpty()) {
            return;
        }

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(entities);

        assertTrue(report.isValid(),
                "Entity-migration validation failed for hcc-service.\n" +
                report.getDetailedMessage());
    }

    @Test
    void validateCriticalHccEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical HCC entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("PatientHccProfileEntity") ||
                           name.equals("DiagnosisHccMapEntity") ||
                           name.equals("RecaptureOpportunityEntity") ||
                           name.equals("DocumentationGapEntity");
                })
                .collect(Collectors.toSet());

        if (criticalEntities.isEmpty()) {
            return;
        }

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed for hcc-service.\n" +
                report.getDetailedMessage());
    }
}
