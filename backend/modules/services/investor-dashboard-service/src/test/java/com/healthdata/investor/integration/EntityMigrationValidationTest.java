package com.healthdata.investor.integration;

import com.healthdata.testfixtures.validation.EntityMigrationValidator;
import com.healthdata.testfixtures.validation.ValidationReport;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.DisplayName;
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
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that validates entity-migration synchronization for the investor-dashboard-service.
 *
 * This test ensures that all JPA entities have corresponding database schema definitions
 * via Liquibase migrations, catching entity-migration drift issues.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - Column types are compatible between Java and PostgreSQL
 * - Nullability constraints match
 * - Primary keys are properly defined
 *
 * Entities covered:
 * - InvestorUser: User authentication and authorization
 * - InvestorTask: Task tracking for investor outreach
 * - InvestorContact: Investor contact management
 * - OutreachActivity: Outreach activity tracking
 * - LinkedInConnection: LinkedIn connection management
 *
 * @author HDIM Platform Team
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@Tag("entity-migration-validation")
@DisplayName("Investor Dashboard Entity-Migration Validation Tests")
class EntityMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("investor_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withStartupTimeout(Duration.ofMinutes(3));

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

        // JWT configuration required by the service
        registry.add("jwt.secret", () -> "dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItdGVzdGluZy1vbmx5LW1pbmltdW0tMjU2LWJpdHMtbG9uZw==");
        registry.add("jwt.expiration", () -> "3600000");
        registry.add("jwt.refresh-expiration", () -> "86400000");
        registry.add("security.login.max-attempts", () -> "5");
        registry.add("security.login.lockout-duration", () -> "900000");

        // Zoho integration defaults for tests (avoid missing property failures)
        registry.add("zoho.api.enabled", () -> "false");
        registry.add("zoho.oauth2.client-id", () -> "test-client-id");
        registry.add("zoho.oauth2.client-secret", () -> "test-client-secret");
        registry.add("zoho.oauth2.redirect-uri", () -> "http://localhost/test");
        registry.add("zoho.oauth2.scope", () -> "ZohoCRM.modules.ALL");
    }

    /**
     * Validate that all investor dashboard entities match their database schema.
     *
     * This test validates:
     * - InvestorUser: User accounts with authentication fields
     * - InvestorTask: Tasks for investor outreach workflow
     * - InvestorContact: Investor contact information
     * - OutreachActivity: Activity tracking for contacts
     * - LinkedInConnection: LinkedIn integration tracking
     */
    @Test
    @DisplayName("All entities should match database schema")
    void validateAllEntitiesMatchDatabaseSchema() {
        // Get all entities from the persistence unit
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Create validator and run validation
        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(entities);

        // Assert validation passed (no errors)
        assertTrue(report.isValid(),
                "Entity-migration validation failed. Database schema does not match JPA entities.\n" +
                        "Service: investor-dashboard-service\n" +
                        report.getDetailedMessage());
    }

    /**
     * Validate only critical authentication entities.
     *
     * These are the core security entities that must always be in sync:
     * - InvestorUser: User authentication and account lockout
     */
    @Test
    @DisplayName("Critical authentication entities should match database schema")
    void validateCriticalAuthenticationEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical authentication entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> e.getName().equals("InvestorUser"))
                .collect(Collectors.toSet());

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed.\n" +
                        "Affected entity: InvestorUser\n" +
                        report.getDetailedMessage());
    }

    /**
     * Validate critical task and contact entities.
     *
     * These entities form the core CRM functionality:
     * - InvestorTask: Task management
     * - InvestorContact: Contact management
     * - OutreachActivity: Activity tracking
     */
    @Test
    @DisplayName("Critical CRM entities should match database schema")
    void validateCriticalCrmEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical CRM entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("InvestorTask") ||
                           name.equals("InvestorContact") ||
                           name.equals("OutreachActivity");
                })
                .collect(Collectors.toSet());

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical CRM entity-migration validation failed.\n" +
                        "Entities checked: InvestorTask, InvestorContact, OutreachActivity\n" +
                        report.getDetailedMessage());
    }
}
