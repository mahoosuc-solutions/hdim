package com.healthdata.qualitymeasure.api.v1.controller;

import com.healthdata.qualitymeasure.api.v1.dto.EvaluateMeasureRequest;
import com.healthdata.qualitymeasure.api.v1.dto.MeasureEventResponse;
import com.healthdata.qualitymeasure.service.QualityMeasureEventApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Quality Measure Event Service REST Controller
 *
 * Handles measure evaluation events:
 * - POST /api/v1/measures/events/evaluate - Evaluate measure for patient
 * - GET /api/v1/measures/events/risk/{patientId} - Get patient risk score
 * - GET /api/v1/measures/events/cohort/compliance - Get cohort compliance rate
 *
 * All endpoints return 202 Accepted (async event processing)
 * Multi-tenant isolation via X-Tenant-ID header
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/measures/events")
@RequiredArgsConstructor
@Validated
public class QualityMeasureEventController {

    private final QualityMeasureEventApplicationService measureEventService;

    /**
     * Evaluate measure for patient
     *
     * @param request Evaluation request with patientId, measureCode, score
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with MeasureEventResponse containing status and score
     */
    @PostMapping(path = "/evaluate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureEventResponse> evaluateMeasure(
            @Valid @RequestBody EvaluateMeasureRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Evaluating measure: {}, patientId: {}, tenant: {}",
            request.getMeasureCode(), request.getPatientId(), tenantId);

        MeasureEventResponse response = measureEventService.evaluateMeasure(request, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get patient risk score
     *
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 200 OK with risk score and classification
     */
    @GetMapping(path = "/risk/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureEventResponse> getRiskScore(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Getting risk score for patient: {}, tenant: {}", patientId, tenantId);

        MeasureEventResponse response = measureEventService.getRiskScore(patientId, tenantId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get cohort compliance rate
     *
     * @param measureCode HEDIS measure code
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 200 OK with cohort compliance metrics
     */
    @GetMapping(path = "/cohort/compliance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureEventResponse> getCohortCompliance(
            @RequestParam String measureCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Getting cohort compliance for measure: {}, tenant: {}", measureCode, tenantId);

        MeasureEventResponse response = measureEventService.getCohortCompliance(measureCode, tenantId);

        return ResponseEntity.ok(response);
    }
}
