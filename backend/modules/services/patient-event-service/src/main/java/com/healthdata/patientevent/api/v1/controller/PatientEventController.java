package com.healthdata.patientevent.api.v1.controller;

import com.healthdata.patientevent.api.v1.dto.CreatePatientRequest;
import com.healthdata.patientevent.api.v1.dto.PatientEventResponse;
import com.healthdata.patientevent.service.PatientEventApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Patient Event Service REST Controller
 *
 * Handles patient lifecycle event submissions:
 * - POST /api/v1/patients/events/create - Create new patient
 * - POST /api/v1/patients/events/enroll - Enroll patient
 * - POST /api/v1/patients/events/demographics - Update patient demographics
 *
 * All endpoints return 202 Accepted (async event processing)
 * Multi-tenant isolation via X-Tenant-ID header
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patients/events")
@RequiredArgsConstructor
@Validated
public class PatientEventController {

    private final PatientEventApplicationService patientEventService;

    /**
     * Create new patient event
     *
     * @param request Patient creation request with firstName, lastName, dateOfBirth
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with PatientEventResponse containing patientId and status
     */
    @PostMapping(path = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientEventResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Creating patient event for tenant: {}", tenantId);

        PatientEventResponse response = patientEventService.createPatientEvent(request, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Enroll patient event
     *
     * @param enrollmentRequest JSON request with patientId, newStatus, reason
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with updated status
     */
    @PostMapping(path = "/enroll", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientEventResponse> enrollPatient(
            @RequestBody String enrollmentRequest,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Enrolling patient for tenant: {}", tenantId);

        PatientEventResponse response = patientEventService.enrollPatientEvent(enrollmentRequest, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Update patient demographics event
     *
     * @param demographicsRequest JSON request with firstName, lastName, dateOfBirth updates
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with updated demographics
     */
    @PostMapping(path = "/demographics", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientEventResponse> updateDemographics(
            @RequestBody String demographicsRequest,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Updating patient demographics for tenant: {}", tenantId);

        PatientEventResponse response = patientEventService.updateDemographicsEvent(demographicsRequest, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
