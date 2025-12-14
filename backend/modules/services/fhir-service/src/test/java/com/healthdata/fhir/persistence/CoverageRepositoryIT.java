package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for CoverageRepository.
 * Tests all custom query methods for finding and tracking insurance coverage.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class CoverageRepositoryIT {

    private static final String H2_URL = "jdbc:h2:mem:healthdata_fhir_coverage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private CoverageRepository coverageRepository;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        PatientEntity patient = PatientEntity.builder()
                .id(PATIENT_ID)
                .tenantId(TENANT_ID)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + PATIENT_ID + "\"}")
                .firstName("John")
                .lastName("Doe")
                .gender("male")
                .birthDate(LocalDate.of(1980, 1, 1))
                .build();
        patientRepository.save(patient);
    }

    @Test
    void shouldPersistAndRetrieveCoverage() {
        // Given
        CoverageEntity entity = createCoverage("active", "MEM123", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        // When
        CoverageEntity saved = coverageRepository.save(entity);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0);

        CoverageEntity found = coverageRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSubscriberId()).isEqualTo("MEM123");
        assertThat(found.getStatus()).isEqualTo("active");
    }

    @Test
    void shouldFindByTenantAndPatient() {
        // Given
        createCoverage("active", "MEM001", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        createCoverage("cancelled", "MEM002", Instant.now().minusSeconds(365 * 24 * 60 * 60), Instant.now().minusSeconds(30 * 24 * 60 * 60));

        // When
        List<CoverageEntity> results = coverageRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByPeriodStartDesc(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindActiveCoverages() {
        // Given
        Instant now = Instant.now();
        createCoverage("active", "MEM001", now.minusSeconds(30 * 24 * 60 * 60), now.plusSeconds(365 * 24 * 60 * 60));
        createCoverage("active", "MEM002", now.plusSeconds(30 * 24 * 60 * 60), now.plusSeconds(365 * 24 * 60 * 60)); // Future start
        createCoverage("cancelled", "MEM003", now.minusSeconds(30 * 24 * 60 * 60), now.plusSeconds(365 * 24 * 60 * 60));

        // When
        List<CoverageEntity> active = coverageRepository.findActiveCoveragesForPatient(TENANT_ID, PATIENT_ID, now);

        // Then
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getSubscriberId()).isEqualTo("MEM001");
    }

    @Test
    void shouldCheckHasActiveCoverage() {
        // Given
        Instant now = Instant.now();
        createCoverage("active", "MEM001", now.minusSeconds(30 * 24 * 60 * 60), now.plusSeconds(365 * 24 * 60 * 60));

        // When
        boolean hasCoverage = coverageRepository.hasActiveCoverage(TENANT_ID, PATIENT_ID, now);

        // Then
        assertThat(hasCoverage).isTrue();
    }

    @Test
    void shouldFindPrimaryCoverage() {
        // Given
        Instant now = Instant.now();
        CoverageEntity primary = createCoverage("active", "MEM001", now.minusSeconds(60 * 24 * 60 * 60), now.plusSeconds(365 * 24 * 60 * 60));
        primary.setCoverageOrder(1);
        coverageRepository.save(primary);

        CoverageEntity secondary = createCoverage("active", "MEM002", now.minusSeconds(30 * 24 * 60 * 60), now.plusSeconds(365 * 24 * 60 * 60));
        secondary.setCoverageOrder(2);
        coverageRepository.save(secondary);

        // When
        Optional<CoverageEntity> primaryCoverage = coverageRepository.findPrimaryCoverage(TENANT_ID, PATIENT_ID, now);

        // Then
        assertThat(primaryCoverage).isPresent();
        assertThat(primaryCoverage.get().getCoverageOrder()).isEqualTo(1);
    }

    @Test
    void shouldFindExpiringCoverages() {
        // Given
        Instant now = Instant.now();
        Instant thirtyDaysFromNow = now.plusSeconds(30 * 24 * 60 * 60);

        createCoverage("active", "MEM001", now.minusSeconds(300 * 24 * 60 * 60), now.plusSeconds(15 * 24 * 60 * 60)); // Expires in 15 days
        createCoverage("active", "MEM002", now.minusSeconds(300 * 24 * 60 * 60), now.plusSeconds(60 * 24 * 60 * 60)); // Expires in 60 days

        // When
        List<CoverageEntity> expiring = coverageRepository.findExpiringCoverages(TENANT_ID, now, thirtyDaysFromNow);

        // Then
        assertThat(expiring).hasSize(1);
        assertThat(expiring.get(0).getSubscriberId()).isEqualTo("MEM001");
    }

    @Test
    void shouldFindByStatus() {
        // Given
        createCoverage("active", "MEM001", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        createCoverage("cancelled", "MEM002", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        createCoverage("draft", "MEM003", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        // When
        List<CoverageEntity> active = coverageRepository.findByTenantIdAndPatientIdAndStatusAndDeletedAtIsNull(TENANT_ID, PATIENT_ID, "active");

        // Then
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getSubscriberId()).isEqualTo("MEM001");
    }

    @Test
    void shouldHandleMultiTenantIsolation() {
        // Given
        String tenant2 = "tenant-2";
        UUID patient2 = UUID.randomUUID();

        PatientEntity patient = PatientEntity.builder()
                .id(patient2)
                .tenantId(tenant2)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + patient2 + "\"}")
                .firstName("Jane")
                .lastName("Smith")
                .gender("female")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        patientRepository.save(patient);

        createCoverage("active", "MEM001", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        UUID tenant2CoverageId = UUID.randomUUID();
        CoverageEntity tenant2Coverage = CoverageEntity.builder()
                .id(tenant2CoverageId)
                .tenantId(tenant2)
                .patientId(patient2)
                .resourceJson("{\"resourceType\":\"Coverage\",\"id\":\"" + tenant2CoverageId + "\"}")
                .status("active")
                .subscriberId("MEM002")
                .typeCode("HIP")
                .payorReference("Organization/payor-2")
                .periodStart(Instant.now())
                .periodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                .build();
        coverageRepository.save(tenant2Coverage);

        // When
        List<CoverageEntity> tenant1Results = coverageRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByPeriodStartDesc(TENANT_ID, PATIENT_ID);
        List<CoverageEntity> tenant2Results = coverageRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByPeriodStartDesc(tenant2, patient2);

        // Then
        assertThat(tenant1Results).hasSize(1);
        assertThat(tenant2Results).hasSize(1);
        assertThat(tenant1Results.get(0).getTenantId()).isEqualTo(TENANT_ID);
        assertThat(tenant2Results.get(0).getTenantId()).isEqualTo(tenant2);
    }

    @Test
    void shouldSoftDeleteCoverage() {
        // Given
        CoverageEntity entity = createCoverage("active", "MEM001", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        UUID coverageId = entity.getId();

        // When
        entity.setDeletedAt(Instant.now());
        coverageRepository.save(entity);

        // Then
        Optional<CoverageEntity> found = coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT_ID, coverageId);
        assertThat(found).isEmpty();
    }

    private CoverageEntity createCoverage(String status, String subscriberId, Instant periodStart, Instant periodEnd) {
        UUID coverageId = UUID.randomUUID();
        CoverageEntity entity = CoverageEntity.builder()
                .id(coverageId)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .resourceJson("{\"resourceType\":\"Coverage\",\"id\":\"" + coverageId + "\"}")
                .status(status)
                .subscriberId(subscriberId)
                .typeCode("HIP")
                .typeDisplay("Health Insurance Plan")
                .payorReference("Organization/payor-1")
                .payorDisplay("Blue Cross")
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .build();

        return coverageRepository.save(entity);
    }
}
