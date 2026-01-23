package com.healthdata.demo.orchestrator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DevOps component status update for WebSocket streaming.
 *
 * Represents status changes in deployment agents, services, or operations.
 * Used for real-time monitoring dashboards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevOpsStatusUpdate {
    /**
     * Component name: FHIR_VALIDATION, DATA_SEEDING, DEPLOYMENT, etc.
     */
    private String component;

    /**
     * Status: PENDING, IN_PROGRESS, COMPLETED, FAILED, PASS, WARN
     */
    private String status;

    /**
     * Tenant ID for multi-tenant filtering
     */
    private String tenantId;

    /**
     * Timestamp of status update
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Status details (e.g., counts, metrics, error messages)
     */
    private java.util.Map<String, Object> details;

    /**
     * Optional: Previous status for change tracking
     */
    private String previousStatus;
}
