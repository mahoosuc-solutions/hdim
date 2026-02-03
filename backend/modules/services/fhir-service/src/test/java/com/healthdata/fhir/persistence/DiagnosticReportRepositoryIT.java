package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for DiagnosticReportRepository.
 * Tests all custom query methods for finding and tracking diagnostic reports.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
@Tag("integration")
class DiagnosticReportRepositoryIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ENCOUNTER_ID = UUID.randomUUID();
    private static final String CODE_CBC = "58410-2";
    private static final String CODE_CMP = "24323-8";

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
    private DiagnosticReportRepository diagnosticReportRepository;

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
    void shouldPersistAndRetrieveReport() {
        // Given
        DiagnosticReportEntity entity = createReport("final", CODE_CBC, "LAB", Instant.now());

        // When
        DiagnosticReportEntity saved = diagnosticReportRepository.save(entity);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        DiagnosticReportEntity found = diagnosticReportRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCode()).isEqualTo(CODE_CBC);
        assertThat(found.getStatus()).isEqualTo("final");
    }

    @Test
    void shouldFindByTenantAndPatient() {
        // Given
        createReport("final", CODE_CBC, "LAB", Instant.now().minusSeconds(60 * 60));
        createReport("final", CODE_CMP, "LAB", Instant.now());

        // When
        List<DiagnosticReportEntity> results = diagnosticReportRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCode()).isEqualTo(CODE_CMP); // Most recent first
    }

    @Test
    void shouldFindFinalReports() {
        // Given
        createReport("final", CODE_CBC, "LAB", Instant.now());
        createReport("preliminary", CODE_CMP, "LAB", Instant.now());
        createReport("final", CODE_CBC, "LAB", Instant.now().minusSeconds(60 * 60));

        // When
        List<DiagnosticReportEntity> finalReports = diagnosticReportRepository.findFinalReportsForPatient(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(finalReports).hasSize(2);
        assertThat(finalReports).allMatch(r -> r.getStatus().equals("final"));
    }

    @Test
    void shouldFindByEncounter() {
        // Given
        DiagnosticReportEntity withEncounter = createReport("final", CODE_CBC, "LAB", Instant.now());
        withEncounter.setEncounterId(ENCOUNTER_ID);
        diagnosticReportRepository.save(withEncounter);

        createReport("final", CODE_CMP, "LAB", Instant.now());

        // When
        List<DiagnosticReportEntity> results = diagnosticReportRepository
                .findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(TENANT_ID, ENCOUNTER_ID);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEncounterId()).isEqualTo(ENCOUNTER_ID);
    }

    @Test
    void shouldFindLabReports() {
        // Given
        createReport("final", CODE_CBC, "LAB", Instant.now());
        createReport("final", "24558-9", "RAD", Instant.now()); // Radiology report

        // When
        List<DiagnosticReportEntity> labReports = diagnosticReportRepository.findLabReportsForPatient(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(labReports).hasSize(1);
        assertThat(labReports.get(0).getCategoryCode()).isEqualTo("LAB");
    }

    @Test
    void shouldFindImagingReports() {
        // Given
        createReport("final", CODE_CBC, "LAB", Instant.now());
        createReport("final", "24558-9", "RAD", Instant.now());

        // When
        List<DiagnosticReportEntity> imagingReports = diagnosticReportRepository.findImagingReportsForPatient(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(imagingReports).hasSize(1);
        assertThat(imagingReports.get(0).getCategoryCode()).isEqualTo("RAD");
    }

    @Test
    void shouldFindPendingReports() {
        // Given
        createReport("final", CODE_CBC, "LAB", Instant.now());
        createReport("preliminary", CODE_CMP, "LAB", Instant.now());
        createReport("registered", CODE_CBC, "LAB", Instant.now());

        // When
        List<DiagnosticReportEntity> pending = diagnosticReportRepository.findPendingReports(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(pending).hasSize(2);
        assertThat(pending).allMatch(r -> r.getStatus().equals("preliminary") || r.getStatus().equals("registered"));
    }

    @Test
    void shouldFindLatestByCode() {
        // Given
        createReport("final", CODE_CBC, "LAB", Instant.now().minusSeconds(2 * 60 * 60));
        createReport("final", CODE_CBC, "LAB", Instant.now().minusSeconds(60 * 60));
        createReport("final", CODE_CBC, "LAB", Instant.now());

        // When
        List<DiagnosticReportEntity> latest = diagnosticReportRepository.findLatestByCode(
                TENANT_ID, PATIENT_ID, CODE_CBC, PageRequest.of(0, 1));

        // Then
        assertThat(latest).hasSize(1);
    }

    @Test
    void shouldFindByEffectiveDateRange() {
        // Given
        Instant startDate = Instant.now().minusSeconds(45 * 24 * 60 * 60);
        Instant endDate = Instant.now().minusSeconds(15 * 24 * 60 * 60);

        createReport("final", CODE_CBC, "LAB", Instant.now().minusSeconds(60 * 24 * 60 * 60));
        createReport("final", CODE_CMP, "LAB", Instant.now().minusSeconds(30 * 24 * 60 * 60));
        createReport("final", CODE_CBC, "LAB", Instant.now().minusSeconds(10 * 24 * 60 * 60));

        // When
        List<DiagnosticReportEntity> inRange = diagnosticReportRepository.findByEffectiveDateRange(
                TENANT_ID, PATIENT_ID, startDate, endDate);

        // Then
        assertThat(inRange).hasSize(1);
        assertThat(inRange.get(0).getCode()).isEqualTo(CODE_CMP);
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

        createReport("final", CODE_CBC, "LAB", Instant.now());

        UUID tenant2ReportId = UUID.randomUUID();
        DiagnosticReportEntity tenant2Report = DiagnosticReportEntity.builder()
                .id(tenant2ReportId)
                .tenantId(tenant2)
                .patientId(patient2)
                .resourceJson("{\"resourceType\":\"DiagnosticReport\",\"id\":\"" + tenant2ReportId + "\"}")
                .status("final")
                .code(CODE_CBC)
                .codeSystem("http://loinc.org")
                .codeDisplay("Complete Blood Count")
                .categoryCode("LAB")
                .effectiveDatetime(Instant.now())
                .issuedDatetime(Instant.now())
                .build();
        diagnosticReportRepository.save(tenant2Report);

        // When
        List<DiagnosticReportEntity> tenant1Results = diagnosticReportRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(TENANT_ID, PATIENT_ID);
        List<DiagnosticReportEntity> tenant2Results = diagnosticReportRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(tenant2, patient2);

        // Then
        assertThat(tenant1Results).hasSize(1);
        assertThat(tenant2Results).hasSize(1);
    }

    private DiagnosticReportEntity createReport(String status, String code, String category, Instant effectiveDatetime) {
        UUID reportId = UUID.randomUUID();
        DiagnosticReportEntity entity = DiagnosticReportEntity.builder()
                .id(reportId)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .resourceJson("{\"resourceType\":\"DiagnosticReport\",\"id\":\"" + reportId + "\"}")
                .status(status)
                .code(code)
                .codeSystem("http://loinc.org")
                .codeDisplay(code.equals(CODE_CBC) ? "Complete Blood Count" : "Comprehensive Metabolic Panel")
                .categoryCode(category)
                .categoryDisplay(category.equals("LAB") ? "Laboratory" : "Radiology")
                .effectiveDatetime(effectiveDatetime)
                .issuedDatetime(effectiveDatetime)
                .conclusion("Normal results")
                .build();

        return diagnosticReportRepository.save(entity);
    }
}
