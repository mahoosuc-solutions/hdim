package com.healthdata.documentation.integration;

import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Entity-migration validation for documentation-service.
 * Part of Phase 2 entity-migration validation framework.
 *
 * NOTE: Excludes DatabaseAutoConfiguration to avoid conflicts with test-specific configs.
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=com.healthdata.database.config.DatabaseAutoConfiguration"
})
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "documentation-service";
    }
}
