package com.healthdata.fhir.rest;

import java.net.URI;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthdata.fhir.service.PatientService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.auth.context.ScopedTenant;

@RestController
@RequestMapping(value = "/fhir", produces = "application/fhir+json")
public class PatientController {

    private static final String DEFAULT_TENANT = "tenant-1";
    private static final String DEFAULT_ACTOR = "admin-portal";

    private final PatientService patientService;
    private final IParser parser;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
        this.parser = FhirContext.forR4().newJsonParser().setPrettyPrint(false);
    }

    @PostMapping(value = "/Patient", consumes = "application/fhir+json")
    @Audited(
            action = AuditAction.CREATE,
            resourceType = "Patient",
            purposeOfUse = "TREATMENT",
            description = "Create new patient record"
    )
    public ResponseEntity<String> createPatient(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestBody String body) {

        Patient patient = parser.parseResource(Patient.class, body);
        Patient created = patientService.createPatient(resolveTenant(tenantId), patient, DEFAULT_ACTOR);
        String payload = parser.encodeResourceToString(created);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/fhir/Patient/" + created.getIdElement().getIdPart())
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(payload);
    }

    @GetMapping("/Patient/{id}")
    @Audited(
            action = AuditAction.READ,
            resourceType = "Patient",
            purposeOfUse = "TREATMENT",
            description = "Read patient record"
    )
    public ResponseEntity<String> getPatient(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @PathVariable("id") String id) {

        return patientService.getPatient(resolveTenant(tenantId), id)
                .map(patient -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf("application/fhir+json"))
                        .body(parser.encodeResourceToString(patient)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/Patient")
    @Audited(
            action = AuditAction.SEARCH,
            resourceType = "Patient",
            purposeOfUse = "TREATMENT",
            description = "Search patient records"
    )
    public ResponseEntity<String> searchPatients(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestParam(value = "family", required = false) String family,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "_count", required = false, defaultValue = "20") int count) {

        String filter = family != null ? family : name;
        Bundle bundle = patientService.searchPatients(resolveTenant(tenantId), filter, count);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(parser.encodeResourceToString(bundle));
    }

    @PutMapping(value = "/Patient/{id}", consumes = "application/fhir+json")
    @Audited(
            action = AuditAction.UPDATE,
            resourceType = "Patient",
            purposeOfUse = "TREATMENT",
            description = "Update patient record"
    )
    public ResponseEntity<String> updatePatient(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @PathVariable("id") String id,
            @RequestBody String body) {

        Patient patient = parser.parseResource(Patient.class, body);
        Patient updated = patientService.updatePatient(resolveTenant(tenantId), id, patient, DEFAULT_ACTOR);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(parser.encodeResourceToString(updated));
    }

    @DeleteMapping("/Patient/{id}")
    @Audited(
            action = AuditAction.DELETE,
            resourceType = "Patient",
            purposeOfUse = "OPERATIONS",
            description = "Delete patient record (soft delete for HIPAA compliance)"
    )
    public ResponseEntity<Void> deletePatient(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @PathVariable("id") String id) {

        patientService.deletePatient(resolveTenant(tenantId), id, DEFAULT_ACTOR);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(PatientService.PatientValidationException.class)
    public ResponseEntity<String> handleValidation(PatientService.PatientValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(PatientService.PatientNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private String resolveTenant(String tenantId) {
        return ScopedTenant.currentTenant().orElseGet(() ->
                (tenantId == null || tenantId.isBlank()) ? DEFAULT_TENANT : tenantId);
    }
}
