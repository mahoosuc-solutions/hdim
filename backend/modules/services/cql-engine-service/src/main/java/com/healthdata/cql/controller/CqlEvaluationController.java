package com.healthdata.cql.controller;

import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.service.CqlEvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST Controller for CQL Evaluation Management
 *
 * Provides HTTP endpoints for executing CQL expressions and retrieving evaluation results.
 * All endpoints are multi-tenant aware via X-Tenant-ID header.
 *
 * Authorization:
 * - Execute operations (POST) require EVALUATOR, ADMIN, or SUPER_ADMIN role
 * - Read operations (GET) require ANALYST, EVALUATOR, ADMIN, or SUPER_ADMIN role
 * - Delete operations require ADMIN or SUPER_ADMIN role
 */
@Tag(name = "CQL Evaluation", description = "APIs for executing Clinical Quality Language (CQL) expressions and managing evaluation results. CQL is the standard language for expressing HEDIS quality measures.")
@RestController
@RequestMapping("/api/v1/cql/evaluations")
@Validated
public class CqlEvaluationController {

    private static final Logger logger = LoggerFactory.getLogger(CqlEvaluationController.class);

    private final CqlEvaluationService evaluationService;

    public CqlEvaluationController(CqlEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Create and execute a CQL evaluation
     * POST /api/v1/cql/evaluations
     */
    @Operation(
        summary = "Create and execute a CQL evaluation",
        description = "Creates a new CQL evaluation for a patient against a specified CQL library and immediately executes it. "
            + "Used to run HEDIS quality measure logic (e.g., HbA1c testing, breast cancer screening) against patient clinical data.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evaluation created and executed successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid library ID or patient ID"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_EXECUTE permission"),
        @ApiResponse(responseCode = "404", description = "CQL library or patient not found")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_EXECUTE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping
    public ResponseEntity<CqlEvaluation> createAndExecuteEvaluation(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the CQL library containing the quality measure logic", required = true) @RequestParam @NotNull(message = "Library ID is required") UUID libraryId,
            @Parameter(description = "UUID of the patient to evaluate", required = true) @RequestParam @NotNull(message = "Patient ID is required") UUID patientId) {
        logger.info("Creating evaluation for patient: {} with library: {}", patientId, libraryId);

        CqlEvaluation evaluation = evaluationService.createEvaluation(tenantId, libraryId, patientId);
        CqlEvaluation executed = evaluationService.executeEvaluation(evaluation.getId(), tenantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(executed);
    }

    /**
     * Execute an existing evaluation
     * POST /api/v1/cql/evaluations/{id}/execute
     */
    @Operation(
        summary = "Execute an existing CQL evaluation",
        description = "Triggers execution of a previously created CQL evaluation. The evaluation runs the associated CQL library "
            + "against the patient's FHIR R4 clinical data to determine quality measure compliance.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evaluation executed successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_EXECUTE permission"),
        @ApiResponse(responseCode = "404", description = "Evaluation not found")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_EXECUTE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/execute")
    public ResponseEntity<CqlEvaluation> executeEvaluation(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the evaluation to execute", required = true) @PathVariable @NotNull(message = "Evaluation ID is required") UUID id) {
        logger.info("Executing evaluation: {}", id);

        CqlEvaluation executed = evaluationService.executeEvaluation(id, tenantId);
        return ResponseEntity.ok(executed);
    }

    /**
     * Get all evaluations for a tenant
     * GET /api/v1/cql/evaluations
     */
    @Operation(
        summary = "Get all CQL evaluations for a tenant",
        description = "Returns a paginated list of all CQL evaluations within the tenant. Includes evaluations across all "
            + "patients, libraries, and statuses. Useful for quality measure dashboards and reporting.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of evaluations returned successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping
    public ResponseEntity<Page<CqlEvaluation>> getAllEvaluations(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        logger.debug("Getting all evaluations for tenant: {}", tenantId);

        Page<CqlEvaluation> evaluations = evaluationService.getAllEvaluations(tenantId, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get an evaluation by ID
     * GET /api/v1/cql/evaluations/{id}
     */
    @Operation(
        summary = "Get a CQL evaluation by ID",
        description = "Retrieves a specific CQL evaluation by its unique identifier. Returns the evaluation status, "
            + "result data, execution duration, and associated CQL library and patient references.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evaluation found and returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission"),
        @ApiResponse(responseCode = "404", description = "Evaluation not found")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/{id}")
    public ResponseEntity<CqlEvaluation> getEvaluation(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the evaluation to retrieve", required = true) @PathVariable UUID id) {
        logger.debug("Getting evaluation: {} for tenant: {}", id, tenantId);

        return evaluationService.getEvaluationById(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all evaluations for a patient
     * GET /api/v1/cql/evaluations/patient/{patientId}
     */
    @Operation(
        summary = "Get all evaluations for a patient",
        description = "Returns a paginated list of all CQL evaluations for a specific patient. Useful for reviewing "
            + "a patient's HEDIS measure compliance history and identifying care gaps.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of patient evaluations returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<CqlEvaluation>> getEvaluationsForPatient(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the patient whose evaluations to retrieve", required = true) @PathVariable UUID patientId,
            Pageable pageable) {
        logger.debug("Getting evaluations for patient: {} in tenant: {}", patientId, tenantId);

        Page<CqlEvaluation> evaluations = evaluationService.getEvaluationsForPatient(
                tenantId, patientId, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get evaluations for a library
     * GET /api/v1/cql/evaluations/library/{libraryId}
     */
    @Operation(
        summary = "Get evaluations for a CQL library",
        description = "Returns a paginated list of all evaluations that used a specific CQL library. Enables tracking "
            + "of how many patients have been evaluated against a particular HEDIS quality measure.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of library evaluations returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/library/{libraryId}")
    public ResponseEntity<Page<CqlEvaluation>> getEvaluationsForLibrary(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the CQL library to filter evaluations by", required = true) @PathVariable UUID libraryId,
            Pageable pageable) {
        logger.debug("Getting evaluations for library: {} in tenant: {}", libraryId, tenantId);

        Page<CqlEvaluation> evaluations = evaluationService.getEvaluationsForLibrary(
                tenantId, libraryId, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get the latest evaluation for a patient and library
     * GET /api/v1/cql/evaluations/patient/{patientId}/library/{libraryId}/latest
     */
    @Operation(
        summary = "Get the latest evaluation for a patient and library",
        description = "Retrieves the most recent CQL evaluation for a specific patient-library combination. "
            + "Commonly used to check the latest HEDIS measure result for a patient (e.g., most recent HbA1c screening evaluation).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Latest evaluation returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission"),
        @ApiResponse(responseCode = "404", description = "No evaluation found for the patient-library combination")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}/library/{libraryId}/latest")
    public ResponseEntity<CqlEvaluation> getLatestEvaluationForPatientAndLibrary(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the patient", required = true) @PathVariable UUID patientId,
            @Parameter(description = "UUID of the CQL library", required = true) @PathVariable UUID libraryId) {
        logger.debug("Getting latest evaluation for patient: {} and library: {} in tenant: {}",
                patientId, libraryId, tenantId);

        return evaluationService.getLatestEvaluationForPatientAndLibrary(tenantId, patientId, libraryId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get evaluations by status
     * GET /api/v1/cql/evaluations/by-status/{status}
     */
    @Operation(
        summary = "Get evaluations by status",
        description = "Returns a paginated list of CQL evaluations filtered by execution status (e.g., PENDING, COMPLETED, FAILED). "
            + "Useful for monitoring evaluation pipelines and identifying failed HEDIS measure runs.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated evaluations filtered by status", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-status/{status}")
    public ResponseEntity<Page<CqlEvaluation>> getEvaluationsByStatus(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "Evaluation status to filter by (e.g., PENDING, COMPLETED, FAILED)", required = true) @PathVariable @NotBlank(message = "Status is required") String status,
            Pageable pageable) {
        logger.debug("Getting evaluations with status: {} for tenant: {}", status, tenantId);

        Page<CqlEvaluation> evaluations = evaluationService.getEvaluationsByStatus(
                tenantId, status, pageable);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get evaluations within a date range
     * GET /api/v1/cql/evaluations/date-range?start={start}&end={end}
     */
    @Operation(
        summary = "Get evaluations within a date range",
        description = "Returns CQL evaluations executed within the specified date range. Supports HEDIS measurement year "
            + "reporting by allowing queries scoped to specific time periods (e.g., calendar year for annual quality reporting).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evaluations within the date range returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid date format - use ISO 8601 datetime"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/date-range")
    public ResponseEntity<List<CqlEvaluation>> getEvaluationsByDateRange(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Start of date range (ISO 8601 datetime)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End of date range (ISO 8601 datetime)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        logger.debug("Getting evaluations between {} and {} for tenant: {}", start, end, tenantId);

        List<CqlEvaluation> evaluations = evaluationService.getEvaluationsByDateRange(
                tenantId, start, end);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get evaluations for a patient within a date range
     * GET /api/v1/cql/evaluations/patient/{patientId}/date-range?start={start}&end={end}
     */
    @Operation(
        summary = "Get evaluations for a patient within a date range",
        description = "Returns CQL evaluations for a specific patient within the specified date range. "
            + "Supports longitudinal care gap analysis and HEDIS measurement year scoping for individual patients.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Patient evaluations within date range returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid date format - use ISO 8601 datetime"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}/date-range")
    public ResponseEntity<List<CqlEvaluation>> getEvaluationsForPatientByDateRange(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the patient", required = true) @PathVariable UUID patientId,
            @Parameter(description = "Start of date range (ISO 8601 datetime)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End of date range (ISO 8601 datetime)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {
        logger.debug("Getting evaluations for patient: {} between {} and {} in tenant: {}",
                patientId, start, end, tenantId);

        List<CqlEvaluation> evaluations = evaluationService.getEvaluationsForPatientByDateRange(
                tenantId, patientId, start, end);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Get successful evaluations for a patient
     * GET /api/v1/cql/evaluations/patient/{patientId}/successful
     */
    @Operation(
        summary = "Get successful evaluations for a patient",
        description = "Returns all successfully completed CQL evaluations for a specific patient. Useful for confirming "
            + "which HEDIS quality measures a patient has satisfied and generating compliance reports.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful evaluations for the patient returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/patient/{patientId}/successful")
    public ResponseEntity<List<CqlEvaluation>> getSuccessfulEvaluationsForPatient(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the patient", required = true) @PathVariable UUID patientId) {
        logger.debug("Getting successful evaluations for patient: {} in tenant: {}",
                patientId, tenantId);

        List<CqlEvaluation> evaluations = evaluationService.getSuccessfulEvaluationsForPatient(
                tenantId, patientId);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Retry a failed evaluation
     * POST /api/v1/cql/evaluations/{id}/retry
     */
    @Operation(
        summary = "Retry a failed CQL evaluation",
        description = "Re-executes a previously failed CQL evaluation. The evaluation is reset and run again against "
            + "the patient's current FHIR R4 clinical data. Useful for recovering from transient errors during HEDIS measure processing.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evaluation retried successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_EXECUTE permission"),
        @ApiResponse(responseCode = "404", description = "Evaluation not found")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_EXECUTE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/retry")
    public ResponseEntity<CqlEvaluation> retryEvaluation(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the failed evaluation to retry", required = true) @PathVariable UUID id) {
        logger.info("Retrying evaluation: {}", id);

        CqlEvaluation retried = evaluationService.retryEvaluation(id, tenantId);
        return ResponseEntity.ok(retried);
    }

    /**
     * Batch evaluate multiple patients
     * POST /api/v1/cql/evaluations/batch
     */
    @Operation(
        summary = "Batch evaluate multiple patients",
        description = "Creates and executes CQL evaluations for multiple patients against a single CQL library in one request. "
            + "Designed for population-level HEDIS measure evaluation across patient panels for value-based care reporting.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Batch evaluations created and executed", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid input - empty patient list or missing library ID"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_EXECUTE permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_EXECUTE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/batch")
    public ResponseEntity<List<CqlEvaluation>> batchEvaluate(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the CQL library to evaluate against", required = true) @RequestParam @NotNull(message = "Library ID is required") UUID libraryId,
            @RequestBody @NotEmpty(message = "Patient IDs list cannot be empty") List<@NotNull UUID> patientIds) {

        logger.info("Starting batch evaluation for {} patients with library: {}",
                patientIds.size(), libraryId);

        List<CqlEvaluation> evaluations = evaluationService.batchEvaluate(
                tenantId, libraryId, patientIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluations);
    }

    /**
     * Get failed evaluations for retry
     * GET /api/v1/cql/evaluations/failed-for-retry?hoursBack={hours}
     */
    @Operation(
        summary = "Get failed evaluations eligible for retry",
        description = "Returns CQL evaluations that failed within the specified lookback window and are eligible for retry. "
            + "Supports operational monitoring of HEDIS measure evaluation pipelines and automated recovery workflows.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Failed evaluations eligible for retry returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid hoursBack parameter"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/failed-for-retry")
    public ResponseEntity<List<CqlEvaluation>> getFailedEvaluationsForRetry(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "Number of hours to look back for failed evaluations (default: 24)") @RequestParam(defaultValue = "24") @Min(value = 1, message = "Hours must be at least 1") int hoursBack) {
        logger.debug("Getting failed evaluations for retry (last {} hours) for tenant: {}",
                hoursBack, tenantId);

        List<CqlEvaluation> evaluations = evaluationService.getFailedEvaluationsForRetry(
                tenantId, hoursBack);
        return ResponseEntity.ok(evaluations);
    }

    /**
     * Count evaluations by status
     * GET /api/v1/cql/evaluations/count/by-status/{status}
     */
    @Operation(
        summary = "Count evaluations by status",
        description = "Returns the total count of CQL evaluations with the specified status. "
            + "Useful for dashboard metrics showing HEDIS evaluation pipeline throughput and failure rates.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evaluation count returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count/by-status/{status}")
    public ResponseEntity<Long> countEvaluationsByStatus(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Evaluation status to count (e.g., PENDING, COMPLETED, FAILED)", required = true) @PathVariable String status) {
        logger.debug("Counting evaluations with status: {} for tenant: {}", status, tenantId);

        long count = evaluationService.countEvaluationsByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }

    /**
     * Count evaluations for a library
     * GET /api/v1/cql/evaluations/count/library/{libraryId}
     */
    @Operation(
        summary = "Count evaluations for a CQL library",
        description = "Returns the total number of evaluations executed against a specific CQL library. "
            + "Useful for tracking HEDIS measure adoption and population coverage statistics.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evaluation count for library returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count/library/{libraryId}")
    public ResponseEntity<Long> countEvaluationsForLibrary(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the CQL library to count evaluations for", required = true) @PathVariable UUID libraryId) {
        logger.debug("Counting evaluations for library: {} in tenant: {}", libraryId, tenantId);

        long count = evaluationService.countEvaluationsForLibrary(tenantId, libraryId);
        return ResponseEntity.ok(count);
    }

    /**
     * Count evaluations for a patient
     * GET /api/v1/cql/evaluations/count/patient/{patientId}
     */
    @Operation(
        summary = "Count evaluations for a patient",
        description = "Returns the total number of CQL evaluations executed for a specific patient. "
            + "Provides a quick summary of how many quality measures have been evaluated for a given patient.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evaluation count for patient returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count/patient/{patientId}")
    public ResponseEntity<Long> countEvaluationsForPatient(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the patient to count evaluations for", required = true) @PathVariable UUID patientId) {
        logger.debug("Counting evaluations for patient: {} in tenant: {}", patientId, tenantId);

        long count = evaluationService.countEvaluationsForPatient(tenantId, patientId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get average evaluation duration for a library
     * GET /api/v1/cql/evaluations/avg-duration/library/{libraryId}
     */
    @Operation(
        summary = "Get average evaluation duration for a CQL library",
        description = "Returns the average execution time (in milliseconds) for evaluations against a specific CQL library. "
            + "Useful for performance monitoring and capacity planning of HEDIS measure evaluation workloads.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Average duration returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/avg-duration/library/{libraryId}")
    public ResponseEntity<Double> getAverageDurationForLibrary(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the CQL library to calculate average duration for", required = true) @PathVariable UUID libraryId) {
        logger.debug("Getting average duration for library: {} in tenant: {}", libraryId, tenantId);

        Double avgDuration = evaluationService.getAverageDurationForLibrary(tenantId, libraryId);
        return ResponseEntity.ok(avgDuration != null ? avgDuration : 0.0);
    }

    /**
     * Delete old evaluations (data retention)
     * DELETE /api/v1/cql/evaluations/old?daysToRetain={days}
     */
    @Operation(
        summary = "Delete old evaluations for data retention",
        description = "Removes CQL evaluations older than the specified retention period. Supports HIPAA-compliant "
            + "data retention policies by purging historical evaluation records beyond the configured threshold.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Old evaluations deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid daysToRetain parameter"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_WRITE permission")
    })
    @PreAuthorize("hasPermission(null, 'MEASURE_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/old")
    public ResponseEntity<Void> deleteOldEvaluations(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "Number of days to retain evaluations (default: 90, minimum: 1)") @RequestParam(defaultValue = "90") @Min(value = 1, message = "Days to retain must be at least 1") int daysToRetain) {
        logger.info("Deleting evaluations older than {} days for tenant: {}", daysToRetain, tenantId);

        evaluationService.deleteOldEvaluations(tenantId, daysToRetain);
        return ResponseEntity.noContent().build();
    }
}
