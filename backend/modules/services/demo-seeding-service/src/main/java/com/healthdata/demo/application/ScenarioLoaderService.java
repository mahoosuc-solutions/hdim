package com.healthdata.demo.application;

import com.healthdata.demo.domain.model.DemoScenario;
import com.healthdata.demo.domain.model.DemoSession;
import com.healthdata.demo.domain.repository.DemoScenarioRepository;
import com.healthdata.demo.domain.repository.DemoSessionRepository;
import com.healthdata.demo.strategy.MultiTenantStrategy;
import com.healthdata.demo.strategy.ScenarioSeedingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for loading demo scenarios.
 *
 * Responsibilities:
 * - Load specific scenarios by name
 * - Manage demo session lifecycle
 * - Coordinate with seeding service for data population
 */
@Service
public class ScenarioLoaderService {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioLoaderService.class);

    private final DemoScenarioRepository scenarioRepository;
    private final DemoSessionRepository sessionRepository;
    private final DemoSeedingService seedingService;
    private final DemoResetService resetService;
    private final DemoProgressService progressService;
    private final MultiTenantStrategy multiTenantStrategy;

    public ScenarioLoaderService(
            DemoScenarioRepository scenarioRepository,
            DemoSessionRepository sessionRepository,
            DemoSeedingService seedingService,
            DemoResetService resetService,
            DemoProgressService progressService,
            MultiTenantStrategy multiTenantStrategy) {
        this.scenarioRepository = scenarioRepository;
        this.sessionRepository = sessionRepository;
        this.seedingService = seedingService;
        this.resetService = resetService;
        this.progressService = progressService;
        this.multiTenantStrategy = multiTenantStrategy;
    }

    /**
     * Load a scenario by name.
     *
     * @param scenarioName The scenario name (e.g., "hedis-evaluation")
     * @return LoadResult with session details
     */
    public LoadResult loadScenario(String scenarioName) {
        logger.info("Loading scenario: {}", scenarioName);
        long startTime = System.currentTimeMillis();

        LoadResult result = new LoadResult();
        result.setScenarioName(scenarioName);

        try {
            // Find the scenario
            DemoScenario scenario = scenarioRepository.findByName(scenarioName)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioName));

            // End any current session
            endCurrentSession();

            // Reset demo data before loading new scenario
            if (scenario.getScenarioType() == DemoScenario.ScenarioType.MULTI_TENANT) {
                List<String> tenantIds = multiTenantStrategy.getTenantIds();
                for (String tenantId : tenantIds) {
                    resetService.resetDemoData(tenantId);
                }
            } else {
                resetService.resetDemoData(scenario.getTenantId());
            }

            // Create new session
            DemoSession session = new DemoSession(scenario, "Demo session for " + scenario.getDisplayName());
            session = sessionRepository.save(session);
            progressService.createForSession(session, scenario);

            // Generate demo data for the scenario
            progressService.updateStage(session.getId(), DemoProgressService.Stage.RESETTING, 10,
                "Resetting tenant data");

            if (scenario.getScenarioType() == DemoScenario.ScenarioType.MULTI_TENANT) {
                progressService.updateStage(session.getId(), DemoProgressService.Stage.GENERATING_PATIENTS, 25,
                    "Seeding multi-tenant data");
                ScenarioSeedingStrategy.SeedingResult seedResult = multiTenantStrategy.seedScenario(scenario.getTenantId());

                if (!seedResult.isSuccess()) {
                    progressService.markFailed(session.getId(), seedResult.getErrorMessage());
                    result.setSuccess(false);
                    result.setErrorMessage(seedResult.getErrorMessage());
                    return result;
                }

                result.setPatientCount(seedResult.getPatientsCreated());
                result.setCareGapCount(seedResult.getCareGapsExpected());
            } else {
                DemoSeedingService.GenerationResult genResult = seedingService.generatePatientCohort(
                    scenario.getPatientCount(),
                    scenario.getTenantId(),
                    calculateCareGapPercentage(scenario),
                    session.getId()
                );

                if (!genResult.isSuccess()) {
                    result.setSuccess(false);
                    result.setErrorMessage(genResult.getErrorMessage());
                    return result;
                }

                result.setPatientCount(genResult.getPatientCount());
                result.setCareGapCount(genResult.getCareGapCount());
            }

            // Mark session as ready
            session.markReady();
            sessionRepository.save(session);

            result.setSuccess(true);
            result.setSessionId(session.getId().toString());
            result.setLoadTimeMs(System.currentTimeMillis() - startTime);
            progressService.updateStage(session.getId(), DemoProgressService.Stage.COMPLETE, 100,
                "Scenario loaded");

            logger.info("Scenario loaded successfully: {} ({} patients, {} care gaps) in {}ms",
                scenarioName, result.getPatientCount(), result.getCareGapCount(), result.getLoadTimeMs());

        } catch (Exception e) {
            logger.error("Failed to load scenario: {}", scenarioName, e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Get the currently loaded scenario.
     */
    @Transactional(readOnly = true)
    public Optional<ScenarioInfo> getCurrentScenario() {
        return sessionRepository.findCurrentSession()
            .map(session -> {
                ScenarioInfo info = new ScenarioInfo();
                if (session.getScenario() != null) {
                    info.setName(session.getScenario().getName());
                    info.setDisplayName(session.getScenario().getDisplayName());
                    info.setDescription(session.getScenario().getDescription());
                    info.setPatientCount(session.getScenario().getPatientCount());
                }
                info.setSessionId(session.getId().toString());
                info.setStatus(session.getStatus().name());
                info.setStartedAt(session.getStartedAt());
                info.setLastResetAt(session.getLastResetAt());
                return info;
            });
    }

    /**
     * Reload the current scenario (quick reset).
     */
    public LoadResult reloadCurrentScenario() {
        return sessionRepository.findCurrentSession()
            .map(session -> {
                if (session.getScenario() != null) {
                    return loadScenario(session.getScenario().getName());
                }
                LoadResult result = new LoadResult();
                result.setSuccess(false);
                result.setErrorMessage("No active scenario to reload");
                return result;
            })
            .orElseGet(() -> {
                LoadResult result = new LoadResult();
                result.setSuccess(false);
                result.setErrorMessage("No active session found");
                return result;
            });
    }

    /**
     * End the current demo session.
     */
    @Transactional
    public void endCurrentSession() {
        sessionRepository.findCurrentSession().ifPresent(session -> {
            session.end();
            sessionRepository.save(session);
            logger.info("Ended session: {}", session.getId());
        });
    }

    /**
     * Calculate care gap percentage based on scenario type.
     */
    private int calculateCareGapPercentage(DemoScenario scenario) {
        return switch (scenario.getScenarioType()) {
            case HEDIS_EVALUATION -> 28; // ~28% care gap rate for demo impact
            case PATIENT_JOURNEY -> 40;  // Higher for patient journey demos
            case RISK_STRATIFICATION -> 35;
            case MULTI_TENANT -> 25;
            case CUSTOM -> 30;
        };
    }

    // Result classes

    public static class LoadResult {
        private String scenarioName;
        private String sessionId;
        private int patientCount;
        private int careGapCount;
        private long loadTimeMs;
        private boolean success;
        private String errorMessage;

        // Getters and setters
        public String getScenarioName() { return scenarioName; }
        public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
        public int getCareGapCount() { return careGapCount; }
        public void setCareGapCount(int careGapCount) { this.careGapCount = careGapCount; }
        public long getLoadTimeMs() { return loadTimeMs; }
        public void setLoadTimeMs(long loadTimeMs) { this.loadTimeMs = loadTimeMs; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class ScenarioInfo {
        private String name;
        private String displayName;
        private String description;
        private int patientCount;
        private String sessionId;
        private String status;
        private java.time.Instant startedAt;
        private java.time.Instant lastResetAt;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getPatientCount() { return patientCount; }
        public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.Instant getStartedAt() { return startedAt; }
        public void setStartedAt(java.time.Instant startedAt) { this.startedAt = startedAt; }
        public java.time.Instant getLastResetAt() { return lastResetAt; }
        public void setLastResetAt(java.time.Instant lastResetAt) { this.lastResetAt = lastResetAt; }
    }
}
