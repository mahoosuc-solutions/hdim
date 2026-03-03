package com.healthdata.cms.service;

import ca.uhn.fhir.context.FhirContext;
import com.healthdata.cms.client.BcdaClient;
import com.healthdata.cms.client.CmsFhirPersistenceClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the CMS BCDA bulk data ingest pipeline.
 *
 * <p>Flow: poll export status → download NDJSON files → parse each line as FHIR
 * resource → batch into 200-resource Bundles → POST to fhir-service.</p>
 */
@Slf4j
@Service
public class CmsBulkIngestService {

    private static final int BATCH_SIZE = 200;

    private final BcdaClient bcdaClient;
    private final CmsFhirPersistenceClient fhirPersistenceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FhirContext fhirContext;

    public CmsBulkIngestService(BcdaClient bcdaClient,
                                 CmsFhirPersistenceClient fhirPersistenceClient,
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.bcdaClient = bcdaClient;
        this.fhirPersistenceClient = fhirPersistenceClient;
        this.kafkaTemplate = kafkaTemplate;
        this.fhirContext = FhirContext.forR4();
    }

    /**
     * Ingest a completed BCDA bulk export into the FHIR store.
     *
     * <p>Call this after an export job reaches "complete" status. It parses the
     * export manifest, downloads each NDJSON file, batches resources, and
     * persists them to the FHIR store.</p>
     *
     * @param jobUrl The BCDA export job URL (Content-Location)
     * @param tenantId Tenant identifier
     * @return IngestResult with counts of processed/failed resources
     */
    @Async
    public void ingestCompletedExport(String jobUrl, String tenantId) {
        log.info("Starting CMS BCDA ingest for job: {} tenant: {}", jobUrl, tenantId);

        BcdaClient.BulkDataExportStatus status = bcdaClient.getExportStatus(jobUrl);
        if (!status.isComplete()) {
            log.warn("Export job not yet complete (status: {}), skipping ingest", status.getStatus());
            return;
        }

        String manifest = status.getOutputManifest();
        if (manifest == null) {
            log.error("Export complete but no manifest available for job: {}", jobUrl);
            return;
        }

        List<BcdaClient.ExportOutputFile> outputFiles = bcdaClient.parseExportManifest(manifest);
        log.info("Export manifest contains {} output files", outputFiles.size());

        int totalProcessed = 0;
        int totalFailed = 0;

        for (BcdaClient.ExportOutputFile file : outputFiles) {
            log.info("Downloading {} file: {}", file.getResourceType(), file.getUrl());
            try {
                String ndjsonContent = bcdaClient.downloadExportFile(file.getUrl());
                IngestCounts counts = processNdjsonContent(ndjsonContent, file.getResourceType(), tenantId);
                totalProcessed += counts.processed;
                totalFailed += counts.failed;
            } catch (Exception e) {
                log.error("Failed to download/process {} file: {}", file.getResourceType(), e.getMessage());
                totalFailed++;
            }
        }

        log.info("CMS BCDA ingest completed: processed={}, failed={}", totalProcessed, totalFailed);

        // Emit completion event for downstream pipeline
        kafkaTemplate.send("cms.bcda.ingest-completed",
                UUID.randomUUID().toString(),
                Map.of(
                        "tenantId", tenantId,
                        "jobUrl", jobUrl,
                        "totalProcessed", totalProcessed,
                        "totalFailed", totalFailed,
                        "timestamp", Instant.now().toString()
                ));
    }

    /**
     * Process NDJSON content: parse each line, batch into Bundles, persist.
     */
    private IngestCounts processNdjsonContent(String ndjsonContent, String resourceType, String tenantId) {
        int processed = 0;
        int failed = 0;

        Bundle currentBatch = createTransactionBundle();

        try (BufferedReader reader = new BufferedReader(new StringReader(ndjsonContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    Resource resource = (Resource) fhirContext.newJsonParser().parseResource(line);
                    addResourceToBundle(currentBatch, resource);

                    if (currentBatch.getEntry().size() >= BATCH_SIZE) {
                        flushBatch(currentBatch, tenantId, resourceType);
                        processed += currentBatch.getEntry().size();
                        currentBatch = createTransactionBundle();
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse NDJSON line as {}: {}", resourceType, e.getMessage());
                    failed++;
                }
            }

            // Flush remaining resources
            if (currentBatch.hasEntry()) {
                flushBatch(currentBatch, tenantId, resourceType);
                processed += currentBatch.getEntry().size();
            }

        } catch (Exception e) {
            log.error("Error reading NDJSON content for {}: {}", resourceType, e.getMessage());
        }

        log.info("Processed {} {} resources ({} failed)", processed, resourceType, failed);
        return new IngestCounts(processed, failed);
    }

    private Bundle createTransactionBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        return bundle;
    }

    private void addResourceToBundle(Bundle bundle, Resource resource) {
        String resourceType = resource.fhirType();
        String resourceId = resource.getIdElement().getIdPart();

        Bundle.BundleEntryComponent entry = bundle.addEntry();
        entry.setResource(resource);
        entry.getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(resourceType + "/" + resourceId);
    }

    private void flushBatch(Bundle batch, String tenantId, String resourceType) {
        try {
            fhirPersistenceClient.persistBundle(batch, tenantId, resourceType);
        } catch (Exception e) {
            log.error("Batch persistence failed for {} ({} entries), attempting individual resources",
                    resourceType, batch.getEntry().size());
            processIndividually(batch, tenantId, resourceType);
        }
    }

    private void processIndividually(Bundle batch, String tenantId, String resourceType) {
        for (Bundle.BundleEntryComponent entry : batch.getEntry()) {
            try {
                Bundle singleBundle = createTransactionBundle();
                singleBundle.addEntry(entry);
                fhirPersistenceClient.persistBundle(singleBundle, tenantId, resourceType);
            } catch (Exception e) {
                log.warn("Individual resource persistence failed: {}", e.getMessage());
            }
        }
    }

    private record IngestCounts(int processed, int failed) {}
}
