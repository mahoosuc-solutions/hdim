package com.healthdata.demo.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DevOps agent log message for WebSocket streaming.
 *
 * Represents a single log entry from deployment agents, data seeding,
 * clearing operations, or FHIR validation processes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevOpsLogMessage {
    /**
     * Log level: DEBUG, INFO, WARN, ERROR
     */
    private String level;

    /**
     * Log message content
     */
    private String message;

    /**
     * Log category: SEED, CLEAR, VALIDATION, DEPLOY, etc.
     */
    private String category;

    /**
     * Tenant ID for multi-tenant filtering
     */
    private String tenantId;

    /**
     * Timestamp of log entry
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Optional: Component that generated the log
     */
    private String component;

    /**
     * Optional: Additional metadata
     */
    private java.util.Map<String, Object> metadata;
}
