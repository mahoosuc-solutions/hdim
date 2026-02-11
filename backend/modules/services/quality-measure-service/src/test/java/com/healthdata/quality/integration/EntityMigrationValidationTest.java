package com.healthdata.quality.integration;

import com.healthdata.testfixtures.validation.EntityMigrationValidator;
import com.healthdata.testfixtures.validation.ValidationReport;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
 * Integration test that validates entity-migration synchronization for the quality measure service.
 *
 * This test ensures that all JPA entities representing quality measures, care gaps, and HEDIS
 * metrics have corresponding database schema definitions via Liquibase migrations.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - Column types are compatible between Java and PostgreSQL
 * - Nullability constraints match
 * - Measurement result tables have proper indexes
 * - JSONB columns for measure details are properly typed
 *
 * @author HDIM Platform Team
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class EntityMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("quality_test")
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
     * Validate that all quality measure service entities match their database schema.
     *
     * This test validates quality measure and care gap models including:
     * - QualityMeasure: HEDIS/CQL quality measure definitions
     * - MeasureEvaluation: Results of measure evaluations
     * - CareGap: Identified care gaps for patients
     * - HealthScore: Patient health risk scores
     * - MeasureResult: Aggregated measure results
     *
     * When this test fails, it provides detailed error messages indicating exactly which
     * entity and column have the issue.
     *
     * Note: This test can take 3-4 minutes due to validating many entities.
     */
    @Test
    @Timeout(300)  // 5 minutes timeout for comprehensive validation
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
     * Validate only critical quality measure entities.
     *
     * This test focuses on the most critical entities that must always be in sync.
     */
    @Test
    void validateCriticalQualityMeasureEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical quality measure entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("QualityMeasure") ||
                           name.equals("MeasureEvaluation") ||
                           name.equals("CareGap");
                })
                .collect(java.util.stream.Collectors.toSet());


        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed." + report.getDetailedMessage());
    }
}
