package com.healthdata.migration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.healthdata.migration.dto.DataQualityReport;
import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationJobRequest;
import com.healthdata.migration.dto.MigrationJobResponse;
import com.healthdata.migration.dto.MigrationProgress;
import com.healthdata.migration.dto.MigrationSummary;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;
import com.healthdata.migration.persistence.MigrationErrorEntity;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.service.MigrationJobService;

/**
 * Unit tests for MigrationJobController
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationJobController")
class MigrationJobControllerTest {

    @Mock
    private MigrationJobService jobService;

    @Mock
    private MigrationErrorRepository errorRepository;

    @InjectMocks
    private MigrationJobController controller;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID JOB_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Create Job")
    class CreateJobTests {

        @Test
        @DisplayName("Should create job successfully")
        void shouldCreateJob() {
            // Given
            MigrationJobRequest request = createJobRequest("Test Job");
            MigrationJobResponse expectedResponse = createJobResponse(JOB_ID, "Test Job", JobStatus.PENDING);

            when(jobService.createJob(any(MigrationJobRequest.class))).thenReturn(expectedResponse);

            // When
            ResponseEntity<MigrationJobResponse> response = controller.createJob(request, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getJobName()).isEqualTo("Test Job");
            assertThat(response.getBody().getTenantId()).isEqualTo(TENANT_ID);
            verify(jobService).createJob(any(MigrationJobRequest.class));
        }

        @Test
        @DisplayName("Should set tenant ID from header")
        void shouldSetTenantIdFromHeader() {
            // Given
            MigrationJobRequest request = createJobRequest("Test Job");
            MigrationJobResponse expectedResponse = createJobResponse(JOB_ID, "Test Job", JobStatus.PENDING);

            when(jobService.createJob(any(MigrationJobRequest.class))).thenReturn(expectedResponse);

            // When
            controller.createJob(request, TENANT_ID);

            // Then
            assertThat(request.getTenantId()).isEqualTo(TENANT_ID);
        }
    }

    @Nested
    @DisplayName("List Jobs")
    class ListJobsTests {

        @Test
        @DisplayName("Should list all jobs for tenant")
        void shouldListAllJobs() {
            // Given
            List<MigrationJobResponse> jobs = List.of(
                createJobResponse(UUID.randomUUID(), "Job 1", JobStatus.PENDING),
                createJobResponse(UUID.randomUUID(), "Job 2", JobStatus.RUNNING)
            );
            Page<MigrationJobResponse> page = new PageImpl<>(jobs);

            when(jobService.listJobs(eq(TENANT_ID), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

            // When
            ResponseEntity<Page<MigrationJobResponse>> response =
                controller.listJobs(TENANT_ID, null, null, Pageable.unpaged());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should filter jobs by status")
        void shouldFilterByStatus() {
            // Given
            List<MigrationJobResponse> jobs = List.of(
                createJobResponse(UUID.randomUUID(), "Running Job", JobStatus.RUNNING)
            );
            Page<MigrationJobResponse> page = new PageImpl<>(jobs);

            when(jobService.listJobs(eq(TENANT_ID), eq(JobStatus.RUNNING), eq(null), any(Pageable.class)))
                .thenReturn(page);

            // When
            ResponseEntity<Page<MigrationJobResponse>> response =
                controller.listJobs(TENANT_ID, JobStatus.RUNNING, null, Pageable.unpaged());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            verify(jobService).listJobs(eq(TENANT_ID), eq(JobStatus.RUNNING), eq(null), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter jobs by name")
        void shouldFilterByName() {
            // Given
            List<MigrationJobResponse> jobs = List.of(
                createJobResponse(UUID.randomUUID(), "Test Job 1", JobStatus.PENDING)
            );
            Page<MigrationJobResponse> page = new PageImpl<>(jobs);

            when(jobService.listJobs(eq(TENANT_ID), eq(null), eq("Test"), any(Pageable.class)))
                .thenReturn(page);

            // When
            ResponseEntity<Page<MigrationJobResponse>> response =
                controller.listJobs(TENANT_ID, null, "Test", Pageable.unpaged());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            verify(jobService).listJobs(eq(TENANT_ID), eq(null), eq("Test"), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Get Job")
    class GetJobTests {

        @Test
        @DisplayName("Should get job by ID")
        void shouldGetJobById() {
            // Given
            MigrationJobResponse expectedResponse = createJobResponse(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobService.getJob(JOB_ID, TENANT_ID)).thenReturn(expectedResponse);

            // When
            ResponseEntity<MigrationJobResponse> response = controller.getJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(JOB_ID);
            assertThat(response.getBody().getJobName()).isEqualTo("Test Job");
            verify(jobService).getJob(JOB_ID, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Job Lifecycle Operations")
    class JobLifecycleTests {

        @Test
        @DisplayName("Should start job")
        void shouldStartJob() {
            // Given
            MigrationJobResponse expectedResponse = createJobResponse(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobService.startJob(JOB_ID, TENANT_ID)).thenReturn(expectedResponse);

            // When
            ResponseEntity<MigrationJobResponse> response = controller.startJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(JobStatus.RUNNING);
            verify(jobService).startJob(JOB_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should pause job")
        void shouldPauseJob() {
            // Given
            MigrationJobResponse expectedResponse = createJobResponse(JOB_ID, "Test Job", JobStatus.PAUSED);
            when(jobService.pauseJob(JOB_ID, TENANT_ID)).thenReturn(expectedResponse);

            // When
            ResponseEntity<MigrationJobResponse> response = controller.pauseJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(JobStatus.PAUSED);
            verify(jobService).pauseJob(JOB_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should resume job")
        void shouldResumeJob() {
            // Given
            MigrationJobResponse expectedResponse = createJobResponse(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobService.resumeJob(JOB_ID, TENANT_ID)).thenReturn(expectedResponse);

            // When
            ResponseEntity<MigrationJobResponse> response = controller.resumeJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(JobStatus.RUNNING);
            verify(jobService).resumeJob(JOB_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should cancel job")
        void shouldCancelJob() {
            // Given
            MigrationJobResponse expectedResponse = createJobResponse(JOB_ID, "Test Job", JobStatus.CANCELLED);
            when(jobService.cancelJob(JOB_ID, TENANT_ID)).thenReturn(expectedResponse);

            // When
            ResponseEntity<MigrationJobResponse> response = controller.cancelJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo(JobStatus.CANCELLED);
            verify(jobService).cancelJob(JOB_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should delete job")
        void shouldDeleteJob() {
            // When
            ResponseEntity<Void> response = controller.deleteJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
            verify(jobService).deleteJob(JOB_ID, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Progress and Monitoring")
    class ProgressTests {

        @Test
        @DisplayName("Should get job progress")
        void shouldGetProgress() {
            // Given
            MigrationProgress progress = MigrationProgress.builder()
                .jobId(JOB_ID)
                .status(JobStatus.RUNNING)
                .totalRecords(1000L)
                .processedCount(500L)
                .successCount(480L)
                .failureCount(20L)
                .skippedCount(0L)
                .timestamp(Instant.now())
                .build();

            when(jobService.getProgress(JOB_ID, TENANT_ID)).thenReturn(progress);

            // When
            ResponseEntity<MigrationProgress> response = controller.getProgress(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getJobId()).isEqualTo(JOB_ID);
            assertThat(response.getBody().getTotalRecords()).isEqualTo(1000L);
            assertThat(response.getBody().getProcessedCount()).isEqualTo(500L);
            verify(jobService).getProgress(JOB_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should get job summary")
        void shouldGetSummary() {
            // Given
            MigrationSummary summary = MigrationSummary.builder()
                .jobId(JOB_ID)
                .jobName("Test Job")
                .finalStatus(JobStatus.COMPLETED)
                .totalRecords(1000L)
                .successCount(980L)
                .failureCount(20L)
                .successRate(98.0)
                .build();

            when(jobService.getSummary(JOB_ID, TENANT_ID)).thenReturn(summary);

            // When
            ResponseEntity<MigrationSummary> response = controller.getSummary(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getJobId()).isEqualTo(JOB_ID);
            assertThat(response.getBody().getSuccessRate()).isEqualTo(98.0);
            verify(jobService).getSummary(JOB_ID, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("Errors")
    class ErrorsTests {

        @Test
        @DisplayName("Should get job errors")
        void shouldGetErrors() {
            // Given
            MigrationJobResponse jobResponse = createJobResponse(JOB_ID, "Test Job", JobStatus.RUNNING);
            Page<MigrationErrorEntity> errors = Page.empty();

            when(jobService.getJob(JOB_ID, TENANT_ID)).thenReturn(jobResponse);
            when(errorRepository.findByJobId(eq(JOB_ID), any(Pageable.class))).thenReturn(errors);

            // When
            ResponseEntity<Page<MigrationErrorEntity>> response =
                controller.getErrors(JOB_ID, TENANT_ID, null, null, Pageable.unpaged());

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            verify(jobService).getJob(JOB_ID, TENANT_ID);
            verify(errorRepository).findByJobId(eq(JOB_ID), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Quality Reports")
    class QualityReportTests {

        @Test
        @DisplayName("Should get quality report")
        void shouldGetQualityReport() {
            // Given
            DataQualityReport report = DataQualityReport.builder()
                .jobId(JOB_ID)
                .jobName("Test Job")
                .build();

            when(jobService.getQualityReport(JOB_ID, TENANT_ID)).thenReturn(report);

            // When
            ResponseEntity<DataQualityReport> response = controller.getQualityReport(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getJobId()).isEqualTo(JOB_ID);
            verify(jobService).getQualityReport(JOB_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should export quality report as CSV")
        void shouldExportQualityReportCsv() {
            // Given
            String csvContent = "Quality Report CSV";
            when(jobService.exportQualityReportCsv(JOB_ID, TENANT_ID)).thenReturn(csvContent);

            // When
            ResponseEntity<String> response = controller.exportQualityReport(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(csvContent);
            assertThat(response.getHeaders().getContentDisposition().toString())
                .contains("attachment")
                .contains("quality-report-" + JOB_ID + ".csv");
            verify(jobService).exportQualityReportCsv(JOB_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should export errors as CSV")
        void shouldExportErrorsCsv() throws Exception {
            // Given
            String csvContent = "Errors CSV";
            when(jobService.exportErrorsCsv(JOB_ID, TENANT_ID)).thenReturn(csvContent);

            // When
            ResponseEntity<String> response = controller.exportErrors(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(csvContent);
            assertThat(response.getHeaders().getContentDisposition().toString())
                .contains("attachment")
                .contains("errors-" + JOB_ID + ".csv");
            verify(jobService).exportErrorsCsv(JOB_ID, TENANT_ID);
        }
    }

    // Helper methods
    private MigrationJobRequest createJobRequest(String jobName) {
        SourceConfig sourceConfig = SourceConfig.builder()
            .sourceType(SourceType.FILE)
            .path("/data/hl7")
            .filePattern("*.hl7")
            .build();

        return MigrationJobRequest.builder()
            .jobName(jobName)
            .sourceType(SourceType.FILE)
            .sourceConfig(sourceConfig)
            .dataType(DataType.HL7V2)
            .build();
    }

    private MigrationJobResponse createJobResponse(UUID id, String name, JobStatus status) {
        return MigrationJobResponse.builder()
            .id(id)
            .tenantId(TENANT_ID)
            .jobName(name)
            .status(status)
            .sourceType(SourceType.FILE)
            .dataType(DataType.HL7V2)
            .build();
    }
}
