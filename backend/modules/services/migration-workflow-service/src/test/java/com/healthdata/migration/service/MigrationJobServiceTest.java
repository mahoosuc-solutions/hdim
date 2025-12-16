package com.healthdata.migration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.healthdata.migration.dto.DataQualityReport;
import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationErrorCategory;
import com.healthdata.migration.dto.MigrationJobRequest;
import com.healthdata.migration.dto.MigrationJobResponse;
import com.healthdata.migration.dto.MigrationProgress;
import com.healthdata.migration.dto.MigrationSummary;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;
import com.healthdata.migration.persistence.MigrationCheckpointEntity;
import com.healthdata.migration.persistence.MigrationJobEntity;
import com.healthdata.migration.repository.MigrationCheckpointRepository;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.repository.MigrationJobRepository;

/**
 * Unit tests for MigrationJobService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MigrationJobService")
class MigrationJobServiceTest {

    @Mock
    private MigrationJobRepository jobRepository;

    @Mock
    private MigrationErrorRepository errorRepository;

    @Mock
    private MigrationCheckpointRepository checkpointRepository;

    @Mock
    private DataQualityService dataQualityService;

    @InjectMocks
    private MigrationJobService service;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID JOB_ID = UUID.randomUUID();

    @Nested
    @DisplayName("Create Job")
    class CreateJobTests {

        @Test
        @DisplayName("Should create job with correct defaults")
        void shouldCreateJobWithDefaults() {
            // Given
            MigrationJobRequest request = createJobRequest("Test Job");
            request.setTenantId(TENANT_ID);

            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MigrationJobResponse response = service.createJob(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getJobName()).isEqualTo("Test Job");
            assertThat(response.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(response.getStatus()).isEqualTo(JobStatus.PENDING);

            ArgumentCaptor<MigrationJobEntity> captor = ArgumentCaptor.forClass(MigrationJobEntity.class);
            verify(jobRepository).save(captor.capture());

            MigrationJobEntity saved = captor.getValue();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getStatus()).isEqualTo(JobStatus.PENDING);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should create job with custom settings")
        void shouldCreateJobWithCustomSettings() {
            // Given
            MigrationJobRequest request = createJobRequest("Custom Job");
            request.setTenantId(TENANT_ID);
            request.setBatchSize(500);
            request.setMaxRetries(5);
            request.setContinueOnError(false);
            request.setResumable(false);

            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MigrationJobResponse response = service.createJob(request);

            // Then
            assertThat(response.getBatchSize()).isEqualTo(500);
            assertThat(response.getMaxRetries()).isEqualTo(5);
            assertThat(response.isContinueOnError()).isFalse();
            assertThat(response.isResumable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Get Job")
    class GetJobTests {

        @Test
        @DisplayName("Should get job by ID and tenant")
        void shouldGetJobByIdAndTenant() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When
            MigrationJobResponse response = service.getJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(JOB_ID);
            assertThat(response.getJobName()).isEqualTo("Test Job");
            assertThat(response.getStatus()).isEqualTo(JobStatus.RUNNING);
            verify(jobRepository).findByIdAndTenantId(JOB_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Should throw exception when job not found")
        void shouldThrowExceptionWhenJobNotFound() {
            // Given
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.getJob(JOB_ID, TENANT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Job not found");
        }

        @Test
        @DisplayName("Should enforce tenant isolation")
        void shouldEnforceTenantIsolation() {
            // Given
            when(jobRepository.findByIdAndTenantId(JOB_ID, "wrong-tenant"))
                .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.getJob(JOB_ID, "wrong-tenant"))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("List Jobs")
    class ListJobsTests {

        @Test
        @DisplayName("Should list jobs for tenant")
        void shouldListJobsForTenant() {
            // Given
            List<MigrationJobEntity> jobs = List.of(
                createJobEntity(UUID.randomUUID(), "Job 1", JobStatus.PENDING),
                createJobEntity(UUID.randomUUID(), "Job 2", JobStatus.RUNNING)
            );
            Page<MigrationJobEntity> page = new PageImpl<>(jobs);

            when(jobRepository.findByTenantIdWithFilters(
                eq(TENANT_ID), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);

            // When
            Page<MigrationJobResponse> result = service.listJobs(TENANT_ID, null, null, Pageable.unpaged());

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getJobName()).isEqualTo("Job 1");
            assertThat(result.getContent().get(1).getJobName()).isEqualTo("Job 2");
        }

        @Test
        @DisplayName("Should filter jobs by status")
        void shouldFilterByStatus() {
            // Given
            List<MigrationJobEntity> jobs = List.of(
                createJobEntity(UUID.randomUUID(), "Running Job", JobStatus.RUNNING)
            );
            Page<MigrationJobEntity> page = new PageImpl<>(jobs);

            when(jobRepository.findByTenantIdWithFilters(
                eq(TENANT_ID), eq(JobStatus.RUNNING), eq(null), any(Pageable.class)))
                .thenReturn(page);

            // When
            Page<MigrationJobResponse> result = service.listJobs(
                TENANT_ID, JobStatus.RUNNING, null, Pageable.unpaged());

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(jobRepository).findByTenantIdWithFilters(
                eq(TENANT_ID), eq(JobStatus.RUNNING), eq(null), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Job Lifecycle Operations")
    class JobLifecycleTests {

        @Test
        @DisplayName("Should start pending job")
        void shouldStartPendingJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.PENDING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MigrationJobResponse response = service.startJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatus()).isEqualTo(JobStatus.RUNNING);
            assertThat(entity.getStartedAt()).isNotNull();
            verify(jobRepository).save(entity);
        }

        @Test
        @DisplayName("Should not start completed job")
        void shouldNotStartCompletedJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.COMPLETED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When / Then
            assertThatThrownBy(() -> service.startJob(JOB_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be started");
        }

        @Test
        @DisplayName("Should pause running job")
        void shouldPauseRunningJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MigrationJobResponse response = service.pauseJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatus()).isEqualTo(JobStatus.PAUSED);
            verify(jobRepository).save(entity);
        }

        @Test
        @DisplayName("Should not pause non-running job")
        void shouldNotPauseNonRunningJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.PENDING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When / Then
            assertThatThrownBy(() -> service.pauseJob(JOB_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only running jobs can be paused");
        }

        @Test
        @DisplayName("Should resume paused job")
        void shouldResumePausedJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.PAUSED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MigrationJobResponse response = service.resumeJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatus()).isEqualTo(JobStatus.RUNNING);
            verify(jobRepository).save(entity);
        }

        @Test
        @DisplayName("Should cancel job")
        void shouldCancelJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MigrationJobResponse response = service.cancelJob(JOB_ID, TENANT_ID);

            // Then
            assertThat(response.getStatus()).isEqualTo(JobStatus.CANCELLED);
            assertThat(entity.getCompletedAt()).isNotNull();
            verify(jobRepository).save(entity);
        }

        @Test
        @DisplayName("Should not cancel completed job")
        void shouldNotCancelCompletedJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.COMPLETED);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When / Then
            assertThatThrownBy(() -> service.cancelJob(JOB_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a job that has already completed");
        }

        @Test
        @DisplayName("Should delete job")
        void shouldDeleteJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.PENDING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When
            service.deleteJob(JOB_ID, TENANT_ID);

            // Then
            verify(jobRepository).delete(entity);
        }

        @Test
        @DisplayName("Should not delete running job")
        void shouldNotDeleteRunningJob() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When / Then
            assertThatThrownBy(() -> service.deleteJob(JOB_ID, TENANT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete a running job");
        }
    }

    @Nested
    @DisplayName("Progress Tracking")
    class ProgressTrackingTests {

        @Test
        @DisplayName("Should get current progress")
        void shouldGetCurrentProgress() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.RUNNING);
            entity.setTotalRecords(1000L);
            entity.setProcessedCount(500L);
            entity.setSuccessCount(480L);
            entity.setFailureCount(20L);
            entity.setSkippedCount(0L);

            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));

            // When
            MigrationProgress progress = service.getProgress(JOB_ID, TENANT_ID);

            // Then
            assertThat(progress.getJobId()).isEqualTo(JOB_ID);
            assertThat(progress.getTotalRecords()).isEqualTo(1000L);
            assertThat(progress.getProcessedCount()).isEqualTo(500L);
            assertThat(progress.getSuccessCount()).isEqualTo(480L);
            assertThat(progress.getFailureCount()).isEqualTo(20L);
        }

        @Test
        @DisplayName("Should update progress")
        void shouldUpdateProgress() {
            // When
            service.updateProgress(JOB_ID, 100L, 95L, 5L, 0L);

            // Then
            verify(jobRepository).updateProgress(
                eq(JOB_ID), eq(100L), eq(95L), eq(5L), eq(0L), any(Instant.class));
        }

        @Test
        @DisplayName("Should mark job completed")
        void shouldMarkJobCompleted() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(entity));
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.markCompleted(JOB_ID);

            // Then
            assertThat(entity.getStatus()).isEqualTo(JobStatus.COMPLETED);
            assertThat(entity.getCompletedAt()).isNotNull();
            verify(jobRepository).save(entity);
        }

        @Test
        @DisplayName("Should mark job failed")
        void shouldMarkJobFailed() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.RUNNING);
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(entity));
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.markFailed(JOB_ID, "Connection error");

            // Then
            assertThat(entity.getStatus()).isEqualTo(JobStatus.FAILED);
            assertThat(entity.getCompletedAt()).isNotNull();
            verify(jobRepository).save(entity);
        }
    }

    @Nested
    @DisplayName("Summary and Reporting")
    class SummaryTests {

        @Test
        @DisplayName("Should generate summary")
        void shouldGenerateSummary() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.COMPLETED);
            entity.setTotalRecords(1000L);
            entity.setProcessedCount(1000L);
            entity.setSuccessCount(980L);
            entity.setFailureCount(20L);
            entity.setSkippedCount(0L);
            entity.setStartedAt(Instant.now().minusSeconds(100));
            entity.setCompletedAt(Instant.now());

            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID))
                .thenReturn(List.of());
            when(checkpointRepository.countByJobId(JOB_ID))
                .thenReturn(5L);

            // When
            MigrationSummary summary = service.getSummary(JOB_ID, TENANT_ID);

            // Then
            assertThat(summary.getJobId()).isEqualTo(JOB_ID);
            assertThat(summary.getTotalRecords()).isEqualTo(1000L);
            assertThat(summary.getSuccessCount()).isEqualTo(980L);
            assertThat(summary.getFailureCount()).isEqualTo(20L);
            assertThat(summary.getSuccessRate()).isEqualTo(98.0);
            assertThat(summary.getFailureRate()).isEqualTo(2.0);
            assertThat(summary.getCheckpointsSaved()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should calculate metrics correctly")
        void shouldCalculateMetricsCorrectly() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.COMPLETED);
            entity.setTotalRecords(100L);
            entity.setProcessedCount(100L);
            entity.setSuccessCount(95L);
            entity.setFailureCount(5L);
            entity.setStartedAt(Instant.now().minusSeconds(10)); // 10 seconds
            entity.setCompletedAt(Instant.now());

            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(errorRepository.countByJobIdGroupByCategory(JOB_ID))
                .thenReturn(List.of());
            when(checkpointRepository.countByJobId(JOB_ID))
                .thenReturn(0L);

            // When
            MigrationSummary summary = service.getSummary(JOB_ID, TENANT_ID);

            // Then
            assertThat(summary.getRecordsPerSecond()).isGreaterThan(0);
            assertThat(summary.getAvgProcessingTimeMs()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Checkpoints")
    class CheckpointTests {

        @Test
        @DisplayName("Should save checkpoint")
        void shouldSaveCheckpoint() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.RUNNING);
            Map<String, Object> checkpointData = new HashMap<>();
            checkpointData.put("lastOffset", 1000L);

            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(entity));
            when(checkpointRepository.countByJobId(JOB_ID)).thenReturn(2L);
            when(jobRepository.save(any(MigrationJobEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.saveCheckpoint(JOB_ID, checkpointData, "file.hl7", 1000L);

            // Then
            verify(checkpointRepository).save(any(MigrationCheckpointEntity.class));
            verify(jobRepository).save(entity);
            assertThat(entity.getCheckpoint()).isEqualTo(checkpointData);
            assertThat(entity.getLastCheckpointAt()).isNotNull();
        }

        @Test
        @DisplayName("Should get latest checkpoint")
        void shouldGetLatestCheckpoint() {
            // Given
            MigrationCheckpointEntity checkpoint = new MigrationCheckpointEntity();
            when(checkpointRepository.findLatestByJobId(JOB_ID))
                .thenReturn(Optional.of(checkpoint));

            // When
            MigrationCheckpointEntity result = service.getLatestCheckpoint(JOB_ID);

            // Then
            assertThat(result).isEqualTo(checkpoint);
            verify(checkpointRepository).findLatestByJobId(JOB_ID);
        }

        @Test
        @DisplayName("Should return null when no checkpoint exists")
        void shouldReturnNullWhenNoCheckpoint() {
            // Given
            when(checkpointRepository.findLatestByJobId(JOB_ID))
                .thenReturn(Optional.empty());

            // When
            MigrationCheckpointEntity result = service.getLatestCheckpoint(JOB_ID);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Quality Reports")
    class QualityReportTests {

        @Test
        @DisplayName("Should delegate quality report generation")
        void shouldDelegateQualityReportGeneration() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.COMPLETED);
            DataQualityReport report = DataQualityReport.builder()
                .jobId(JOB_ID)
                .build();

            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(dataQualityService.generateReport(JOB_ID))
                .thenReturn(report);

            // When
            DataQualityReport result = service.getQualityReport(JOB_ID, TENANT_ID);

            // Then
            assertThat(result).isEqualTo(report);
            verify(dataQualityService).generateReport(JOB_ID);
        }

        @Test
        @DisplayName("Should delegate CSV export")
        void shouldDelegateCsvExport() {
            // Given
            MigrationJobEntity entity = createJobEntity(JOB_ID, "Test Job", JobStatus.COMPLETED);
            String csv = "CSV content";

            when(jobRepository.findByIdAndTenantId(JOB_ID, TENANT_ID))
                .thenReturn(Optional.of(entity));
            when(dataQualityService.exportReportToCsv(JOB_ID))
                .thenReturn(csv);

            // When
            String result = service.exportQualityReportCsv(JOB_ID, TENANT_ID);

            // Then
            assertThat(result).isEqualTo(csv);
            verify(dataQualityService).exportReportToCsv(JOB_ID);
        }
    }

    // Helper methods
    private MigrationJobRequest createJobRequest(String jobName) {
        SourceConfig sourceConfig = SourceConfig.builder()
            .sourceType(SourceType.FILE)
            .path("/data/hl7")
            .build();

        return MigrationJobRequest.builder()
            .jobName(jobName)
            .sourceType(SourceType.FILE)
            .sourceConfig(sourceConfig)
            .dataType(DataType.HL7V2)
            .batchSize(100)
            .maxRetries(3)
            .build();
    }

    private MigrationJobEntity createJobEntity(UUID id, String name, JobStatus status) {
        SourceConfig sourceConfig = SourceConfig.builder()
            .sourceType(SourceType.FILE)
            .path("/data/hl7")
            .build();

        return MigrationJobEntity.builder()
            .id(id)
            .tenantId(TENANT_ID)
            .jobName(name)
            .status(status)
            .sourceType(SourceType.FILE)
            .sourceConfig(sourceConfig)
            .dataType(DataType.HL7V2)
            .batchSize(100)
            .maxRetries(3)
            .totalRecords(0L)
            .processedCount(0L)
            .successCount(0L)
            .failureCount(0L)
            .skippedCount(0L)
            .retryCount(0)
            .build();
    }
}
