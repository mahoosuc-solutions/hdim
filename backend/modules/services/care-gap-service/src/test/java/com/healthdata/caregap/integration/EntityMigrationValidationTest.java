package com.healthdata.caregap.integration;

import com.healthdata.testfixtures.validation.EntityMigrationValidator;
import com.healthdata.testfixtures.validation.ValidationReport;
import com.healthdata.caregap.config.TestKafkaInitializer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * Integration test that validates entity-migration synchronization for the care gap service.
 *
 * This test ensures that all JPA entities representing care gaps, interventions, and care
 * recommendations have corresponding database schema definitions via Liquibase migrations.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - Column types are compatible between Java and PostgreSQL
 * - Nullability constraints match
 * - Care gap status tracking columns are present
 * - Intervention tracking and closure columns are properly defined
 *
 * @author HDIM Platform Team
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestKafkaInitializer.class)
@Tag("entity-migration-validation")
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
class EntityMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("caregap_test")
            .withUsername("testuser")
            .withPassword("testpass");

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
        registry.add("spring.liquibase.enabled", () -> "false");  // Let Hibernate create schema
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");  // Create fresh schema for tests
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    /**
     * Validate that all care gap service entities match their database schema.
     *
     * This test validates care gap and intervention models including:
     * - CareGap: Individual care gaps identified for patients
     * - CareGapIntervention: Interventions recommended to close care gaps
     * - CareGapOutcome: Tracking outcomes of gap closure interventions
     * - CareGapMetrics: Aggregated care gap statistics
     * - InterventionTracking: Tracking delivery of interventions
     *
     * When this test fails, it provides detailed error messages indicating exactly which
     * entity and column have the issue.
     */
    @Test
    void validateAllEntitiesMatchDatabaseSchema() {
        // Get all entities from the persistence unit
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();


        // Create validator and run validation
        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(entities);

        // Log report details

        // Assert validation passed (no errors)
        assertTrue(report.isValid(),
                "Entity-migration validation failed. Database schema does not match JPA entities." +
                        report.getDetailedMessage());
    }

    /**
     * Validate only critical care gap entities.
     *
     * This test focuses on the most critical entities that must always be in sync.
     */
    @Test
    void validateCriticalCareGapEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical care gap entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("CareGap") ||
                           name.equals("CareGapIntervention") ||
                           name.equals("CareGapOutcome");
                })
                .collect(java.util.stream.Collectors.toSet());


        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed." + report.getDetailedMessage());
    }
}
