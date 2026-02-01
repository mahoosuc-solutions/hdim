package com.healthdata.nurseworkflow.api.v1;

import com.healthdata.nurseworkflow.application.MedicationReconciliationService;
import com.healthdata.nurseworkflow.domain.model.MedicationReconciliationEntity;
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
import java.util.UUID;

/**
 * Medication Reconciliation REST Controller
 *
 * Provides REST endpoints for medication reconciliation management including:
 * - Starting med rec workflow when triggered by transitions of care
 * - Tracking medications, discrepancies, and patient education
 * - Completing med rec process with teach-back verification
 * - Retrieving pending med recs for task queue
 * - Quality reporting and metrics
 *
 * Implements Joint Commission NPSG.03.06.01 requirement.
 *
 * All endpoints are multi-tenant aware and require X-Tenant-ID header.
 *
 * API Version: v1
 * Base Path: /nurse-workflow/medication-reconciliations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/medication-reconciliations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Medication Reconciliation", description = "Medication reconciliation workflow management")
@SecurityRequirement(name = "gateway-auth")
public class MedicationReconciliationController {

    private final MedicationReconciliationService medRecService;

    /**
     * Start medication reconciliation workflow
     *
     * Initiates med rec process triggered by transition of care event:
     * - Hospital admission
     * - Hospital discharge
     * - ED visit
     * - Specialty referral
     * - Routine reconciliation
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param medRec the medication reconciliation to start
     * @return created medication reconciliation with 201 status
     */
    @PostMapping
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Start medication reconciliation",
        description = "Initiates medication reconciliation process at transitions of care"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Medication reconciliation started"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MedicationReconciliationEntity> startReconciliation(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody MedicationReconciliationEntity medRec) {
        log.info("Starting medication reconciliation for patient {} in tenant {}",
            medRec.getPatientId(), tenantId);

        medRec.setTenantId(tenantId);
        MedicationReconciliationEntity created = medRecService.startReconciliation(medRec);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Complete medication reconciliation
     *
     * Finalizes med rec process after:
     * - Gathering all medications (EHR, discharge, patient-reported)
     * - Identifying and resolving discrepancies
     * - Providing patient education
     * - Verifying understanding via teach-back method
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param medRec the medication reconciliation to complete
     * @return completed medication reconciliation with 200 status
     */
    @PutMapping("/complete")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Complete medication reconciliation",
        description = "Finalizes medication reconciliation process after patient education"
    )
    public ResponseEntity<MedicationReconciliationEntity> completeReconciliation(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody MedicationReconciliationEntity medRec) {
        log.info("Completing medication reconciliation {} in tenant {}",
            medRec.getId(), tenantId);

        medRec.setTenantId(tenantId);
        MedicationReconciliationEntity completed = medRecService.completeReconciliation(medRec);

        return ResponseEntity.ok(completed);
    }

    /**
     * Get medication reconciliation by ID
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the reconciliation ID
     * @return medication reconciliation if found, 404 otherwise
     */
    @GetMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get medication reconciliation",
        description = "Retrieves a specific medication reconciliation by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Medication reconciliation found"),
        @ApiResponse(responseCode = "404", description = "Medication reconciliation not found")
    })
    public ResponseEntity<MedicationReconciliationEntity> getMedicationReconciliation(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id) {
        log.info("Retrieving medication reconciliation: {} from tenant {}", id, tenantId);

        return medRecService.getMedicationReconciliationById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get pending medication reconciliations for tenant
     *
     * Returns med recs in REQUESTED or IN_PROGRESS status, ordered by
     * start time (oldest first for priority queue).
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of pending med recs
     */
    @GetMapping("/pending")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get pending medication reconciliations",
        description = "Retrieves medication reconciliations requiring action"
    )
    public ResponseEntity<Page<MedicationReconciliationEntity>> getPendingReconciliations(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            Pageable pageable) {
        log.info("Retrieving pending medication reconciliations from tenant {}", tenantId);

        Page<MedicationReconciliationEntity> pending =
            medRecService.getPendingReconciliations(tenantId, pageable);

        return ResponseEntity.ok(pending);
    }

    /**
     * Get medication reconciliations by patient
     *
     * Retrieves all med recs for a specific patient, ordered by most recent first.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return paginated patient med rec history
     */
    @GetMapping("/patient/{patientId}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get patient medication reconciliation history",
        description = "Retrieves all medication reconciliations for a specific patient"
    )
    public ResponseEntity<Page<MedicationReconciliationEntity>> getPatientMedicationReconciliationHistory(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID patientId,
            Pageable pageable) {
        log.info("Retrieving med rec history for patient {} from tenant {}",
            patientId, tenantId);

        Page<MedicationReconciliationEntity> history =
            medRecService.getPatientMedicationReconciliationHistory(
                tenantId, patientId, pageable);

        return ResponseEntity.ok(history);
    }

    /**
     * Get medication reconciliations by trigger type
     *
     * Filters med recs by what event triggered them (admission, discharge, etc.)
     * Useful for quality reporting and trend analysis.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param triggerType the trigger type filter
     * @param pageable pagination parameters
     * @return paginated med recs with specified trigger
     */
    @GetMapping("/trigger/{triggerType}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get medication reconciliations by trigger type",
        description = "Retrieves medication reconciliations filtered by what triggered them"
    )
    public ResponseEntity<Page<MedicationReconciliationEntity>> getByTriggerType(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable MedicationReconciliationEntity.TriggerType triggerType,
            Pageable pageable) {
        log.info("Retrieving med recs by trigger type: {} from tenant {}",
            triggerType, tenantId);

        Page<MedicationReconciliationEntity> results =
            medRecService.getReconciliationsByTriggerType(tenantId, triggerType, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * Update medication reconciliation
     *
     * Allows updating medication count, discrepancy count, patient education status,
     * and notes throughout the reconciliation process.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the reconciliation ID
     * @param medRec the reconciliation with updates
     * @return updated medication reconciliation
     */
    @PutMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')") // Write operation
    @Operation(
        summary = "Update medication reconciliation",
        description = "Updates medication reconciliation with medication counts, discrepancies, and education status"
    )
    public ResponseEntity<MedicationReconciliationEntity> updateReconciliation(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody MedicationReconciliationEntity medRec) {
        log.info("Updating medication reconciliation {} in tenant {}", id, tenantId);

        medRec.setId(id);
        medRec.setTenantId(tenantId);
        MedicationReconciliationEntity updated = medRecService.updateReconciliation(medRec);

        return ResponseEntity.ok(updated);
    }

    /**
     * Get medication reconciliation metrics
     *
     * Returns summary metrics for quality reporting: total reconciliations,
     * pending count, completion rate.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return medication reconciliation metrics
     */
    @GetMapping("/metrics/summary")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get medication reconciliation metrics",
        description = "Retrieves metrics for medication reconciliation completion and quality"
    )
    public ResponseEntity<MedicationReconciliationService.MedicationReconciliationMetrics> getMetrics(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {
        log.info("Retrieving med rec metrics from tenant {}", tenantId);

        MedicationReconciliationService.MedicationReconciliationMetrics metrics =
            medRecService.getMetrics(tenantId);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Get medication reconciliations with poor patient understanding
     *
     * Returns med recs where patient showed poor understanding in teach-back assessment.
     * These require follow-up education and intervention.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return list of med recs needing follow-up education
     */
    @GetMapping("/poor-understanding")

    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get med recs with poor understanding",
        description = "Retrieves medication reconciliations where patient showed poor understanding"
    )
    public ResponseEntity<java.util.List<MedicationReconciliationEntity>> getPoorUnderstanding(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {
        log.info("Retrieving med recs with poor understanding from tenant {}", tenantId);

        java.util.List<MedicationReconciliationEntity> results =
            medRecService.findWithPoorUnderstanding(tenantId);

        return ResponseEntity.ok(results);
    }
}
