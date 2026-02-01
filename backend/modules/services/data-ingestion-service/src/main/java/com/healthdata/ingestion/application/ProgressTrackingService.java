package com.healthdata.ingestion.application;

import com.healthdata.ingestion.api.v1.IngestionProgressResponse;
import com.healthdata.ingestion.api.v1.IngestionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking ingestion progress in real-time.
 */
@Service
@Slf4j
public class ProgressTrackingService {

    private final Map<String, SessionProgress> sessions = new ConcurrentHashMap<>();

    public void initializeSession(String sessionId, IngestionRequest request) {
        SessionProgress progress = new SessionProgress();
        progress.sessionId = sessionId;
        progress.tenantId = request.getTenantId();
        progress.status = "INITIALIZING";
        progress.progressPercent = 0;
        progress.patientsGenerated = 0L;
        progress.patientsPersisted = 0L;
        progress.careGapsCreated = 0L;
        progress.measuresSeeded = 0L;
        progress.startTimeMs = System.currentTimeMillis();
        progress.currentStage = "INITIALIZING";
        progress.cancelled = false;

        sessions.put(sessionId, progress);
    }

    public void updateProgress(String sessionId, String stage, int percent) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.currentStage = stage;
            progress.progressPercent = percent;
        }
    }

    public void incrementPersisted(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.patientsPersisted++;
        }
    }

    public void completeSession(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.status = "COMPLETED";
            progress.progressPercent = 100;
            progress.currentStage = "COMPLETE";
        }
    }

    public void failSession(String sessionId, String errorMessage) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.status = "FAILED";
            progress.currentStage = "FAILED: " + errorMessage;
        }
    }

    public void cancelSession(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress != null) {
            progress.cancelled = true;
        }
    }

    public boolean isCancelled(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        return progress != null && progress.cancelled;
    }

    public IngestionProgressResponse getProgress(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress == null) {
            return IngestionProgressResponse.builder()
                    .sessionId(sessionId)
                    .status("NOT_FOUND")
                    .build();
        }

        long elapsedMs = System.currentTimeMillis() - progress.startTimeMs;

        return IngestionProgressResponse.builder()
                .sessionId(progress.sessionId)
                .status(progress.status)
                .progressPercent(progress.progressPercent)
                .patientsGenerated(progress.patientsGenerated)
                .patientsPersisted(progress.patientsPersisted)
                .careGapsCreated(progress.careGapsCreated)
                .measuresSeeded(progress.measuresSeeded)
                .startTimeMs(progress.startTimeMs)
                .elapsedTimeMs(elapsedMs)
                .currentStage(progress.currentStage)
                .build();
    }

    public Map<String, Object> getAuditTrail(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress == null) {
            return Map.of();
        }

        return Map.of(
                "tenantId", progress.tenantId,
                "sessionId", progress.sessionId,
                "events", List.of(
                        Map.of(
                                "event", "SESSION_STARTED",
                                "timestamp", progress.startTimeMs
                        )
                )
        );
    }

    public Map<String, Object> getServiceImpact(String sessionId) {
        SessionProgress progress = sessions.get(sessionId);
        if (progress == null) {
            return Map.of("totalServiceCalls", 0);
        }

        long totalServiceCalls = progress.patientsPersisted
                + progress.careGapsCreated
                + progress.measuresSeeded;

        return Map.of(
                "totalServiceCalls", (int) totalServiceCalls,
                "patientsPersisted", progress.patientsPersisted,
                "careGapsCreated", progress.careGapsCreated,
                "measuresSeeded", progress.measuresSeeded
        );
    }

    private static class SessionProgress {
        String sessionId;
        String tenantId;
        String status;
        Integer progressPercent;
        Long patientsGenerated;
        Long patientsPersisted;
        Long careGapsCreated;
        Long measuresSeeded;
        Long startTimeMs;
        String currentStage;
        boolean cancelled;
    }
}
