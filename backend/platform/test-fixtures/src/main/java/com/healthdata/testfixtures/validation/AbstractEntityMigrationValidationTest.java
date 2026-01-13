package com.healthdata.testfixtures.validation;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract base class for entity-migration validation tests across all HDIM microservices.
 *
 * This class provides a standardized approach to validating that JPA entities match their
 * corresponding database schema definitions via Liquibase migrations. By extending this
 * class, individual services avoid duplicating test infrastructure and configuration.
 *
 * Key features:
 * - Shared PostgreSQL Testcontainer configuration
 * - Standardized Hibernate DDL-auto (create-drop) for test isolation
 * - Dynamic property injection for Testcontainers
 * - Reusable entity validation logic
 *
 * Subclasses should:
 * 1. Extend this class
 * 2. Add @SpringBootTest with appropriate configuration
 * 3. Implement getServiceName() to return the service name
 * 4. Override getCriticalEntityNames() if testing specific entities
 *
 * Example usage:
 * <pre>
 * @SpringBootTest(classes = TestPatientApplication.class)
 * @Testcontainers
 * @ActiveProfiles("test")
 * @Tag("entity-migration-validation")
 * class EntityMigrationValidationTest extends AbstractEntityMigrationValidationTest {
 *
 *     @Override
 *     protected String getServiceName() {
 *         return "patient-service";
 *     }
 *
 *     @Override
 *     protected Set<String> getCriticalEntityNames() {
 *         return Set.of("Patient", "PatientIdentifier", "PatientAddress");
 *     }
 * }
 * </pre>
 *
 * @author HDIM Platform Team
 */
@Testcontainers
@Tag("entity-migration-validation")
public abstract class AbstractEntityMigrationValidationTest {

    /**
     * PostgreSQL container for testing. Uses lightweight Alpine image and avoids
     * container reuse to prevent stale state issues from previous test runs.
     */
    @Container
    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("entity_validation_test")
            .withUsername("testuser")
            .withPassword("testpass");

    /**
     * Autowired EntityManagerFactory for accessing JPA metamodel.
     * Used to extract entity definitions during validation.
     */
    @Autowired
    protected EntityManagerFactory entityManagerFactory;

    /**
     * Autowired DataSource for JDBC schema introspection.
     * Used by EntityMigrationValidator to check database schema.
     */
    @Autowired
    protected DataSource dataSource;

    /**
     * Configure dynamic properties for Testcontainer JDBC connectivity.
     * This method is called automatically by Spring Test to inject
     * Testcontainer connection details into the test application context.
     *
     * Properties configured:
     * - spring.datasource.url: JDBC URL from PostgreSQL container
     * - spring.datasource.username/password: Test credentials
     * - spring.datasource.driver-class-name: PostgreSQL JDBC driver
     * - spring.liquibase.enabled: false (use Hibernate for schema generation)
     * - spring.jpa.hibernate.ddl-auto: create-drop (fresh schema per test)
     * - spring.jpa.properties.hibernate.dialect: PostgreSQL dialect
     *
     * @param registry Spring's DynamicPropertyRegistry for setting properties
     */
    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");

        // JWT configuration for security modules (JwtConfig and SecurityProperties)
        registry.add("jwt.secret", () -> "test_jwt_secret_key_for_testing_only_minimum_256_bits_required_for_hs512_algorithm_this_is_long_enough");
        registry.add("jwt.access-token-expiration", () -> "15m");
        registry.add("jwt.refresh-token-expiration", () -> "7d");
        registry.add("jwt.issuer", () -> "healthdata-in-motion-test");
        registry.add("jwt.audience", () -> "healthdata-api-test");
        registry.add("healthdata.security.jwt.secret", () -> "test_jwt_secret_key_for_testing_only_minimum_256_bits_required_for_hs512_algorithm_this_is_long_enough");
        registry.add("healthdata.security.jwt.issuer", () -> "healthdata-in-motion-test");
    }

    /**
     * Validate that all JPA entities in the service match their database schema.
     *
     * This test:
     * 1. Extracts all @Entity classes from the JPA metamodel
     * 2. Creates an EntityMigrationValidator with the test datasource
     * 3. Validates each entity against the actual database schema
     * 4. Reports all mismatches (missing tables, missing columns, type mismatches, etc.)
     *
     * Success means:
     * - All entities have corresponding database tables
     * - All entity fields have corresponding database columns
     * - Column types, nullability, and constraints match between Java and database
     * - All indexes and foreign keys are properly defined
     *
     * Failure indicates schema drift - likely caused by:
     * - Creating a new entity without a Liquibase migration
     * - Modifying an entity field without updating the corresponding migration
     * - Deleting a migration before it was applied to production
     */
    @Test
    void validateAllEntitiesMatchDatabaseSchema() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(entities);

        assertTrue(report.isValid(),
                "Entity-migration validation failed for " + getServiceName() + ".\n" +
                "This indicates schema drift between JPA entities and database schema.\n" +
                "Common causes:\n" +
                "  1. New entity created without Liquibase migration\n" +
                "  2. Entity field modified without corresponding migration\n" +
                "  3. Column type mismatch (e.g., VARCHAR vs TEXT, INT vs BIGINT)\n" +
                "  4. Nullability mismatch (nullable=false in entity but NULL allowed in DB)\n\n" +
                report.getDetailedMessage());
    }

    /**
     * Validate only critical entities that must always be in sync.
     *
     * Subclasses should override getCriticalEntityNames() to specify which
     * entities are critical. This allows focusing validation on core domain
     * entities without validating all transitive dependencies.
     *
     * Useful for services that aggregate many shared entities but only own
     * a subset that require strict synchronization.
     */
    @Test
    void validateCriticalEntitiesMatchDatabaseSchema() {
        Set<String> criticalEntityNames = getCriticalEntityNames();

        if (criticalEntityNames.isEmpty()) {
            // Skip test if no critical entities defined
            return;
        }

        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> criticalEntityNames.contains(e.getName()))
                .collect(java.util.stream.Collectors.toSet());

        if (criticalEntities.isEmpty()) {
            throw new IllegalStateException(
                    "No critical entities found for " + getServiceName() + ". " +
                    "Defined: " + criticalEntityNames + ", Available: " +
                    entities.stream().map(EntityType::getName).collect(java.util.stream.Collectors.toSet()));
        }

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical entity-migration validation failed for " + getServiceName() + ".\n" +
                report.getDetailedMessage());
    }

    /**
     * Return the human-readable name of the service being tested.
     * Used in error messages to identify which service had validation failures.
     *
     * Example: "patient-service", "quality-measure-service"
     *
     * @return the service name
     */
    protected abstract String getServiceName();

    /**
     * Return the set of critical entity names that must be validated.
     * By default, returns empty set (validates all entities).
     *
     * Subclasses can override to validate only specific critical entities.
     * This is useful for services with many transitive entity dependencies
     * where only a few are under this service's control.
     *
     * Example:
     * <pre>
     * @Override
     * protected Set<String> getCriticalEntityNames() {
     *     return Set.of("CareGapEntity", "CareGapMetric", "CareGapExecution");
     * }
     * </pre>
     *
     * @return set of critical entity class names, or empty set to validate all
     */
    protected Set<String> getCriticalEntityNames() {
        return Collections.emptySet();
    }
}
