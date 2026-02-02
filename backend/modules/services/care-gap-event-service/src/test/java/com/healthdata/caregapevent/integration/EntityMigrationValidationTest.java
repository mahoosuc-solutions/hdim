package com.healthdata.caregapevent.integration;

import com.healthdata.caregap.CareGapEventServiceApplication;
import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Set;

/**
 * Entity-Migration Validation Test for Care Gap Event Service
 *
 * Validates that all JPA entities in the care-gap-event-service match their
 * corresponding Liquibase database migrations. Ensures schema consistency
 * between the domain model and actual database structure.
 *
 * This test validates the event sourcing projection entities:
 * - CareGapProjection
 * - CareGapEventLog
 * - Other projection entities for care gap detection results
 *
 * @author HDIM Platform Team
 */
@SpringBootTest(
        classes = CareGapEventServiceApplication.class,
        properties = "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml"
)
@Testcontainers
@ActiveProfiles("test")
@Tag("entity-migration-validation")
@Tag("integration")
@Tag("slow")
@Tag("heavyweight")
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "care-gap-event-service";
    }

    @Override
    protected Set<String> getCriticalEntityNames() {
        // Validate core projection entities for care gap event sourcing
        return Collections.emptySet();  // Validate all entities
    }
}
