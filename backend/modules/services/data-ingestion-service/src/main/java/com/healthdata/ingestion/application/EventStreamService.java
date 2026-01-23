package com.healthdata.ingestion.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events (SSE) streaming service for real-time ingestion visibility.
 *
 * Provides live event streaming to connected clients for customer demonstrations.
 * Shows data flowing through the platform in real-time with visibility into:
 * - Patient generation and persistence
 * - Care gap creation
 * - Quality measure seeding
 * - Service interactions
 * - Audit logging
 * - Pipeline stage transitions
 *
 * Event types:
 * - SESSION_STARTED: Ingestion pipeline initiated
 * - STAGE_STARTED: New pipeline stage beginning
 * - STAGE_COMPLETED: Pipeline stage finished
 * - PATIENT_GENERATED: Synthetic patient created
 * - PATIENT_PERSISTED: Patient saved to FHIR service
 * - CARE_GAP_CREATED: Care gap identified for patient
 * - MEASURE_SEEDED: Quality measure seeded
 * - AUDIT_LOGGED: Operation recorded in audit database
 * - VALIDATION_PASSED: Validation check succeeded
 * - ERROR: Error occurred during ingestion
 * - SESSION_COMPLETED: Ingestion pipeline completed successfully
 * - SESSION_FAILED: Ingestion pipeline failed
 */
@Service
@Slf4j
public class EventStreamService {

    // Map of session ID to list of active emitters
    private final Map<String, List<SseEmitter>> sessionEmitters = new ConcurrentHashMap<>();

    // Emitter timeout (5 minutes)
    private static final long EMITTER_TIMEOUT = 5 * 60 * 1000L;

    /**
     * Create a new SSE emitter for a session.
     *
     * @param sessionId Session to stream events for
     * @return SSE emitter configured for long-lived connection
     */
    public SseEmitter createEmitter(String sessionId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        // Register emitter for this session
        sessionEmitters.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        log.info("Client connected to event stream for session: {} (total clients: {})",
                sessionId, sessionEmitters.get(sessionId).size());

        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of(
                            "sessionId", sessionId,
                            "message", "Connected to event stream",
                            "timestamp", System.currentTimeMillis()
                    ))
                    .build());
        } catch (IOException e) {
            log.error("Failed to send connection event: {}", e.getMessage());
            removeEmitter(sessionId, emitter);
        }

        // Set up completion handlers
        emitter.onCompletion(() -> {
            log.info("Client disconnected from session: {}", sessionId);
            removeEmitter(sessionId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("Client timeout for session: {}", sessionId);
            removeEmitter(sessionId, emitter);
        });

        emitter.onError((ex) -> {
            log.error("SSE error for session {}: {}", sessionId, ex.getMessage());
            removeEmitter(sessionId, emitter);
        });

        return emitter;
    }

    /**
     * Publish an event to all clients listening to a session.
     *
     * @param sessionId Session to publish event for
     * @param eventType Type of event (e.g., "PATIENT_PERSISTED")
     * @param message   Event message
     */
    public void publishEvent(String sessionId, String eventType, String message) {
        publishEvent(sessionId, eventType, message, null);
    }

    /**
     * Publish an event with additional data to all clients listening to a session.
     *
     * @param sessionId   Session to publish event for
     * @param eventType   Type of event
     * @param message     Event message
     * @param additionalData Additional structured data (optional)
     */
    public void publishEvent(String sessionId, String eventType, String message, Map<String, Object> additionalData) {
        List<SseEmitter> emitters = sessionEmitters.get(sessionId);

        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active clients for session: {}", sessionId);
            return;
        }

        // Build event data
        Map<String, Object> eventData = new ConcurrentHashMap<>();
        eventData.put("sessionId", sessionId);
        eventData.put("eventType", eventType);
        eventData.put("message", message);
        eventData.put("timestamp", System.currentTimeMillis());

        if (additionalData != null) {
            eventData.putAll(additionalData);
        }

        // Send to all active emitters
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventType)
                        .data(eventData)
                        .build());

                log.debug("Published {} event to session {}: {}", eventType, sessionId, message);

            } catch (IOException e) {
                log.warn("Failed to send event to client (session: {}): {}", sessionId, e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        // Remove dead emitters
        deadEmitters.forEach(emitter -> removeEmitter(sessionId, emitter));
    }

    /**
     * Complete all emitters for a session (send final event and close connections).
     *
     * @param sessionId Session to complete
     */
    public void completeSession(String sessionId) {
        List<SseEmitter> emitters = sessionEmitters.get(sessionId);

        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("SESSION_ENDED")
                            .data(Map.of(
                                    "sessionId", sessionId,
                                    "message", "Session completed - closing connection",
                                    "timestamp", System.currentTimeMillis()
                            ))
                            .build());

                    emitter.complete();
                } catch (IOException e) {
                    log.error("Failed to send completion event: {}", e.getMessage());
                }
            }

            sessionEmitters.remove(sessionId);
            log.info("Completed all SSE connections for session: {}", sessionId);
        }
    }

    /**
     * Remove a specific emitter from a session.
     */
    private void removeEmitter(String sessionId, SseEmitter emitter) {
        List<SseEmitter> emitters = sessionEmitters.get(sessionId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                sessionEmitters.remove(sessionId);
            }
        }
    }

    /**
     * Get count of active clients for a session.
     *
     * @param sessionId Session to check
     * @return Number of active SSE clients
     */
    public int getActiveClientCount(String sessionId) {
        List<SseEmitter> emitters = sessionEmitters.get(sessionId);
        return emitters != null ? emitters.size() : 0;
    }

    /**
     * Get total active client count across all sessions.
     *
     * @return Total active SSE connections
     */
    public int getTotalActiveClientCount() {
        return sessionEmitters.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}
