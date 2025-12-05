package com.healthdata.quality.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Tests for SavedReportRepository
 * These tests define the expected behavior before implementation
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("SavedReportRepository Tests")
class SavedReportRepositoryTest {

    @Autowired
    private SavedReportRepository repository;

    private static final String TENANT_1 = "tenant-1";
    private static final String TENANT_2 = "tenant-2";
    private static final UUID PATIENT_ID_1 = UUID.randomUUID();
    private static final UUID PATIENT_ID_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should save and retrieve a patient report")
    void shouldSaveAndRetrievePatientReport() {
        // Arrange
        SavedReportEntity report = createPatientReport(TENANT_1, PATIENT_ID_1, "Patient Report 1");

        // Act
        SavedReportEntity saved = repository.save(report);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_1);
        assertThat(saved.getReportType()).isEqualTo("PATIENT");
        assertThat(saved.getReportName()).isEqualTo("Patient Report 1");
        assertThat(saved.getPatientId()).isEqualTo(PATIENT_ID_1);
        assertThat(saved.getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should save and retrieve a population report")
    void shouldSaveAndRetrievePopulationReport() {
        // Arrange
        SavedReportEntity report = createPopulationReport(TENANT_1, 2024, "Population Report 2024");

        // Act
        SavedReportEntity saved = repository.save(report);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_1);
        assertThat(saved.getReportType()).isEqualTo("POPULATION");
        assertThat(saved.getYear()).isEqualTo(2024);
        assertThat(saved.getReportName()).isEqualTo("Population Report 2024");
    }

    @Test
    @DisplayName("Should find all reports for a tenant ordered by creation date")
    void shouldFindAllReportsByTenantOrderedByDate() {
        // Arrange
        SavedReportEntity report1 = createPatientReport(TENANT_1, PATIENT_ID_1, "Report 1");
        report1.setCreatedAt(LocalDateTime.now().minusDays(2));
        repository.save(report1);

        SavedReportEntity report2 = createPatientReport(TENANT_1, PATIENT_ID_2, "Report 2");
        report2.setCreatedAt(LocalDateTime.now().minusDays(1));
        repository.save(report2);

        SavedReportEntity report3 = createPatientReport(TENANT_1, PATIENT_ID_1, "Report 3");
        report3.setCreatedAt(LocalDateTime.now());
        repository.save(report3);

        // Create report for different tenant
        repository.save(createPatientReport(TENANT_2, PATIENT_ID_1, "Other Tenant Report"));

        // Act
        List<SavedReportEntity> reports = repository.findByTenantIdOrderByCreatedAtDesc(TENANT_1);

        // Assert
        assertThat(reports).hasSize(3);
        assertThat(reports.get(0).getReportName()).isEqualTo("Report 3"); // Newest first
        assertThat(reports.get(1).getReportName()).isEqualTo("Report 2");
        assertThat(reports.get(2).getReportName()).isEqualTo("Report 1");
    }

    @Test
    @DisplayName("Should find reports by tenant and report type")
    void shouldFindReportsByTenantAndType() {
        // Arrange
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_1, "Patient Report 1"));
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_2, "Patient Report 2"));
        repository.save(createPopulationReport(TENANT_1, 2024, "Population Report 1"));
        repository.save(createPopulationReport(TENANT_1, 2023, "Population Report 2"));

        // Act
        List<SavedReportEntity> patientReports = repository.findByTenantIdAndReportTypeOrderByCreatedAtDesc(TENANT_1, "PATIENT");
        List<SavedReportEntity> populationReports = repository.findByTenantIdAndReportTypeOrderByCreatedAtDesc(TENANT_1, "POPULATION");

        // Assert
        assertThat(patientReports).hasSize(2);
        assertThat(populationReports).hasSize(2);
        assertThat(patientReports).allMatch(r -> r.getReportType().equals("PATIENT"));
        assertThat(populationReports).allMatch(r -> r.getReportType().equals("POPULATION"));
    }

    @Test
    @DisplayName("Should find reports by tenant and creator")
    void shouldFindReportsByTenantAndCreator() {
        // Arrange
        SavedReportEntity report1 = createPatientReport(TENANT_1, PATIENT_ID_1, "Report by User 1");
        report1.setCreatedBy("user1");
        repository.save(report1);

        SavedReportEntity report2 = createPatientReport(TENANT_1, PATIENT_ID_2, "Report by User 1");
        report2.setCreatedBy("user1");
        repository.save(report2);

        SavedReportEntity report3 = createPatientReport(TENANT_1, PATIENT_ID_1, "Report by User 2");
        report3.setCreatedBy("user2");
        repository.save(report3);

        // Act
        List<SavedReportEntity> user1Reports = repository.findByTenantIdAndCreatedByOrderByCreatedAtDesc(TENANT_1, "user1");

        // Assert
        assertThat(user1Reports).hasSize(2);
        assertThat(user1Reports).allMatch(r -> r.getCreatedBy().equals("user1"));
    }

    @Test
    @DisplayName("Should find report by tenant and ID (tenant isolation)")
    void shouldFindReportByTenantAndId() {
        // Arrange
        SavedReportEntity report1 = repository.save(createPatientReport(TENANT_1, PATIENT_ID_1, "Tenant 1 Report"));
        SavedReportEntity report2 = repository.save(createPatientReport(TENANT_2, PATIENT_ID_1, "Tenant 2 Report"));

        // Act
        Optional<SavedReportEntity> found1 = repository.findByTenantIdAndId(TENANT_1, report1.getId());
        Optional<SavedReportEntity> notFound = repository.findByTenantIdAndId(TENANT_1, report2.getId());

        // Assert
        assertThat(found1).isPresent();
        assertThat(found1.get().getReportName()).isEqualTo("Tenant 1 Report");
        assertThat(notFound).isEmpty(); // Should not find report from different tenant
    }

    @Test
    @DisplayName("Should find reports by tenant and patient ID")
    void shouldFindReportsByTenantAndPatient() {
        // Arrange
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_1, "Patient 1 Report 1"));
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_1, "Patient 1 Report 2"));
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_2, "Patient 2 Report"));

        // Act
        List<SavedReportEntity> patient1Reports = repository.findByTenantIdAndPatientIdOrderByCreatedAtDesc(TENANT_1, PATIENT_ID_1);

        // Assert
        assertThat(patient1Reports).hasSize(2);
        assertThat(patient1Reports).allMatch(r -> r.getPatientId().equals(PATIENT_ID_1));
    }

    @Test
    @DisplayName("Should find reports by tenant and year")
    void shouldFindReportsByTenantAndYear() {
        // Arrange
        repository.save(createPopulationReport(TENANT_1, 2023, "2023 Report 1"));
        repository.save(createPopulationReport(TENANT_1, 2023, "2023 Report 2"));
        repository.save(createPopulationReport(TENANT_1, 2024, "2024 Report"));

        // Act
        List<SavedReportEntity> reports2023 = repository.findByTenantIdAndYearOrderByCreatedAtDesc(TENANT_1, 2023);

        // Assert
        assertThat(reports2023).hasSize(2);
        assertThat(reports2023).allMatch(r -> r.getYear().equals(2023));
    }

    @Test
    @DisplayName("Should find reports by tenant and status")
    void shouldFindReportsByTenantAndStatus() {
        // Arrange
        SavedReportEntity completed1 = createPatientReport(TENANT_1, PATIENT_ID_1, "Completed 1");
        completed1.setStatus("COMPLETED");
        repository.save(completed1);

        SavedReportEntity completed2 = createPatientReport(TENANT_1, PATIENT_ID_2, "Completed 2");
        completed2.setStatus("COMPLETED");
        repository.save(completed2);

        SavedReportEntity generating = createPatientReport(TENANT_1, PATIENT_ID_1, "Generating");
        generating.setStatus("GENERATING");
        repository.save(generating);

        // Act
        List<SavedReportEntity> completedReports = repository.findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_1, "COMPLETED");

        // Assert
        assertThat(completedReports).hasSize(2);
        assertThat(completedReports).allMatch(r -> r.getStatus().equals("COMPLETED"));
    }

    @Test
    @DisplayName("Should count reports by tenant")
    void shouldCountReportsByTenant() {
        // Arrange
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_1, "Report 1"));
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_2, "Report 2"));
        repository.save(createPopulationReport(TENANT_1, 2024, "Report 3"));
        repository.save(createPatientReport(TENANT_2, PATIENT_ID_1, "Other Tenant"));

        // Act
        long count = repository.countByTenantId(TENANT_1);

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should count reports by tenant and type")
    void shouldCountReportsByTenantAndType() {
        // Arrange
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_1, "Patient 1"));
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_2, "Patient 2"));
        repository.save(createPopulationReport(TENANT_1, 2024, "Population 1"));

        // Act
        long patientCount = repository.countByTenantIdAndReportType(TENANT_1, "PATIENT");
        long populationCount = repository.countByTenantIdAndReportType(TENANT_1, "POPULATION");

        // Assert
        assertThat(patientCount).isEqualTo(2);
        assertThat(populationCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle JSONB report data storage")
    void shouldHandleJsonbReportData() {
        // Arrange
        String jsonData = "{\"patientId\":\"123\",\"totalMeasures\":5,\"compliantMeasures\":4,\"qualityScore\":80.0}";
        SavedReportEntity report = createPatientReport(TENANT_1, PATIENT_ID_1, "JSON Test Report");
        report.setReportData(jsonData);

        // Act
        SavedReportEntity saved = repository.save(report);
        SavedReportEntity retrieved = repository.findById(saved.getId()).orElseThrow();

        // Assert
        assertThat(retrieved.getReportData()).isEqualTo(jsonData);
    }

    @Test
    @DisplayName("Should auto-generate ID on save")
    void shouldAutoGenerateId() {
        // Arrange
        SavedReportEntity report = createPatientReport(TENANT_1, PATIENT_ID_1, "Auto ID Test");
        report.setId(null); // Don't set ID

        // Act
        SavedReportEntity saved = repository.save(report);

        // Assert
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("Should auto-set timestamps on create")
    void shouldAutoSetTimestampsOnCreate() {
        // Arrange
        SavedReportEntity report = createPatientReport(TENANT_1, PATIENT_ID_1, "Timestamp Test");
        report.setCreatedAt(null);
        report.setGeneratedAt(null);

        // Act
        SavedReportEntity saved = repository.save(report);

        // Assert
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getGeneratedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should support optimistic locking with version field")
    void shouldSupportOptimisticLocking() {
        // Arrange
        SavedReportEntity report = repository.save(createPatientReport(TENANT_1, PATIENT_ID_1, "Version Test"));

        // Assert - Version field should be managed by JPA
        assertThat(report.getVersion()).isNotNull();
        assertThat(report.getVersion()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should isolate reports by tenant")
    void shouldIsolateReportsByTenant() {
        // Arrange
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_1, "Tenant 1 Report 1"));
        repository.save(createPatientReport(TENANT_1, PATIENT_ID_2, "Tenant 1 Report 2"));
        repository.save(createPatientReport(TENANT_2, PATIENT_ID_1, "Tenant 2 Report 1"));
        repository.save(createPatientReport(TENANT_2, PATIENT_ID_2, "Tenant 2 Report 2"));

        // Act
        List<SavedReportEntity> tenant1Reports = repository.findByTenantIdOrderByCreatedAtDesc(TENANT_1);
        List<SavedReportEntity> tenant2Reports = repository.findByTenantIdOrderByCreatedAtDesc(TENANT_2);

        // Assert
        assertThat(tenant1Reports).hasSize(2);
        assertThat(tenant2Reports).hasSize(2);
        assertThat(tenant1Reports).allMatch(r -> r.getTenantId().equals(TENANT_1));
        assertThat(tenant2Reports).allMatch(r -> r.getTenantId().equals(TENANT_2));
    }

    // Helper methods to create test reports
    private SavedReportEntity createPatientReport(String tenantId, UUID patientId, String reportName) {
        return SavedReportEntity.builder()
                .tenantId(tenantId)
                .reportType("PATIENT")
                .reportName(reportName)
                .description("Test patient report")
                .patientId(patientId)
                .reportData("{\"test\": \"data\"}")
                .createdBy("test-user")
                .status("COMPLETED")
                .build();
    }

    private SavedReportEntity createPopulationReport(String tenantId, Integer year, String reportName) {
        return SavedReportEntity.builder()
                .tenantId(tenantId)
                .reportType("POPULATION")
                .reportName(reportName)
                .description("Test population report")
                .year(year)
                .reportData("{\"test\": \"data\"}")
                .createdBy("test-user")
                .status("COMPLETED")
                .build();
    }
}
