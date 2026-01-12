package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.client.CareGapServiceClient;
import com.healthdata.quality.client.PatientServiceClient;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.persistence.SavedReportEntity;
import com.healthdata.quality.persistence.SavedReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for QualityReportService - Save and Retrieve functionality
 * Written BEFORE implementation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QualityReportService - Save/Retrieve Tests")
class QualityReportServiceSaveTest {

    @Mock
    private QualityMeasureResultRepository resultRepository;

    @Mock
    private SavedReportRepository savedReportRepository;

    @Mock
    private PatientServiceClient patientServiceClient;

    @Mock
    private CareGapServiceClient careGapServiceClient;

    @Mock
    private MeasureCalculationService calculationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private QualityReportService reportService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String CREATED_BY = "test-user";

    @BeforeEach
    void setUp() throws Exception {
        // reportService will be created with mocked dependencies
        // Note: ObjectMapper mocking moved to individual tests that need it
    }

    @Test
    @DisplayName("Should save patient report with all required fields")
    void shouldSavePatientReportWithAllFields() throws Exception {
        // Arrange
        String reportName = "Patient Quality Report - John Doe";
        List<QualityMeasureResultEntity> results = createMockResults();
        MeasureCalculationService.QualityScore score = new MeasureCalculationService.QualityScore(5, 4, 80.0);

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"patientId\":\"test\",\"totalMeasures\":5}");
        when(calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID)).thenReturn(results);
        when(calculationService.getQualityScore(TENANT_ID, PATIENT_ID)).thenReturn(score);
        when(careGapServiceClient.getCareGapSummary(TENANT_ID, PATIENT_ID)).thenReturn("{\"gaps\": 1}");
        when(savedReportRepository.save(any(SavedReportEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SavedReportEntity savedReport = reportService.savePatientReport(TENANT_ID, PATIENT_ID, reportName, CREATED_BY);

        // Assert
        assertThat(savedReport).isNotNull();
        assertThat(savedReport.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(savedReport.getReportType()).isEqualTo("PATIENT");
        assertThat(savedReport.getReportName()).isEqualTo(reportName);
        assertThat(savedReport.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(savedReport.getCreatedBy()).isEqualTo(CREATED_BY);
        assertThat(savedReport.getStatus()).isEqualTo("COMPLETED");
        assertThat(savedReport.getReportData()).isNotNull();

        verify(savedReportRepository).save(any(SavedReportEntity.class));
    }

    @Test
    @DisplayName("Should save population report for a specific year")
    void shouldSavePopulationReportForYear() throws Exception {
        // Arrange
        String reportName = "2024 Population Quality Report";
        int year = 2024;
        List<QualityMeasureResultEntity> results = createMockResults();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"patientId\":\"test\",\"totalMeasures\":5}");
        when(resultRepository.findByMeasureYear(TENANT_ID, year)).thenReturn(results);
        when(savedReportRepository.save(any(SavedReportEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SavedReportEntity savedReport = reportService.savePopulationReport(TENANT_ID, year, reportName, CREATED_BY);

        // Assert
        assertThat(savedReport).isNotNull();
        assertThat(savedReport.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(savedReport.getReportType()).isEqualTo("POPULATION");
        assertThat(savedReport.getReportName()).isEqualTo(reportName);
        assertThat(savedReport.getYear()).isEqualTo(year);
        assertThat(savedReport.getCreatedBy()).isEqualTo(CREATED_BY);
        assertThat(savedReport.getStatus()).isEqualTo("COMPLETED");

        verify(savedReportRepository).save(any(SavedReportEntity.class));
    }

    @Test
    @DisplayName("Should retrieve all saved reports for a tenant")
    void shouldRetrieveAllSavedReportsForTenant() {
        // Arrange
        List<SavedReportEntity> mockReports = Arrays.asList(
                createMockSavedReport("Report 1"),
                createMockSavedReport("Report 2"),
                createMockSavedReport("Report 3")
        );
        when(savedReportRepository.findByTenantIdOrderByCreatedAtDesc(TENANT_ID)).thenReturn(mockReports);

        // Act
        List<SavedReportEntity> reports = reportService.getSavedReports(TENANT_ID);

        // Assert
        assertThat(reports).hasSize(3);
        assertThat(reports.get(0).getReportName()).isEqualTo("Report 1");
        verify(savedReportRepository).findByTenantIdOrderByCreatedAtDesc(TENANT_ID);
    }

    @Test
    @DisplayName("Should retrieve saved report by ID with tenant isolation")
    void shouldRetrieveSavedReportByIdWithTenantIsolation() {
        // Arrange
        UUID reportId = UUID.randomUUID();
        SavedReportEntity mockReport = createMockSavedReport("Test Report");
        mockReport.setId(reportId);

        when(savedReportRepository.findByTenantIdAndId(TENANT_ID, reportId)).thenReturn(Optional.of(mockReport));

        // Act
        SavedReportEntity report = reportService.getSavedReport(TENANT_ID, reportId);

        // Assert
        assertThat(report).isNotNull();
        assertThat(report.getId()).isEqualTo(reportId);
        assertThat(report.getTenantId()).isEqualTo(TENANT_ID);
        verify(savedReportRepository).findByTenantIdAndId(TENANT_ID, reportId);
    }

    @Test
    @DisplayName("Should throw exception when saved report not found")
    void shouldThrowExceptionWhenReportNotFound() {
        // Arrange
        UUID reportId = UUID.randomUUID();
        when(savedReportRepository.findByTenantIdAndId(TENANT_ID, reportId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reportService.getSavedReport(TENANT_ID, reportId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Report not found");

        verify(savedReportRepository).findByTenantIdAndId(TENANT_ID, reportId);
    }

    @Test
    @DisplayName("Should filter saved reports by report type")
    void shouldFilterSavedReportsByType() {
        // Arrange
        List<SavedReportEntity> patientReports = Arrays.asList(
                createMockSavedReport("Patient Report 1"),
                createMockSavedReport("Patient Report 2")
        );
        when(savedReportRepository.findByTenantIdAndReportTypeOrderByCreatedAtDesc(TENANT_ID, "PATIENT"))
                .thenReturn(patientReports);

        // Act
        List<SavedReportEntity> reports = reportService.getSavedReportsByType(TENANT_ID, "PATIENT");

        // Assert
        assertThat(reports).hasSize(2);
        verify(savedReportRepository).findByTenantIdAndReportTypeOrderByCreatedAtDesc(TENANT_ID, "PATIENT");
    }

    @Test
    @DisplayName("Should delete saved report with tenant isolation")
    void shouldDeleteSavedReportWithTenantIsolation() {
        // Arrange
        UUID reportId = UUID.randomUUID();
        SavedReportEntity mockReport = createMockSavedReport("Report to Delete");
        mockReport.setId(reportId);

        when(savedReportRepository.findByTenantIdAndId(TENANT_ID, reportId)).thenReturn(Optional.of(mockReport));
        doNothing().when(savedReportRepository).delete(mockReport);

        // Act
        reportService.deleteSavedReport(TENANT_ID, reportId);

        // Assert
        verify(savedReportRepository).findByTenantIdAndId(TENANT_ID, reportId);
        verify(savedReportRepository).delete(mockReport);
    }

    @Test
    @DisplayName("Should handle care gap service failure gracefully when saving patient report")
    void shouldHandleCareGapServiceFailureGracefully() throws Exception {
        // Arrange
        String reportName = "Patient Report with Care Gap Failure";
        List<QualityMeasureResultEntity> results = createMockResults();
        MeasureCalculationService.QualityScore score = new MeasureCalculationService.QualityScore(5, 4, 80.0);

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"patientId\":\"test\",\"totalMeasures\":5}");
        when(calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID)).thenReturn(results);
        when(calculationService.getQualityScore(TENANT_ID, PATIENT_ID)).thenReturn(score);
        when(careGapServiceClient.getCareGapSummary(TENANT_ID, PATIENT_ID))
                .thenThrow(new RuntimeException("Care Gap Service unavailable"));
        when(savedReportRepository.save(any(SavedReportEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SavedReportEntity savedReport = reportService.savePatientReport(TENANT_ID, PATIENT_ID, reportName, CREATED_BY);

        // Assert - Report should still be saved even if care gap service fails
        assertThat(savedReport).isNotNull();
        assertThat(savedReport.getStatus()).isEqualTo("COMPLETED");
        verify(savedReportRepository).save(any(SavedReportEntity.class));
    }

    @Test
    @DisplayName("Should serialize report data as valid JSON")
    void shouldSerializeReportDataAsValidJson() throws Exception {
        // Arrange
        String reportName = "JSON Test Report";
        List<QualityMeasureResultEntity> results = createMockResults();
        MeasureCalculationService.QualityScore score = new MeasureCalculationService.QualityScore(5, 4, 80.0);

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"patientId\":\"test\",\"totalMeasures\":5}");
        when(calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID)).thenReturn(results);
        when(calculationService.getQualityScore(TENANT_ID, PATIENT_ID)).thenReturn(score);

        ArgumentCaptor<SavedReportEntity> captor = ArgumentCaptor.forClass(SavedReportEntity.class);
        when(savedReportRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        reportService.savePatientReport(TENANT_ID, PATIENT_ID, reportName, CREATED_BY);

        // Assert
        SavedReportEntity captured = captor.getValue();
        assertThat(captured.getReportData()).isNotNull();

        // Verify it's valid JSON (use real ObjectMapper for parsing)
        new ObjectMapper().readTree(captured.getReportData());
    }

    @Test
    @DisplayName("Should count saved reports by tenant")
    void shouldCountSavedReportsByTenant() {
        // Arrange
        when(savedReportRepository.countByTenantId(TENANT_ID)).thenReturn(42L);

        // Act
        long count = reportService.countSavedReports(TENANT_ID);

        // Assert
        assertThat(count).isEqualTo(42);
        verify(savedReportRepository).countByTenantId(TENANT_ID);
    }

    // Helper methods
    private List<QualityMeasureResultEntity> createMockResults() {
        return Arrays.asList(
                QualityMeasureResultEntity.builder()
                        .tenantId(TENANT_ID)
                        .patientId(PATIENT_ID)
                        .measureId("HEDIS_CDC_A1C9")
                        .measureName("Diabetes Care A1C")
                        .measureCategory("HEDIS")
                        .measureYear(2024)
                        .numeratorCompliant(true)
                        .denominatorElligible(true)
                        .calculationDate(LocalDate.now())
                        .createdBy(CREATED_BY)
                        .build(),
                QualityMeasureResultEntity.builder()
                        .tenantId(TENANT_ID)
                        .patientId(PATIENT_ID)
                        .measureId("HEDIS_CBP")
                        .measureName("Blood Pressure Control")
                        .measureCategory("HEDIS")
                        .measureYear(2024)
                        .numeratorCompliant(false)
                        .denominatorElligible(true)
                        .calculationDate(LocalDate.now())
                        .createdBy(CREATED_BY)
                        .build()
        );
    }

    private SavedReportEntity createMockSavedReport(String reportName) {
        return SavedReportEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .reportType("PATIENT")
                .reportName(reportName)
                .patientId(PATIENT_ID)
                .reportData("{\"test\": \"data\"}")
                .createdBy(CREATED_BY)
                .status("COMPLETED")
                .build();
    }
}
