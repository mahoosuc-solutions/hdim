package com.healthdata.cms.integration;

import com.healthdata.cache.CacheEvictionService;
import com.healthdata.cms.auth.OAuth2Manager;
import com.healthdata.cms.client.BcdaClient;
import com.healthdata.cms.client.DpcClient;
import com.healthdata.cms.repository.CmsIntegrationConfigRepository;
import com.healthdata.testfixtures.validation.EntityMigrationValidator;
import com.healthdata.testfixtures.validation.ValidationReport;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that validates entity-migration synchronization for the CMS Connector service.
 *
 * This test ensures that all JPA entities representing CMS claims data have corresponding
 * database schema definitions via Liquibase migrations, catching entity-migration drift issues.
 *
 * Key validations:
 * - All @Entity classes have corresponding database tables
 * - All @Column fields have matching database columns
 * - Column types are compatible between Java and PostgreSQL
 * - Nullability constraints match
 * - Multi-tenant isolation columns are present
 *
 * @author HDIM Platform Team
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
// Override the restricted @EntityScan from AuthenticationAutoConfiguration shared library,
// which otherwise limits entity scanning to com.healthdata.authentication.domain only.
@EntityScan(basePackages = {
    "com.healthdata.cms.model",
    "com.healthdata.authentication.domain",
    "com.healthdata.audit.entity"
})
class EntityMigrationValidationTest {

    // Mock the two RestTemplate beans by name — RestTemplateConfig.cmsRestTemplate() and
    // RestTemplateConfig.oauth2RestTemplate() both use Apache HttpClient 5 APIs which throw
    // NoSuchMethodError (DefaultHttpRequestWriterFactory API mismatch in test classpath).
    // Schema validation requires no HTTP, so these can safely be mocked.
    @MockBean(name = "cmsRestTemplate")
    private RestTemplate cmsRestTemplate;

    @MockBean(name = "oauth2RestTemplate")
    private RestTemplate oauth2RestTemplate;

    // Mock the Resilience4j event consumer beans produced by RestTemplateConfig:
    // RestTemplateConfig defines circuitBreakerEventConsumer(CircuitBreakerRegistry) which
    // causes a circular dependency when Resilience4j's auto-config tries to build
    // CircuitBreakerRegistry (it depends on RegistryEventConsumer<CircuitBreaker> beans, which
    // include the bean currently being created). Mocking these beans by name prevents
    // RestTemplateConfig's @Bean factory methods from running.
    @MockBean(name = "circuitBreakerEventConsumer")
    @SuppressWarnings("rawtypes")
    private RegistryEventConsumer circuitBreakerEventConsumer;

    @MockBean(name = "retryEventConsumer")
    @SuppressWarnings("rawtypes")
    private RegistryEventConsumer retryEventConsumer;

    // Mock out CMS HTTP client beans — their constructors depend on the RestTemplate beans above
    // and OAuth2 token exchange which is not needed for schema validation.
    @MockBean
    private OAuth2Manager oAuth2Manager;

    @MockBean
    private BcdaClient bcdaClient;

    @MockBean
    private DpcClient dpcClient;

    // CmsIntegrationConfigRepository.findDueForSync() uses MySQL-syntax TIMESTAMPDIFF() JPQL,
    // which fails Hibernate JPQL validation with the PostgreSQL dialect. Since this test only
    // validates entity-migration synchronization, the actual repository logic is not needed.
    @MockBean
    private CmsIntegrationConfigRepository cmsIntegrationConfigRepository;

    // Mock CacheEvictionService and CacheManager — the shared cache module's CacheAutoConfiguration
    // only activates for spring.cache.type=redis. In test mode we use caffeine, so no CacheManager
    // bean is created by the shared module. We don't need real caching for schema validation.
    @MockBean
    private CacheManager cacheManager;

    @MockBean
    private CacheEvictionService cacheEvictionService;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cms_connector_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.change-log", () -> "classpath:db/changelog/db.changelog-master.xml");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Disable audit module — its @EnableJpaRepositories restricts entity scanning
        // to com.healthdata.audit.* only, causing CmsClaim to fail as "not a managed type"
        registry.add("audit.enabled", () -> "false");
        // JWT stub config required by AuthenticationAutoConfiguration
        registry.add("jwt.secret", () -> "test-secret-key-for-entity-migration-validation-test-only");
        registry.add("jwt.expiration", () -> "3600000");
    }

    /**
     * Validate that all CMS Connector service entities match their database schema.
     *
     * This test validates core CMS claims data models:
     * - CmsClaim: Primary claims record from CMS APIs (BCDA, DPC, AB2D)
     * - SyncAuditLog: Audit trail for all CMS data sync operations
     * - CmsCondition: Clinical conditions imported from CMS
     * - CmsIntegrationConfig: CMS API integration configuration per tenant
     * - CmsMedicationRequest: Medication requests imported from CMS
     * - CmsObservation: Clinical observations imported from CMS
     * - CmsProcedure: Procedures imported from CMS
     *
     * Only entities from the com.healthdata.cms.model package are validated.
     * Shared module entities (authentication, audit) are excluded because their
     * tables are managed by those respective services, not by the CMS connector service.
     *
     * When this test fails, it provides detailed error messages indicating exactly which
     * entity and column have the issue, making debugging straightforward.
     */
    @Test
    void validateAllEntitiesMatchDatabaseSchema() {
        // Get all entities from the persistence unit, filtered to CMS service entities only.
        // The @EntityScan includes shared module packages (authentication, audit) to satisfy
        // Spring's entity scanning requirements, but those tables don't exist in this
        // service's database — only com.healthdata.cms.model entities belong here.
        Set<EntityType<?>> allEntities = entityManagerFactory.getMetamodel().getEntities();
        Set<EntityType<?>> cmsEntities = allEntities.stream()
                .filter(e -> e.getJavaType() != null &&
                        e.getJavaType().getPackageName().startsWith("com.healthdata.cms.model"))
                .collect(java.util.stream.Collectors.toSet());

        // Create validator and run validation
        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(cmsEntities);

        // Assert validation passed (no errors)
        assertTrue(report.isValid(),
                "Entity-migration validation failed. Database schema does not match JPA entities." +
                        report.getDetailedMessage());
    }

    /**
     * Validate only critical CMS claims entities.
     *
     * This test focuses on the core claims entities that are fundamental to the
     * CMS data import pipeline. These must always be in sync with the database schema.
     */
    @Test
    void validateCriticalCmsEntities() {
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        // Filter to critical CMS entities
        Set<EntityType<?>> criticalEntities = entities.stream()
                .filter(e -> {
                    String name = e.getName();
                    return name.equals("CmsClaim") ||
                           name.equals("SyncAuditLog");
                })
                .collect(java.util.stream.Collectors.toSet());

        EntityMigrationValidator validator = new EntityMigrationValidator(dataSource);
        ValidationReport report = validator.validate(criticalEntities);

        assertTrue(report.isValid(),
                "Critical CMS entity-migration validation failed." + report.getDetailedMessage());
    }
}
