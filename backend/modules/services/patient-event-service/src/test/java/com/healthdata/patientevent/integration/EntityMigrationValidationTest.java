package com.healthdata.patientevent.integration;

import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Set;

/**
 * Entity-Migration Validation Test for Patient Event Service
 *
 * Validates that all JPA entities in the patient-event-service match their
 * corresponding Liquibase database migrations. Ensures schema consistency
 * between the domain model and actual database structure.
 *
 * This test validates the event sourcing projection entities:
 * - PatientActiveProjection
 * - PatientEventLog
 * - Other projection entities
 *
 * @author HDIM Platform Team
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "patient-event-service";
    }

    @Override
    protected Set<String> getCriticalEntityNames() {
        // Validate core projection entities for patient event sourcing
        return Collections.emptySet();  // Validate all entities
    }
}
