package com.healthdata.fhir.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.fhir.service.AllergyIntoleranceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

@RestController
@RequestMapping("/AllergyIntolerance")
@Slf4j
@RequiredArgsConstructor
public class AllergyIntoleranceController {

    private final AllergyIntoleranceService allergyIntoleranceService;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

    // Create AllergyIntolerance
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> createAllergyIntolerance(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId,
            @RequestBody String allergyIntoleranceJson) {

        log.info("Creating AllergyIntolerance for tenant: {}", tenantId);

        AllergyIntolerance allergyIntolerance = jsonParser.parseResource(AllergyIntolerance.class, allergyIntoleranceJson);
        AllergyIntolerance created = allergyIntoleranceService.createAllergyIntolerance(tenantId, allergyIntolerance, userId);

        String responseJson = jsonParser.encodeResourceToString(created);
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Get AllergyIntolerance by ID
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getAllergyIntolerance(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {

        log.info("Fetching AllergyIntolerance: {} for tenant: {}", id, tenantId);

        return allergyIntoleranceService.getAllergyIntolerance(tenantId, id)
                .map(allergyIntolerance -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf("application/fhir+json"))
                        .body(jsonParser.encodeResourceToString(allergyIntolerance)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Update AllergyIntolerance
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> updateAllergyIntolerance(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String allergyIntoleranceJson) {

        log.info("Updating AllergyIntolerance: {} for tenant: {}", id, tenantId);

        AllergyIntolerance allergyIntolerance = jsonParser.parseResource(AllergyIntolerance.class, allergyIntoleranceJson);
        AllergyIntolerance updated = allergyIntoleranceService.updateAllergyIntolerance(tenantId, id, allergyIntolerance, userId);

        String responseJson = jsonParser.encodeResourceToString(updated);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Delete AllergyIntolerance
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_WRITE\')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAllergyIntolerance(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId,
            @PathVariable String id) {

        log.info("Deleting AllergyIntolerance: {} for tenant: {}", id, tenantId);

        allergyIntoleranceService.deleteAllergyIntolerance(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // Search by patient
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> searchByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patient,
            Pageable pageable) {

        log.info("Searching allergies for patient: {} in tenant: {}", patient, tenantId);

        if (patient == null) {
            return ResponseEntity.badRequest().build();
        }

        // Remove "Patient/" prefix if present
        String patientId = patient.replace("Patient/", "");

        Bundle bundle = allergyIntoleranceService.getAllergiesByPatient(tenantId, patientId, pageable);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Get active allergies
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/active", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getActiveAllergies(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Fetching active allergies for patient: {} in tenant: {}", patient, tenantId);

        String patientId = patient.replace("Patient/", "");
        Bundle bundle = allergyIntoleranceService.getActiveAllergies(tenantId, patientId);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Get critical allergies
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/critical", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getCriticalAllergies(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Fetching critical allergies for patient: {} in tenant: {}", patient, tenantId);

        String patientId = patient.replace("Patient/", "");
        Bundle bundle = allergyIntoleranceService.getCriticalAllergies(tenantId, patientId);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Get medication allergies
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/medication", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getMedicationAllergies(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Fetching medication allergies for patient: {} in tenant: {}", patient, tenantId);

        String patientId = patient.replace("Patient/", "");
        Bundle bundle = allergyIntoleranceService.getMedicationAllergies(tenantId, patientId);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Get food allergies
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/food", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getFoodAllergies(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Fetching food allergies for patient: {} in tenant: {}", patient, tenantId);

        String patientId = patient.replace("Patient/", "");
        Bundle bundle = allergyIntoleranceService.getFoodAllergies(tenantId, patientId);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Get confirmed allergies
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/confirmed", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getConfirmedAllergies(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Fetching confirmed allergies for patient: {} in tenant: {}", patient, tenantId);

        String patientId = patient.replace("Patient/", "");
        Bundle bundle = allergyIntoleranceService.getConfirmedAllergies(tenantId, patientId);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Check if patient has specific allergy
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/has-allergy", produces = "application/json")
    public ResponseEntity<Map<String, Boolean>> hasActiveAllergy(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient,
            @RequestParam(value = "code") String allergyCode) {

        log.info("Checking if patient {} has active allergy: {}", patient, allergyCode);

        String patientId = patient.replace("Patient/", "");
        boolean hasAllergy = allergyIntoleranceService.hasActiveAllergy(tenantId, patientId, allergyCode);

        return ResponseEntity.ok(Map.of("hasAllergy", hasAllergy));
    }

    // Count active allergies
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/count", produces = "application/json")
    public ResponseEntity<Map<String, Long>> countActiveAllergies(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Counting active allergies for patient: {}", patient);

        String patientId = patient.replace("Patient/", "");
        long count = allergyIntoleranceService.countActiveAllergies(tenantId, patientId);

        return ResponseEntity.ok(Map.of("count", count));
    }

    // Health check
    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, \'PATIENT_READ\')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/_health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
