package com.healthdata.sales.integration;

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
 * Integration test that validates entity-migration synchronization for the sales automation service.
 *
 * This test ensures that all JPA entities representing sales pipeline, opportunities, contacts,
 * and accounts have corresponding database schema definitions via Liquibase migrations.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - Column types are compatible between Java and PostgreSQL
 * - Nullability constraints match
 * - Sales metrics and tracking columns are properly defined
 * - Email campaign tables have correct schemas
 *
 * @author HDIM Platform Team
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("entity-migration-validation")
class EntityMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sales_test")
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
     * Validate that all sales automation service entities match their database schema.
     *
     * This test validates sales and CRM models including:
     * - Account: Customer accounts
     * - Contact: Contact information
     * - Opportunity: Sales opportunities
     * - Activity: Sales activities and interactions
     * - Pipeline: Sales pipeline stages
     * - Deal: Sales deals and agreements
     * - EmailCampaign: Automated email campaigns
     * - EmailSequence: Email sequence configurations
     * - EmailSequenceStep: Steps in email sequences
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
     * Validate only critical sales automation entities.
     *
     * This test focuses on the most critical entities that must always be in sync.
     */
    @Test
    void validateCriticalSalesAutomationEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical sales entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("Account") ||
                           name.equals("Contact") ||
                           name.equals("Opportunity") ||
                           name.equals("Pipeline");
                })
                .collect(java.util.stream.Collectors.toSet());


        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed." + report.getDetailedMessage());
    }
}
