package com.healthdata.migration.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.migration.dto.DataQualityReport;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationErrorCategory;
import com.healthdata.migration.dto.MigrationJobRequest;
import com.healthdata.migration.dto.MigrationJobResponse;
import com.healthdata.migration.dto.MigrationProgress;
import com.healthdata.migration.dto.MigrationSummary;
import com.healthdata.migration.persistence.MigrationCheckpointEntity;
import com.healthdata.migration.persistence.MigrationJobEntity;
import com.healthdata.migration.repository.MigrationCheckpointRepository;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.repository.MigrationJobRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing migration jobs
 */
@Service
@RequiredArgsConstructor
public class MigrationJobService {

    private static final Logger log = LoggerFactory.getLogger(MigrationJobService.class);

    private final MigrationJobRepository jobRepository;
    private final MigrationErrorRepository errorRepository;
    private final MigrationCheckpointRepository checkpointRepository;
    private final DataQualityService dataQualityService;

    /**
     * Create a new migration job
     */
    @Transactional
    public MigrationJobResponse createJob(MigrationJobRequest request) {
        log.info("Creating migration job: name={}, tenant={}, sourceType={}",
                request.getJobName(), request.getTenantId(), request.getSourceType());

        MigrationJobEntity job = MigrationJobEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(request.getTenantId())
                .jobName(request.getJobName())
                .description(request.getDescription())
                .sourceType(request.getSourceType())
                .sourceConfig(request.getSourceConfig())
                .dataType(request.getDataType())
                .convertToFhir(request.isConvertToFhir())
                .continueOnError(request.isContinueOnError())
                .batchSize(request.getBatchSize())
                .resumable(request.isResumable())
                .maxRetries(request.getMaxRetries())
                .targetFhirUrl(request.getTargetFhirUrl())
                .callbackUrl(request.getCallbackUrl())
                .status(JobStatus.PENDING)
                .build();

        job = jobRepository.save(job);
        log.info("Created migration job: id={}", job.getId());

        return toResponse(job);
    }

    /**
     * Get job by ID
     */
    @Transactional(readOnly = true)
    public MigrationJobResponse getJob(UUID jobId, String tenantId) {
        MigrationJobEntity job = findJobForTenant(jobId, tenantId);
        return toResponse(job);
    }

    /**
     * List jobs for a tenant
     */
    @Transactional(readOnly = true)
    public Page<MigrationJobResponse> listJobs(String tenantId, JobStatus status, String nameFilter, Pageable pageable) {
        Page<MigrationJobEntity> jobs = jobRepository.findByTenantIdWithFilters(tenantId, status, nameFilter, pageable);
        return jobs.map(this::toResponse);
    }

    /**
     * Start a job
     */
    @Transactional
    public MigrationJobResponse startJob(UUID jobId, String tenantId) {
        MigrationJobEntity job = findJobForTenant(jobId, tenantId);

        if (!job.canStart()) {
            throw new IllegalStateException(
                    String.format("Job cannot be started in status: %s", job.getStatus()));
        }

        job.markStarted();
        job = jobRepository.save(job);

        log.info("Started migration job: id={}, status={}", job.getId(), job.getStatus());
        return toResponse(job);
    }

    /**
     * Pause a running job
     */
    @Transactional
    public MigrationJobResponse pauseJob(UUID jobId, String tenantId) {
        MigrationJobEntity job = findJobForTenant(jobId, tenantId);

        if (job.getStatus() != JobStatus.RUNNING) {
            throw new IllegalStateException("Only running jobs can be paused");
        }

        job.markPaused();
        job = jobRepository.save(job);

        log.info("Paused migration job: id={}", job.getId());
        return toResponse(job);
    }

    /**
     * Resume a paused job
     */
    @Transactional
    public MigrationJobResponse resumeJob(UUID jobId, String tenantId) {
        MigrationJobEntity job = findJobForTenant(jobId, tenantId);

        if (job.getStatus() != JobStatus.PAUSED) {
            throw new IllegalStateException("Only paused jobs can be resumed");
        }

        job.setStatus(JobStatus.RUNNING);
        job = jobRepository.save(job);

        log.info("Resumed migration job: id={}", job.getId());
        return toResponse(job);
    }

    /**
     * Cancel a job
     */
    @Transactional
    public MigrationJobResponse cancelJob(UUID jobId, String tenantId) {
        MigrationJobEntity job = findJobForTenant(jobId, tenantId);

        if (job.isTerminal()) {
            throw new IllegalStateException("Cannot cancel a job that has already completed");
        }

        job.markCancelled();
        job = jobRepository.save(job);

        log.info("Cancelled migration job: id={}", job.getId());
        return toResponse(job);
    }

    /**
     * Delete a job and all associated data
     */
    @Transactional
    public void deleteJob(UUID jobId, String tenantId) {
        MigrationJobEntity job = findJobForTenant(jobId, tenantId);

        if (job.getStatus() == JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot delete a running job");
        }

        // Cascade delete will handle errors and checkpoints
        jobRepository.delete(job);
        log.info("Deleted migration job: id={}", job.getId());
    }

    /**
     * Get current progress for a job
     */
    @Transactional(readOnly = true)
    public MigrationProgress getProgress(UUID jobId, String tenantId) {
        MigrationJobEntity job = findJobForTenant(jobId, tenantId);

        return MigrationProgress.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .processedCount(job.getProcessedCount())
                .successCount(job.getSuccessCount())
                .failureCount(job.getFailureCount())
                .skippedCount(job.getSkippedCount())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Get job summary (for completed jobs)
     */
    @Transactional(readOnly = true)
    public MigrationSummary getSummary(UUID jobId, String tenantId) {
        MigrationJobEntity job = findJobForTenant(jobId, tenantId);

        // Get error breakdown
        List<Object[]> errorCounts = errorRepository.countByJobIdGroupByCategory(jobId);
        Map<MigrationErrorCategory, Long> errorsByCategory = new HashMap<>();
        for (Object[] row : errorCounts) {
            errorsByCategory.put((MigrationErrorCategory) row[0], (Long) row[1]);
        }

        long durationMs = 0;
        if (job.getStartedAt() != null && job.getCompletedAt() != null) {
            durationMs = job.getCompletedAt().toEpochMilli() - job.getStartedAt().toEpochMilli();
        }

        double recordsPerSecond = durationMs > 0 ?
                (double) job.getProcessedCount() / (durationMs / 1000.0) : 0;

        return MigrationSummary.builder()
                .jobId(job.getId())
                .jobName(job.getJobName())
                .tenantId(job.getTenantId())
                .finalStatus(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .successCount(job.getSuccessCount())
                .failureCount(job.getFailureCount())
                .skippedCount(job.getSkippedCount())
                .successRate(job.getTotalRecords() > 0 ?
                        (double) job.getSuccessCount() / job.getTotalRecords() * 100 : 0)
                .failureRate(job.getTotalRecords() > 0 ?
                        (double) job.getFailureCount() / job.getTotalRecords() * 100 : 0)
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .totalDurationMs(durationMs)
                .avgProcessingTimeMs(job.getProcessedCount() > 0 ?
                        durationMs / job.getProcessedCount() : 0)
                .recordsPerSecond(recordsPerSecond)
                .fhirResourcesCreated(job.getFhirResourcesCreated())
                .errorsByCategory(errorsByCategory)
                .checkpointsSaved((int) checkpointRepository.countByJobId(jobId))
                .retriesPerformed(job.getRetryCount())
                .build();
    }

    /**
     * Get latest checkpoint for resume
     */
    @Transactional(readOnly = true)
    public MigrationCheckpointEntity getLatestCheckpoint(UUID jobId) {
        return checkpointRepository.findLatestByJobId(jobId).orElse(null);
    }

    /**
     * Update job progress
     */
    @Transactional
    public void updateProgress(UUID jobId, long processed, long success, long failure, long skipped) {
        jobRepository.updateProgress(jobId, processed, success, failure, skipped, Instant.now());
    }

    /**
     * Mark job as completed
     */
    @Transactional
    public void markCompleted(UUID jobId) {
        MigrationJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.markCompleted();
        jobRepository.save(job);
        log.info("Migration job completed: id={}, success={}, failure={}",
                jobId, job.getSuccessCount(), job.getFailureCount());
    }

    /**
     * Mark job as failed
     */
    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        MigrationJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.markFailed();
        jobRepository.save(job);
        log.error("Migration job failed: id={}, error={}", jobId, errorMessage);
    }

    /**
     * Save checkpoint
     */
    @Transactional
    public void saveCheckpoint(UUID jobId, Map<String, Object> checkpointData,
                               String currentFile, Long currentOffset) {
        MigrationJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        int nextNumber = (int) checkpointRepository.countByJobId(jobId) + 1;

        MigrationCheckpointEntity checkpoint = MigrationCheckpointEntity.create(
                job, nextNumber, checkpointData, currentFile, currentOffset);
        checkpointRepository.save(checkpoint);

        job.saveCheckpoint(checkpointData);
        jobRepository.save(job);

        log.debug("Saved checkpoint #{} for job {}", nextNumber, jobId);
    }

    /**
     * Get data quality report
     */
    @Transactional(readOnly = true)
    public DataQualityReport getQualityReport(UUID jobId, String tenantId) {
        // Verify tenant has access
        findJobForTenant(jobId, tenantId);
        // Delegate to DataQualityService for comprehensive report
        return dataQualityService.generateReport(jobId);
    }

    /**
     * Export quality report to CSV
     */
    @Transactional(readOnly = true)
    public String exportQualityReportCsv(UUID jobId, String tenantId) {
        findJobForTenant(jobId, tenantId);
        return dataQualityService.exportReportToCsv(jobId);
    }

    /**
     * Export errors to CSV
     */
    @Transactional(readOnly = true)
    public String exportErrorsCsv(UUID jobId, String tenantId) throws java.io.IOException {
        findJobForTenant(jobId, tenantId);
        return dataQualityService.exportErrorsToCsv(jobId);
    }

    private MigrationJobEntity findJobForTenant(UUID jobId, String tenantId) {
        return jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Job not found: " + jobId + " for tenant: " + tenantId));
    }

    private MigrationJobResponse toResponse(MigrationJobEntity job) {
        String baseUrl = "/api/v1/migrations/" + job.getId();

        return MigrationJobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .jobName(job.getJobName())
                .description(job.getDescription())
                .sourceType(job.getSourceType())
                .sourceConfig(job.getSourceConfig())
                .dataType(job.getDataType())
                .convertToFhir(job.isConvertToFhir())
                .continueOnError(job.isContinueOnError())
                .batchSize(job.getBatchSize())
                .resumable(job.isResumable())
                .status(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .processedCount(job.getProcessedCount())
                .successCount(job.getSuccessCount())
                .failureCount(job.getFailureCount())
                .skippedCount(job.getSkippedCount())
                .createdAt(job.getCreatedAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .lastCheckpointAt(job.getLastCheckpointAt())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .nextRetryAt(job.getNextRetryAt())
                .progressUrl(baseUrl + "/progress")
                .errorsUrl(baseUrl + "/errors")
                .qualityReportUrl(baseUrl + "/quality")
                .build();
    }
}
