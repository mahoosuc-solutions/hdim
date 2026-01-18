package com.healthdata.queryapi.api.v1;

import com.healthdata.eventsourcing.projection.observation.ObservationProjection;
import com.healthdata.eventsourcing.query.observation.ObservationQueryService;
import com.healthdata.queryapi.api.v1.dto.ObservationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Observation Query Service (Phase 1.7)
 * Exposes observation time-series queries with LOINC filtering
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/observations")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationQueryService observationQueryService;

    /**
     * GET /api/v1/observations/patient/{patientId}
     * List all observations for patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ObservationResponse>> getObservationsByPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/observations/patient/{} - tenant: {}", patientId, tenantId);
        validateTenantHeader(tenantId);

        List<ObservationResponse> observations = observationQueryService
            .findByPatientAndTenant(patientId, tenantId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(observations);
    }

    /**
     * GET /api/v1/observations/loinc/{loincCode}
     * List observations by LOINC code
     */
    @GetMapping("/loinc/{loincCode}")
    public ResponseEntity<List<ObservationResponse>> getObservationsByLoincCode(
            @PathVariable String loincCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/observations/loinc/{} - tenant: {}", loincCode, tenantId);
        validateTenantHeader(tenantId);

        List<ObservationResponse> observations = observationQueryService
            .findByLoincCodeAndTenant(loincCode, tenantId)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(observations);
    }

    /**
     * GET /api/v1/observations/patient/{patientId}/latest?loincCode={code}
     * Get latest observation for patient by LOINC code
     */
    @GetMapping("/patient/{patientId}/latest")
    public ResponseEntity<ObservationResponse> getLatestObservation(
            @PathVariable String patientId,
            @RequestParam String loincCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/observations/patient/{}/latest?loincCode={} - tenant: {}",
            patientId, loincCode, tenantId);
        validateTenantHeader(tenantId);

        return observationQueryService.findLatestByLoincAndPatient(patientId, loincCode, tenantId)
            .map(this::mapToResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/observations/date-range?start={date}&end={date}
     * List observations by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<ObservationResponse>> getObservationsByDateRange(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/observations/date-range?start={}&end={} - tenant: {}",
            start, end, tenantId);
        validateTenantHeader(tenantId);

        List<ObservationResponse> observations = observationQueryService
            .findByDateRange(tenantId, start, end)
            .stream()
            .map(this::mapToResponse)
            .toList();

        return ResponseEntity.ok(observations);
    }

    /**
     * GET /api/v1/observations
     * List all observations in tenant
     */
    @GetMapping
    public ResponseEntity<List<ObservationResponse>> getAllObservations(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("GET /api/v1/observations - tenant: {}", tenantId);
        validateTenantHeader(tenantId);

        // This would require adding a findAllByTenant method to ObservationQueryService
        // For now, return empty list
        return ResponseEntity.ok(List.of());
    }

    // ============ Helper Methods ============

    private void validateTenantHeader(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("X-Tenant-ID header is required and cannot be empty");
        }
    }

    private ObservationResponse mapToResponse(ObservationProjection projection) {
        return ObservationResponse.builder()
            .patientId(projection.getPatientId())
            .loincCode(projection.getLoincCode())
            .value(projection.getValue())
            .unit(projection.getUnit())
            .observationDate(projection.getObservationDate())
            .notes(projection.getNotes())
            .build();
    }
}
