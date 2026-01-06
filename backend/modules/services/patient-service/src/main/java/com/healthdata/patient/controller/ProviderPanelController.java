package com.healthdata.patient.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.patient.dto.ProviderPanelRequest;
import com.healthdata.patient.dto.ProviderPanelResponse;
import com.healthdata.patient.entity.ProviderPanelAssignmentEntity;
import com.healthdata.patient.service.ProviderPanelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for provider panel management.
 * Issue #135: Create Provider Panel Assignment API
 *
 * HIPAA Compliance:
 * - All responses include Cache-Control: no-store headers
 * - All PHI access is audited
 * - Multi-tenant filtering is enforced via X-Auth-Tenant-Ids header
 */
@RestController
@RequestMapping("/api/v1/providers/{providerId}/panel")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Provider Panel", description = "Manage provider patient panels")
public class ProviderPanelController {

    private final ProviderPanelService panelService;

    /**
     * Get provider's patient panel with pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Get provider panel",
               description = "Returns paginated list of patients assigned to the provider")
    @Audited(eventType = "PROVIDER_PANEL_VIEW", description = "View provider panel")
    public ResponseEntity<Page<ProviderPanelResponse>> getProviderPanel(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable UUID providerId,
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "50") int size,
            HttpServletResponse response) {

        // HIPAA: Set cache control headers
        setCacheControlHeaders(response);

        log.info("Getting panel for provider {} in tenant {}, page={}, size={}",
                providerId, tenantId, page, size);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // Max 100 per page

        Page<ProviderPanelAssignmentEntity> assignments =
                panelService.getProviderPanel(tenantId, providerId, pageable);

        Page<ProviderPanelResponse> responsePage = assignments.map(ProviderPanelResponse::fromEntity);

        return ResponseEntity.ok(responsePage);
    }

    /**
     * Get count of patients in provider's panel.
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Get panel count",
               description = "Returns count of patients in the provider's panel")
    public ResponseEntity<Map<String, Long>> getPanelCount(
            @PathVariable UUID providerId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        long count = panelService.getProviderPanelCount(tenantId, providerId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get patient IDs in provider's panel.
     */
    @GetMapping("/patient-ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Get panel patient IDs",
               description = "Returns list of patient IDs in the provider's panel")
    @Audited(eventType = "PROVIDER_PANEL_IDS", description = "Get panel patient IDs")
    public ResponseEntity<List<UUID>> getPanelPatientIds(
            @PathVariable UUID providerId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        List<UUID> patientIds = panelService.getProviderPanelPatientIds(tenantId, providerId);
        return ResponseEntity.ok(patientIds);
    }

    /**
     * Check if a patient is in the provider's panel.
     */
    @GetMapping("/patients/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Check patient in panel",
               description = "Check if a specific patient is assigned to the provider")
    public ResponseEntity<Map<String, Boolean>> isPatientInPanel(
            @PathVariable UUID providerId,
            @PathVariable UUID patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        boolean inPanel = panelService.isPatientInPanel(tenantId, providerId, patientId);
        return ResponseEntity.ok(Map.of("inPanel", inPanel));
    }

    /**
     * Assign a patient to the provider's panel.
     */
    @PostMapping("/patients")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Assign patient to panel",
               description = "Assign a patient to the provider's panel")
    @Audited(eventType = "PROVIDER_PANEL_ASSIGN", description = "Assign patient to panel")
    public ResponseEntity<ProviderPanelResponse> assignPatient(
            @PathVariable UUID providerId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody ProviderPanelRequest request,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        log.info("Assigning patient {} to provider {} in tenant {}",
                request.getPatientId(), providerId, tenantId);

        ProviderPanelAssignmentEntity assignment = panelService.assignPatientToProvider(
                tenantId,
                providerId,
                request.getPatientId(),
                request.getAssignmentType(),
                request.getNotes()
        );

        return ResponseEntity.ok(ProviderPanelResponse.fromEntity(assignment));
    }

    /**
     * Bulk assign patients to the provider's panel.
     */
    @PostMapping("/patients/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk assign patients",
               description = "Bulk assign multiple patients to the provider's panel (Admin only)")
    @Audited(eventType = "PROVIDER_PANEL_BULK_ASSIGN", description = "Bulk assign patients")
    public ResponseEntity<Map<String, Integer>> bulkAssignPatients(
            @PathVariable UUID providerId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody ProviderPanelRequest.BulkAssignment request,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        log.info("Bulk assigning {} patients to provider {} in tenant {}",
                request.getPatientIds().size(), providerId, tenantId);

        int assigned = panelService.bulkAssignPatientsToProvider(
                tenantId,
                providerId,
                request.getPatientIds(),
                request.getAssignmentType()
        );

        return ResponseEntity.ok(Map.of("assignedCount", assigned));
    }

    /**
     * Remove a patient from the provider's panel.
     */
    @DeleteMapping("/patients/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Remove patient from panel",
               description = "Remove a patient from the provider's panel")
    @Audited(eventType = "PROVIDER_PANEL_REMOVE", description = "Remove patient from panel")
    public ResponseEntity<Void> removePatient(
            @PathVariable UUID providerId,
            @PathVariable UUID patientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            HttpServletResponse response) {

        setCacheControlHeaders(response);

        log.info("Removing patient {} from provider {} in tenant {}", patientId, providerId, tenantId);

        panelService.removePatientFromProvider(tenantId, providerId, patientId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set HIPAA-compliant cache control headers.
     */
    private void setCacheControlHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }
}
