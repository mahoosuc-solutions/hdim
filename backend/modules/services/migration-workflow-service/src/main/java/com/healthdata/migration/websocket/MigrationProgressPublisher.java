package com.healthdata.migration.websocket;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.migration.dto.MigrationProgress;

/**
 * Publishes migration progress updates to WebSocket subscribers
 */
@Component
public class MigrationProgressPublisher {

    private static final Logger log = LoggerFactory.getLogger(MigrationProgressPublisher.class);

    private final ObjectMapper objectMapper;

    // Map of jobId -> set of subscribed sessions
    private final Map<UUID, Set<WebSocketSession>> subscribers = new ConcurrentHashMap<>();

    public MigrationProgressPublisher() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Subscribe a session to progress updates for a job
     */
    public void subscribe(UUID jobId, WebSocketSession session) {
        subscribers.computeIfAbsent(jobId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Session {} subscribed to job {}", session.getId(), jobId);
    }

    /**
     * Unsubscribe a session from progress updates
     */
    public void unsubscribe(UUID jobId, WebSocketSession session) {
        Set<WebSocketSession> sessions = subscribers.get(jobId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                subscribers.remove(jobId);
            }
        }
        log.debug("Session {} unsubscribed from job {}", session.getId(), jobId);
    }

    /**
     * Unsubscribe a session from all jobs
     */
    public void unsubscribeAll(WebSocketSession session) {
        subscribers.values().forEach(sessions -> sessions.remove(session));
    }

    /**
     * Publish progress update to all subscribers
     */
    public void publishProgress(UUID jobId, MigrationProgress progress) {
        Set<WebSocketSession> sessions = subscribers.get(jobId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            MigrationEvent event = MigrationEvent.builder()
                    .type(MigrationEventType.PROGRESS_UPDATE)
                    .jobId(jobId)
                    .timestamp(java.time.Instant.now())
                    .payload(progress)
                    .build();

            String message = objectMapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        log.warn("Failed to send progress to session {}: {}",
                                session.getId(), e.getMessage());
                        sessions.remove(session);
                    }
                } else {
                    sessions.remove(session);
                }
            }
        } catch (Exception e) {
            log.error("Error publishing progress for job {}", jobId, e);
        }
    }

    /**
     * Publish an error event to subscribers
     */
    public void publishError(UUID jobId, String errorMessage) {
        Set<WebSocketSession> sessions = subscribers.get(jobId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            MigrationEvent event = MigrationEvent.builder()
                    .type(MigrationEventType.ERROR_OCCURRED)
                    .jobId(jobId)
                    .timestamp(java.time.Instant.now())
                    .payload(Map.of("error", errorMessage))
                    .build();

            String message = objectMapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        log.warn("Failed to send error to session {}", session.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error publishing error event for job {}", jobId, e);
        }
    }

    /**
     * Publish status change event
     */
    public void publishStatusChange(UUID jobId, String newStatus) {
        Set<WebSocketSession> sessions = subscribers.get(jobId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            MigrationEvent event = MigrationEvent.builder()
                    .type(MigrationEventType.STATUS_CHANGED)
                    .jobId(jobId)
                    .timestamp(java.time.Instant.now())
                    .payload(Map.of("status", newStatus))
                    .build();

            String message = objectMapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        log.warn("Failed to send status change to session {}", session.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error publishing status change for job {}", jobId, e);
        }
    }

    /**
     * Publish job completion event
     */
    public void publishJobCompleted(UUID jobId, Object summary) {
        Set<WebSocketSession> sessions = subscribers.get(jobId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            MigrationEvent event = MigrationEvent.builder()
                    .type(MigrationEventType.JOB_COMPLETED)
                    .jobId(jobId)
                    .timestamp(java.time.Instant.now())
                    .payload(summary)
                    .build();

            String message = objectMapper.writeValueAsString(event);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        log.warn("Failed to send completion to session {}", session.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error publishing completion for job {}", jobId, e);
        }

        // Clean up subscribers after completion
        subscribers.remove(jobId);
    }

    /**
     * Get count of active subscribers for a job
     */
    public int getSubscriberCount(UUID jobId) {
        Set<WebSocketSession> sessions = subscribers.get(jobId);
        return sessions != null ? sessions.size() : 0;
    }
}
