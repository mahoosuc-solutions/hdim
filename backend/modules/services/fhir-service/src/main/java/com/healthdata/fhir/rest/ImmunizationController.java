package com.healthdata.fhir.rest;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.fhir.service.ImmunizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Immunization;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

@RestController
@RequestMapping("/fhir/Immunization")
@Slf4j
@RequiredArgsConstructor
public class ImmunizationController {

    private final ImmunizationService immunizationService;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser jsonParser = fhirContext.newJsonParser().setPrettyPrint(true);

    // Create Immunization
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> createImmunization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId,
            @RequestBody String immunizationJson) {

        log.info("Creating Immunization for tenant: {}", tenantId);

        Immunization immunization = jsonParser.parseResource(Immunization.class, immunizationJson);
        Immunization created = immunizationService.createImmunization(tenantId, immunization, userId);

        String responseJson = jsonParser.encodeResourceToString(created);
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Get Immunization by ID
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getImmunization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {

        log.info("Fetching Immunization: {} for tenant: {}", id, tenantId);

        return immunizationService.getImmunization(tenantId, id)
                .map(immunization -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf("application/fhir+json"))
                        .body(jsonParser.encodeResourceToString(immunization)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Update Immunization
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> updateImmunization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String immunizationJson) {

        log.info("Updating Immunization: {} for tenant: {}", id, tenantId);

        Immunization immunization = jsonParser.parseResource(Immunization.class, immunizationJson);
        Immunization updated = immunizationService.updateImmunization(tenantId, id, immunization, userId);

        String responseJson = jsonParser.encodeResourceToString(updated);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Delete Immunization
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImmunization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", defaultValue = "system") String userId,
            @PathVariable String id) {

        log.info("Deleting Immunization: {} for tenant: {}", id, tenantId);

        immunizationService.deleteImmunization(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // Search by patient
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> searchByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patient,
            @RequestParam(value = "vaccine", required = false) String vaccine,
            Pageable pageable) {

        log.info("Searching immunizations for patient: {} in tenant: {}", patient, tenantId);

        if (patient == null) {
            return ResponseEntity.badRequest().build();
        }

        String patientId = patient.replace("Patient/", "");

        Bundle bundle;
        if (vaccine != null) {
            bundle = immunizationService.getImmunizationsByVaccineCode(tenantId, patientId, vaccine);
        } else {
            bundle = immunizationService.getImmunizationsByPatient(tenantId, patientId, pageable);
        }

        String responseJson = jsonParser.encodeResourceToString(bundle);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Get completed immunizations
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/completed", produces = "application/fhir+json")
    public ResponseEntity<String> getCompletedImmunizations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Fetching completed immunizations for patient: {} in tenant: {}", patient, tenantId);

        String patientId = patient.replace("Patient/", "");
        Bundle bundle = immunizationService.getCompletedImmunizations(tenantId, patientId);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Check if patient has immunization
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/has-immunization", produces = "application/json")
    public ResponseEntity<Map<String, Boolean>> hasImmunization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient,
            @RequestParam(value = "vaccine") String vaccineCode) {

        log.info("Checking if patient {} has immunization: {}", patient, vaccineCode);

        String patientId = patient.replace("Patient/", "");
        boolean hasImmunization = immunizationService.hasImmunization(tenantId, patientId, vaccineCode);

        return ResponseEntity.ok(Map.of("hasImmunization", hasImmunization));
    }

    // Get vaccine series
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/series", produces = "application/fhir+json")
    public ResponseEntity<String> getVaccineSeries(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient,
            @RequestParam(value = "vaccine") String vaccineCode) {

        log.info("Fetching vaccine series for: {} patient: {}", vaccineCode, patient);

        String patientId = patient.replace("Patient/", "");
        Bundle bundle = immunizationService.getVaccineSeries(tenantId, patientId, vaccineCode);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Check series completion
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/series-complete", produces = "application/json")
    public ResponseEntity<Map<String, Boolean>> isSeriesComplete(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient,
            @RequestParam(value = "vaccine") String vaccineCode,
            @RequestParam(value = "requiredDoses") int requiredDoses) {

        log.info("Checking if vaccine series is complete for patient: {}", patient);

        String patientId = patient.replace("Patient/", "");
        boolean isComplete = immunizationService.isSeriesComplete(tenantId, patientId, vaccineCode, requiredDoses);

        return ResponseEntity.ok(Map.of("isComplete", isComplete));
    }

    // Get immunizations with reactions
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/with-reactions", produces = "application/fhir+json")
    public ResponseEntity<String> getImmunizationsWithReactions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Fetching immunizations with reactions for patient: {}", patient);

        String patientId = patient.replace("Patient/", "");
        Bundle bundle = immunizationService.getImmunizationsWithReactions(tenantId, patientId);
        String responseJson = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(responseJson);
    }

    // Count immunizations
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/count", produces = "application/json")
    public ResponseEntity<Map<String, Long>> countImmunizations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient,
            @RequestParam(value = "vaccine", required = false) String vaccineCode) {

        log.info("Counting immunizations for patient: {}", patient);

        String patientId = patient.replace("Patient/", "");
        long count;

        if (vaccineCode != null) {
            count = immunizationService.countByVaccineCode(tenantId, patientId, vaccineCode);
        } else {
            count = immunizationService.countCompletedImmunizations(tenantId, patientId);
        }

        return ResponseEntity.ok(Map.of("count", count));
    }

    // Compliance report endpoint
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/compliance", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getComplianceReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient") String patient) {

        log.info("Generating compliance report for patient: {}", patient);

        String patientId = patient.replace("Patient/", "");
        long totalImmunizations = immunizationService.countCompletedImmunizations(tenantId, patientId);

        // Example compliance check (can be enhanced with specific vaccine requirements)
        return ResponseEntity.ok(Map.of(
                "totalImmunizations", totalImmunizations,
                "complianceStatus", totalImmunizations > 0 ? "compliant" : "non-compliant"
        ));
    }

    // Health check
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/_health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
