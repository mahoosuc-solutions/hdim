package com.healthdata.fhir.bulk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Core service for FHIR Bulk Data Export operations
 *
 * Implements the FHIR Bulk Data Access specification:
 * - Kick off export jobs
 * - Poll job status
 * - Cancel running jobs
 * - Get export file manifests
 */
@Service
@Slf4j
public class BulkExportService {

    private final BulkExportRepository exportRepository;
    private final BulkExportProcessor exportProcessor;
    private final BulkExportConfig config;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BulkExportService(
            BulkExportRepository exportRepository,
            BulkExportProcessor exportProcessor,
            BulkExportConfig config,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.exportRepository = exportRepository;
        this.exportProcessor = exportProcessor;
        this.config = config;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Kick off a new bulk export job
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param exportLevel Export level (SYSTEM, PATIENT, GROUP)
     * @param resourceId Resource ID for PATIENT or GROUP level exports
     * @param resourceTypes List of resource types to export (null = all)
     * @param since Export only resources modified since this timestamp
     * @param typeFilters FHIR search parameters for filtering
     * @param requestUrl Original request URL
     * @param requestedBy User who requested the export
     * @return Export job ID
     */
    @Transactional
    public UUID kickOffExport(
            String tenantId,
            BulkExportJob.ExportLevel exportLevel,
            String resourceId,
            List<String> resourceTypes,
            Instant since,
            List<String> typeFilters,
            String requestUrl,
            String requestedBy) {

        // Check concurrent export limits
        long activeJobs = exportRepository.countActiveJobsByTenant(tenantId);
        if (activeJobs >= config.getMaxConcurrentExports()) {
            throw new ExportLimitExceededException(
                "Maximum concurrent exports (" + config.getMaxConcurrentExports() + ") exceeded for tenant");
        }

        // Default resource types if not specified
        List<String> effectiveResourceTypes = resourceTypes != null && !resourceTypes.isEmpty()
            ? resourceTypes
            : getDefaultResourceTypes(exportLevel);

        // Create export job
        BulkExportJob job = BulkExportJob.builder()
            .jobId(UUID.randomUUID())
            .tenantId(tenantId)
            .status(BulkExportJob.ExportStatus.PENDING)
            .exportLevel(exportLevel)
            .resourceId(resourceId)
            .resourceTypes(effectiveResourceTypes)
            .outputFormat("ndjson")
            .sinceParam(since)
            .typeFilters(typeFilters)
            .requestUrl(requestUrl)
            .requestedAt(Instant.now())
            .requestedBy(requestedBy)
            .build();

        BulkExportJob saved = exportRepository.save(job);
        log.info("Created bulk export job: {} for tenant: {} level: {}",
            saved.getJobId(), tenantId, exportLevel);

        // Publish event
        kafkaTemplate.send("fhir.bulk-export.initiated", saved.getJobId().toString(),
            new BulkExportEvent(saved.getJobId().toString(), tenantId, "INITIATED", Instant.now(), requestedBy));

        // Start async processing
        exportProcessor.processExport(saved.getJobId());

        return saved.getJobId();
    }

    /**
     * Get export job status
     *
     * @param tenantId Tenant identifier
     * @param jobId Export job ID
     * @return Export job with current status
     */
    @Transactional(readOnly = true)
    public Optional<BulkExportJob> getJobStatus(String tenantId, UUID jobId) {
        return exportRepository.findByJobIdAndTenantId(jobId, tenantId);
    }

    /**
     * Cancel a running export job
     *
     * @param tenantId Tenant identifier
     * @param jobId Export job ID
     * @param cancelledBy User who cancelled the job
     */
    @Transactional
    public void cancelJob(String tenantId, UUID jobId, String cancelledBy) {
        BulkExportJob job = exportRepository.findByJobIdAndTenantId(jobId, tenantId)
            .orElseThrow(() -> new ExportJobNotFoundException(jobId.toString()));

        if (job.getStatus() == BulkExportJob.ExportStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed export job");
        }

        if (job.getStatus() == BulkExportJob.ExportStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel failed export job");
        }

        if (job.getStatus() == BulkExportJob.ExportStatus.CANCELLED) {
            return; // Already cancelled
        }

        BulkExportJob updated = job.toBuilder()
            .status(BulkExportJob.ExportStatus.CANCELLED)
            .completedAt(Instant.now())
            .errorMessage("Cancelled by user: " + cancelledBy)
            .build();

        exportRepository.save(updated);
        log.info("Cancelled bulk export job: {} by: {}", jobId, cancelledBy);

        // Publish event
        kafkaTemplate.send("fhir.bulk-export.cancelled", jobId.toString(),
            new BulkExportEvent(jobId.toString(), tenantId, "CANCELLED", Instant.now(), cancelledBy));
    }

    /**
     * Build export manifest for completed job
     *
     * @param job Completed export job
     * @return Export manifest
     */
    public ExportManifest buildManifest(BulkExportJob job) {
        if (job.getStatus() != BulkExportJob.ExportStatus.COMPLETED) {
            throw new IllegalStateException("Export job is not completed");
        }

        return ExportManifest.builder()
            .transactionTime(job.getTransactionTime())
            .request(job.getRequestUrl())
            .requiresAccessToken(config.isRequireAccessToken())
            .output(job.getOutputFiles())
            .error(job.getErrorFiles())
            .build();
    }

    /**
     * Clean up old export jobs and files
     */
    @Transactional
    public void cleanupOldExports() {
        Instant cutoffTime = Instant.now().minusSeconds(config.getRetentionDays() * 86400L);
        List<BulkExportJob> oldJobs = exportRepository.findByCompletedAtBefore(cutoffTime);

        for (BulkExportJob job : oldJobs) {
            log.info("Cleaning up old export job: {}", job.getJobId());
            exportProcessor.deleteExportFiles(job);
            exportRepository.delete(job);
        }
    }

    /**
     * Get default resource types based on export level
     */
    private List<String> getDefaultResourceTypes(BulkExportJob.ExportLevel exportLevel) {
        // For system and patient level, export common clinical resources
        return Arrays.asList(
            "Patient",
            "Observation",
            "Condition",
            "MedicationRequest",
            "Procedure",
            "Encounter",
            "AllergyIntolerance",
            "Immunization"
        );
    }

    /**
     * Export manifest response
     */
    public record ExportManifest(
        Instant transactionTime,
        String request,
        boolean requiresAccessToken,
        List<BulkExportJob.OutputFile> output,
        List<BulkExportJob.OutputFile> error
    ) {
        public static ExportManifestBuilder builder() {
            return new ExportManifestBuilder();
        }

        public static class ExportManifestBuilder {
            private Instant transactionTime;
            private String request;
            private boolean requiresAccessToken;
            private List<BulkExportJob.OutputFile> output;
            private List<BulkExportJob.OutputFile> error;

            public ExportManifestBuilder transactionTime(Instant transactionTime) {
                this.transactionTime = transactionTime;
                return this;
            }

            public ExportManifestBuilder request(String request) {
                this.request = request;
                return this;
            }

            public ExportManifestBuilder requiresAccessToken(boolean requiresAccessToken) {
                this.requiresAccessToken = requiresAccessToken;
                return this;
            }

            public ExportManifestBuilder output(List<BulkExportJob.OutputFile> output) {
                this.output = output;
                return this;
            }

            public ExportManifestBuilder error(List<BulkExportJob.OutputFile> error) {
                this.error = error;
                return this;
            }

            public ExportManifest build() {
                return new ExportManifest(transactionTime, request, requiresAccessToken, output, error);
            }
        }
    }

    /**
     * Bulk export event for Kafka
     */
    public record BulkExportEvent(
        String jobId,
        String tenantId,
        String eventType,
        Instant occurredAt,
        String actor
    ) {}

    /**
     * Exception thrown when export limit is exceeded
     */
    public static class ExportLimitExceededException extends RuntimeException {
        public ExportLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when export job is not found
     */
    public static class ExportJobNotFoundException extends RuntimeException {
        public ExportJobNotFoundException(String jobId) {
            super("Export job not found: " + jobId);
        }
    }
}
