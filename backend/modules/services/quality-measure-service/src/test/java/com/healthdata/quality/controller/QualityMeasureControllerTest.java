package com.healthdata.quality.controller;

import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.SavedReportEntity;
import com.healthdata.quality.service.MeasureCalculationService;
import com.healthdata.quality.service.PopulationCalculationService;
import com.healthdata.quality.service.QualityReportService;
import com.healthdata.quality.service.ReportExportService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class QualityMeasureControllerTest {

    @Test
    void shouldCalculateMeasure() {
        MeasureCalculationService calculationService = mock(MeasureCalculationService.class);
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityReportService reportService = mock(QualityReportService.class);
        ReportExportService exportService = mock(ReportExportService.class);

        QualityMeasureController controller = new QualityMeasureController(
            calculationService,
            populationService,
            reportService,
            exportService
        );

        UUID patientId = UUID.randomUUID();
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .patientId(patientId)
            .measureId("measure-1")
            .createdAt(java.time.LocalDateTime.now())
            .build();

        when(calculationService.calculateMeasure("tenant-1", patientId, "measure-1", "user-1"))
            .thenReturn(entity);

        var response = controller.calculateMeasure("tenant-1", patientId, "measure-1", "user-1");

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMeasureId()).isEqualTo("measure-1");
    }

    @Test
    void shouldReturnPatientResultsWhenPatientIdProvided() {
        MeasureCalculationService calculationService = mock(MeasureCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            calculationService,
            mock(PopulationCalculationService.class),
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        UUID patientId = UUID.randomUUID();
        when(calculationService.getPatientMeasureResults("tenant-1", patientId))
            .thenReturn(List.of(QualityMeasureResultEntity.builder().measureId("measure-1").build()));

        var response = controller.getPatientResults("tenant-1", patientId, 0, 20);

        assertThat(response.getBody()).hasSize(1);
        verify(calculationService).getPatientMeasureResults("tenant-1", patientId);
    }

    @Test
    void shouldReturnAllResultsWhenPatientIdMissing() {
        MeasureCalculationService calculationService = mock(MeasureCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            calculationService,
            mock(PopulationCalculationService.class),
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        when(calculationService.getAllMeasureResults("tenant-1", 1, 10))
            .thenReturn(List.of(QualityMeasureResultEntity.builder().measureId("measure-2").build()));

        var response = controller.getPatientResults("tenant-1", null, 1, 10);

        assertThat(response.getBody()).hasSize(1);
        verify(calculationService).getAllMeasureResults("tenant-1", 1, 10);
    }

    @Test
    void shouldRejectInvalidYearForPopulationReport() {
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        assertThatThrownBy(() -> controller.getPopulationQualityReport("tenant-1", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldStartPopulationCalculation() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        Map<String, Object> mockResponse = Map.of(
            "jobId", "job-1",
            "status", "STARTING",
            "tenantId", "tenant-1"
        );
        when(populationService.startPopulationCalculation(
            eq("tenant-1"), anyString(), eq("user-1"), any(), any()))
            .thenReturn(mockResponse);

        var response = controller.startPopulationCalculation("tenant-1", "http://fhir", "user-1", null);

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).containsEntry("jobId", "job-1");
    }

    @Test
    void shouldHandlePopulationCalculationFailure() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        when(populationService.startPopulationCalculation(
            eq("tenant-1"), anyString(), eq("system"), any(), any()))
            .thenThrow(new RuntimeException("boom"));

        var response = controller.startPopulationCalculation("tenant-1", "http://fhir", "system", null);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Failed to start population calculation");
    }

    @Test
    void shouldHandleMissingJobStatus() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        when(populationService.getJobStatus("job-1")).thenReturn(null);

        var response = controller.getJobStatus("tenant-1", "job-1");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("Job not found");
    }

    @Test
    void shouldBlockJobStatusFromOtherTenant() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-2", "user");
        when(populationService.getJobStatus("job-1")).thenReturn(job);

        var response = controller.getJobStatus("tenant-1", "job-1");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void shouldReturnJobStatusForTenant() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-1", "user");
        job.setTotalPatients(10);
        job.setTotalMeasures(5);
        job.setTotalCalculations(50);
        job.setCompletedCalculations(25);
        when(populationService.getJobStatus("job-1")).thenReturn(job);

        var response = controller.getJobStatus("tenant-1", "job-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(((Map<?, ?>) response.getBody()).get("jobId")).isEqualTo("job-1");
    }

    @Test
    void shouldReturnAllJobsForTenant() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-1", "user");
        when(populationService.getActiveJobs("tenant-1")).thenReturn(List.of(job));

        var response = controller.getAllJobs("tenant-1");

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0)).containsEntry("jobId", "job-1");
    }

    @Test
    void shouldReturnQualityScore() {
        MeasureCalculationService calculationService = mock(MeasureCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            calculationService,
            mock(PopulationCalculationService.class),
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        UUID patientId = UUID.randomUUID();
        MeasureCalculationService.QualityScore score =
            new MeasureCalculationService.QualityScore(10, 8, 80.0);
        when(calculationService.getQualityScore("tenant-1", patientId)).thenReturn(score);

        var response = controller.getQualityScore("tenant-1", patientId);

        assertThat(response.getBody()).isEqualTo(score);
    }

    @Test
    void shouldReturnPatientQualityReport() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        UUID patientId = UUID.randomUUID();
        QualityReportService.QualityReport report =
            new QualityReportService.QualityReport(patientId, 3, 2, 66.7, List.of(), Map.of(), null);
        when(reportService.getPatientQualityReport("tenant-1", patientId)).thenReturn(report);

        var response = controller.getPatientQualityReport("tenant-1", patientId);

        assertThat(response.getBody()).isEqualTo(report);
    }

    @Test
    void shouldReturnPopulationQualityReportWithDefaultYear() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        int currentYear = LocalDate.now().getYear();
        QualityReportService.PopulationQualityReport report =
            new QualityReportService.PopulationQualityReport(currentYear, 5, 20, 15, 75.0, Map.of());
        when(reportService.getPopulationQualityReport("tenant-1", currentYear)).thenReturn(report);

        var response = controller.getPopulationQualityReport("tenant-1", null);

        assertThat(response.getBody()).isEqualTo(report);
    }

    @Test
    void shouldReturnHealthCheck() {
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        var response = controller.healthCheck();

        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsEntry("service", "quality-measure-service");
    }

    @Test
    void shouldCancelJobWhenNotFound() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        when(populationService.getJobStatus("job-1")).thenReturn(null);

        var response = controller.cancelJob("tenant-1", "job-1");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldRejectCancelWhenTenantMismatch() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-2", "user");
        when(populationService.getJobStatus("job-1")).thenReturn(job);

        var response = controller.cancelJob("tenant-1", "job-1");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void shouldCancelJobSuccessfully() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-1", "user");
        when(populationService.getJobStatus("job-1")).thenReturn(job);
        when(populationService.cancelJob("job-1")).thenReturn(true);

        var response = controller.cancelJob("tenant-1", "job-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "CANCELLED");
    }

    @Test
    void shouldRejectCancelWhenJobNotCancelable() {
        PopulationCalculationService populationService = mock(PopulationCalculationService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            populationService,
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        PopulationCalculationService.BatchCalculationJob job =
            new PopulationCalculationService.BatchCalculationJob("job-1", "tenant-1", "user");
        job.updateStatus(PopulationCalculationService.JobStatus.COMPLETED);
        when(populationService.getJobStatus("job-1")).thenReturn(job);
        when(populationService.cancelJob("job-1")).thenReturn(false);

        var response = controller.cancelJob("tenant-1", "job-1");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("currentStatus", "COMPLETED");
    }

    @Test
    void shouldSaveReports() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        UUID patientId = UUID.randomUUID();
        SavedReportEntity patientReport = SavedReportEntity.builder()
            .id(UUID.randomUUID())
            .reportName("patient-report")
            .build();
        when(reportService.savePatientReport("tenant-1", patientId, "patient-report", "user-1"))
            .thenReturn(patientReport);

        var patientResponse = controller.savePatientReport("tenant-1", patientId, "patient-report", "user-1");
        assertThat(patientResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(patientResponse.getBody()).isEqualTo(patientReport);

        SavedReportEntity populationReport = SavedReportEntity.builder()
            .id(UUID.randomUUID())
            .reportName("population-report")
            .build();
        when(reportService.savePopulationReport("tenant-1", 2024, "population-report", "user-2"))
            .thenReturn(populationReport);

        var populationResponse = controller.savePopulationReport("tenant-1", 2024, "population-report", "user-2");
        assertThat(populationResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(populationResponse.getBody()).isEqualTo(populationReport);
    }

    @Test
    void shouldReturnSavedReports() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        when(reportService.getSavedReports("tenant-1"))
            .thenReturn(List.of(SavedReportEntity.builder().reportName("r1").build()));

        var response = controller.getSavedReports("tenant-1", null);

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void shouldReturnSavedReportsByType() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        when(reportService.getSavedReportsByType("tenant-1", "PATIENT"))
            .thenReturn(List.of(SavedReportEntity.builder().reportName("patient").build()));

        var response = controller.getSavedReports("tenant-1", "PATIENT");

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void shouldReturnSavedReportById() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        UUID reportId = UUID.randomUUID();
        SavedReportEntity report = SavedReportEntity.builder().id(reportId).reportName("report").build();
        when(reportService.getSavedReport("tenant-1", reportId)).thenReturn(report);

        var response = controller.getSavedReport("tenant-1", reportId.toString());

        assertThat(response.getBody()).isEqualTo(report);
    }

    @Test
    void shouldReturnNotFoundWhenSavedReportMissing() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        UUID reportId = UUID.randomUUID();
        when(reportService.getSavedReport("tenant-1", reportId))
            .thenThrow(new RuntimeException("not found"));

        var response = controller.getSavedReport("tenant-1", reportId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldThrowForInvalidSavedReportId() {
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            mock(QualityReportService.class),
            mock(ReportExportService.class)
        );

        assertThatThrownBy(() -> controller.getSavedReport("tenant-1", "bad-id"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDeleteSavedReport() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        UUID reportId = UUID.randomUUID();

        var response = controller.deleteSavedReport("tenant-1", reportId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(reportService).deleteSavedReport("tenant-1", reportId);
    }

    @Test
    void shouldReturnNotFoundWhenDeleteReportMissing() {
        QualityReportService reportService = mock(QualityReportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            mock(ReportExportService.class)
        );

        UUID reportId = UUID.randomUUID();
        doThrow(new RuntimeException("not found")).when(reportService)
            .deleteSavedReport("tenant-1", reportId);

        var response = controller.deleteSavedReport("tenant-1", reportId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldExportReportToCsvAndExcel() throws Exception {
        QualityReportService reportService = mock(QualityReportService.class);
        ReportExportService exportService = mock(ReportExportService.class);
        QualityMeasureController controller = new QualityMeasureController(
            mock(MeasureCalculationService.class),
            mock(PopulationCalculationService.class),
            reportService,
            exportService
        );

        UUID reportId = UUID.randomUUID();
        SavedReportEntity report = SavedReportEntity.builder()
            .id(reportId)
            .reportName("Clinical Report 2024")
            .build();
        when(reportService.getSavedReport("tenant-1", reportId)).thenReturn(report);
        when(exportService.exportToCsv(report)).thenReturn("csv".getBytes());
        when(exportService.exportToExcel(report)).thenReturn("excel".getBytes());

        var csvResponse = controller.exportReportToCsv("tenant-1", reportId.toString());
        assertThat(csvResponse.getBody()).isNotEmpty();
        assertThat(csvResponse.getHeaders().getFirst("Content-Disposition"))
            .contains("Clinical_Report_2024.csv");

        var excelResponse = controller.exportReportToExcel("tenant-1", reportId.toString());
        assertThat(excelResponse.getBody()).isNotEmpty();
        assertThat(excelResponse.getHeaders().getFirst("Content-Disposition"))
            .contains("Clinical_Report_2024.xlsx");
    }
}
