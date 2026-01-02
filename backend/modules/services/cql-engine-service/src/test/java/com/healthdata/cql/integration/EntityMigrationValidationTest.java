package com.healthdata.cql.integration;

import com.healthdata.cql.TestCqlEngineApplication;
import com.healthdata.testfixtures.validation.AbstractEntityMigrationValidationTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

/**
 * Integration test that validates entity-migration synchronization for the CQL Engine service.
 *
 * This test ensures that all JPA entities for CQL library storage and measure evaluation
 * have corresponding database schema definitions via Liquibase migrations.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - CQL library definitions are properly stored
 * - FHIR measure definitions are properly stored
 * - Multi-tenant isolation is enforced at the database level
 *
 * @author HDIM Platform Team
 */
@SpringBootTest(classes = TestCqlEngineApplication.class)
class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {

    @Override
    protected String getServiceName() {
        return "cql-engine-service";
    }

    @Override
    protected Set<String> getCriticalEntityNames() {
        return Set.of("CqlLibrary", "CqlEvaluationResult", "MeasureDefinition");
    }
}
