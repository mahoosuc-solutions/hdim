package com.healthdata.ingestion.application;

import com.healthdata.ingestion.api.v1.dto.IngestionRequest;
import com.healthdata.ingestion.api.v1.dto.IngestionResponse;
import com.healthdata.ingestion.client.CareGapIngestionClient;
import com.healthdata.ingestion.client.FhirIngestionClient;
import com.healthdata.ingestion.client.QualityMeasureIngestionClient;
import com.healthdata.ingestion.generator.SyntheticPatientGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Core orchestration service for data ingestion pipeline.
 *
 * Manages the multi-stage ingestion process:
 * 1. Generate synthetic FHIR R4 patient bundles (0-40% progress)
 * 2. Persist bundles to FHIR service via HTTP (40-70% progress)
 * 3. Create care gaps for eligible patients (70-90% progress)
 * 4. Seed quality measures and evaluations (90-100% progress)
 *
 * Runs asynchronously to avoid blocking the REST API. Progress can be
 * tracked via ProgressTrackingService and events streamed via EventStreamService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataIngestionService {

    private final SyntheticPatientGenerator patientGenerator;
    private final FhirIngestionClient fhirClient;
    private final CareGapIngestionClient careGapClient;
    private final QualityMeasureIngestionClient measureClient;
    private final ProgressTrackingService progressService;
    private final ValidationService validationService;
    private final EventStreamService eventStreamService;

    /**
     * Start asynchronous data ingestion pipeline.
     *
     * @param request Ingestion configuration
     * @return Session information for tracking
     */
    public IngestionResponse startIngestion(IngestionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        log.info("Initializing ingestion session: {} for tenant: {} with {} patients",
                sessionId, request.getTenantId(), request.getPatientCount());

        // Initialize progress tracking
        progressService.initializeSession(sessionId, request);

        // Publish session start event
        eventStreamService.publishEvent(sessionId, "SESSION_STARTED",
                "Ingestion session started for " + request.getPatientCount() + " patients");

        // Run ingestion asynchronously
        CompletableFuture.runAsync(() -> executeIngestionPipeline(sessionId, request));

        return IngestionResponse.builder()
                .sessionId(sessionId)
                .status("STARTED")
                .message("Ingestion pipeline initiated. Use /progress?sessionId=" + sessionId + " to track progress.")
                .build();
    }

    /**
     * Execute the full ingestion pipeline with progress tracking.
     */
    private void executeIngestionPipeline(String sessionId, IngestionRequest request) {
        try {
            log.info("[Session {}] Starting pipeline execution", sessionId);

            // Stage 1: Generate patients (0-40%)
            progressService.updateStage(sessionId, "GENERATING", 0);
            eventStreamService.publishEvent(sessionId, "STAGE_STARTED", "Generating patients");

            // Generate cohort of patients (single bundle containing all patients)
            Bundle patientCohort = patientGenerator.generateCohort(
                    request.getPatientCount(),
                    request.getTenantId()
            );

            // Split cohort into individual patient bundles for processing
            List<Bundle> patientBundles = splitCohortIntoPatientBundles(patientCohort);

            log.info("[Session {}] Generated {} patient bundles", sessionId, patientBundles.size());
            progressService.updateProgress(sessionId, 40);
            eventStreamService.publishEvent(sessionId, "STAGE_COMPLETED",
                    "Generated " + patientBundles.size() + " patients");

            // Stage 2: Persist to FHIR (40-70%)
            progressService.updateStage(sessionId, "PERSISTING", 40);
            eventStreamService.publishEvent(sessionId, "STAGE_STARTED", "Persisting to FHIR service");

            int persisted = 0;
            int totalPatients = patientBundles.size();
            int progressIncrement = 30 / totalPatients; // 30% allocated to this stage

            for (Bundle bundle : patientBundles) {
                try {
                    fhirClient.persistBundle(bundle, request.getTenantId());
                    persisted++;
                    progressService.incrementPersisted(sessionId);

                    // Update progress proportionally
                    int currentProgress = 40 + (persisted * progressIncrement);
                    progressService.updateProgress(sessionId, currentProgress);

                    // Publish event for each patient (visible in stream)
                    String patientId = extractPatientId(bundle);
                    eventStreamService.publishEvent(sessionId, "PATIENT_PERSISTED",
                            "Patient " + patientId + " persisted to FHIR service");

                    // Log every 10% milestone
                    if (persisted % Math.max(1, totalPatients / 10) == 0) {
                        log.info("[Session {}] Persisted {}/{} patients ({}}%)",
                                sessionId, persisted, totalPatients, (persisted * 100 / totalPatients));
                    }

                } catch (Exception e) {
                    log.error("[Session {}] Failed to persist bundle: {}", sessionId, e.getMessage());
                    eventStreamService.publishEvent(sessionId, "ERROR", "Failed to persist patient: " + e.getMessage());
                    // Continue with remaining patients
                }
            }

            log.info("[Session {}] Persisted {}/{} patient bundles", sessionId, persisted, totalPatients);
            progressService.updateProgress(sessionId, 70);
            eventStreamService.publishEvent(sessionId, "STAGE_COMPLETED",
                    "Persisted " + persisted + " patients to FHIR service");

            // Stage 3: Create care gaps (70-90%)
            if (request.getIncludeCareGaps()) {
                progressService.updateStage(sessionId, "CARE_GAPS", 70);
                eventStreamService.publishEvent(sessionId, "STAGE_STARTED", "Creating care gaps");

                int careGapCount = careGapClient.createCareGaps(request.getTenantId());
                log.info("[Session {}] Created {} care gaps", sessionId, careGapCount);

                progressService.setCareGapsCreated(sessionId, careGapCount);
                progressService.updateProgress(sessionId, 90);
                eventStreamService.publishEvent(sessionId, "STAGE_COMPLETED",
                        "Created " + careGapCount + " care gaps");
            } else {
                log.info("[Session {}] Skipping care gaps (disabled in request)", sessionId);
                progressService.updateProgress(sessionId, 90);
            }

            // Stage 4: Seed measures (90-100%)
            if (request.getIncludeQualityMeasures()) {
                progressService.updateStage(sessionId, "MEASURES", 90);
                eventStreamService.publishEvent(sessionId, "STAGE_STARTED", "Seeding quality measures");

                int measureCount = measureClient.seedMeasures(request.getTenantId());
                log.info("[Session {}] Seeded {} quality measures", sessionId, measureCount);

                progressService.setMeasuresSeeded(sessionId, measureCount);
                progressService.updateProgress(sessionId, 100);
                eventStreamService.publishEvent(sessionId, "STAGE_COMPLETED",
                        "Seeded " + measureCount + " quality measures");
            } else {
                log.info("[Session {}] Skipping quality measures (disabled in request)", sessionId);
                progressService.updateProgress(sessionId, 100);
            }

            // Complete session
            progressService.completeSession(sessionId);
            eventStreamService.publishEvent(sessionId, "SESSION_COMPLETED",
                    "Ingestion pipeline completed successfully");

            log.info("[Session {}] Pipeline execution completed successfully", sessionId);

        } catch (Exception e) {
            log.error("[Session {}] Pipeline execution failed: {}", sessionId, e.getMessage(), e);
            progressService.failSession(sessionId, e.getMessage());
            eventStreamService.publishEvent(sessionId, "SESSION_FAILED",
                    "Pipeline failed: " + e.getMessage());
        }
    }

    /**
     * Reset tenant data - delete all ingested patients, care gaps, and measures.
     *
     * WARNING: This is destructive and should only be used in demo environments.
     *
     * @param tenantId Tenant to reset
     */
    public void resetTenant(String tenantId) {
        log.warn("RESET OPERATION: Deleting all data for tenant: {}", tenantId);

        try {
            // Delete all FHIR resources
            fhirClient.deleteAllPatients(tenantId);
            log.info("Deleted all FHIR patients for tenant: {}", tenantId);

            // Delete all care gaps
            careGapClient.deleteAllCareGaps(tenantId);
            log.info("Deleted all care gaps for tenant: {}", tenantId);

            // Delete all measures
            measureClient.deleteAllMeasures(tenantId);
            log.info("Deleted all measures for tenant: {}", tenantId);

            log.info("RESET COMPLETE for tenant: {}", tenantId);

        } catch (Exception e) {
            log.error("RESET FAILED for tenant {}: {}", tenantId, e.getMessage(), e);
            throw new RuntimeException("Failed to reset tenant: " + tenantId, e);
        }
    }

    /**
     * Split a cohort bundle into individual patient bundles.
     *
     * The generator creates a single bundle containing all patients.
     * For incremental processing and progress tracking, we split it into
     * individual bundles (one per patient).
     */
    private List<Bundle> splitCohortIntoPatientBundles(Bundle cohortBundle) {
        List<Bundle> patientBundles = new ArrayList<>();

        // For now, just wrap the entire cohort as a single bundle
        // TODO: Implement proper bundle splitting if needed for better progress tracking
        patientBundles.add(cohortBundle);

        return patientBundles;
    }

    /**
     * Extract patient ID from FHIR bundle for logging/tracking.
     */
    private String extractPatientId(Bundle bundle) {
        if (bundle == null || bundle.getEntry().isEmpty()) {
            return "unknown";
        }

        return bundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType().name().equals("Patient"))
                .findFirst()
                .map(entry -> entry.getResource().getIdElement().getIdPart())
                .orElse("unknown");
    }
}
