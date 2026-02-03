package com.healthdata.qualitymeasureevent.integration;

import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Set;

/**
 * Entity-Migration Validation Test for Quality Measure Event Service
 *
 * Validates that all JPA entities in the quality-measure-event-service match their
 * corresponding Liquibase database migrations. Ensures schema consistency
 * between the domain model and actual database structure.
 *
 * This test validates the event sourcing projection entities:
 * - MeasureEvaluationProjection
 * - QualityEventLog
 * - Other projection entities for HEDIS measure evaluation results
 *
 * @author HDIM Platform Team
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("entity-migration-validation")
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "quality-measure-event-service";
    }

    @Override
    protected Set<String> getCriticalEntityNames() {
        // Validate core projection entities for quality measure event sourcing
        return Collections.emptySet();  // Validate all entities
    }
}
