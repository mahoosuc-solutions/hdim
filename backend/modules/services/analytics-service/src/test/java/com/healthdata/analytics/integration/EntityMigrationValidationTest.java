package com.healthdata.analytics.integration;

import com.healthdata.testfixtures.validation.EntityMigrationValidator;
import com.healthdata.testfixtures.validation.ValidationReport;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entity-migration validation for analytics-service.
 *
 * Uses @DataJpaTest with Liquibase migrations to properly handle PostgreSQL JSONB types.
 * The Hypersistence Utils JsonBinaryType requires actual PostgreSQL with JSONB support,
 * which Liquibase migrations create correctly.
 */
@DataJpaTest(
    properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml",
        "spring.flyway.enabled=false",
        "spring.data.jpa.repositories.enabled=false"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackages = "com.healthdata.analytics.persistence")
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class EntityMigrationValidationTest {

    @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("analytics_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true)
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)))
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
    }

    @Test
    void validateAllEntitiesMatchDatabaseSchema() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Skip if no entities found (service may not have JPA entities)
        if (entities.isEmpty()) {
            return;
        }

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(entities);

        assertTrue(report.isValid(),
                "Entity-migration validation failed for analytics-service.\n" +
                report.getDetailedMessage());
    }

    @Test
    void validateCriticalAnalyticsEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical analytics entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("MetricAggregation") ||
                           name.equals("ReportDefinition") ||
                           name.equals("ReportGeneration");
                })
                .collect(java.util.stream.Collectors.toSet());

        // Skip if no critical entities found
        if (criticalEntities.isEmpty()) {
            return;
        }

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed for analytics-service.\n" +
                report.getDetailedMessage());
    }
}
