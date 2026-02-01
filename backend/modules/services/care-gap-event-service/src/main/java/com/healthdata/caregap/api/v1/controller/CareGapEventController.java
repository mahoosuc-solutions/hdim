package com.healthdata.caregap.api.v1.controller;

import com.healthdata.caregap.api.v1.dto.CareGapEventResponse;
import com.healthdata.caregap.api.v1.dto.CloseGapRequest;
import com.healthdata.caregap.api.v1.dto.DetectGapRequest;
import com.healthdata.caregap.api.v1.dto.InterventionRequest;
import com.healthdata.caregap.api.v1.dto.QualifyGapRequest;
import com.healthdata.caregap.service.CareGapEventApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Care Gap Event Service REST Controller
 *
 * Handles care gap detection and closure events:
 * - POST /api/v1/gaps/events/detect - Detect care gap for patient
 * - POST /api/v1/gaps/events/qualify - Qualify patient for care gap
 * - POST /api/v1/gaps/events/intervene - Recommend intervention
 * - POST /api/v1/gaps/events/close - Close care gap
 * - POST /api/v1/gaps/events/close/{gapId} - Close care gap by id
 * - GET /api/v1/gaps/events/population/health - Get population health metrics
 *
 * All endpoints return 202 Accepted (async event processing)
 * Multi-tenant isolation via X-Tenant-ID header
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gaps/events")
@RequiredArgsConstructor
@Validated
public class CareGapEventController {

    private final CareGapEventApplicationService gapEventService;

    /**
     * Detect care gap for patient
     *
     * @param request Detection request with patientId, gapCode, description, severity
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with CareGapEventResponse
     */
    @PostMapping(path = "/detect", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapEventResponse> detectGap(
            @Valid @RequestBody DetectGapRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Detecting gap: {}, patient: {}, severity: {}, tenant: {}",
            request.getGapCode(), request.getPatientId(), request.getSeverity(), tenantId);

        CareGapEventResponse response = gapEventService.detectGap(request, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Qualify patient for a care gap
     */
    @PostMapping(path = "/qualify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapEventResponse> qualifyGap(
            @Valid @RequestBody QualifyGapRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Qualifying patient {} for gap {} in tenant {}",
            request.getPatientId(), request.getGapCode(), tenantId);

        CareGapEventResponse response = gapEventService.qualifyPatient(request, tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Recommend intervention for a care gap
     */
    @PostMapping(path = "/intervene", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapEventResponse> recommendIntervention(
            @Valid @RequestBody InterventionRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Recommending intervention for patient {} gap {} in tenant {}",
            request.getPatientId(), request.getGapCode(), tenantId);

        CareGapEventResponse response = gapEventService.recommendIntervention(request, tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Close care gap by request payload
     */
    @PostMapping(path = "/close", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapEventResponse> closeGap(
            @Valid @RequestBody CloseGapRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Closing gap {} for patient {} in tenant {}",
            request.getGapCode(), request.getPatientId(), tenantId);

        CareGapEventResponse response = gapEventService.closeGap(request, tenantId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Close care gap
     *
     * @param gapId Gap identifier
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with updated gap status
     */
    @PostMapping(path = "/close/{gapId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapEventResponse> closeGapById(
            @PathVariable String gapId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Closing gap: {}, tenant: {}", gapId, tenantId);

        CareGapEventResponse response = gapEventService.closeGap(gapId, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get population health metrics
     *
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 200 OK with aggregated gap metrics
     */
    @GetMapping(path = "/population/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareGapEventResponse> getPopulationHealth(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Getting population health for tenant: {}", tenantId);

        CareGapEventResponse response = gapEventService.getPopulationHealth(tenantId);

        return ResponseEntity.ok(response);
    }
}
