package com.healthdata.ingestion.api.v1;

import com.healthdata.ingestion.api.v1.dto.IngestionRequest;
import com.healthdata.ingestion.api.v1.dto.IngestionResponse;
import com.healthdata.ingestion.api.v1.dto.IngestionProgressResponse;
import com.healthdata.ingestion.application.DataIngestionService;
import com.healthdata.ingestion.application.EventStreamService;
import com.healthdata.ingestion.application.ProgressTrackingService;
import com.healthdata.ingestion.application.ValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * REST API controller for data ingestion operations.
 *
 * This service is designed as a separate integration engine to push test data
 * into the HDIM platform with visible real-time streaming for customer demonstrations.
 *
 * Key features:
 * - Real-time progress tracking
 * - Server-Sent Events for live event streaming
 * - AI-powered validation
 * - Comprehensive audit logging
 * - Distributed tracing integration
 */
@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
@Slf4j
public class IngestionController {

    private final DataIngestionService dataIngestionService;
    private final ProgressTrackingService progressService;
    private final ValidationService validationService;
    private final EventStreamService eventStreamService;

    /**
     * Start data ingestion process.
     *
     * Generates synthetic patient data and pushes it through the HDIM platform
     * to validate data ingress pathways, service consumption, storage, and audit logging.
     *
     * @param request Ingestion configuration (patient count, tenant, scenario)
     * @return Session information with tracking ID
     */
    @PostMapping("/start")
    public ResponseEntity<IngestionResponse> startIngestion(@Valid @RequestBody IngestionRequest request) {
        log.info("Starting data ingestion - Tenant: {}, Patient Count: {}, Scenario: {}",
                request.getTenantId(), request.getPatientCount(), request.getScenario());

        IngestionResponse response = dataIngestionService.startIngestion(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get real-time progress of ingestion session.
     *
     * Polls every 1-5 seconds for progress updates during active ingestion.
     *
     * @param sessionId Unique session identifier from /start endpoint
     * @return Current progress including percent complete, stage, and metrics
     */
    @GetMapping("/progress")
    public ResponseEntity<IngestionProgressResponse> getProgress(@RequestParam String sessionId) {
        IngestionProgressResponse progress = progressService.getProgress(sessionId);
        return ResponseEntity.ok(progress);
    }

    /**
     * Server-Sent Events stream for real-time event notifications.
     *
     * Provides live streaming of ingestion events for customer demonstrations:
     * - PATIENT_GENERATED
     * - PATIENT_PERSISTED
     * - CARE_GAP_CREATED
     * - MEASURE_SEEDED
     * - AUDIT_LOGGED
     * - VALIDATION_PASSED
     * - STAGE_COMPLETED
     *
     * @param sessionId Unique session identifier
     * @return SSE emitter for streaming events
     */
    @GetMapping(value = "/stream-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@RequestParam String sessionId) {
        log.info("Client connected to event stream for session: {}", sessionId);
        return eventStreamService.createEmitter(sessionId);
    }

    /**
     * Cancel an in-progress ingestion session.
     *
     * @param sessionId Session to cancel
     * @return Cancellation confirmation
     */
    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancelIngestion(@RequestParam String sessionId) {
        log.info("Cancelling ingestion session: {}", sessionId);
        progressService.cancelSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "status", "CANCELLED",
                "sessionId", sessionId
        ));
    }

    /**
     * Service health check endpoint.
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "data-ingestion-service"
        ));
    }

    /**
     * Get detailed ingestion metrics.
     *
     * Provides aggregate metrics across all sessions including:
     * - Total patients generated
     * - Total care gaps created
     * - Total measures seeded
     * - Average processing time
     * - Success/failure rates
     *
     * @return Metrics summary
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = progressService.getAggregateMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get full audit trail for a session.
     *
     * Shows every operation recorded in the audit database, useful for
     * demonstrating HIPAA compliance and distributed tracing.
     *
     * @param sessionId Session to retrieve audit trail for
     * @return List of audit events
     */
    @GetMapping("/audit-trail")
    public ResponseEntity<Map<String, Object>> getAuditTrail(@RequestParam String sessionId) {
        Map<String, Object> auditTrail = progressService.getAuditTrail(sessionId);
        return ResponseEntity.ok(auditTrail);
    }

    /**
     * AI-powered validation of ingested data.
     *
     * Uses AI to validate that the system is behaving correctly:
     * - Patient count matches expected
     * - Care gaps created within expected range
     * - Audit trail completeness
     * - Anomaly detection
     *
     * @param sessionId Session to validate
     * @return Validation report with AI analysis
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateIngestion(@RequestParam String sessionId) {
        log.info("Running AI validation for session: {}", sessionId);
        Map<String, Object> validationReport = validationService.validateIngestion(sessionId);
        return ResponseEntity.ok(validationReport);
    }

    /**
     * Show which microservices were impacted by ingestion.
     *
     * Useful for demonstrating distributed architecture and service interactions.
     *
     * @param sessionId Session to analyze
     * @return Service impact dashboard
     */
    @GetMapping("/service-impact")
    public ResponseEntity<Map<String, Object>> getServiceImpact(@RequestParam String sessionId) {
        Map<String, Object> impact = progressService.getServiceImpact(sessionId);
        return ResponseEntity.ok(impact);
    }

    /**
     * Get distributed trace visualization link.
     *
     * Returns Jaeger trace ID and URL for visualizing request flow through microservices.
     *
     * @param sessionId Session to trace
     * @return Trace information with Jaeger URL
     */
    @GetMapping("/trace/{sessionId}")
    public ResponseEntity<Map<String, String>> getTrace(@PathVariable String sessionId) {
        Map<String, String> traceInfo = progressService.getTraceInfo(sessionId);
        return ResponseEntity.ok(traceInfo);
    }

    /**
     * Clear all ingested data for a tenant (reset to clean state).
     *
     * WARNING: This is destructive and should only be used in demo environments.
     *
     * @param tenantId Tenant to reset
     * @return Reset confirmation
     */
    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> reset(@RequestParam String tenantId) {
        log.warn("RESET requested for tenant: {} - This will DELETE all ingested data", tenantId);
        dataIngestionService.resetTenant(tenantId);
        return ResponseEntity.ok(Map.of(
                "status", "RESET_COMPLETE",
                "tenantId", tenantId
        ));
    }
}
