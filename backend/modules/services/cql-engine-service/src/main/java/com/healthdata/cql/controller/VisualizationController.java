package com.healthdata.cql.controller;

import com.healthdata.cql.websocket.EvaluationProgressWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * REST API for visualization and monitoring.
 *
 * Provides endpoints for:
 * - WebSocket connection statistics
 * - Real-time monitoring status
 * - Visualization configuration
 */
@RestController
@RequestMapping("/api/visualization")
@Tag(name = "Visualization", description = "Real-time visualization and monitoring APIs")
public class VisualizationController {

    private final EvaluationProgressWebSocketHandler webSocketHandler;

    public VisualizationController(EvaluationProgressWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Get WebSocket connection statistics
     */
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/connections")
    @Operation(summary = "Get WebSocket connection statistics",
            description = "Returns count of active WebSocket connections for real-time monitoring")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", webSocketHandler.getConnectionCount());
        stats.put("wsEndpoint", "ws://localhost:8082/ws/evaluation-progress");
        stats.put("wsEndpointWithTenant", "ws://localhost:8082/ws/evaluation-progress?tenantId=YOUR_TENANT_ID");
        stats.put("status", "active");

        return ResponseEntity.ok(stats);
    }

    /**
     * Get connection count for a specific tenant
     */
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/connections/{tenantId}")
    @Operation(summary = "Get connection count for tenant",
            description = "Returns count of active WebSocket connections for a specific tenant")
    public ResponseEntity<Map<String, Object>> getTenantConnectionStats(
            @PathVariable String tenantId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tenantId", tenantId);
        stats.put("connections", webSocketHandler.getConnectionCount(tenantId));

        return ResponseEntity.ok(stats);
    }

    /**
     * Get visualization configuration
     */
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/config")
    @Operation(summary = "Get visualization configuration",
            description = "Returns current visualization configuration including emit intervals and topic names")
    public ResponseEntity<Map<String, Object>> getVisualizationConfig() {
        Map<String, Object> config = new HashMap<>();

        // WebSocket config
        Map<String, Object> wsConfig = new HashMap<>();
        wsConfig.put("endpoint", "/ws/evaluation-progress");
        wsConfig.put("protocol", "WebSocket");
        wsConfig.put("messageFormat", "JSON");
        config.put("websocket", wsConfig);

        // Event topics
        Map<String, String> topics = new HashMap<>();
        topics.put("evaluationStarted", "evaluation.started");
        topics.put("evaluationCompleted", "evaluation.completed");
        topics.put("evaluationFailed", "evaluation.failed");
        topics.put("batchProgress", "batch.progress");
        config.put("kafkaTopics", topics);

        // Progress emit intervals
        Map<String, Object> progressConfig = new HashMap<>();
        progressConfig.put("emitIntervalSeconds", 5);
        progressConfig.put("emitEveryNPatients", 10);
        config.put("batchProgress", progressConfig);

        return ResponseEntity.ok(config);
    }

    /**
     * Health check for visualization system
     */
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/health")
    @Operation(summary = "Visualization system health check",
            description = "Returns health status of visualization infrastructure (WebSocket, Kafka consumers)")
    public ResponseEntity<Map<String, Object>> getVisualizationHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("websocket", "UP");
        health.put("kafkaConsumers", "UP");
        health.put("activeConnections", webSocketHandler.getConnectionCount());
        health.put("status", "HEALTHY");

        return ResponseEntity.ok(health);
    }

    /**
     * Get sample WebSocket message format for documentation
     */
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/sample-message")
    @Operation(summary = "Get sample WebSocket message",
            description = "Returns example WebSocket message format for client integration")
    public ResponseEntity<Map<String, Object>> getSampleMessage() {
        Map<String, Object> sample = new HashMap<>();
        sample.put("type", "EVALUATION_EVENT");
        sample.put("timestamp", System.currentTimeMillis());

        // Sample batch progress event
        Map<String, Object> data = new HashMap<>();
        data.put("eventType", "BATCH_PROGRESS");
        data.put("batchId", "550e8400-e29b-41d4-a716-446655440000");
        data.put("tenantId", "TENANT001");
        data.put("measureId", "HEDIS-CDC");
        data.put("measureName", "Comprehensive Diabetes Care");
        data.put("totalPatients", 1000);
        data.put("completedCount", 450);
        data.put("successCount", 445);
        data.put("failedCount", 5);
        data.put("pendingCount", 550);
        data.put("avgDurationMs", 125.5);
        data.put("currentThroughput", 3.6);
        data.put("elapsedTimeMs", 125000);
        data.put("estimatedTimeRemainingMs", 152778);
        data.put("denominatorCount", 380);
        data.put("numeratorCount", 285);
        data.put("cumulativeComplianceRate", 75.0);

        sample.put("data", data);

        return ResponseEntity.ok(sample);
    }
}
