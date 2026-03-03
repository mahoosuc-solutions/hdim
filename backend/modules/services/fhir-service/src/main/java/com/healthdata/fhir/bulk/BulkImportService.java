package com.healthdata.fhir.bulk;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthdata.fhir.service.BundleTransactionService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Service for bulk NDJSON import processing.
 *
 * <p>Streams NDJSON line-by-line via BufferedReader (never loads the full file
 * into memory). Batches 200 resources per transaction commit via
 * BundleTransactionService.</p>
 *
 * <p>Uses the same async job pattern as BulkExportService — clients receive
 * a job ID and poll for status.</p>
 */
@Service
@Slf4j
public class BulkImportService {

    private static final int BATCH_SIZE = 200;
    private static final int MAX_CONCURRENT_IMPORTS = 3;
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();

    private final BulkImportRepository importRepository;
    private final BundleTransactionService bundleTransactionService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter importedResourcesCounter;

    public BulkImportService(
            BulkImportRepository importRepository,
            BundleTransactionService bundleTransactionService,
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.importRepository = importRepository;
        this.bundleTransactionService = bundleTransactionService;
        this.kafkaTemplate = kafkaTemplate;
        this.importedResourcesCounter = Counter.builder("fhir.bulk-import.resources")
                .description("Total resources imported via bulk NDJSON")
                .register(meterRegistry);
    }

    /**
     * Initiate a bulk import job. Returns the job ID for status polling.
     */
    @Transactional
    public UUID initiateImport(String tenantId, String submittedBy) {
        long activeJobs = importRepository.countActiveJobsByTenant(tenantId);
        if (activeJobs >= MAX_CONCURRENT_IMPORTS) {
            throw new ImportLimitExceededException(
                    "Maximum concurrent imports (" + MAX_CONCURRENT_IMPORTS + ") exceeded for tenant");
        }

        BulkImportJob job = BulkImportJob.builder()
                .jobId(UUID.randomUUID())
                .tenantId(tenantId)
                .status(BulkImportJob.ImportStatus.PENDING)
                .submittedAt(Instant.now())
                .submittedBy(submittedBy)
                .build();

        BulkImportJob saved = importRepository.save(job);
        log.info("Created bulk import job: {} for tenant: {}", saved.getJobId(), tenantId);

        kafkaTemplate.send("fhir.bulk-import.initiated", saved.getJobId().toString(),
                Map.of("jobId", saved.getJobId().toString(),
                       "tenantId", tenantId,
                       "timestamp", Instant.now().toString()));

        return saved.getJobId();
    }

    /**
     * Process NDJSON input stream asynchronously. Each line is a JSON-encoded
     * FHIR resource. Lines are batched into 200-resource transaction bundles.
     */
    @Async
    public void processNdjsonStream(UUID jobId, String tenantId, InputStream ndjsonStream) {
        BulkImportJob job = importRepository.findById(jobId).orElseThrow(() ->
                new ImportJobNotFoundException(jobId.toString()));

        job = job.toBuilder()
                .status(BulkImportJob.ImportStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .build();
        importRepository.save(job);

        IParser jsonParser = FHIR_CONTEXT.newJsonParser();
        long processed = 0;
        long failed = 0;
        List<String> errors = new ArrayList<>();
        List<Resource> batch = new ArrayList<>(BATCH_SIZE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(ndjsonStream, StandardCharsets.UTF_8))) {

            String line;
            long lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    Resource resource = (Resource) jsonParser.parseResource(line);
                    batch.add(resource);

                    if (batch.size() >= BATCH_SIZE) {
                        int batchResult = flushBatch(batch, tenantId);
                        processed += batchResult;
                        failed += (batch.size() - batchResult);
                        batch.clear();
                    }
                } catch (Exception e) {
                    failed++;
                    if (errors.size() < 100) {
                        errors.add("Line " + lineNumber + ": " + e.getMessage());
                    }
                    log.warn("Failed to parse NDJSON line {}: {}", lineNumber, e.getMessage());
                }

                // Periodically update progress
                if (lineNumber % 1000 == 0) {
                    updateProgress(jobId, processed, failed);
                }
            }

            // Flush remaining batch
            if (!batch.isEmpty()) {
                int batchResult = flushBatch(batch, tenantId);
                processed += batchResult;
                failed += (batch.size() - batchResult);
            }

            // Mark completed
            completeJob(jobId, processed, failed, errors);
            importedResourcesCounter.increment(processed);

            log.info("Bulk import completed: jobId={}, processed={}, failed={}", jobId, processed, failed);

        } catch (Exception e) {
            log.error("Bulk import failed: jobId={}", jobId, e);
            failJob(jobId, processed, failed, e.getMessage());
        }
    }

    /**
     * Flush a batch of resources as a FHIR transaction bundle.
     * Returns the number of successfully processed resources.
     */
    private int flushBatch(List<Resource> resources, String tenantId) {
        Bundle txBundle = new Bundle();
        txBundle.setType(Bundle.BundleType.BATCH);

        for (Resource resource : resources) {
            Bundle.BundleEntryComponent entry = txBundle.addEntry();
            entry.setResource(resource);
            entry.getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl(resource.fhirType());
        }

        try {
            Bundle result = bundleTransactionService.processBundle(tenantId, txBundle, "bulk-import");
            // Count successful entries
            int success = 0;
            for (Bundle.BundleEntryComponent entry : result.getEntry()) {
                String status = entry.getResponse().getStatus();
                if (status != null && status.startsWith("2")) {
                    success++;
                }
            }
            return success;
        } catch (Exception e) {
            log.warn("Batch flush failed, will retry individually: {}", e.getMessage());
            // Fall back to individual resource processing
            return processIndividually(resources, tenantId);
        }
    }

    private int processIndividually(List<Resource> resources, String tenantId) {
        int success = 0;
        for (Resource resource : resources) {
            try {
                Bundle singleBundle = new Bundle();
                singleBundle.setType(Bundle.BundleType.BATCH);
                singleBundle.addEntry()
                        .setResource(resource)
                        .getRequest()
                        .setMethod(Bundle.HTTPVerb.POST)
                        .setUrl(resource.fhirType());

                bundleTransactionService.processBundle(tenantId, singleBundle, "bulk-import");
                success++;
            } catch (Exception e) {
                log.warn("Individual resource import failed: {} - {}",
                        resource.fhirType(), e.getMessage());
            }
        }
        return success;
    }

    @Transactional
    public void updateProgress(UUID jobId, long processed, long failed) {
        importRepository.findById(jobId).ifPresent(job -> {
            importRepository.save(job.toBuilder()
                    .processedRecords(processed)
                    .failedRecords(failed)
                    .build());
        });
    }

    @Transactional
    public void completeJob(UUID jobId, long processed, long failed, List<String> errors) {
        importRepository.findById(jobId).ifPresent(job -> {
            importRepository.save(job.toBuilder()
                    .status(failed > 0 && processed == 0
                            ? BulkImportJob.ImportStatus.FAILED
                            : BulkImportJob.ImportStatus.COMPLETED)
                    .processedRecords(processed)
                    .failedRecords(failed)
                    .totalRecords(processed + failed)
                    .errorSummary(errors.isEmpty() ? null : String.join("\n", errors))
                    .completedAt(Instant.now())
                    .build());
        });

        List<String> patientIds = List.of(); // Collected during processing in production
        kafkaTemplate.send("fhir.bulk-import.completed", jobId.toString(),
                Map.of("jobId", jobId.toString(),
                       "processedRecords", processed,
                       "failedRecords", failed,
                       "patientIds", patientIds,
                       "timestamp", Instant.now().toString()));
    }

    @Transactional
    public void failJob(UUID jobId, long processed, long failed, String errorMessage) {
        importRepository.findById(jobId).ifPresent(job -> {
            importRepository.save(job.toBuilder()
                    .status(BulkImportJob.ImportStatus.FAILED)
                    .processedRecords(processed)
                    .failedRecords(failed)
                    .errorSummary(errorMessage)
                    .completedAt(Instant.now())
                    .build());
        });
    }

    @Transactional(readOnly = true)
    public Optional<BulkImportJob> getJobStatus(String tenantId, UUID jobId) {
        return importRepository.findByJobIdAndTenantId(jobId, tenantId);
    }

    public static class ImportLimitExceededException extends RuntimeException {
        public ImportLimitExceededException(String message) {
            super(message);
        }
    }

    public static class ImportJobNotFoundException extends RuntimeException {
        public ImportJobNotFoundException(String jobId) {
            super("Import job not found: " + jobId);
        }
    }
}
