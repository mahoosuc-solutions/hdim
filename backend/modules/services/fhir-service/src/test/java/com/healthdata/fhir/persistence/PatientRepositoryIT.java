package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.healthdata.fhir.config.TestCacheConfiguration;
import com.healthdata.fhir.config.TestContainersConfiguration;

/**
 * Integration test for {@link PatientRepository} using Testcontainers.
 * <p>
 * This test uses real containerized infrastructure:
 * <ul>
 *   <li>PostgreSQL for database operations (via Testcontainers)</li>
 *   <li>Redis for caching (mocked via TestCacheConfiguration)</li>
 *   <li>Kafka for messaging (available but not used in this test)</li>
 * </ul>
 * <p>
 * The PostgreSQL container is automatically managed by {@link TestContainersConfiguration}
 * and connection properties are configured via {@code @DynamicPropertySource}.
 * <p>
 * This demonstrates best practices for integration testing with Spring Boot:
 * <ul>
 *   <li>Real database for accurate persistence testing</li>
 *   <li>Isolated test environment via Docker containers</li>
 *   <li>No manual infrastructure setup required</li>
 *   <li>Tests can run on any machine with Docker</li>
 * </ul>
 */
@SpringBootTest(
    properties = {
        "spring.cache.type=simple",
        "spring.data.redis.repositories.enabled=false",
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
    }
)
@Import({TestContainersConfiguration.class, TestCacheConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
class PatientRepositoryIT {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * Configures dynamic properties for test containers.
     * <p>
     * This method is called before the Spring context is loaded and provides
     * connection details from the Testcontainers PostgreSQL instance.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureContainerProperties(DynamicPropertyRegistry registry) {
        // Configure PostgreSQL connection from Testcontainers
        TestContainersConfiguration.configurePostgres(registry);

        // Disable Liquibase for tests (tables will be created by Hibernate)
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Enable SQL logging for debugging if needed
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
    }

    @Test
    void shouldPersistAndRetrievePatient() {
        // Given: a patient entity
        PatientEntity entity = PatientEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"123\"}")
                .firstName("Maya")
                .lastName("Chen")
                .gender("female")
                .birthDate(LocalDate.of(1985, 5, 20))
                .build();

        // When: we save the patient
        PatientEntity saved = patientRepository.save(entity);

        // Then: the patient should be persisted with version and timestamp
        assertThat(saved.getVersion()).isEqualTo(0);
        assertThat(saved.getCreatedAt()).isNotNull();

        // And: we should be able to retrieve it by ID
        PatientEntity found = patientRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getFirstName()).isEqualTo("Maya");
        assertThat(found.getLastName()).isEqualTo("Chen");
        assertThat(found.getResourceJson()).contains("\"Patient\"");
    }

    @Test
    void shouldSearchByLastNameCaseInsensitive() {
        // Given: a patient with a specific last name
        patientRepository.save(PatientEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"abc\"}")
                .firstName("Rafael")
                .lastName("Iglesias")
                .gender("male")
                .birthDate(LocalDate.of(1990, 2, 15))
                .build());

        // When: we search by a partial last name (case-insensitive)
        List<PatientEntity> results = patientRepository
                .findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc("tenant-1", "gles");

        // Then: we should find the patient
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getLastName()).isEqualTo("Iglesias");
    }
}
