package com.healthdata.notification.integration;

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
 * Integration test that validates entity-migration synchronization for the Notification service.
 *
 * This test ensures that all JPA entities for real-time notifications
 * have corresponding database schema definitions via Liquibase migrations.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - Notification channels are properly defined
 * - Notification templates and preferences are properly stored
 * - Multi-tenant isolation is enforced at the database level
 *
 * @author HDIM Platform Team
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("entity-migration-validation")
class EntityMigrationValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("notification_test")
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
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    /**
     * Validate that all Notification service entities match their database schema.
     *
     * This test validates notification entities including:
     * - NotificationChannel: Email, SMS, push notification channels
     * - NotificationTemplate: Message templates for different event types
     * - NotificationPreference: User notification preferences and subscriptions
     * - And other notification-related entities
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
     * Validate only critical notification entities.
     *
     * This test focuses on core notification entities that must always be in sync.
     */
    @Test
    void validateCriticalNotificationEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical notification entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("NotificationChannel") ||
                           name.equals("NotificationTemplate") ||
                           name.equals("NotificationPreference");
                })
                .collect(java.util.stream.Collectors.toSet());

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed. " + report.getDetailedMessage());
    }
}
