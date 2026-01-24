package com.healthdata.nurseworkflow.api.v1;

import com.healthdata.nurseworkflow.application.ReferralCoordinationService;
import com.healthdata.nurseworkflow.domain.model.ReferralCoordinationEntity;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * Referral Coordination REST Controller
 *
 * Provides REST endpoints for referral coordination management including:
 * - Creating referral orders to specialists
 * - Tracking insurance authorization status
 * - Managing specialist appointment scheduling
 * - Monitoring results receipt and follow-up completion
 * - Retrieving pending referrals for workqueue management
 * - Quality reporting and metrics
 *
 * Implements PCMH (Patient-Centered Medical Home) care coordination requirements
 * and closed-loop referral tracking.
 *
 * All endpoints are multi-tenant aware and require X-Tenant-ID header.
 *
 * API Version: v1
 * Base Path: /nurse-workflow/referral-coordinations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/referral-coordinations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Referral Coordination", description = "Referral coordination and closed-loop tracking management")
@SecurityRequirement(name = "gateway-auth")
public class ReferralCoordinationController {

    private final ReferralCoordinationService referralCoordinationService;

    /**
     * Create referral to specialist
     *
     * Initiates referral request to specialist. Sets initial status to
     * PENDING_AUTHORIZATION if insurance auth required, or AUTHORIZED if not.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param referral the referral to create
     * @return created referral with 201 status
     */
    @PostMapping
    
    @PreAuthorize("hasPermission(#tenantId, 'CARE_GAP_WRITE')") // Care coordination write
    @Operation(
        summary = "Create referral",
        description = "Initiates referral request to specialist"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Referral created"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ReferralCoordinationEntity> createReferral(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @Valid @RequestBody ReferralCoordinationEntity referral) {
        log.info("Creating referral for patient {} to specialty: {} in tenant {}",
            referral.getPatientId(), referral.getSpecialtyType(), tenantId);

        referral.setTenantId(tenantId);
        ReferralCoordinationEntity created = referralCoordinationService.createReferral(referral);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get referral by ID
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the referral ID
     * @return referral if found, 404 otherwise
     */
    @GetMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get referral",
        description = "Retrieves a specific referral by ID"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Referral found"),
        @ApiResponse(responseCode = "404", description = "Referral not found")
    })
    public ResponseEntity<ReferralCoordinationEntity> getReferral(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id) {
        log.info("Retrieving referral: {} from tenant {}", id, tenantId);

        return referralCoordinationService.getReferralById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get pending referrals for tenant
     *
     * Returns referrals not yet completed (PENDING_AUTH, AUTHORIZED, SCHEDULED, AWAITING_APT).
     * Ordered by request time (oldest first for priority).
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of pending referrals
     */
    @GetMapping("/pending")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get pending referrals",
        description = "Retrieves referrals requiring action"
    )
    public ResponseEntity<Page<ReferralCoordinationEntity>> getPendingReferrals(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            Pageable pageable) {
        log.info("Retrieving pending referrals from tenant {}", tenantId);

        Page<ReferralCoordinationEntity> pending =
            referralCoordinationService.getPendingReferrals(tenantId, pageable);

        return ResponseEntity.ok(pending);
    }

    /**
     * Get patient referral history
     *
     * Retrieves all referrals for a specific patient, ordered by most recent first.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param patientId the patient ID
     * @param pageable pagination parameters
     * @return paginated patient referral history
     */
    @GetMapping("/patient/{patientId}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation
    @Operation(
        summary = "Get patient referral history",
        description = "Retrieves all referrals for a specific patient"
    )
    public ResponseEntity<Page<ReferralCoordinationEntity>> getPatientReferralHistory(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID patientId,
            Pageable pageable) {
        log.info("Retrieving referral history for patient {} from tenant {}",
            patientId, tenantId);

        Page<ReferralCoordinationEntity> history =
            referralCoordinationService.getPatientReferralHistory(tenantId, patientId, pageable);

        return ResponseEntity.ok(history);
    }

    /**
     * Get referrals by status
     *
     * Filters referrals by status (authorization pending, scheduled, completed, etc.)
     * for workqueue management.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param status the status filter
     * @param pageable pagination parameters
     * @return paginated referrals with specified status
     */
    @GetMapping("/status/{status}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get referrals by status",
        description = "Retrieves referrals filtered by status"
    )
    public ResponseEntity<Page<ReferralCoordinationEntity>> getReferralsByStatus(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable ReferralCoordinationEntity.ReferralStatus status,
            Pageable pageable) {
        log.info("Retrieving referrals by status: {} from tenant {}", status, tenantId);

        Page<ReferralCoordinationEntity> results =
            referralCoordinationService.getReferralsByStatus(tenantId, status, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * Get referrals by specialty type
     *
     * Filters referrals by specialty (cardiology, podiatry, etc.) for
     * specialty-specific coordination and metrics.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param specialtyType the specialty type filter
     * @param pageable pagination parameters
     * @return paginated referrals to specified specialty
     */
    @GetMapping("/specialty/{specialtyType}")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get referrals by specialty",
        description = "Retrieves referrals filtered by specialty type"
    )
    public ResponseEntity<Page<ReferralCoordinationEntity>> getReferralsBySpecialty(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable String specialtyType,
            Pageable pageable) {
        log.info("Retrieving referrals by specialty: {} from tenant {}", specialtyType, tenantId);

        Page<ReferralCoordinationEntity> results =
            referralCoordinationService.getReferralsBySpecialty(tenantId, specialtyType, pageable);

        return ResponseEntity.ok(results);
    }

    /**
     * Get referrals awaiting appointment scheduling
     *
     * Returns referrals that have authorization but no appointment yet.
     * These need immediate coordination action.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return list of referrals needing appointment scheduling
     */
    @GetMapping("/awaiting-appointment-scheduling")
    
    @PreAuthorize("hasPermission(#tenantId, 'CARE_GAP_WRITE')") // Care coordination write
    @Operation(
        summary = "Get referrals awaiting appointment scheduling",
        description = "Retrieves referrals that need appointment scheduling coordination"
    )
    public ResponseEntity<List<ReferralCoordinationEntity>> findAwaitingAppointmentScheduling(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {
        log.info("Finding referrals awaiting appointment scheduling in tenant {}", tenantId);

        List<ReferralCoordinationEntity> results =
            referralCoordinationService.findAwaitingAppointmentScheduling(tenantId);

        return ResponseEntity.ok(results);
    }

    /**
     * Get referrals awaiting results follow-up
     *
     * Returns referrals where patient attended appointment but results not
     * yet received. These need follow-up action to close the loop.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return list of referrals needing results follow-up
     */
    @GetMapping("/awaiting-results")
    
    @PreAuthorize("hasPermission(#tenantId, 'CARE_GAP_WRITE')") // Care coordination write
    @Operation(
        summary = "Get referrals awaiting results follow-up",
        description = "Retrieves referrals where patient attended appointment but results pending"
    )
    public ResponseEntity<List<ReferralCoordinationEntity>> findAwaitingResults(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {
        log.info("Finding referrals awaiting results in tenant {}", tenantId);

        List<ReferralCoordinationEntity> results =
            referralCoordinationService.findAwaitingResults(tenantId);

        return ResponseEntity.ok(results);
    }

    /**
     * Get urgent referrals awaiting scheduling
     *
     * Returns URGENT priority referrals that don't yet have appointments.
     * These need immediate action.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return list of urgent referrals needing scheduling
     */
    @GetMapping("/urgent-awaiting-scheduling")
    
    @PreAuthorize("hasPermission(#tenantId, 'CARE_GAP_WRITE')") // Care coordination write
    @Operation(
        summary = "Get urgent referrals awaiting scheduling",
        description = "Retrieves URGENT priority referrals that need immediate scheduling coordination"
    )
    public ResponseEntity<List<ReferralCoordinationEntity>> findUrgentAwaitingScheduling(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {
        log.info("Finding urgent referrals awaiting scheduling in tenant {}", tenantId);

        List<ReferralCoordinationEntity> results =
            referralCoordinationService.findUrgentAwaitingScheduling(tenantId);

        return ResponseEntity.ok(results);
    }

    /**
     * Update referral
     *
     * Allows updating status, authorization number, appointment details,
     * and results as referral progresses through workflow.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @param id the referral ID
     * @param referral the referral with updates
     * @return updated referral
     */
    @PutMapping("/{id}")
    
    @PreAuthorize("hasPermission(#tenantId, 'CARE_GAP_WRITE')") // Care coordination write
    @Operation(
        summary = "Update referral",
        description = "Updates referral status, authorization, appointment, and results tracking"
    )
    public ResponseEntity<ReferralCoordinationEntity> updateReferral(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody ReferralCoordinationEntity referral) {
        log.info("Updating referral {} in tenant {}", id, tenantId);

        referral.setId(id);
        referral.setTenantId(tenantId);
        ReferralCoordinationEntity updated = referralCoordinationService.updateReferral(referral);

        return ResponseEntity.ok(updated);
    }

    /**
     * Get referral completion metrics
     *
     * Returns summary metrics for quality reporting: total referrals,
     * pending count, completion rate.
     *
     * @param tenantId the tenant ID from X-Tenant-ID header
     * @return referral metrics summary
     */
    @GetMapping("/metrics/summary")
    
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')") // Read operation (analytics)
    @Operation(
        summary = "Get referral metrics",
        description = "Retrieves metrics for referral completion and quality"
    )
    public ResponseEntity<ReferralCoordinationService.ReferralMetrics> getMetrics(
            @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId) {
        log.info("Retrieving referral metrics from tenant {}", tenantId);

        ReferralCoordinationService.ReferralMetrics metrics =
            referralCoordinationService.getMetrics(tenantId);

        return ResponseEntity.ok(metrics);
    }
}
