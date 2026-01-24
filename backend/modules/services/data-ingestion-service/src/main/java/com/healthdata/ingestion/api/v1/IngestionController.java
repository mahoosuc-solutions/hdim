package com.healthdata.ingestion.api.v1;

import com.healthdata.ingestion.application.DataIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for data ingestion operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Starting data ingestion (POST /api/v1/ingestion/start)</li>
 *   <li>Tracking progress (GET /api/v1/ingestion/progress)</li>
 *   <li>Cancelling ingestion (POST /api/v1/ingestion/cancel)</li>
 *   <li>Health checks (GET /api/v1/ingestion/health)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Ingestion", description = "Load testing and data seeding operations")
public class IngestionController {

    private final DataIngestionService dataIngestionService;

    /**
     * Start data ingestion session.
     *
     * @param request Ingestion configuration
     * @return Session details with tracking ID
     */
    @PostMapping("/start")
    @Operation(summary = "Start data ingestion",
               description = "Begins asynchronous data generation and ingestion into HDIM platform")
    public ResponseEntity<IngestionResponse> startIngestion(
            @Valid @RequestBody IngestionRequest request) {
        
        log.info("Starting data ingestion: tenant={}, patients={}, scenario={}",
                request.getTenantId(), request.getPatientCount(), request.getScenario());

        IngestionResponse response = dataIngestionService.startIngestion(request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get real-time progress of active ingestion session.
     *
     * @param sessionId Optional session ID (defaults to latest)
     * @return Progress details
     */
    @GetMapping("/progress")
    @Operation(summary = "Get ingestion progress",
               description = "Returns real-time progress metrics for active or recent ingestion session")
    public ResponseEntity<IngestionProgressResponse> getProgress(
            @Parameter(description = "Session ID (optional - defaults to latest session)")
            @RequestParam(required = false) String sessionId) {
        
        IngestionProgressResponse progress = dataIngestionService.getProgress(sessionId);
        
        return ResponseEntity.ok(progress);
    }

    /**
     * Cancel active ingestion session.
     *
     * @param sessionId Session ID to cancel
     * @return Cancellation confirmation
     */
    @PostMapping("/cancel")
    @Operation(summary = "Cancel ingestion",
               description = "Stops active ingestion session gracefully")
    public ResponseEntity<IngestionResponse> cancelIngestion(
            @Parameter(description = "Session ID to cancel")
            @RequestParam String sessionId) {
        
        log.info("Cancelling ingestion session: {}", sessionId);
        
        IngestionResponse response = dataIngestionService.cancelIngestion(sessionId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     *
     * @return Service health status
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Returns service health status")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Data Ingestion Service - HEALTHY");
    }
}
