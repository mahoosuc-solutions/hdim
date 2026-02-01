package com.hdim.patient.controller;

import com.hdim.patient.dto.PatientResponse;
import com.hdim.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Test controller with HIPAA violations for agent validation.
 * This file intentionally contains violations to test the HIPAA Compliance Agent.
 */
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientTestController {

    private final PatientService patientService;

    // VIOLATION 1: Missing @Audited annotation on PHI endpoint
    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        PatientResponse patient = patientService.getPatient(patientId, tenantId);

        // VIOLATION 2: Missing Cache-Control headers on PHI response
        return ResponseEntity.ok(patient);
    }

    // VIOLATION 3: Missing @PreAuthorize annotation (authorization check)
    @GetMapping("/{patientId}/observations")
    public ResponseEntity<List<ObservationResponse>> getObservations(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<ObservationResponse> observations = patientService.getObservations(patientId, tenantId);
        return ResponseEntity.ok(observations);
    }

    // Correct example for comparison
    @GetMapping("/{patientId}/conditions")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(action = AuditAction.READ, resourceType = "Condition", encryptPayload = true)
    public ResponseEntity<List<ConditionResponse>> getConditions(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<ConditionResponse> conditions = patientService.getConditions(patientId, tenantId);

        return ResponseEntity.ok()
            .header("Cache-Control", "no-store, no-cache, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .body(conditions);
    }
}
