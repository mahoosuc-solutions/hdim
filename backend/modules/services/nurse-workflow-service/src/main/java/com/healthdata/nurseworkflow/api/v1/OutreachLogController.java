package com.healthdata.nurseworkflow.api.v1;

import com.healthdata.nurseworkflow.application.OutreachLogService;
import com.healthdata.nurseworkflow.domain.model.OutreachLogEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * Outreach Log REST Controller
 *
 * Provides REST endpoints for patient outreach management including:
 * - Creating outreach logs when nurses contact patients
 * - Retrieving outreach history by patient or date range
 * - Updating outreach logs with follow-up information
 * - Retrieving outreach metrics for quality reporting
 *
 * All endpoints are multi-tenant aware and require X-Tenant-ID header.
 * Authentication is enforced via gateway-trust architecture.
 *
 * HIPAA Compliance:
 * - All outreach activities are logged with nurse identifier
 * - Audit logging via @Audited (in future implementation)
 * - Multi-tenant isolation enforced
 *
 * API Version: v1
 * Base Path: /nurse-workflow/outreach-logs
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/outreach-logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Outreach Logs", description = "Patient outreach and follow-up management")
@SecurityRequirement(name = "gateway-auth")
public class OutreachLogController {

    private final OutreachLogService outreachLogService;

    /**
     * Create a new outreach log
     *
     * Records when a nurse attempts to contact a patient, including:
     * - Contact method (phone, email, SMS, in-person)
     * - Outcome (successful contact, no answer, left message, etc.)
     * - Reason for outreach (post-discharge, medication reminder, screening reminder)
     * - Optional notes and follow-up scheduling
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param outreachLog the outreach log to create
     * @return created outreach log with 201 status
     */
    @PostMapping
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Create outreach log",
        description = "Records patient outreach attempt with outcome and follow-up information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Outreach log created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<OutreachLogEntity> createOutreachLog(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody OutreachLogEntity outreachLog) {
        log.info("Creating outreach log for patient {} in tenant {}",
            outreachLog.getPatientId(), tenantId);

        outreachLog.setTenantId(tenantId);
        OutreachLogEntity created = outreachLogService.createOutreachLog(outreachLog);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Retrieve outreach log by ID
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the outreach log ID
     * @return outreach log if found, 404 otherwise
     */
    @GetMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get outreach log",
        description = "Retrieves a specific outreach log by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Outreach log found"),
        @ApiResponse(responseCode = "404", description = "Outreach log not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<OutreachLogEntity> getOutreachLog(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id) {
        log.info("Retrieving outreach log: {} from tenant {}",id, tenantId);

        return outreachLogService.getOutreachLogById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get patient outreach history with pagination
     *
     * Retrieves all outreach attempts for a patient, ordered by most recent first.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param patientId the patient ID
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated outreach logs
     */
    @GetMapping("/patient/{patientId}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get patient outreach history",
        description = "Retrieves all outreach attempts for a specific patient"
    )
    @ApiResponse(responseCode = "200", description = "Patient outreach history retrieved")
    public ResponseEntity<Page<OutreachLogEntity>> getPatientOutreachHistory(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID patientId,
            Pageable pageable) {
        log.info("Retrieving outreach history for patient {} from tenant {}",
            patientId, tenantId);

        Page<OutreachLogEntity> history = outreachLogService.getPatientOutreachHistory(
            tenantId, patientId, pageable);

        return ResponseEntity.ok(history);
    }

    /**
     * Get outreach logs by outcome type
     *
     * Filters outreach attempts by outcome (successful contact, no answer, etc.)
     * for quality metrics and analytics.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param outcomeType the outcome type filter
     * @param pageable pagination parameters
     * @return paginated outreach logs with specified outcome
     */
    @GetMapping("/outcome/{outcomeType}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get outreach logs by outcome type",
        description = "Retrieves outreach logs filtered by outcome (e.g., successful contact, no answer)"
    )
    public ResponseEntity<Page<OutreachLogEntity>> getOutreachByOutcome(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable OutreachLogEntity.OutcomeType outcomeType,
            Pageable pageable) {
        log.info("Retrieving outreach logs by outcome type: {} from tenant {}",
            outcomeType, tenantId);

        Page<OutreachLogEntity> logs = outreachLogService.getOutreachByOutcomeType(
            tenantId, outcomeType, pageable);

        return ResponseEntity.ok(logs);
    }

    /**
     * Update outreach log
     *
     * Allows updating notes and scheduling follow-ups after initial creation.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the outreach log ID
     * @param outreachLog the updated outreach log
     * @return updated outreach log
     */
    @PutMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Update outreach log",
        description = "Updates an existing outreach log with new notes or follow-up information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Outreach log updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Outreach log not found")
    })
    public ResponseEntity<OutreachLogEntity> updateOutreachLog(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody OutreachLogEntity outreachLog) {
        log.info("Updating outreach log: {} from tenant {}", id, tenantId);

        outreachLog.setId(id);
        outreachLog.setTenantId(tenantId);
        OutreachLogEntity updated = outreachLogService.updateOutreachLog(outreachLog);

        return ResponseEntity.ok(updated);
    }

    /**
     * Delete outreach log
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the outreach log ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation (delete)
    @Operation(
        summary = "Delete outreach log",
        description = "Deletes an outreach log (soft delete)"
    )
    @ApiResponse(responseCode = "204", description = "Outreach log deleted")
    public ResponseEntity<Void> deleteOutreachLog(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id) {
        log.info("Deleting outreach log: {} from tenant {}", id, tenantId);

        outreachLogService.deleteOutreachLog(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get outreach metrics for patient
     *
     * Returns summary metrics: total outreach attempts, successful contacts,
     * success rate for quality reporting.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param patientId the patient ID
     * @return outreach metrics summary
     */
    @GetMapping("/metrics/{patientId}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get outreach metrics",
        description = "Retrieves outreach metrics for a patient (total attempts, success rate, etc.)"
    )
    public ResponseEntity<OutreachLogService.OutreachMetrics> getOutreachMetrics(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID patientId) {
        log.info("Retrieving outreach metrics for patient {} from tenant {}",
            patientId, tenantId);

        OutreachLogService.OutreachMetrics metrics = outreachLogService.getPatientOutreachMetrics(
            tenantId, patientId);

        return ResponseEntity.ok(metrics);
    }
}
