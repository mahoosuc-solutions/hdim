package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for ImmunizationRepository.
 * Tests all custom query methods for finding and tracking immunizations.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
@Tag("integration")
class ImmunizationRepositoryIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String CVX_CODE_COVID = "213"; // COVID-19 vaccine
    private static final String CVX_CODE_FLU = "141"; // Influenza vaccine

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private ImmunizationRepository immunizationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        // Create a test patient
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
    void shouldPersistAndRetrieveImmunization() {
        // Given
        ImmunizationEntity entity = ImmunizationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .vaccineCode(CVX_CODE_COVID)
                .vaccineSystem("http://hl7.org/fhir/sid/cvx")
                .vaccineDisplay("COVID-19 vaccine")
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .primarySource(true)
                .lotNumber("LOT123")
                .expirationDate(LocalDate.now().plusYears(1))
                .site("LA")
                .route("IM")
                .doseNumber(1)
                .seriesDoses(2)
                .build();

        // When
        ImmunizationEntity saved = immunizationRepository.save(entity);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0);

        ImmunizationEntity found = immunizationRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getVaccineCode()).isEqualTo(CVX_CODE_COVID);
        assertThat(found.getStatus()).isEqualTo("completed");
        assertThat(found.getDoseNumber()).isEqualTo(1);
    }

    @Test
    void shouldFindByTenantAndPatient() {
        // Given
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 1, 2);
        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15), null, null);

        // When
        List<ImmunizationEntity> results = immunizationRepository
                .findByTenantIdAndPatientIdOrderByOccurrenceDateDesc(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getVaccineCode()).isEqualTo(CVX_CODE_FLU); // Most recent first
        assertThat(results.get(1).getVaccineCode()).isEqualTo(CVX_CODE_COVID);
    }

    @Test
    void shouldFindCompletedImmunizations() {
        // Given
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 1, 2);
        createImmunization(CVX_CODE_FLU, "not-done", LocalDate.now().minusDays(15), null, null);
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(10), 2, 2);

        // When
        List<ImmunizationEntity> completed = immunizationRepository
                .findCompletedImmunizations(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(completed).hasSize(2);
        assertThat(completed).allMatch(imm -> imm.getStatus().equals("completed"));
    }

    @Test
    void shouldFindByVaccineCode() {
        // Given
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60), 1, 2);
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 2, 2);
        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15), null, null);

        // When
        List<ImmunizationEntity> covidVaccines = immunizationRepository
                .findByVaccineCode(TENANT_ID, PATIENT_ID, CVX_CODE_COVID);

        // Then
        assertThat(covidVaccines).hasSize(2);
        assertThat(covidVaccines).allMatch(imm -> imm.getVaccineCode().equals(CVX_CODE_COVID));
    }

    @Test
    void shouldCheckHasImmunization() {
        // Given
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 1, 2);

        // When
        boolean hasCovid = immunizationRepository.hasImmunization(TENANT_ID, PATIENT_ID, CVX_CODE_COVID);
        boolean hasFlu = immunizationRepository.hasImmunization(TENANT_ID, PATIENT_ID, CVX_CODE_FLU);

        // Then
        assertThat(hasCovid).isTrue();
        assertThat(hasFlu).isFalse();
    }

    @Test
    void shouldCountByVaccineCode() {
        // Given
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60), 1, 2);
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 2, 2);
        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15), null, null);

        // When
        long covidCount = immunizationRepository.countByVaccineCode(TENANT_ID, PATIENT_ID, CVX_CODE_COVID);
        long fluCount = immunizationRepository.countByVaccineCode(TENANT_ID, PATIENT_ID, CVX_CODE_FLU);

        // Then
        assertThat(covidCount).isEqualTo(2);
        assertThat(fluCount).isEqualTo(1);
    }

    @Test
    void shouldFindByOccurrenceDateRange() {
        // Given
        LocalDate startDate = LocalDate.now().minusDays(45);
        LocalDate endDate = LocalDate.now().minusDays(15);

        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60), 1, 2);
        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(30), null, null);
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(10), 2, 2);

        // When
        List<ImmunizationEntity> inRange = immunizationRepository
                .findByOccurrenceDateRange(TENANT_ID, PATIENT_ID, startDate, endDate);

        // Then
        assertThat(inRange).hasSize(1);
        assertThat(inRange.get(0).getVaccineCode()).isEqualTo(CVX_CODE_FLU);
    }

    @Test
    void shouldFindVaccineSeries() {
        // Given
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60), 1, 2);
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 2, 2);
        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15), null, null);

        // When
        List<ImmunizationEntity> covidSeries = immunizationRepository
                .findVaccineSeries(TENANT_ID, PATIENT_ID, CVX_CODE_COVID);

        // Then
        assertThat(covidSeries).hasSize(2);
        assertThat(covidSeries.get(0).getDoseNumber()).isEqualTo(1);
        assertThat(covidSeries.get(1).getDoseNumber()).isEqualTo(2);
    }

    @Test
    void shouldCheckSeriesComplete() {
        // Given
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60), 1, 2);
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 2, 2);

        // When
        boolean isComplete = immunizationRepository.isSeriesComplete(TENANT_ID, PATIENT_ID, CVX_CODE_COVID, 2);
        boolean needsMore = immunizationRepository.isSeriesComplete(TENANT_ID, PATIENT_ID, CVX_CODE_COVID, 3);

        // Then
        assertThat(isComplete).isTrue();
        assertThat(needsMore).isFalse();
    }

    @Test
    void shouldFindImmunizationsWithReactions() {
        // Given
        ImmunizationEntity withReaction = createImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now().minusDays(30), 1, 2);
        withReaction.setHadReaction(true);
        withReaction.setReactionDetail("Mild fever and arm soreness");
        immunizationRepository.save(withReaction);

        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(15), null, null);

        // When
        List<ImmunizationEntity> withReactions = immunizationRepository
                .findImmunizationsWithReactions(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(withReactions).hasSize(1);
        assertThat(withReactions.get(0).getHadReaction()).isTrue();
        assertThat(withReactions.get(0).getReactionDetail()).contains("fever");
    }

    @Test
    void shouldCountCompletedImmunizations() {
        // Given
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60), 1, 2);
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 2, 2);
        createImmunization(CVX_CODE_FLU, "not-done", LocalDate.now().minusDays(15), null, null);

        // When
        long count = immunizationRepository.countCompletedImmunizations(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldFindRecentImmunizations() {
        // Given
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60), 1, 2);
        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now().minusDays(20), null, null);
        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(10), 2, 2);

        // When
        List<ImmunizationEntity> recent = immunizationRepository
                .findRecentImmunizations(TENANT_ID, PATIENT_ID, thirtyDaysAgo);

        // Then
        assertThat(recent).hasSize(2);
        assertThat(recent).allMatch(imm -> !imm.getOccurrenceDate().isBefore(thirtyDaysAgo));
    }

    @Test
    void shouldFindByEncounter() {
        // Given
        UUID encounterId = UUID.randomUUID();
        ImmunizationEntity withEncounter = createImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now(), 1, 2);
        withEncounter.setEncounterId(encounterId);
        immunizationRepository.save(withEncounter);

        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now(), null, null);

        // When
        List<ImmunizationEntity> encounterImmunizations = immunizationRepository
                .findByTenantIdAndEncounterId(TENANT_ID, encounterId);

        // Then
        assertThat(encounterImmunizations).hasSize(1);
        assertThat(encounterImmunizations.get(0).getEncounterId()).isEqualTo(encounterId);
    }

    @Test
    void shouldFindByLotNumber() {
        // Given
        String lotNumber = "LOT-ABC-123";
        ImmunizationEntity withLot = createImmunization(CVX_CODE_COVID, "completed",
                LocalDate.now(), 1, 2);
        withLot.setLotNumber(lotNumber);
        immunizationRepository.save(withLot);

        createImmunization(CVX_CODE_FLU, "completed", LocalDate.now(), null, null);

        // When
        List<ImmunizationEntity> byLot = immunizationRepository
                .findByLotNumber(TENANT_ID, lotNumber);

        // Then
        assertThat(byLot).hasSize(1);
        assertThat(byLot.get(0).getLotNumber()).isEqualTo(lotNumber);
    }

    @Test
    void shouldHandleMultiTenantIsolation() {
        // Given
        String tenant2 = "tenant-2";
        UUID patient2 = UUID.randomUUID();

        // Create patient in tenant-2
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

        createImmunization(CVX_CODE_COVID, "completed", LocalDate.now(), 1, 2);

        ImmunizationEntity tenant2Imm = ImmunizationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant2)
                .patientId(patient2)
                .vaccineCode(CVX_CODE_COVID)
                .vaccineSystem("http://hl7.org/fhir/sid/cvx")
                .vaccineDisplay("COVID-19 vaccine")
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .build();
        immunizationRepository.save(tenant2Imm);

        // When
        List<ImmunizationEntity> tenant1Results = immunizationRepository
                .findByTenantIdAndPatientIdOrderByOccurrenceDateDesc(TENANT_ID, PATIENT_ID);
        List<ImmunizationEntity> tenant2Results = immunizationRepository
                .findByTenantIdAndPatientIdOrderByOccurrenceDateDesc(tenant2, patient2);

        // Then
        assertThat(tenant1Results).hasSize(1);
        assertThat(tenant2Results).hasSize(1);
        assertThat(tenant1Results.get(0).getTenantId()).isEqualTo(TENANT_ID);
        assertThat(tenant2Results.get(0).getTenantId()).isEqualTo(tenant2);
    }

    // Helper method to create immunization records
    private ImmunizationEntity createImmunization(String vaccineCode, String status,
                                                   LocalDate occurrenceDate,
                                                   Integer doseNumber, Integer seriesDoses) {
        ImmunizationEntity entity = ImmunizationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .vaccineCode(vaccineCode)
                .vaccineSystem("http://hl7.org/fhir/sid/cvx")
                .vaccineDisplay(vaccineCode.equals(CVX_CODE_COVID) ? "COVID-19 vaccine" : "Influenza vaccine")
                .status(status)
                .occurrenceDate(occurrenceDate)
                .primarySource(true)
                .doseNumber(doseNumber)
                .seriesDoses(seriesDoses)
                .build();

        return immunizationRepository.save(entity);
    }
}
