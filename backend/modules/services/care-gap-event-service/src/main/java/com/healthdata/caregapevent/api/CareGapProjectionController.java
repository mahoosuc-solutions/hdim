package com.healthdata.caregapevent.api;

import com.healthdata.caregapevent.projection.CareGapProjection;
import com.healthdata.caregapevent.repository.CareGapProjectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Care Gap Projection Query API
 *
 * Provides fast read-only queries on denormalized care gap data.
 * Part of CQRS pattern - this service contains the read model.
 */
@RestController
@RequestMapping("/api/v1/care-gap-projections")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Care Gap Projections", description = "CQRS Read Model - Care Gap Projections")
public class CareGapProjectionController {

    private final CareGapProjectionRepository careGapRepository;

    /**
     * Get care gap projection by ID
     */
    @GetMapping("/{careGapId}")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get care gap projection", description = "Retrieve denormalized care gap view")
    public ResponseEntity<CareGapProjection> getCareGapProjection(
            @PathVariable UUID careGapId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching care gap projection for care gap {} in tenant {}", careGapId, tenantId);

        return careGapRepository.findByTenantIdAndCareGapId(tenantId, careGapId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all care gaps for a patient
     */
    @GetMapping("/by-patient/{patientId}")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get care gaps for patient", description = "Retrieve all care gaps for a patient")
    public ResponseEntity<List<CareGapProjection>> getCareGapsForPatient(
            @PathVariable UUID patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching care gap projections for patient {} in tenant {}", patientId, tenantId);

        List<CareGapProjection> careGaps = careGapRepository
            .findByTenantIdAndPatientIdOrderByPriorityDesc(tenantId, patientId);
        return ResponseEntity.ok(careGaps);
    }

    /**
     * Get open care gaps for a tenant (paginated)
     */
    @GetMapping("/open")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get open care gaps", description = "Retrieve all open care gaps for tenant")
    public ResponseEntity<Page<CareGapProjection>> getOpenCareGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        log.debug("Fetching open care gap projections for tenant {}", tenantId);

        Page<CareGapProjection> careGaps = careGapRepository
            .findOpenCareGapsForTenant(tenantId, pageable);
        return ResponseEntity.ok(careGaps);
    }

    /**
     * Get urgent care gaps
     */
    @GetMapping("/urgent")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get urgent care gaps", description = "Retrieve all urgent care gaps")
    public ResponseEntity<List<CareGapProjection>> getUrgentCareGaps(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching urgent care gap projections for tenant {}", tenantId);

        List<CareGapProjection> careGaps = careGapRepository.findUrgentCareGaps(tenantId);
        return ResponseEntity.ok(careGaps);
    }

    /**
     * Get care gaps by priority
     */
    @GetMapping("/by-priority/{priority}")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get care gaps by priority", description = "Retrieve care gaps for specific priority level")
    public ResponseEntity<List<CareGapProjection>> getCareGapsByPriority(
            @PathVariable String priority,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching care gap projections for tenant {} with priority {}", tenantId, priority);

        List<CareGapProjection> careGaps = careGapRepository
            .findCareGapsByPriority(tenantId, priority);
        return ResponseEntity.ok(careGaps);
    }

    /**
     * Get overdue care gaps
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get overdue care gaps", description = "Retrieve all overdue care gaps")
    public ResponseEntity<List<CareGapProjection>> getOverdueCareGaps(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching overdue care gap projections for tenant {}", tenantId);

        List<CareGapProjection> careGaps = careGapRepository.findOverdueCareGaps(tenantId);
        return ResponseEntity.ok(careGaps);
    }

    /**
     * Get care gaps due within N days
     */
    @GetMapping("/due-within-days")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get care gaps due within days", description = "Retrieve care gaps due within specified days")
    public ResponseEntity<List<CareGapProjection>> getCareGapsDueWithinDays(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam int days) {
        log.debug("Fetching care gap projections due within {} days for tenant {}", days, tenantId);

        List<CareGapProjection> careGaps = careGapRepository
            .findCareGapsDueWithinDays(tenantId, days);
        return ResponseEntity.ok(careGaps);
    }

    /**
     * Get care gaps for a specific measure
     */
    @GetMapping("/by-measure/{measureId}")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get care gaps by measure", description = "Retrieve care gaps for specific measure")
    public ResponseEntity<List<CareGapProjection>> getCareGapsByMeasure(
            @PathVariable String measureId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching care gap projections for measure {} in tenant {}", measureId, tenantId);

        List<CareGapProjection> careGaps = careGapRepository
            .findCareGapsByMeasure(tenantId, measureId);
        return ResponseEntity.ok(careGaps);
    }

    /**
     * Get care gaps assigned to a user
     */
    @GetMapping("/assigned-to/{assignedTo}")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get assigned care gaps", description = "Retrieve care gaps assigned to user")
    public ResponseEntity<List<CareGapProjection>> getCareGapsAssignedTo(
            @PathVariable String assignedTo,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching care gap projections assigned to {} in tenant {}", assignedTo, tenantId);

        List<CareGapProjection> careGaps = careGapRepository
            .findCareGapsAssignedTo(tenantId, assignedTo);
        return ResponseEntity.ok(careGaps);
    }

    /**
     * Get statistics for tenant
     */
    @GetMapping("/stats")
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    @Operation(summary = "Get care gap statistics", description = "Retrieve aggregated care gap statistics")
    public ResponseEntity<CareGapStatistics> getStatistics(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Calculating care gap statistics for tenant {}", tenantId);

        long totalOpen = careGapRepository.countOpenCareGapsForTenant(tenantId);
        long urgent = careGapRepository.countUrgentCareGapsForTenant(tenantId);
        long overdue = careGapRepository.countOverdueCareGapsForTenant(tenantId);

        CareGapStatistics stats = CareGapStatistics.builder()
            .totalOpenCareGaps(totalOpen)
            .urgentCareGaps(urgent)
            .overdueCareGaps(overdue)
            .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Service health status")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Care gap event service is healthy");
    }
}
