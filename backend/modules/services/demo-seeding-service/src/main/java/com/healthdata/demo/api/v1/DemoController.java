package com.healthdata.demo.api.v1;

import com.healthdata.demo.api.v1.dto.*;
import com.healthdata.demo.application.DemoProgressService;
import com.healthdata.demo.application.DemoResetService;
import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.ScenarioLoaderService;
import com.healthdata.demo.domain.model.DemoScenario;
import com.healthdata.demo.domain.model.DemoSession;
import com.healthdata.demo.domain.model.DemoSessionProgress;
import com.healthdata.demo.domain.model.DemoSnapshot;
import com.healthdata.demo.domain.repository.DemoSessionRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API for demo platform operations.
 *
 * Endpoints:
 * - POST /api/v1/demo/reset - Full reset
 * - POST /api/v1/demo/scenarios/{name} - Load scenario
 * - GET /api/v1/demo/scenarios - List scenarios
 * - POST /api/v1/demo/patients/generate - Generate patients
 * - GET /api/v1/demo/status - Demo status
 * - Snapshot operations
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    private final DemoSeedingService seedingService;
    private final ScenarioLoaderService scenarioLoaderService;
    private final DemoResetService resetService;
    private final DemoSessionRepository sessionRepository;
    private final DemoProgressService progressService;

    public DemoController(
            DemoSeedingService seedingService,
            ScenarioLoaderService scenarioLoaderService,
            DemoResetService resetService,
            DemoSessionRepository sessionRepository,
            DemoProgressService progressService) {
        this.seedingService = seedingService;
        this.scenarioLoaderService = scenarioLoaderService;
        this.resetService = resetService;
        this.sessionRepository = sessionRepository;
        this.progressService = progressService;
    }

    /**
     * Get demo platform status.
     */
    @GetMapping("/status")
    public ResponseEntity<DemoStatusResponse> getStatus() {
        logger.info("GET /api/v1/demo/status");

        DemoSeedingService.DemoStatus status = seedingService.getDemoStatus();

        DemoStatusResponse response = new DemoStatusResponse();
        response.setReady(status.isReady());
        response.setScenarioCount(status.getScenarioCount());
        response.setTemplateCount(status.getTemplateCount());
        response.setCurrentSessionId(status.getCurrentSessionId() != null ?
            status.getCurrentSessionId().toString() : null);
        response.setCurrentScenario(status.getCurrentScenario());
        response.setSessionStatus(status.getSessionStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Full reset of demo data.
     */
    @PostMapping("/reset")
    public ResponseEntity<ResetResponse> fullReset() {
        logger.info("POST /api/v1/demo/reset");

        DemoResetService.ResetResult result = resetService.fullReset();

        ResetResponse response = new ResetResponse();
        response.setSuccess(result.isSuccess());
        response.setPatientsDeleted(result.getPatientsDeleted());
        response.setConditionsDeleted(result.getConditionsDeleted());
        response.setCareGapsDeleted(result.getCareGapsDeleted());
        response.setResetTimeMs(result.getResetTimeMs());
        response.setErrorMessage(result.getErrorMessage());

        return result.isSuccess() ?
            ResponseEntity.ok(response) :
            ResponseEntity.internalServerError().body(response);
    }

    /**
     * Reset demo data for the current scenario's tenant.
     */
    @PostMapping("/reset/current-tenant")
    public ResponseEntity<ResetResponse> resetCurrentTenant() {
        logger.info("POST /api/v1/demo/reset/current-tenant");

        var sessionOpt = sessionRepository.findCurrentSession();
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DemoSession session = sessionOpt.get();
        if (session.getScenario() == null) {
            return ResponseEntity.badRequest().build();
        }
        resetService.resetDemoData(session.getScenario().getTenantId());
        session.reset();
        sessionRepository.save(session);

        ResetResponse response = new ResetResponse();
        response.setSuccess(true);
        response.setResetTimeMs(0);
        return ResponseEntity.ok(response);
    }

    /**
     * List available scenarios.
     */
    @GetMapping("/scenarios")
    public ResponseEntity<List<ScenarioResponse>> listScenarios() {
        logger.info("GET /api/v1/demo/scenarios");

        List<DemoScenario> scenarios = seedingService.getAvailableScenarios();

        List<ScenarioResponse> response = scenarios.stream()
            .map(this::toScenarioResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Load a specific scenario.
     */
    @PostMapping("/scenarios/{scenarioName}")
    public ResponseEntity<LoadScenarioResponse> loadScenario(
            @PathVariable String scenarioName) {
        logger.info("POST /api/v1/demo/scenarios/{}", scenarioName);

        ScenarioLoaderService.LoadResult result = scenarioLoaderService.loadScenario(scenarioName);

        LoadScenarioResponse response = new LoadScenarioResponse();
        response.setScenarioName(result.getScenarioName());
        response.setSessionId(result.getSessionId());
        response.setPatientCount(result.getPatientCount());
        response.setCareGapCount(result.getCareGapCount());
        response.setLoadTimeMs(result.getLoadTimeMs());
        response.setSuccess(result.isSuccess());
        response.setErrorMessage(result.getErrorMessage());

        return result.isSuccess() ?
            ResponseEntity.ok(response) :
            ResponseEntity.badRequest().body(response);
    }

    /**
     * Get current scenario info.
     */
    @GetMapping("/scenarios/current")
    public ResponseEntity<CurrentScenarioResponse> getCurrentScenario() {
        logger.info("GET /api/v1/demo/scenarios/current");

        return scenarioLoaderService.getCurrentScenario()
            .map(info -> {
                CurrentScenarioResponse response = new CurrentScenarioResponse();
                response.setName(info.getName());
                response.setDisplayName(info.getDisplayName());
                response.setDescription(info.getDescription());
                response.setPatientCount(info.getPatientCount());
                response.setSessionId(info.getSessionId());
                response.setStatus(info.getStatus());
                response.setStartedAt(info.getStartedAt());
                response.setLastResetAt(info.getLastResetAt());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.<Void>notFound().build());
    }

    /**
     * Reload current scenario (quick reset).
     */
    @PostMapping("/scenarios/reload")
    public ResponseEntity<LoadScenarioResponse> reloadScenario() {
        logger.info("POST /api/v1/demo/scenarios/reload");

        ScenarioLoaderService.LoadResult result = scenarioLoaderService.reloadCurrentScenario();

        LoadScenarioResponse response = new LoadScenarioResponse();
        response.setScenarioName(result.getScenarioName());
        response.setSessionId(result.getSessionId());
        response.setPatientCount(result.getPatientCount());
        response.setCareGapCount(result.getCareGapCount());
        response.setLoadTimeMs(result.getLoadTimeMs());
        response.setSuccess(result.isSuccess());
        response.setErrorMessage(result.getErrorMessage());

        return result.isSuccess() ?
            ResponseEntity.ok(response) :
            ResponseEntity.badRequest().body(response);
    }

    /**
     * Get progress for a specific session.
     */
    @GetMapping("/sessions/{sessionId}/progress")
    public ResponseEntity<ProgressResponse> getProgress(@PathVariable String sessionId) {
        logger.info("GET /api/v1/demo/sessions/{}/progress", sessionId);
        return progressService.getBySessionId(UUID.fromString(sessionId))
            .map(progress -> ResponseEntity.ok(toProgressResponse(progress)))
            .orElse(ResponseEntity.<Void>notFound().build());
    }

    /**
     * Get progress for the current session.
     */
    @GetMapping("/sessions/current/progress")
    public ResponseEntity<ProgressResponse> getCurrentProgress() {
        logger.info("GET /api/v1/demo/sessions/current/progress");
        var sessionOpt = sessionRepository.findCurrentSession();
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return progressService.getBySessionId(sessionOpt.get().getId())
            .map(progress -> ResponseEntity.ok(toProgressResponse(progress)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request cancellation of the current session load.
     */
    @PostMapping("/sessions/current/cancel")
    public ResponseEntity<Void> cancelCurrentSession() {
        logger.info("POST /api/v1/demo/sessions/current/cancel");
        var sessionOpt = sessionRepository.findCurrentSession();
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        progressService.requestCancel(sessionOpt.get().getId(), "Cancellation requested");
        return ResponseEntity.accepted().build();
    }

    /**
     * Stop the current session.
     */
    @PostMapping("/sessions/current/stop")
    public ResponseEntity<Void> stopCurrentSession() {
        logger.info("POST /api/v1/demo/sessions/current/stop");
        var sessionOpt = sessionRepository.findCurrentSession();
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DemoSession session = sessionOpt.get();
        progressService.getBySessionId(session.getId()).ifPresent(progress -> {
            String stage = progress.getStage();
            if (!"COMPLETE".equals(stage) && !"FAILED".equals(stage) && !"CANCELLED".equals(stage)) {
                progressService.markCancelled(session.getId(), "Session stopped");
            }
        });
        session.end();
        sessionRepository.save(session);
        return ResponseEntity.ok().build();
    }

    /**
     * Generate synthetic patients.
     */
    @PostMapping("/patients/generate")
    public ResponseEntity<GeneratePatientsResponse> generatePatients(
            @Valid @RequestBody GeneratePatientsRequest request) {
        logger.info("POST /api/v1/demo/patients/generate - count={}, tenant={}",
            request.getCount(), request.getTenantId());

        DemoSeedingService.GenerationResult result = seedingService.generatePatientCohort(
            request.getCount(),
            request.getTenantId(),
            request.getCareGapPercentage() != null ? request.getCareGapPercentage() : 28
        );

        GeneratePatientsResponse response = new GeneratePatientsResponse();
        response.setTenantId(result.getTenantId());
        response.setPatientCount(result.getPatientCount());
        response.setCareGapCount(result.getCareGapCount());
        response.setMedicationCount(result.getMedicationCount());
        response.setObservationCount(result.getObservationCount());
        response.setEncounterCount(result.getEncounterCount());
        response.setProcedureCount(result.getProcedureCount());
        response.setGenerationTimeMs(result.getGenerationTimeMs());
        response.setSuccess(result.isSuccess());
        response.setErrorMessage(result.getErrorMessage());

        return result.isSuccess() ?
            ResponseEntity.ok(response) :
            ResponseEntity.internalServerError().body(response);
    }

    /**
     * Initialize scenarios and templates.
     */
    @PostMapping("/initialize")
    public ResponseEntity<InitializeResponse> initialize() {
        logger.info("POST /api/v1/demo/initialize");

        try {
            seedingService.initializeScenarios();
            seedingService.seedPatientTemplates("demo");

            InitializeResponse response = new InitializeResponse();
            response.setSuccess(true);
            response.setMessage("Demo platform initialized successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to initialize demo platform", e);
            InitializeResponse response = new InitializeResponse();
            response.setSuccess(false);
            response.setMessage("Failed to initialize: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Snapshot endpoints

    /**
     * List snapshots.
     */
    @GetMapping("/snapshots")
    public ResponseEntity<List<SnapshotResponse>> listSnapshots() {
        logger.info("GET /api/v1/demo/snapshots");

        List<DemoSnapshot> snapshots = resetService.listSnapshots();

        List<SnapshotResponse> response = snapshots.stream()
            .map(this::toSnapshotResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Create a snapshot.
     */
    @PostMapping("/snapshots")
    public ResponseEntity<SnapshotResponse> createSnapshot(
            @Valid @RequestBody CreateSnapshotRequest request) {
        logger.info("POST /api/v1/demo/snapshots - name={}", request.getName());

        try {
            DemoSnapshot snapshot = resetService.createSnapshot(
                request.getName(),
                request.getDescription()
            );

            return ResponseEntity.ok(toSnapshotResponse(snapshot));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to create snapshot", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Restore a snapshot.
     */
    @PostMapping("/snapshots/{snapshotId}/restore")
    public ResponseEntity<RestoreSnapshotResponse> restoreSnapshot(
            @PathVariable UUID snapshotId) {
        logger.info("POST /api/v1/demo/snapshots/{}/restore", snapshotId);

        DemoResetService.RestoreResult result = resetService.restoreSnapshot(snapshotId);

        RestoreSnapshotResponse response = new RestoreSnapshotResponse();
        response.setSnapshotName(result.getSnapshotName());
        response.setSuccess(result.isSuccess());
        response.setRestoreTimeMs(result.getRestoreTimeMs());
        response.setErrorMessage(result.getErrorMessage());

        return result.isSuccess() ?
            ResponseEntity.ok(response) :
            ResponseEntity.internalServerError().body(response);
    }

    /**
     * Restore a snapshot by name.
     */
    @PostMapping("/snapshots/restore/{name}")
    public ResponseEntity<RestoreSnapshotResponse> restoreSnapshotByName(
            @PathVariable String name) {
        logger.info("POST /api/v1/demo/snapshots/restore/{}", name);

        DemoResetService.RestoreResult result = resetService.restoreSnapshotByName(name);

        RestoreSnapshotResponse response = new RestoreSnapshotResponse();
        response.setSnapshotName(result.getSnapshotName());
        response.setSuccess(result.isSuccess());
        response.setRestoreTimeMs(result.getRestoreTimeMs());
        response.setErrorMessage(result.getErrorMessage());

        return result.isSuccess() ?
            ResponseEntity.ok(response) :
            ResponseEntity.badRequest().body(response);
    }

    /**
     * Delete a snapshot.
     */
    @DeleteMapping("/snapshots/{name}")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable String name) {
        logger.info("DELETE /api/v1/demo/snapshots/{}", name);

        try {
            resetService.deleteSnapshot(name);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to delete snapshot", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods

    private ScenarioResponse toScenarioResponse(DemoScenario scenario) {
        ScenarioResponse response = new ScenarioResponse();
        response.setId(scenario.getId().toString());
        response.setName(scenario.getName());
        response.setDisplayName(scenario.getDisplayName());
        response.setDescription(scenario.getDescription());
        response.setScenarioType(scenario.getScenarioType().name());
        response.setPatientCount(scenario.getPatientCount());
        response.setTenantId(scenario.getTenantId());
        response.setEstimatedLoadTimeSeconds(scenario.getEstimatedLoadTimeSeconds());
        response.setActive(scenario.getIsActive());
        return response;
    }

    private SnapshotResponse toSnapshotResponse(DemoSnapshot snapshot) {
        SnapshotResponse response = new SnapshotResponse();
        response.setId(snapshot.getId().toString());
        response.setName(snapshot.getName());
        response.setDescription(snapshot.getDescription());
        response.setFileSizeBytes(snapshot.getFileSizeBytes());
        response.setStatus(snapshot.getStatus().name());
        response.setCreatedAt(snapshot.getCreatedAt());
        response.setCreatedBy(snapshot.getCreatedBy());
        response.setLastRestoredAt(snapshot.getLastRestoredAt());
        response.setRestoreCount(snapshot.getRestoreCount());
        return response;
    }

    private ProgressResponse toProgressResponse(DemoSessionProgress progress) {
        ProgressResponse response = new ProgressResponse();
        response.setSessionId(progress.getSessionId().toString());
        response.setScenarioName(progress.getScenarioName());
        response.setTenantId(progress.getTenantId());
        response.setStage(progress.getStage());
        response.setProgressPercent(progress.getProgressPercent());
        response.setPatientsGenerated(progress.getPatientsGenerated());
        response.setPatientsPersisted(progress.getPatientsPersisted());
        response.setCareGapsCreated(progress.getCareGapsCreated());
        response.setMeasuresSeeded(progress.getMeasuresSeeded());
        response.setMessage(progress.getMessage());
        response.setUpdatedAt(progress.getUpdatedAt() != null ? progress.getUpdatedAt().toString() : null);
        response.setCancelRequested(progress.isCancelRequested());
        return response;
    }
}
