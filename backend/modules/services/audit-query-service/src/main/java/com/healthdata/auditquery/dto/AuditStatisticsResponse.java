package com.healthdata.auditquery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for audit log statistics and aggregations.
 *
 * <p>Provides high-level metrics for compliance dashboards and security monitoring:
 * <ul>
 *   <li>Total event counts by action, outcome, resource type</li>
 *   <li>Top users and resources by access frequency</li>
 *   <li>Failed access attempt statistics</li>
 *   <li>Time range summary</li>
 * </ul>
 *
 * <p>Used for:
 * <ul>
 *   <li>HIPAA compliance reporting (who accessed what, when)</li>
 *   <li>Security monitoring (failed login attempts, unauthorized access)</li>
 *   <li>Usage analytics (most active users, most accessed resources)</li>
 * </ul>
 */
@Schema(description = "Audit log statistics and aggregations")
public record AuditStatisticsResponse(

    @Schema(description = "Total number of audit events in the time range", example = "15234")
    long totalEvents,

    @Schema(description = "Start of time range", example = "2026-01-15T00:00:00Z")
    Instant startTime,

    @Schema(description = "End of time range", example = "2026-01-22T23:59:59Z")
    Instant endTime,

    @Schema(description = "Event counts by action type (READ: 10000, CREATE: 2000, etc.)")
    Map<String, Long> eventsByAction,

    @Schema(description = "Event counts by outcome (SUCCESS: 14500, FAILURE: 734)")
    Map<String, Long> eventsByOutcome,

    @Schema(description = "Event counts by resource type (Patient: 8000, Observation: 5000, etc.)")
    Map<String, Long> eventsByResourceType,

    @Schema(description = "Event counts by service (patient-service: 5000, fhir-service: 3000, etc.)")
    Map<String, Long> eventsByService,

    @Schema(description = "Top 10 users by event count")
    Map<String, Long> topUsers,

    @Schema(description = "Top 10 resources by access count")
    Map<String, Long> topResources,

    @Schema(description = "Number of failed events", example = "734")
    long failedEvents,

    @Schema(description = "Number of PHI access events", example = "8523")
    long phiAccessEvents,

    @Schema(description = "Number of unique users in the time range", example = "127")
    long uniqueUsers,

    @Schema(description = "Number of unique resources accessed", example = "3456")
    long uniqueResources
) {
}
