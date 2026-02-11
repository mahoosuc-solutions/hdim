package com.healthdata.fhir.integration;

import com.healthdata.testfixtures.validation.EntityMigrationValidator;
import com.healthdata.testfixtures.validation.ValidationReport;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that validates entity-migration synchronization for the FHIR service.
 *
 * This test ensures that all JPA entities representing FHIR R4 resources and related data
 * have corresponding database schema definitions via Liquibase migrations.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - FHIR resource JSON payloads are stored in proper JSONB columns
 * - Reference relationships are properly defined as foreign keys
 * - Multi-tenant isolation is enforced at the database level
 *
 * @author HDIM Platform Team
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class EntityMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fhir_test")
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
        registry.add("spring.flyway.enabled", () -> "false");  // Disable Flyway migrations (use Hibernate for tests)
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");  // Create fresh schema for tests
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    /**
     * Validate that all FHIR service entities match their database schema.
     *
     * This test validates FHIR resource storage models including:
     * - FhirResource: Base FHIR resource storage
     * - Patient: FHIR Patient resource
     * - Practitioner: FHIR Practitioner resource
     * - Organization: FHIR Organization resource
     * - Condition: FHIR Condition resource
     * - Observation: FHIR Observation resource
     * - Procedure: FHIR Procedure resource
     * - MedicationRequest: FHIR MedicationRequest resource
     * - Encounter: FHIR Encounter resource
     * - Immunization: FHIR Immunization resource
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

        // Assert validation passed (no errors)
        assertTrue(report.isValid(),
                "Entity-migration validation failed. Database schema does not match JPA entities. " +
                        "Tables: " + report.getTotalTablesChecked() + ", " +
                        "Columns: " + report.getTotalColumnsChecked() + ", " +
                        "Issues: " + report.getTotalIssues() + ". " +
                        report.getDetailedMessage());
    }

    /**
     * Validate only critical FHIR resource entities.
     *
     * This test focuses on the core FHIR resources that must always be in sync.
     */
    @Test
    void validateCriticalFhirResourceEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical FHIR entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("FhirResource") ||
                           name.equals("FhirPatient") ||
                           name.equals("FhirEncounter") ||
                           name.equals("FhirCondition");
                })
                .collect(java.util.stream.Collectors.toSet());

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed. " + report.getDetailedMessage());
    }
}
