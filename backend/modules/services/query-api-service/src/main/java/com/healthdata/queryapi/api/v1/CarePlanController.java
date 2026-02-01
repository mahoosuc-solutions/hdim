package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.careplan.CarePlanProjection;
import com.healthdata.eventsourcing.query.careplan.CarePlanQueryService;
import com.healthdata.queryapi.api.v1.dto.CarePlanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for CarePlan Query Service (Phase 1.7)
 * Exposes care plan queries with coordinator and status filtering
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/care-plans")
@RequiredArgsConstructor
public class CarePlanController {

    private final CarePlanQueryService carePlanQueryService;

    /**
     * GET /api/v1/care-plans/patient/{patientId}
     * List all care plans for patient
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    public ResponseEntity<List<CarePlanResponse>> getCarePlansByPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/care-plans/patient/{} - tenant: {}", patientId, tenantId);
        validateTenantHeader(tenantId);

        List<CarePlanResponse> plans = carePlanQueryService
            .findByPatientAndTenant(patientId, tenantId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(plans);
    }

    /**
     * GET /api/v1/care-plans/coordinator/{coordinatorId}
     * List all care plans assigned to coordinator
     */
    @GetMapping("/coordinator/{coordinatorId}")
    @PreAuthorize("hasPermission('PATIENT_SEARCH')")
    public ResponseEntity<List<CarePlanResponse>> getCarePlansByCoordinator(
            @PathVariable String coordinatorId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/care-plans/coordinator/{} - tenant: {}", coordinatorId, tenantId);
        validateTenantHeader(tenantId);

        List<CarePlanResponse> plans = carePlanQueryService
            .findByTenantAndCoordinator(tenantId, coordinatorId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(plans);
    }

    /**
     * GET /api/v1/care-plans/patient/{patientId}/active
     * List active care plans for patient
     */
    @GetMapping("/patient/{patientId}/active")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    public ResponseEntity<List<CarePlanResponse>> getActiveCarePlans(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/care-plans/patient/{}/active - tenant: {}", patientId, tenantId);
        validateTenantHeader(tenantId);

        List<CarePlanResponse> plans = carePlanQueryService
            .findActiveCarePlansByPatientAndTenant(patientId, tenantId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(plans);
    }

    /**
     * GET /api/v1/care-plans?status={status}
     * List care plans by status (draft, active, completed)
     */
    @GetMapping
    @PreAuthorize("hasPermission('PATIENT_SEARCH')")
    public ResponseEntity<List<CarePlanResponse>> getCarePlansByStatus(
            @RequestParam(required = false) String status,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/care-plans?status={} - tenant: {}", status, tenantId);
        validateTenantHeader(tenantId);

        List<CarePlanResponse> plans;
        if (status != null && !status.trim().isEmpty()) {
            plans = carePlanQueryService.findCarePlansByStatusAndTenant(tenantId, status)
                .stream()
                .map(this::mapToResponse)
                .toList();
        } else {
            plans = carePlanQueryService.findAllByTenant(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        }

        return ResponseEntity.ok(plans);
    }

    /**
     * GET /api/v1/care-plans/patient/{patientId}/title/{title}
     * Find care plan by patient and title
     */
    @GetMapping("/patient/{patientId}/title/{title}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    public ResponseEntity<CarePlanResponse> getCarePlanByTitle(
            @PathVariable String patientId,
            @PathVariable String title,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/care-plans/patient/{}/title/{} - tenant: {}",
            patientId, title, tenantId);
        validateTenantHeader(tenantId);

        return carePlanQueryService.findByPatientAndTenantAndTitle(patientId, tenantId, title)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ============ Helper Methods ============

    private void validateTenantHeader(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("X-Tenant-ID header is required and cannot be empty");
        }
    }

    private CarePlanResponse mapToResponse(CarePlanProjection projection) {
        return CarePlanResponse.builder()
            .patientId(projection.getPatientId())
            .title(projection.getTitle())
            .status(projection.getStatus())
            .coordinatorId(projection.getCoordinatorId())
            .startDate(projection.getStartDate())
            .endDate(projection.getEndDate())
            .goalCount(projection.getGoalCount())
            .build();
    }
}
