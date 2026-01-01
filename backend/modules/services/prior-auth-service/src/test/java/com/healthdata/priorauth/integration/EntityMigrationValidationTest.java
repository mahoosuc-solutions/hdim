package com.healthdata.priorauth.integration;

import com.healthdata.priorauth.TestPriorAuthApplication;
import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

/**
 * Integration test that validates entity-migration synchronization for the Prior Authorization service.
 *
 * This test ensures that all JPA entities for prior authorization workflows
 * have corresponding database schema definitions via Liquibase migrations.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - Prior authorization requests and responses are properly stored
 * - HIPAA audit requirements are met (soft deletes, created/updated timestamps)
 * - Multi-tenant isolation is enforced at the database level
 *
 * @author HDIM Platform Team
 */
@SpringBootTest(classes = TestPriorAuthApplication.class)
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "prior-auth-service";
    }

    @Override
    protected Set<String> getCriticalEntityNames() {
        return Set.of("PriorAuthRequest", "PriorAuthResponse", "WorkflowStep");
    }
}
