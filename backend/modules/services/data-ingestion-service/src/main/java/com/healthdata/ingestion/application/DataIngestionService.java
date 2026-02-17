package com.healthdata.ingestion.application;

import com.healthdata.ingestion.api.v1.IngestionProgressResponse;
import com.healthdata.ingestion.api.v1.IngestionRequest;
import com.healthdata.ingestion.api.v1.IngestionResponse;
import com.healthdata.ingestion.client.CareGapIngestionClient;
import com.healthdata.ingestion.client.FhirIngestionClient;
import com.healthdata.ingestion.client.QualityMeasureIngestionClient;
import com.healthdata.ingestion.generator.SyntheticPatientGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Core data ingestion orchestration service.
 *
 * <p>Manages the end-to-end data ingestion pipeline:
 * <ol>
 *   <li>Generate synthetic patient bundles</li>
 *   <li>Persist to FHIR service</li>
 *   <li>Create care gaps (optional)</li>
 *   <li>Seed quality measures (optional)</li>
 *   <li>Track progress in real-time</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataIngestionService {

    private final SyntheticPatientGenerator patientGenerator;
    private final FhirIngestionClient fhirClient;
    private final CareGapIngestionClient careGapClient;
    private final QualityMeasureIngestionClient qualityMeasureClient;
    private final ProgressTrackingService progressTrackingService;

    // In-memory session storage (for simplicity - could be Redis in production)
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private String latestSessionId;

    /**
     * Start asynchronous data ingestion.
     *
     * @param request Ingestion configuration
     * @return Response with session ID
     */
    public IngestionResponse startIngestion(IngestionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        latestSessionId = sessionId;

        // Initialize progress tracking
        progressTrackingService.initializeSession(sessionId, request);

        // Store active session
        activeSessions.put(sessionId, "RUNNING");

        // Start async ingestion
        performIngestionAsync(sessionId, request);

        return IngestionResponse.builder()
                .sessionId(sessionId)
                .status("STARTED")
                .message(String.format("Data ingestion started: %d patients for tenant %s",
                        request.getPatientCount(), request.getTenantId()))
                .build();
    }

    /**
     * Get progress for active or recent session.
     *
     * @param sessionId Session ID (null = latest)
     * @return Progress details
     */
    public IngestionProgressResponse getProgress(String sessionId) {
        String targetSessionId = sessionId != null ? sessionId : latestSessionId;

        if (targetSessionId == null) {
            // No sessions yet
            return IngestionProgressResponse.builder()
                    .status("NO_SESSION")
                    .progressPercent(0)
                    .build();
        }

        return progressTrackingService.getProgress(targetSessionId);
    }

    /**
     * Cancel active ingestion session.
     *
     * @param sessionId Session ID to cancel
     * @return Cancellation confirmation
     */
    public IngestionResponse cancelIngestion(String sessionId) {
        String status = activeSessions.get(sessionId);

        if (status == null) {
            return IngestionResponse.builder()
                    .sessionId(sessionId)
                    .status("NOT_FOUND")
                    .message("Session not found: " + sessionId)
                    .build();
        }

        if (!"RUNNING".equals(status)) {
            return IngestionResponse.builder()
                    .sessionId(sessionId)
                    .status("ALREADY_COMPLETED")
                    .message("Session already completed or cancelled")
                    .build();
        }

        // Mark for cancellation
        progressTrackingService.cancelSession(sessionId);
        activeSessions.put(sessionId, "CANCELLED");

        return IngestionResponse.builder()
                .sessionId(sessionId)
                .status("CANCELLED")
                .message("Ingestion session cancelled")
                .build();
    }

    /**
     * Perform data ingestion asynchronously.
     *
     * @param sessionId Session identifier
     * @param request Ingestion configuration
     */
    @Async
    protected void performIngestionAsync(String sessionId, IngestionRequest request) {
        try {
            log.info("[{}] Starting data ingestion pipeline", sessionId);

            // Stage 1: Generate patients (0-40%)
            progressTrackingService.updateProgress(sessionId, "GENERATING", 0);
            Bundle cohortBundle = patientGenerator.generateCohort(
                    request.getPatientCount(),
                    request.getTenantId()
            );
            List<Bundle> patientBundles = List.of(cohortBundle);
            progressTrackingService.updateProgress(sessionId, "GENERATING", 40);
            log.info("[{}] Generated {} patient bundle(s)", sessionId, patientBundles.size());

            // Stage 2: Persist to FHIR (40-70%)
            progressTrackingService.updateProgress(sessionId, "PERSISTING", 40);
            int persistedCount = 0;
            for (Bundle bundle : patientBundles) {
                // Check for cancellation
                if (progressTrackingService.isCancelled(sessionId)) {
                    log.info("[{}] Cancellation requested, stopping ingestion", sessionId);
                    return;
                }

                fhirClient.persistBundle(bundle, request.getTenantId());
                persistedCount++;
                progressTrackingService.incrementPersisted(sessionId);

                // Update progress incrementally
                int progressPercent = 40 + (int) ((persistedCount / (double) patientBundles.size()) * 30);
                progressTrackingService.updateProgress(sessionId, "PERSISTING", progressPercent);
            }
            log.info("[{}] Persisted {} patients to FHIR service", sessionId, persistedCount);

            // Stage 3: Create care gaps (70-90%) - Optional
            if (request.getIncludeCareGaps()) {
                progressTrackingService.updateProgress(sessionId, "CARE_GAPS", 70);
                int gapsCreated = careGapClient.createCareGaps(request.getTenantId());
                log.info("[{}] Created {} care gaps for tenant {}", sessionId, gapsCreated, request.getTenantId());
                progressTrackingService.updateProgress(sessionId, "CARE_GAPS", 90);
            }

            // Stage 4: Seed measures (90-100%) - Optional
            if (request.getIncludeQualityMeasures()) {
                progressTrackingService.updateProgress(sessionId, "MEASURES", 90);
                int measuresSeeded = qualityMeasureClient.seedMeasures(request.getTenantId());
                log.info("[{}] Seeded {} quality measures for tenant {}", sessionId, measuresSeeded, request.getTenantId());
                progressTrackingService.updateProgress(sessionId, "MEASURES", 100);
            }

            // Complete
            progressTrackingService.completeSession(sessionId);
            activeSessions.put(sessionId, "COMPLETED");
            log.info("[{}] Data ingestion pipeline completed successfully", sessionId);

        } catch (Exception e) {
            log.error("[{}] Data ingestion failed", sessionId, e);
            progressTrackingService.failSession(sessionId, e.getMessage());
            activeSessions.put(sessionId, "FAILED");
        }
    }
}
