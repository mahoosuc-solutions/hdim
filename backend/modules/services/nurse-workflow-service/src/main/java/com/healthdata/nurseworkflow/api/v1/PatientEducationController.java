package com.healthdata.nurseworkflow.api.v1;

import com.healthdata.nurseworkflow.application.PatientEducationService;
import com.healthdata.nurseworkflow.domain.model.PatientEducationLogEntity;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Patient Education REST Controller
 *
 * Provides REST endpoints for patient education delivery management including:
 * - Logging education sessions with teach-back assessment
 * - Tracking material types and delivery methods
 * - Identifying patients needing follow-up education
 * - Generating patient education metrics and reports
 *
 * Implements health literacy principles and HEDIS compliance for chronic disease management.
 *
 * All endpoints are multi-tenant aware and require X-Tenant-ID header.
 *
 * API Version: v1
 * Base Path: /nurse-workflow/patient-education
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patient-education")
@RequiredArgsConstructor
@Validated
@Tag(name = "Patient Education", description = "Patient education delivery and tracking management")
@SecurityRequirement(name = "gateway-auth")
public class PatientEducationController {

    private final PatientEducationService patientEducationService;

    /**
     * Log patient education delivery
     *
     * Documents education session when delivered to patient, including:
     * - Material type (condition-specific education)
     * - Delivery method (in-person, phone, email, portal)
     * - Teach-back assessment (verify understanding)
     * - Identified barriers to learning
     * - Interpreter usage if applicable
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param educationLog the education delivery to log
     * @return created education log with 201 status
     */
    @PostMapping
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Log patient education delivery",
        description = "Documents education session with teach-back assessment and barrier identification"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Patient education logged"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PatientEducationLogEntity> logEducationDelivery(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody PatientEducationLogEntity educationLog) {
        log.info("Logging patient education for patient {} in tenant {}",
            educationLog.getPatientId(), tenantId);

        educationLog.setTenantId(tenantId);
        PatientEducationLogEntity created = patientEducationService.logEducationDelivery(educationLog);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get patient education log by ID
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the education log ID
     * @return education log if found, 404 otherwise
     */
    @GetMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get patient education log",
        description = "Retrieves a specific patient education log by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Education log found"),
        @ApiResponse(responseCode = "404", description = "Education log not found")
    })
    public ResponseEntity<PatientEducationLogEntity> getEducationLog(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id) {
        log.info("Retrieving patient education log: {} from tenant {}", id, tenantId);

        return patientEducationService.getEducationLogById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get patient education history
     *
     * Retrieves all education delivered to a specific patient, ordered by most recent first.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param patientId the patient ID
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of education logs for patient
     */
    @GetMapping("/patient/{patientId}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get patient education history",
        description = "Retrieves all education delivered to a specific patient"
    )
    public ResponseEntity<Page<PatientEducationLogEntity>> getPatientEducationHistory(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID patientId,
            Pageable pageable) {
        log.info("Retrieving education history for patient {} from tenant {}",
            patientId, tenantId);

        Page<PatientEducationLogEntity> history =
            patientEducationService.getPatientEducationHistory(tenantId, patientId, pageable);

        return ResponseEntity.ok(history);
    }

    /**
     * Get education logs by material type
     *
     * Filters education logs by disease/condition type (diabetes, hypertension, etc.)
     * for tracking patient education on specific conditions.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param materialType the material type filter
     * @param pageable pagination parameters
     * @return paginated education logs with specified material
     */
    @GetMapping("/material/{materialType}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get education logs by material type",
        description = "Retrieves education logs filtered by condition/disease type"
    )
    public ResponseEntity<Page<PatientEducationLogEntity>> getEducationByMaterialType(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable PatientEducationLogEntity.MaterialType materialType,
            Pageable pageable) {
        log.info("Retrieving education logs by material type: {} from tenant {}",
            materialType, tenantId);

        Page<PatientEducationLogEntity> results =
            patientEducationService.getEducationByMaterialType(tenantId, materialType, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * Get education logs by delivery method
     *
     * Filters by how education was delivered (in-person, phone, email, portal, etc.)
     * for tracking outreach methods and modality preferences.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param deliveryMethod the delivery method filter
     * @param pageable pagination parameters
     * @return paginated education logs with specified delivery method
     */
    @GetMapping("/delivery/{deliveryMethod}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get education logs by delivery method",
        description = "Retrieves education logs filtered by delivery method"
    )
    public ResponseEntity<Page<PatientEducationLogEntity>> getEducationByDeliveryMethod(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable PatientEducationLogEntity.DeliveryMethod deliveryMethod,
            Pageable pageable) {
        log.info("Retrieving education logs by delivery method: {} from tenant {}",
            deliveryMethod, tenantId);

        Page<PatientEducationLogEntity> results =
            patientEducationService.getEducationByDeliveryMethod(tenantId, deliveryMethod, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * Get education logs within date range
     *
     * Retrieves education delivered within specified date range for
     * quality reporting and analytics.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param patientId the patient ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return paginated education logs in date range
     */
    @GetMapping("/patient/{patientId}/date-range")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get education logs within date range",
        description = "Retrieves education delivered within specified date range"
    )
    public ResponseEntity<Page<PatientEducationLogEntity>> getEducationByDateRange(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            Pageable pageable) {
        log.info("Retrieving education for patient {} in date range: {} to {} for tenant {}",
            patientId, startDate, endDate, tenantId);

        Page<PatientEducationLogEntity> results =
            patientEducationService.getEducationByDateRange(
                tenantId, patientId, startDate, endDate, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * Find education sessions with poor patient understanding
     *
     * Identifies education sessions where patient showed poor or fair understanding.
     * These require follow-up education and reinforcement.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return list of education logs needing follow-up
     */
    @GetMapping("/poor-understanding")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Get education sessions with poor understanding",
        description = "Retrieves education sessions needing follow-up due to poor patient understanding"
    )
    public ResponseEntity<List<PatientEducationLogEntity>> findWithPoorUnderstanding(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {
        log.info("Retrieving education sessions with poor understanding from tenant {}", tenantId);

        List<PatientEducationLogEntity> results =
            patientEducationService.findWithPoorUnderstanding(tenantId);

        return ResponseEntity.ok(results);
    }

    /**
     * Find interpreted education sessions
     *
     * Retrieves education sessions where interpreter was used, for tracking
     * language services and ensuring accessibility.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param pageable pagination parameters
     * @return paginated interpreted education sessions
     */
    @GetMapping("/interpreted-sessions")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get interpreted education sessions",
        description = "Retrieves education sessions where interpreter was used"
    )
    public ResponseEntity<Page<PatientEducationLogEntity>> findInterpretedSessions(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            Pageable pageable) {
        log.info("Finding interpreted education sessions in tenant {}", tenantId);

        Page<PatientEducationLogEntity> results =
            patientEducationService.findInterpretedSessions(tenantId, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * Update patient education log
     *
     * Allows updating notes and follow-up status after initial creation.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the education log ID
     * @param educationLog the education log with updates
     * @return updated education log
     */
    @PutMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Update patient education log",
        description = "Updates education log with follow-up notes and status"
    )
    public ResponseEntity<PatientEducationLogEntity> updateEducationLog(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody PatientEducationLogEntity educationLog) {
        log.info("Updating patient education log {} in tenant {}", id, tenantId);

        educationLog.setId(id);
        educationLog.setTenantId(tenantId);
        PatientEducationLogEntity updated = patientEducationService.updateEducationLog(educationLog);

        return ResponseEntity.ok(updated);
    }

    /**
     * Delete patient education log
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the education log ID to delete
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Delete patient education log",
        description = "Deletes a patient education log record"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Education log deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Void> deleteEducationLog(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id) {
        log.info("Deleting patient education log {} in tenant {}", id, tenantId);

        patientEducationService.deleteEducationLog(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get patient education metrics
     *
     * Returns summary metrics for a specific patient: total sessions, material types covered, etc.
     * Used for dashboard analytics and quality reporting.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param patientId the patient ID
     * @return education metrics summary
     */
    @GetMapping("/metrics/{patientId}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get patient education metrics",
        description = "Retrieves education metrics for a specific patient"
    )
    public ResponseEntity<PatientEducationService.PatientEducationMetrics> getPatientEducationMetrics(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID patientId) {
        log.info("Retrieving education metrics for patient {} from tenant {}", patientId, tenantId);

        PatientEducationService.PatientEducationMetrics metrics =
            patientEducationService.getPatientEducationMetrics(tenantId, patientId);

        return ResponseEntity.ok(metrics);
    }
}
