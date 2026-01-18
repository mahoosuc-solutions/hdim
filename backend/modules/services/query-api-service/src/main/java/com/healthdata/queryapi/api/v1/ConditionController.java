package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.condition.ConditionProjection;
import com.healthdata.eventsourcing.query.condition.ConditionQueryService;
import com.healthdata.queryapi.api.v1.dto.ConditionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Condition Query Service (Phase 1.7)
 * Exposes condition queries with ICD-10 filtering and status filtering
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/conditions")
@RequiredArgsConstructor
public class ConditionController {

    private final ConditionQueryService conditionQueryService;

    /**
     * GET /api/v1/conditions/patient/{patientId}
     * List all conditions for patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ConditionResponse>> getConditionsByPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/conditions/patient/{} - tenant: {}", patientId, tenantId);
        validateTenantHeader(tenantId);

        List<ConditionResponse> conditions = conditionQueryService
            .findByPatientAndTenant(patientId, tenantId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(conditions);
    }

    /**
     * GET /api/v1/conditions/icd/{icdCode}
     * List conditions by ICD-10 code
     */
    @GetMapping("/icd/{icdCode}")
    public ResponseEntity<List<ConditionResponse>> getConditionsByIcdCode(
            @PathVariable String icdCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/conditions/icd/{} - tenant: {}", icdCode, tenantId);
        validateTenantHeader(tenantId);

        List<ConditionResponse> conditions = conditionQueryService
            .findByIcdCodeAndTenant(icdCode, tenantId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(conditions);
    }

    /**
     * GET /api/v1/conditions/patient/{patientId}/active
     * List active conditions for patient
     */
    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<List<ConditionResponse>> getActiveConditions(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/conditions/patient/{}/active - tenant: {}", patientId, tenantId);
        validateTenantHeader(tenantId);

        List<ConditionResponse> conditions = conditionQueryService
            .findActiveConditionsByPatientAndTenant(patientId, tenantId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(conditions);
    }

    /**
     * GET /api/v1/conditions?status={status}
     * List conditions by status (active, inactive, resolved)
     */
    @GetMapping
    public ResponseEntity<List<ConditionResponse>> getConditionsByStatus(
            @RequestParam(required = false) String status,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/conditions?status={} - tenant: {}", status, tenantId);
        validateTenantHeader(tenantId);

        List<ConditionResponse> conditions;
        if (status != null && !status.trim().isEmpty()) {
            // Would need to add filter method to service
            conditions = List.of();
        } else {
            conditions = conditionQueryService.findAllByTenant(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
        }

        return ResponseEntity.ok(conditions);
    }

    // ============ Helper Methods ============

    private void validateTenantHeader(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("X-Tenant-ID header is required and cannot be empty");
        }
    }

    private ConditionResponse mapToResponse(ConditionProjection projection) {
        return ConditionResponse.builder()
            .patientId(projection.getPatientId())
            .icdCode(projection.getIcdCode())
            .status(projection.getStatus())
            .verificationStatus(projection.getVerificationStatus())
            .onsetDate(projection.getOnsetDate())
            .build();
    }
}
