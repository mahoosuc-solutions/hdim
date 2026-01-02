package com.healthdata.predictive.controller.integration;

import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Entity-migration validation test for predictive-analytics-service.
 *
 * Validates that all JPA entities match their database schema definitions.
 * Part of Phase 2: Entity-Migration Synchronization.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("entity-migration-validation")
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "predictive-analytics-service";
    }
}
