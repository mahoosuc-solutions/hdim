package com.healthdata.patient.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.patient.dto.PreVisitSummaryResponse;
import com.healthdata.patient.service.PreVisitPlanningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Pre-Visit Planning Controller
 *
 * Issue #6: Provides comprehensive pre-visit summary for providers preparing
 * for patient visits. Aggregates data from multiple services including
 * FHIR resources, care gaps, and generates AI-suggested agenda items.
 *
 * HIPAA Compliance:
 * - All endpoints are audited with @Audited annotation
 * - No caching to ensure fresh PHI data
 * - Multi-tenant filtering enforced by service layer
 * - Cache-Control headers set to no-store
 */
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pre-Visit Planning", description = "APIs for pre-visit planning and patient summaries")
public class PreVisitPlanningController {

    private final PreVisitPlanningService preVisitPlanningService;

    /**
     * Get pre-visit summary for a specific patient.
     *
     * Aggregates care gaps, recent results, medications, and generates
     * an AI-suggested agenda with time estimates for each topic.
     *
     * @param providerId Provider ID requesting the summary
     * @param patientId  Patient ID for the summary
     * @param tenantId   Tenant ID from auth header
     * @return PreVisitSummaryResponse with comprehensive pre-visit data
     */
    @GetMapping(value = "/{providerId}/patients/{patientId}/pre-visit-summary",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'PROVIDER')")
    @Audited(action = AuditAction.READ, resourceType = "PreVisitSummary", description = "Pre-visit summary access")
    @Operation(
            summary = "Get pre-visit summary for a patient",
            description = "Returns a comprehensive pre-visit summary including care gaps, " +
                    "recent lab results, medications, and AI-suggested agenda items. " +
                    "This endpoint is HIPAA-audited and returns fresh data (no caching)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pre-visit summary generated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PreVisitSummaryResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing authentication",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - insufficient permissions",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Patient not found",
                    content = @Content
            )
    })
    public ResponseEntity<PreVisitSummaryResponse> getPreVisitSummary(
            @Parameter(description = "Provider ID requesting the summary", required = true)
            @PathVariable String providerId,

            @Parameter(description = "Patient ID for the summary", required = true)
            @PathVariable String patientId,

            @Parameter(description = "Tenant ID for multi-tenant filtering", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId
    ) {
        log.info("GET /api/v1/providers/{}/patients/{}/pre-visit-summary - tenant: {}",
                providerId, patientId, tenantId);

        PreVisitSummaryResponse response = preVisitPlanningService.getPreVisitSummary(
                tenantId, providerId, patientId);

        // HIPAA: Set no-cache headers for PHI
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(response);
    }

    /**
     * Get pre-visit summaries for multiple patients (batch endpoint).
     *
     * Useful for preparing for a day's appointments.
     *
     * @param providerId Provider ID requesting the summaries
     * @param tenantId   Tenant ID from auth header
     * @param date       Optional date filter (defaults to tomorrow)
     * @return List of PreVisitSummaryResponse for scheduled patients
     */
    @GetMapping(value = "/{providerId}/pre-visit-summaries",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'PROVIDER')")
    @Audited(action = AuditAction.READ, resourceType = "PreVisitSummary", description = "Batch pre-visit summaries access")
    @Operation(
            summary = "Get pre-visit summaries for scheduled patients",
            description = "Returns pre-visit summaries for all patients scheduled for the " +
                    "specified date. Defaults to tomorrow if no date is provided."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Pre-visit summaries generated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content
            )
    })
    public ResponseEntity<BatchPreVisitResponse> getBatchPreVisitSummaries(
            @Parameter(description = "Provider ID requesting the summaries", required = true)
            @PathVariable String providerId,

            @Parameter(description = "Tenant ID for multi-tenant filtering", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,

            @Parameter(description = "Date to get summaries for (YYYY-MM-DD), defaults to tomorrow")
            @RequestParam(required = false) String date
    ) {
        log.info("GET /api/v1/providers/{}/pre-visit-summaries - tenant: {}, date: {}",
                providerId, tenantId, date);

        // TODO: Implement batch endpoint when scheduling service is available
        // For now, return empty response
        BatchPreVisitResponse response = BatchPreVisitResponse.builder()
                .providerId(providerId)
                .date(date != null ? date : java.time.LocalDate.now().plusDays(1).toString())
                .totalPatients(0)
                .summaries(java.util.Collections.emptyList())
                .message("Scheduling service integration pending")
                .build();

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .header("Pragma", "no-cache")
                .body(response);
    }

    /**
     * Response DTO for batch pre-visit summaries.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BatchPreVisitResponse {
        private String providerId;
        private String date;
        private int totalPatients;
        private java.util.List<PreVisitSummaryResponse> summaries;
        private String message;
    }
}
