package com.healthdata.quality.service;

import com.healthdata.quality.client.CareGapServiceClient;
import com.healthdata.quality.client.PatientServiceClient;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.persistence.SavedReportRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QualityReportService Tests")
class QualityReportServiceTest {

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

    @InjectMocks
    private QualityReportService reportService;

    @Test
    @DisplayName("Should build patient report with unknown category and care gap failure")
    void shouldBuildPatientReportWithUnknownCategoryAndCareGapFailure() {
        String tenantId = "tenant-1";
        UUID patientId = UUID.randomUUID();

        List<QualityMeasureResultEntity> results = List.of(
            QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId("HEDIS_CBP")
                .measureCategory("HEDIS")
                .measureYear(2024)
                .numeratorCompliant(true)
                .denominatorElligible(true)
                .calculationDate(LocalDate.now())
                .build(),
            QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId("CUSTOM_1")
                .measureCategory(null)
                .measureYear(2024)
                .numeratorCompliant(false)
                .denominatorElligible(true)
                .calculationDate(LocalDate.now())
                .build()
        );

        when(calculationService.getPatientMeasureResults(tenantId, patientId)).thenReturn(results);
        when(calculationService.getQualityScore(tenantId, patientId))
            .thenReturn(new MeasureCalculationService.QualityScore(2, 1, 50.0));
        when(careGapServiceClient.getCareGapSummary(tenantId, patientId))
            .thenThrow(new RuntimeException("service down"));

        QualityReportService.QualityReport report =
            reportService.getPatientQualityReport(tenantId, patientId);

        assertThat(report.totalMeasures()).isEqualTo(2);
        assertThat(report.compliantMeasures()).isEqualTo(1);
        assertThat(report.qualityScore()).isEqualTo(50.0);
        assertThat(report.careGapSummary()).isNull();
        assertThat(report.measuresByCategory()).containsEntry("HEDIS", 1L);
        assertThat(report.measuresByCategory()).containsEntry("Unknown", 1L);
    }

    @Test
    @DisplayName("Should build population report with no results")
    void shouldBuildPopulationReportWithNoResults() {
        when(resultRepository.findByMeasureYear("tenant-1", 2024)).thenReturn(List.of());

        QualityReportService.PopulationQualityReport report =
            reportService.getPopulationQualityReport("tenant-1", 2024);

        assertThat(report.totalMeasures()).isEqualTo(0);
        assertThat(report.compliantMeasures()).isEqualTo(0);
        assertThat(report.uniquePatients()).isEqualTo(0);
        assertThat(report.overallScore()).isEqualTo(0.0);
        assertThat(report.measuresByCategory()).isEmpty();
    }

    @Test
    @DisplayName("Should calculate population report metrics")
    void shouldCalculatePopulationReportMetrics() {
        UUID patientA = UUID.randomUUID();
        UUID patientB = UUID.randomUUID();
        int year = 2024;

        List<QualityMeasureResultEntity> results = List.of(
            QualityMeasureResultEntity.builder()
                .tenantId("tenant-1")
                .patientId(patientA)
                .measureId("M1")
                .measureCategory("HEDIS")
                .measureYear(year)
                .numeratorCompliant(true)
                .denominatorElligible(true)
                .calculationDate(LocalDate.now())
                .build(),
            QualityMeasureResultEntity.builder()
                .tenantId("tenant-1")
                .patientId(patientB)
                .measureId("M2")
                .measureCategory("CMS")
                .measureYear(year)
                .numeratorCompliant(false)
                .denominatorElligible(true)
                .calculationDate(LocalDate.now())
                .build(),
            QualityMeasureResultEntity.builder()
                .tenantId("tenant-1")
                .patientId(patientA)
                .measureId("M3")
                .measureCategory("HEDIS")
                .measureYear(year)
                .numeratorCompliant(true)
                .denominatorElligible(true)
                .calculationDate(LocalDate.now())
                .build()
        );

        when(resultRepository.findByMeasureYear("tenant-1", year)).thenReturn(results);

        QualityReportService.PopulationQualityReport report =
            reportService.getPopulationQualityReport("tenant-1", year);

        assertThat(report.totalMeasures()).isEqualTo(3);
        assertThat(report.compliantMeasures()).isEqualTo(2);
        assertThat(report.uniquePatients()).isEqualTo(2);
        assertThat(report.overallScore()).isCloseTo(66.666, within(0.01));
        assertThat(report.measuresByCategory()).containsEntry("HEDIS", 2L);
        assertThat(report.measuresByCategory()).containsEntry("CMS", 1L);
    }

    @Test
    @DisplayName("Should throw when patient report serialization fails")
    void shouldThrowWhenPatientReportSerializationFails() throws Exception {
        String tenantId = "tenant-1";
        UUID patientId = UUID.randomUUID();

        when(calculationService.getPatientMeasureResults(tenantId, patientId))
            .thenReturn(List.of(
                QualityMeasureResultEntity.builder()
                    .tenantId(tenantId)
                    .patientId(patientId)
                    .measureId("HEDIS_CBP")
                    .measureCategory("HEDIS")
                    .measureYear(2024)
                    .numeratorCompliant(true)
                    .denominatorElligible(true)
                    .calculationDate(LocalDate.now())
                    .build()
            ));
        when(calculationService.getQualityScore(tenantId, patientId))
            .thenReturn(new MeasureCalculationService.QualityScore(1, 1, 100.0));

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = Mockito.mock(
            com.fasterxml.jackson.databind.ObjectMapper.class);
        when(objectMapper.writeValueAsString(Mockito.any()))
            .thenThrow(new RuntimeException("json fail"));
        ReflectionTestUtils.setField(reportService, "objectMapper", objectMapper);

        assertThatThrownBy(() -> reportService.savePatientReport(
            tenantId, patientId, "Patient Report", "tester"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to serialize report");
    }

    @Test
    @DisplayName("Should throw when population report serialization fails")
    void shouldThrowWhenPopulationReportSerializationFails() throws Exception {
        String tenantId = "tenant-1";
        int year = 2024;

        when(resultRepository.findByMeasureYear(tenantId, year))
            .thenReturn(List.of(
                QualityMeasureResultEntity.builder()
                    .tenantId(tenantId)
                    .patientId(UUID.randomUUID())
                    .measureId("M1")
                    .measureCategory("HEDIS")
                    .measureYear(year)
                    .numeratorCompliant(true)
                    .denominatorElligible(true)
                    .calculationDate(LocalDate.now())
                    .build()
            ));

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = Mockito.mock(
            com.fasterxml.jackson.databind.ObjectMapper.class);
        when(objectMapper.writeValueAsString(Mockito.any()))
            .thenThrow(new RuntimeException("json fail"));
        ReflectionTestUtils.setField(reportService, "objectMapper", objectMapper);

        assertThatThrownBy(() -> reportService.savePopulationReport(
            tenantId, year, "Population Report", "tester"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to serialize report");
    }
}
