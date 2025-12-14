package com.healthdata.migration.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.healthdata.migration.connector.SourceConnector;
import com.healthdata.migration.connector.SourceConnectorFactory;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationErrorCategory;
import com.healthdata.migration.dto.MigrationProgress;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.persistence.MigrationCheckpointEntity;
import com.healthdata.migration.persistence.MigrationErrorEntity;
import com.healthdata.migration.persistence.MigrationJobEntity;
import com.healthdata.migration.repository.MigrationErrorRepository;
import com.healthdata.migration.repository.MigrationJobRepository;
import com.healthdata.migration.websocket.MigrationProgressPublisher;

import lombok.RequiredArgsConstructor;

/**
 * Service for executing migration jobs asynchronously.
 * Handles record processing, checkpointing, and progress updates.
 */
@Service
@RequiredArgsConstructor
public class MigrationExecutorService {

    private static final Logger log = LoggerFactory.getLogger(MigrationExecutorService.class);

    private final MigrationJobRepository jobRepository;
    private final MigrationErrorRepository errorRepository;
    private final MigrationJobService jobService;
    private final SourceConnectorFactory connectorFactory;
    private final MigrationProgressPublisher progressPublisher;

    // Track running jobs for cancellation
    private final Map<UUID, AtomicBoolean> runningJobs = new ConcurrentHashMap<>();

    @Value("${migration.checkpoint-interval:500}")
    private int checkpointInterval;

    @Value("${migration.progress-update-interval:1000}")
    private int progressUpdateInterval;

    /**
     * Execute a migration job asynchronously
     */
    @Async("migrationExecutor")
    public CompletableFuture<Void> executeJob(UUID jobId) {
        log.info("Starting migration job execution: {}", jobId);

        AtomicBoolean shouldContinue = new AtomicBoolean(true);
        runningJobs.put(jobId, shouldContinue);

        try {
            MigrationJobEntity job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

            // Initialize connector
            try (SourceConnector connector = connectorFactory.createAndConnect(job.getSourceConfig())) {

                // Count total records if possible
                long totalRecords = connector.countRecords();
                if (totalRecords >= 0) {
                    job.setTotalRecords(totalRecords);
                    jobRepository.save(job);
                }

                // Check for checkpoint to resume from
                MigrationCheckpointEntity checkpoint = jobService.getLatestCheckpoint(jobId);
                if (checkpoint != null && job.isResumable()) {
                    connector.restoreFromCheckpoint(checkpoint.getCheckpointData());
                    log.info("Resumed from checkpoint: records={}", checkpoint.getRecordsProcessed());
                }

                // Process records
                processRecords(job, connector, shouldContinue);

            } catch (Exception e) {
                log.error("Migration job failed: {}", jobId, e);
                jobService.markFailed(jobId, e.getMessage());
                return CompletableFuture.failedFuture(e);
            }

            return CompletableFuture.completedFuture(null);

        } finally {
            runningJobs.remove(jobId);
        }
    }

    /**
     * Request cancellation of a running job
     */
    public void requestCancellation(UUID jobId) {
        AtomicBoolean shouldContinue = runningJobs.get(jobId);
        if (shouldContinue != null) {
            shouldContinue.set(false);
            log.info("Cancellation requested for job: {}", jobId);
        }
    }

    /**
     * Check if a job is currently running
     */
    public boolean isJobRunning(UUID jobId) {
        return runningJobs.containsKey(jobId);
    }

    private void processRecords(MigrationJobEntity job, SourceConnector connector,
                                AtomicBoolean shouldContinue) throws Exception {
        UUID jobId = job.getId();
        int batchSize = job.getBatchSize();

        long processed = job.getProcessedCount();
        long success = job.getSuccessCount();
        long failure = job.getFailureCount();
        long skipped = job.getSkippedCount();

        int recordsSinceCheckpoint = 0;
        long lastProgressUpdate = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();

        Iterator<SourceRecord> iterator = connector.readRecords(batchSize);

        while (iterator.hasNext() && shouldContinue.get()) {
            // Check if job was paused or cancelled
            MigrationJobEntity currentJob = jobRepository.findById(jobId).orElse(null);
            if (currentJob == null || currentJob.getStatus() == JobStatus.PAUSED ||
                currentJob.getStatus() == JobStatus.CANCELLED) {
                log.info("Job {} interrupted: status={}", jobId,
                        currentJob != null ? currentJob.getStatus() : "deleted");
                break;
            }

            SourceRecord record = iterator.next();
            processed++;

            try {
                // Process the record
                boolean processedOk = processRecord(job, record);

                if (processedOk) {
                    success++;
                } else {
                    skipped++;
                }

            } catch (Exception e) {
                failure++;

                // Record error
                MigrationErrorEntity error = MigrationErrorEntity.fromException(
                        job, record.getRecordId(), record.getSourceFile(),
                        record.getOffset(), categorizeError(e), e, record.getContent());
                errorRepository.save(error);

                // Check if we should continue on error
                if (!job.isContinueOnError()) {
                    log.error("Job {} stopping due to error (continueOnError=false)", jobId);
                    throw e;
                }
            }

            recordsSinceCheckpoint++;

            // Save checkpoint periodically
            if (recordsSinceCheckpoint >= checkpointInterval) {
                saveCheckpoint(job, connector, processed, success, failure, skipped);
                recordsSinceCheckpoint = 0;
            }

            // Publish progress periodically
            long now = System.currentTimeMillis();
            if (now - lastProgressUpdate >= progressUpdateInterval) {
                publishProgress(job, processed, success, failure, skipped, startTime, connector);
                lastProgressUpdate = now;
            }
        }

        // Final update
        job.updateProgress(processed, success, failure, skipped);

        if (shouldContinue.get()) {
            job.markCompleted();
            log.info("Job {} completed: processed={}, success={}, failure={}",
                    jobId, processed, success, failure);
        } else {
            job.markPaused();
            log.info("Job {} paused: processed={}", jobId, processed);
        }

        jobRepository.save(job);

        // Final progress update
        publishProgress(job, processed, success, failure, skipped, startTime, connector);
    }

    private boolean processRecord(MigrationJobEntity job, SourceRecord record) throws Exception {
        // TODO: Integrate with CDR Processor Service for actual parsing/conversion
        // For now, validate record format

        String content = record.getContent();
        if (content == null || content.trim().isEmpty()) {
            return false; // Skip empty records
        }

        switch (record.getDataType()) {
            case HL7V2:
                if (!content.startsWith("MSH")) {
                    throw new IllegalArgumentException("Invalid HL7 v2 message: missing MSH segment");
                }
                break;

            case CDA:
                if (!content.contains("<ClinicalDocument")) {
                    throw new IllegalArgumentException("Invalid CDA document: missing ClinicalDocument root");
                }
                break;

            case FHIR_BUNDLE:
                if (!content.contains("\"resourceType\"")) {
                    throw new IllegalArgumentException("Invalid FHIR Bundle: missing resourceType");
                }
                break;
        }

        // In production: call CDR Processor Service to parse and convert
        // cdrProcessorClient.process(record.getContent(), record.getDataType(), job.getTenantId());

        // Track FHIR resources created
        Map<String, Long> resources = job.getFhirResourcesCreated();
        if (resources == null) {
            resources = new HashMap<>();
            job.setFhirResourcesCreated(resources);
        }

        // Placeholder: increment resource count based on data type
        String resourceType = switch (record.getDataType()) {
            case HL7V2 -> "Patient"; // Simplified - ADT creates Patient
            case CDA -> "DocumentReference";
            case FHIR_BUNDLE -> "Bundle";
        };
        resources.merge(resourceType, 1L, Long::sum);

        return true;
    }

    private MigrationErrorCategory categorizeError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (message.contains("parse") || message.contains("syntax") || message.contains("invalid")) {
            return MigrationErrorCategory.PARSE_ERROR;
        } else if (message.contains("validation") || message.contains("fhir")) {
            return MigrationErrorCategory.VALIDATION_ERROR;
        } else if (message.contains("mapping") || message.contains("convert")) {
            return MigrationErrorCategory.MAPPING_ERROR;
        } else if (message.contains("duplicate") || message.contains("already exists")) {
            return MigrationErrorCategory.DUPLICATE_RECORD;
        } else if (message.contains("required") || message.contains("missing")) {
            return MigrationErrorCategory.MISSING_REQUIRED;
        } else if (message.contains("code") || message.contains("loinc") || message.contains("snomed")) {
            return MigrationErrorCategory.INVALID_CODE;
        } else if (message.contains("connection") || message.contains("timeout")) {
            return MigrationErrorCategory.CONNECTIVITY_ERROR;
        }

        return MigrationErrorCategory.SYSTEM_ERROR;
    }

    private void saveCheckpoint(MigrationJobEntity job, SourceConnector connector,
                                long processed, long success, long failure, long skipped) {
        Map<String, Object> checkpointData = connector.getCheckpointData();
        String currentFile = connector.getCurrentFile();
        Long currentOffset = connector.getCurrentPosition();

        job.updateProgress(processed, success, failure, skipped);
        jobRepository.save(job);

        jobService.saveCheckpoint(job.getId(), checkpointData, currentFile, currentOffset);

        log.debug("Checkpoint saved for job {}: processed={}", job.getId(), processed);
    }

    private void publishProgress(MigrationJobEntity job, long processed, long success,
                                 long failure, long skipped, long startTime,
                                 SourceConnector connector) {
        long elapsed = System.currentTimeMillis() - startTime;
        double recordsPerSecond = elapsed > 0 ? (double) processed / (elapsed / 1000.0) : 0;
        long avgMs = processed > 0 ? elapsed / processed : 0;

        long remaining = job.getTotalRecords() - processed;
        long etaMs = recordsPerSecond > 0 ? (long) (remaining / recordsPerSecond * 1000) : 0;

        MigrationProgress progress = MigrationProgress.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .processedCount(processed)
                .successCount(success)
                .failureCount(failure)
                .skippedCount(skipped)
                .recordsPerSecond(recordsPerSecond)
                .avgProcessingTimeMs(avgMs)
                .estimatedTimeRemainingMs(etaMs)
                .currentFile(connector.getCurrentFile())
                .currentOffset(connector.getCurrentPosition())
                .timestamp(Instant.now())
                .build();

        progressPublisher.publishProgress(job.getId(), progress);
    }
}
