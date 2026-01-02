package com.healthdata.ehr.integration;

import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Entity-migration validation for ehr-connector-service.
 * Part of Phase 2 entity-migration validation framework.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("entity-migration-validation")
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "ehr-connector-service";
    }
}
