package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.patient.PatientProjection;
import com.healthdata.eventsourcing.query.patient.PatientQueryService;
import com.healthdata.queryapi.api.v1.dto.PatientResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Patient Query Service (Phase 1.7)
 * Exposes patient lookup endpoints with multi-tenant isolation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientQueryService patientQueryService;

    /**
     * GET /api/v1/patients/{patientId}
     * Find patient by ID
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<PatientResponse> getPatientById(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/patients/{} - tenant: {}", patientId, tenantId);
        validateTenantHeader(tenantId);

        return patientQueryService.findByIdAndTenant(patientId, tenantId)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/patients/mrn/{mrn}
     * Find patient by MRN
     */
    @GetMapping("/mrn/{mrn}")
    public ResponseEntity<PatientResponse> getPatientByMrn(
            @PathVariable String mrn,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/patients/mrn/{} - tenant: {}", mrn, tenantId);
        validateTenantHeader(tenantId);

        return patientQueryService.findByMrnAndTenant(mrn, tenantId)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/patients/insurance/{memberId}
     * Find patient by insurance member ID
     */
    @GetMapping("/insurance/{memberId}")
    public ResponseEntity<PatientResponse> getPatientByInsuranceMemberId(
            @PathVariable String memberId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/patients/insurance/{} - tenant: {}", memberId, tenantId);
        validateTenantHeader(tenantId);

        return patientQueryService.findByInsuranceMemberIdAndTenant(memberId, tenantId)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/patients
     * List all patients in tenant
     */
    @GetMapping
    public ResponseEntity<List<PatientResponse>> getAllPatients(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/patients - tenant: {}", tenantId);
        validateTenantHeader(tenantId);

        List<PatientResponse> patients = patientQueryService.findAllByTenant(tenantId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(patients);
    }

    /**
     * OPTIONS /api/v1/patients
     * CORS preflight support
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> options() {
        return ResponseEntity.ok().build();
    }

    /**
     * HEAD /api/v1/patients/{patientId}
     * Check if patient exists
     */
    @RequestMapping(value = "/{patientId}", method = RequestMethod.HEAD)
    public ResponseEntity<?> headPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("HEAD /api/v1/patients/{} - tenant: {}", patientId, tenantId);
        validateTenantHeader(tenantId);

        return patientQueryService.findByIdAndTenant(patientId, tenantId)
            .map(p -> ResponseEntity.ok().<PatientResponse>build())
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ============ Helper Methods ============

    private void validateTenantHeader(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("X-Tenant-ID header is required and cannot be empty");
        }
    }

    private PatientResponse mapToResponse(PatientProjection projection) {
        return PatientResponse.builder()
            .patientId(projection.getPatientId())
            .firstName(projection.getFirstName())
            .lastName(projection.getLastName())
            .dateOfBirth(projection.getDateOfBirth())
            .mrn(projection.getMrn())
            .insuranceMemberId(projection.getInsuranceMemberId())
            .build();
    }
}
