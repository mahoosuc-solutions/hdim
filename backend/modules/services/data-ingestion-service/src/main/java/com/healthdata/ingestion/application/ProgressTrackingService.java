package com.healthdata.ingestion.application;

import com.healthdata.ingestion.api.v1.dto.IngestionProgressResponse;
import com.healthdata.ingestion.api.v1.dto.IngestionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks ingestion progress across multiple sessions.
 *
 * Maintains in-memory state for active sessions with real-time updates.
 * Provides progress metrics, aggregate statistics, audit trail access,
 * and service impact analysis.
 */
@Service
@Slf4j
public class ProgressTrackingService {

    private final Map<String, SessionProgress> sessions = new ConcurrentHashMap<>();

    /**
     * Initialize a new ingestion session with starting parameters.
     */
    public void initializeSession(String sessionId, IngestionRequest request) {
        SessionProgress progress = new SessionProgress();
        progress.sessionId = sessionId;
        progress.tenantId = request.getTenantId();
        progress.targetPatientCount = request.getPatientCount();
        progress.scenario = request.getScenario();
        progress.includeCareGaps = request.getIncludeCareGaps();
        progress.includeQualityMeasures = request.getIncludeQualityMeasures();
        progress.status = "INITIALIZING";
        progress.progressPercent = 0;
        progress.currentStage = "INITIALIZING";
        progress.startTimeMs = System.currentTimeMillis();

        sessions.put(sessionId, progress);
        log.info("Initialized session {}: {} patients for tenant {}", sessionId, request.getPatientCount(), request.getTenantId());
    }

    /**
     * Update session progress percentage (0-100).
     */
    public void updateProgress(String sessionId, int progressPercent) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.progressPercent = Math.min(100, Math.max(0, progressPercent));
            progress.lastUpdateMs = System.currentTimeMillis();
        }
    }

    /**
     * Update current pipeline stage.
     */
    public void updateStage(String sessionId, String stage, int progressPercent) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.currentStage = stage;
            progress.progressPercent = progressPercent;
            progress.status = "RUNNING";
            progress.lastUpdateMs = System.currentTimeMillis();
            log.info("Session {} entering stage: {} ({}%)", sessionId, stage, progressPercent);
        }
    }

    /**
     * Increment persisted patient count.
     */
    public void incrementPersisted(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.patientsPersisted++;
            progress.lastUpdateMs = System.currentTimeMillis();
        }
    }

    /**
     * Set care gaps created count.
     */
    public void setCareGapsCreated(String sessionId, int count) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.careGapsCreated = count;
            progress.lastUpdateMs = System.currentTimeMillis();
        }
    }

    /**
     * Set measures seeded count.
     */
    public void setMeasuresSeeded(String sessionId, int count) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.measuresSeeded = count;
            progress.lastUpdateMs = System.currentTimeMillis();
        }
    }

    /**
     * Mark session as completed successfully.
     */
    public void completeSession(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.status = "COMPLETED";
            progress.progressPercent = 100;
            progress.currentStage = "COMPLETE";
            progress.endTimeMs = System.currentTimeMillis();
            log.info("Session {} completed in {} ms", sessionId, progress.endTimeMs - progress.startTimeMs);
        }
    }

    /**
     * Mark session as failed with error message.
     */
    public void failSession(String sessionId, String errorMessage) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.status = "FAILED";
            progress.errorMessage = errorMessage;
            progress.endTimeMs = System.currentTimeMillis();
            log.error("Session {} failed: {}", sessionId, errorMessage);
        }
    }

    /**
     * Cancel session (user-initiated).
     */
    public void cancelSession(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.status = "CANCELLED";
            progress.endTimeMs = System.currentTimeMillis();
            log.info("Session {} cancelled by user", sessionId);
        }
    }

    /**
     * Get current progress for a session.
     */
    public IngestionProgressResponse getProgress(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        long elapsedMs = progress.endTimeMs != null
                ? progress.endTimeMs - progress.startTimeMs
                : System.currentTimeMillis() - progress.startTimeMs;

        return IngestionProgressResponse.builder()
                .sessionId(progress.sessionId)
                .status(progress.status)
                .progressPercent(progress.progressPercent)
                .patientsGenerated((long) progress.targetPatientCount)
                .patientsPersisted((long) progress.patientsPersisted)
                .careGapsCreated((long) progress.careGapsCreated)
                .measuresSeeded((long) progress.measuresSeeded)
                .startTime(Instant.ofEpochMilli(progress.startTimeMs))
                .elapsedTimeMs(elapsedMs)
                .currentStage(progress.currentStage)
                .errorMessage(progress.errorMessage)
                .build();
    }

    /**
     * Get aggregate metrics across all sessions.
     */
    public Map<String, Object> getAggregateMetrics() {
        long totalSessions = sessions.size();
        long completedSessions = sessions.values().stream()
                .filter(s -> "COMPLETED".equals(s.status))
                .count();
        long failedSessions = sessions.values().stream()
                .filter(s -> "FAILED".equals(s.status))
                .count();
        long totalPatients = sessions.values().stream()
                .mapToLong(s -> s.patientsPersisted)
                .sum();
        long totalCareGaps = sessions.values().stream()
                .mapToLong(s -> s.careGapsCreated)
                .sum();

        double avgProcessingTimeMs = sessions.values().stream()
                .filter(s -> s.endTimeMs != null)
                .mapToLong(s -> s.endTimeMs - s.startTimeMs)
                .average()
                .orElse(0.0);

        return Map.of(
                "totalSessions", totalSessions,
                "completedSessions", completedSessions,
                "failedSessions", failedSessions,
                "activeSessions", totalSessions - completedSessions - failedSessions,
                "totalPatientsPersisted", totalPatients,
                "totalCareGapsCreated", totalCareGaps,
                "averageProcessingTimeMs", avgProcessingTimeMs,
                "successRate", totalSessions > 0 ? (double) completedSessions / totalSessions : 0.0
        );
    }

    /**
     * Get audit trail for a session.
     *
     * TODO: Integrate with actual audit service for persistent audit logs.
     * Currently returns session metadata.
     */
    public Map<String, Object> getAuditTrail(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        return Map.of(
                "sessionId", progress.sessionId,
                "tenantId", progress.tenantId,
                "scenario", progress.scenario,
                "startTime", Instant.ofEpochMilli(progress.startTimeMs).toString(),
                "status", progress.status,
                "patientsGenerated", progress.targetPatientCount,
                "patientsPersisted", progress.patientsPersisted,
                "careGapsCreated", progress.careGapsCreated,
                "measuresSeeded", progress.measuresSeeded,
                "note", "Full audit trail integration pending - this shows session metadata"
        );
    }

    /**
     * Get service impact analysis for a session.
     *
     * Shows which microservices were called during ingestion.
     */
    public Map<String, Object> getServiceImpact(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        return Map.of(
                "sessionId", sessionId,
                "servicesImpacted", Map.of(
                        "fhir-service", Map.of(
                                "calls", progress.patientsPersisted,
                                "endpoint", "POST /fhir/Bundle"
                        ),
                        "care-gap-service", Map.of(
                                "calls", progress.includeCareGaps ? 1 : 0,
                                "endpoint", "POST /care-gap/"
                        ),
                        "quality-measure-service", Map.of(
                                "calls", progress.includeQualityMeasures ? 1 : 0,
                                "endpoint", "POST /quality-measure/api/v1/measures/seed"
                        )
                ),
                "totalServiceCalls", progress.patientsPersisted
                        + (progress.includeCareGaps ? 1 : 0)
                        + (progress.includeQualityMeasures ? 1 : 0)
        );
    }

    /**
     * Get Jaeger trace information for a session.
     *
     * TODO: Integrate with actual OpenTelemetry to get real trace IDs.
     * Currently returns placeholder.
     */
    public Map<String, String> getTraceInfo(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        return Map.of(
                "sessionId", sessionId,
                "traceId", "placeholder-trace-" + sessionId.substring(0, 8),
                "jaegerUrl", "http://localhost:16686/trace/placeholder-trace-" + sessionId.substring(0, 8),
                "note", "OpenTelemetry trace integration pending"
        );
    }

    /**
     * Internal session state tracker.
     */
    private static class SessionProgress {
        String sessionId;
        String tenantId;
        String scenario;
        int targetPatientCount;
        boolean includeCareGaps;
        boolean includeQualityMeasures;
        String status;
        int progressPercent;
        String currentStage;
        int patientsPersisted;
        int careGapsCreated;
        int measuresSeeded;
        long startTimeMs;
        Long endTimeMs;
        Long lastUpdateMs;
        String errorMessage;
    }
}
