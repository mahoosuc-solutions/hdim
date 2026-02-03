package com.healthdata.cdr.integration;

import com.healthdata.testfixtures.validation.EntityMigrationValidator;
import com.healthdata.testfixtures.validation.ValidationReport;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Entity-migration validation for cdr-processor-service.
 *
 * DISABLED: Requires PostgreSQL for JSONB/ENUM types. In production, schema validation
 * is performed by Liquibase migrations which are PostgreSQL-compatible.
 *
 * For local H2 testing, entity mapping and DDL-auto create schemas work.
 * Production schema validation uses Liquibase changesets.
 */
@Disabled("Phase 2: Skipped in H2 tests - requires PostgreSQL for JSONB/ENUM types")
@DataJpaTest(
    properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.flyway.enabled=false",
        "spring.data.jpa.repositories.enabled=false"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EntityScan(basePackages = "com.healthdata.cdr.domain")
@ActiveProfiles("test")
@Tag("integration")
@Tag("integration")
class EntityMigrationValidationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

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
                "Entity-migration validation failed for cdr-processor-service.\n" +
                report.getDetailedMessage());
    }

    @Test
    void validateCriticalEntitiesMatchDatabaseSchema() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Skip if no entities found
        if (entities.isEmpty()) {
            return;
        }

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(entities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed for cdr-processor-service.\n" +
                report.getDetailedMessage());
    }
}
