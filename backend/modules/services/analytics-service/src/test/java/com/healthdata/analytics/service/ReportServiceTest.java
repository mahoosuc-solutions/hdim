package com.healthdata.analytics.service;

import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.dto.ReportDto;
import com.healthdata.analytics.dto.ReportExecutionDto;
import com.healthdata.analytics.persistence.ReportEntity;
import com.healthdata.analytics.persistence.ReportExecutionEntity;
import com.healthdata.analytics.repository.ReportExecutionRepository;
import com.healthdata.analytics.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportService.
 * Tests report CRUD operations and execution.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Report Service Tests")
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportExecutionRepository executionRepository;

    @Mock
    private KpiService kpiService;

    private ReportService service;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final UUID REPORT_ID = UUID.randomUUID();
    private static final UUID EXECUTION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ReportService(reportRepository, executionRepository, kpiService);
    }

    @Nested
    @DisplayName("Get Reports Tests")
    class GetReportsTests {

        @Test
        @DisplayName("Should return all reports for tenant")
        void shouldReturnAllReports() {
            // Given
            List<ReportEntity> entities = List.of(
                    createReport("Quality Report", "QUALITY"),
                    createReport("HCC Report", "HCC")
            );
            when(reportRepository.findByTenantId(TENANT_ID)).thenReturn(entities);

            // When
            List<ReportDto> result = service.getReports(TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Quality Report");
            assertThat(result.get(1).getName()).isEqualTo("HCC Report");
        }

        @Test
        @DisplayName("Should return empty list when no reports exist")
        void shouldReturnEmptyList() {
            // Given
            when(reportRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

            // When
            List<ReportDto> result = service.getReports(TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return paginated reports")
        void shouldReturnPaginatedReports() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<ReportEntity> entities = List.of(createReport("Report 1", "QUALITY"));
            Page<ReportEntity> page = new PageImpl<>(entities, pageable, 1);
            when(reportRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

            // When
            Page<ReportDto> result = service.getReportsPaginated(TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get Single Report Tests")
    class GetSingleReportTests {

        @Test
        @DisplayName("Should return report by ID with latest execution")
        void shouldReturnReportById() {
            // Given
            ReportEntity entity = createReport("My Report", "QUALITY");
            ReportExecutionEntity execution = createExecution("COMPLETED");

            when(reportRepository.findByIdAndTenantId(REPORT_ID, TENANT_ID))
                    .thenReturn(Optional.of(entity));
            when(executionRepository.findLatestSuccessfulExecution(REPORT_ID))
                    .thenReturn(Optional.of(execution));

            // When
            Optional<ReportDto> result = service.getReport(REPORT_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("My Report");
            assertThat(result.get().getLatestExecution()).isNotNull();
        }

        @Test
        @DisplayName("Should return empty when report not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(reportRepository.findByIdAndTenantId(REPORT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ReportDto> result = service.getReport(REPORT_ID, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Create Report Tests")
    class CreateReportTests {

        @Test
        @DisplayName("Should create report successfully")
        void shouldCreateReport() {
            // Given
            ReportDto dto = ReportDto.builder()
                    .name("New Report")
                    .description("Description")
                    .reportType("QUALITY")
                    .scheduleCron("0 0 * * *")
                    .scheduleEnabled(true)
                    .outputFormat("PDF")
                    .build();

            ReportEntity savedEntity = createReport("New Report", "QUALITY");
            when(reportRepository.save(any(ReportEntity.class))).thenReturn(savedEntity);

            // When
            ReportDto result = service.createReport(dto, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getName()).isEqualTo("New Report");
            verify(reportRepository).save(any(ReportEntity.class));
        }

        @Test
        @DisplayName("Should set default values when not provided")
        void shouldSetDefaults() {
            // Given
            ReportDto dto = ReportDto.builder()
                    .name("Simple Report")
                    .reportType("CARE_GAP")
                    .build();

            ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
            when(reportRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createReport(dto, TENANT_ID, USER_ID);

            // Then
            ReportEntity saved = captor.getValue();
            assertThat(saved.getScheduleEnabled()).isFalse();
            assertThat(saved.getOutputFormat()).isEqualTo("PDF");
        }

        @Test
        @DisplayName("Should set createdBy to current user")
        void shouldSetCreatedBy() {
            // Given
            ReportDto dto = ReportDto.builder()
                    .name("User Report")
                    .reportType("HCC")
                    .build();

            ArgumentCaptor<ReportEntity> captor = ArgumentCaptor.forClass(ReportEntity.class);
            when(reportRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createReport(dto, TENANT_ID, USER_ID);

            // Then
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("Update Report Tests")
    class UpdateReportTests {

        @Test
        @DisplayName("Should update report successfully")
        void shouldUpdateReport() {
            // Given
            ReportEntity existing = createReport("Old Name", "QUALITY");
            ReportDto dto = ReportDto.builder()
                    .name("Updated Name")
                    .description("New Description")
                    .reportType("HCC")
                    .scheduleEnabled(true)
                    .build();

            when(reportRepository.findByIdAndTenantId(REPORT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(reportRepository.save(any(ReportEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<ReportDto> result = service.updateReport(REPORT_ID, dto, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Should return empty when report not found")
        void shouldReturnEmptyWhenUpdatingNonExistent() {
            // Given
            when(reportRepository.findByIdAndTenantId(REPORT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ReportDto> result = service.updateReport(REPORT_ID, new ReportDto(), TENANT_ID);

            // Then
            assertThat(result).isEmpty();
            verify(reportRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Report Tests")
    class DeleteReportTests {

        @Test
        @DisplayName("Should delete report successfully")
        void shouldDeleteReport() {
            // Given
            when(reportRepository.existsByIdAndTenantId(REPORT_ID, TENANT_ID)).thenReturn(true);

            // When
            boolean result = service.deleteReport(REPORT_ID, TENANT_ID);

            // Then
            assertThat(result).isTrue();
            verify(reportRepository).deleteById(REPORT_ID);
        }

        @Test
        @DisplayName("Should return false when report not found")
        void shouldReturnFalseWhenNotFound() {
            // Given
            when(reportRepository.existsByIdAndTenantId(REPORT_ID, TENANT_ID)).thenReturn(false);

            // When
            boolean result = service.deleteReport(REPORT_ID, TENANT_ID);

            // Then
            assertThat(result).isFalse();
            verify(reportRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Execute Report Tests")
    class ExecuteReportTests {

        @Test
        @DisplayName("Should execute report successfully")
        void shouldExecuteReport() {
            // Given
            ReportEntity report = createReport("My Report", "QUALITY");
            Map<String, Object> params = Map.of("startDate", "2024-01-01");

            when(reportRepository.findByIdAndTenantId(REPORT_ID, TENANT_ID))
                    .thenReturn(Optional.of(report));
            when(executionRepository.save(any(ReportExecutionEntity.class)))
                    .thenAnswer(inv -> {
                        ReportExecutionEntity entity = inv.getArgument(0);
                        entity.setId(EXECUTION_ID);
                        return entity;
                    });

            // When
            ReportExecutionDto result = service.executeReport(REPORT_ID, TENANT_ID, USER_ID, params);

            // Then
            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getTriggeredBy()).isEqualTo(USER_ID);
            verify(executionRepository).save(any(ReportExecutionEntity.class));
        }

        @Test
        @DisplayName("Should merge report parameters with execution parameters")
        void shouldMergeParameters() {
            // Given
            Map<String, Object> reportParams = Map.of("format", "PDF");
            ReportEntity report = createReportWithParams("Report", "QUALITY", reportParams);
            Map<String, Object> execParams = Map.of("startDate", "2024-01-01");

            when(reportRepository.findByIdAndTenantId(REPORT_ID, TENANT_ID))
                    .thenReturn(Optional.of(report));

            ArgumentCaptor<ReportExecutionEntity> captor = ArgumentCaptor.forClass(ReportExecutionEntity.class);
            when(executionRepository.save(captor.capture()))
                    .thenAnswer(inv -> {
                        ReportExecutionEntity entity = inv.getArgument(0);
                        entity.setId(EXECUTION_ID);
                        return entity;
                    });

            // When
            service.executeReport(REPORT_ID, TENANT_ID, USER_ID, execParams);

            // Then
            Map<String, Object> savedParams = captor.getValue().getParameters();
            assertThat(savedParams).containsEntry("format", "PDF");
            assertThat(savedParams).containsEntry("startDate", "2024-01-01");
        }

        @Test
        @DisplayName("Should throw exception when report not found")
        void shouldThrowExceptionWhenReportNotFound() {
            // Given
            when(reportRepository.findByIdAndTenantId(REPORT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.executeReport(REPORT_ID, TENANT_ID, USER_ID, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Report not found");
        }
    }

    @Nested
    @DisplayName("Get Executions Tests")
    class GetExecutionsTests {

        @Test
        @DisplayName("Should return all executions for report")
        void shouldReturnAllExecutions() {
            // Given
            List<ReportExecutionEntity> entities = List.of(
                    createExecution("COMPLETED"),
                    createExecution("FAILED")
            );
            when(executionRepository.findByReportIdAndTenantId(REPORT_ID, TENANT_ID))
                    .thenReturn(entities);

            // When
            List<ReportExecutionDto> result = service.getExecutions(REPORT_ID, TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return paginated executions")
        void shouldReturnPaginatedExecutions() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<ReportExecutionEntity> entities = List.of(createExecution("COMPLETED"));
            Page<ReportExecutionEntity> page = new PageImpl<>(entities, pageable, 1);
            when(executionRepository.findByReportIdAndTenantId(REPORT_ID, TENANT_ID, pageable))
                    .thenReturn(page);

            // When
            Page<ReportExecutionDto> result = service.getExecutionsPaginated(REPORT_ID, TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should return single execution by ID")
        void shouldReturnExecutionById() {
            // Given
            ReportExecutionEntity entity = createExecution("COMPLETED");
            when(executionRepository.findByIdAndTenantId(EXECUTION_ID, TENANT_ID))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<ReportExecutionDto> result = service.getExecution(EXECUTION_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getStatus()).isEqualTo("COMPLETED");
        }
    }

    @Nested
    @DisplayName("Async Report Execution Tests")
    class AsyncExecutionTests {

        @Test
        @DisplayName("Should update execution status to RUNNING")
        void shouldUpdateStatusToRunning() {
            // Given
            ReportEntity report = createReport("Report", "QUALITY");
            ReportExecutionEntity execution = createExecution("PENDING");

            when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
            when(kpiService.getQualityKpis(TENANT_ID)).thenReturn(List.of());
            when(executionRepository.save(any(ReportExecutionEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.executeReportAsync(EXECUTION_ID, report, TENANT_ID);

            // Then
            verify(executionRepository, atLeast(2)).save(any(ReportExecutionEntity.class));
        }

        @Test
        @DisplayName("Should handle execution not found")
        void shouldHandleExecutionNotFound() {
            // Given
            ReportEntity report = createReport("Report", "QUALITY");
            when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.empty());

            // When
            service.executeReportAsync(EXECUTION_ID, report, TENANT_ID);

            // Then
            verify(executionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should call quality KPIs for QUALITY report type")
        void shouldCallQualityKpisForQualityType() {
            // Given
            ReportEntity report = createReport("Report", "QUALITY");
            ReportExecutionEntity execution = createExecution("PENDING");

            when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
            when(kpiService.getQualityKpis(TENANT_ID)).thenReturn(createKpiList());
            when(executionRepository.save(any(ReportExecutionEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.executeReportAsync(EXECUTION_ID, report, TENANT_ID);

            // Then
            verify(kpiService).getQualityKpis(TENANT_ID);
        }

        @Test
        @DisplayName("Should call HCC KPIs for HCC report type")
        void shouldCallHccKpisForHccType() {
            // Given
            ReportEntity report = createReport("Report", "HCC");
            ReportExecutionEntity execution = createExecution("PENDING");

            when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
            when(kpiService.getHccKpis(TENANT_ID)).thenReturn(createKpiList());
            when(executionRepository.save(any(ReportExecutionEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.executeReportAsync(EXECUTION_ID, report, TENANT_ID);

            // Then
            verify(kpiService).getHccKpis(TENANT_ID);
        }

        @Test
        @DisplayName("Should call care gap KPIs for CARE_GAP report type")
        void shouldCallCareGapKpisForCareGapType() {
            // Given
            ReportEntity report = createReport("Report", "CARE_GAP");
            ReportExecutionEntity execution = createExecution("PENDING");

            when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
            when(kpiService.getCareGapKpis(TENANT_ID)).thenReturn(createKpiList());
            when(executionRepository.save(any(ReportExecutionEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.executeReportAsync(EXECUTION_ID, report, TENANT_ID);

            // Then
            verify(kpiService).getCareGapKpis(TENANT_ID);
        }

        @Test
        @DisplayName("Should call all KPIs for default report type")
        void shouldCallAllKpisForDefaultType() {
            // Given
            ReportEntity report = createReport("Report", "COMPREHENSIVE");
            ReportExecutionEntity execution = createExecution("PENDING");

            when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of("quality", createKpiList()));
            when(executionRepository.save(any(ReportExecutionEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.executeReportAsync(EXECUTION_ID, report, TENANT_ID);

            // Then
            verify(kpiService).getAllKpis(TENANT_ID);
        }

        @Test
        @DisplayName("Should set status to FAILED on exception")
        void shouldSetStatusToFailedOnException() {
            // Given
            ReportEntity report = createReport("Report", "QUALITY");
            ReportExecutionEntity execution = createExecution("PENDING");

            when(executionRepository.findById(EXECUTION_ID)).thenReturn(Optional.of(execution));
            when(kpiService.getQualityKpis(TENANT_ID)).thenThrow(new RuntimeException("Service error"));

            ArgumentCaptor<ReportExecutionEntity> captor = ArgumentCaptor.forClass(ReportExecutionEntity.class);
            when(executionRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.executeReportAsync(EXECUTION_ID, report, TENANT_ID);

            // Then
            List<ReportExecutionEntity> savedEntities = captor.getAllValues();
            ReportExecutionEntity lastSaved = savedEntities.get(savedEntities.size() - 1);
            assertThat(lastSaved.getStatus()).isEqualTo("FAILED");
            assertThat(lastSaved.getErrorMessage()).isEqualTo("Service error");
        }
    }

    // ==================== Helper Methods ====================

    private ReportEntity createReport(String name, String type) {
        return ReportEntity.builder()
                .id(REPORT_ID)
                .tenantId(TENANT_ID)
                .name(name)
                .reportType(type)
                .scheduleEnabled(false)
                .outputFormat("PDF")
                .createdBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ReportEntity createReportWithParams(String name, String type, Map<String, Object> params) {
        ReportEntity report = createReport(name, type);
        report.setParameters(params);
        return report;
    }

    private ReportExecutionEntity createExecution(String status) {
        return ReportExecutionEntity.builder()
                .id(EXECUTION_ID)
                .reportId(REPORT_ID)
                .tenantId(TENANT_ID)
                .status(status)
                .triggeredBy(USER_ID)
                .build();
    }

    private List<KpiSummaryDto> createKpiList() {
        return List.of(
                KpiSummaryDto.builder()
                        .metricType("QUALITY_SCORE")
                        .metricName("Overall Score")
                        .currentValue(BigDecimal.valueOf(85.0))
                        .asOfDate(LocalDate.now())
                        .build()
        );
    }
}
