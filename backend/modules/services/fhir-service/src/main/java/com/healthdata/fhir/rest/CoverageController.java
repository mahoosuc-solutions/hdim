package com.healthdata.fhir.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.healthdata.fhir.service.CoverageService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * REST Controller for FHIR Coverage resource.
 * Provides CRUD operations and search functionality for insurance coverage data.
 */
@RestController
@RequestMapping("/Coverage")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Coverage", description = "FHIR Coverage resource operations")
public class CoverageController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final CoverageService coverageService;

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Create a new Coverage resource")
    public ResponseEntity<String> createCoverage(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String coverageJson) {

        log.debug("Creating coverage for tenant: {}", tenantId);
        Coverage coverage = JSON_PARSER.parseResource(Coverage.class, coverageJson);
        Coverage created = coverageService.createCoverage(tenantId, coverage, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSON_PARSER.encodeResourceToString(created));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get a Coverage resource by ID")
    public ResponseEntity<String> getCoverage(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.debug("Fetching coverage: tenant={}, id={}", tenantId, id);
        return coverageService.getCoverage(tenantId, id)
                .map(coverage -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(coverage)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Update an existing Coverage resource")
    public ResponseEntity<String> updateCoverage(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody String coverageJson) {

        log.debug("Updating coverage: tenant={}, id={}", tenantId, id);
        Coverage coverage = JSON_PARSER.parseResource(Coverage.class, coverageJson);
        Coverage updated = coverageService.updateCoverage(tenantId, id, coverage, userId);
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
    }

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Coverage resource")
    public ResponseEntity<Void> deleteCoverage(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {

        log.debug("Deleting coverage: tenant={}, id={}", tenantId, id);
        coverageService.deleteCoverage(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search Endpoints ====================

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search Coverage resources")
    public ResponseEntity<String> searchCoverages(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient reference (e.g., Patient/uuid)")
            @RequestParam(required = false) String patient,
            @Parameter(description = "Coverage status (active, cancelled, draft)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Coverage type code")
            @RequestParam(required = false) String type,
            @Parameter(description = "Subscriber ID / Member number")
            @RequestParam(required = false) String subscriber,
            @Parameter(description = "Payor reference")
            @RequestParam(required = false) String payor,
            @RequestParam(defaultValue = "0") int _page,
            @RequestParam(defaultValue = "20") int _count) {

        log.debug("Searching coverages: tenant={}, patient={}, status={}", tenantId, patient, status);

        UUID patientId = null;
        if (patient != null && patient.startsWith("Patient/")) {
            patientId = UUID.fromString(patient.substring(8));
        }

        Page<Coverage> coverages = coverageService.searchCoverages(
                tenantId, patientId, status, type, subscriber, payor,
                PageRequest.of(_page, Math.min(_count, 100)));

        Bundle bundle = toSearchBundle(coverages.getContent(), coverages.getTotalElements());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get all coverages for a patient")
    public ResponseEntity<String> getCoveragesByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching coverages for patient: tenant={}, patient={}", tenantId, patientId);
        List<Coverage> coverages = coverageService.getCoveragesByPatient(tenantId, patientId);
        Bundle bundle = toSearchBundle(coverages, coverages.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/active", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get active coverages for a patient")
    public ResponseEntity<String> getActiveCoverages(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching active coverages: tenant={}, patient={}", tenantId, patientId);
        List<Coverage> coverages = coverageService.getActiveCoverages(tenantId, patientId);
        Bundle bundle = toSearchBundle(coverages, coverages.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/primary", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get primary coverage for a patient")
    public ResponseEntity<String> getPrimaryCoverage(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching primary coverage: tenant={}, patient={}", tenantId, patientId);
        return coverageService.getPrimaryCoverage(tenantId, patientId)
                .map(coverage -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(coverage)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/has-active", produces = "application/json")
    @Operation(summary = "Check if patient has active coverage")
    public ResponseEntity<Boolean> hasActiveCoverage(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        boolean hasActive = coverageService.hasActiveCoverage(tenantId, patientId);
        return ResponseEntity.ok(hasActive);
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/subscriber/{subscriberId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get coverages by subscriber ID")
    public ResponseEntity<String> getCoveragesBySubscriberId(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String subscriberId) {

        log.debug("Fetching coverages by subscriber: tenant={}, subscriber={}", tenantId, subscriberId);
        List<Coverage> coverages = coverageService.getCoveragesBySubscriberId(tenantId, subscriberId);
        Bundle bundle = toSearchBundle(coverages, coverages.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    // ==================== Helper Methods ====================

    private Bundle toSearchBundle(List<Coverage> coverages, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        for (Coverage coverage : coverages) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(coverage);
            entry.setFullUrl("Coverage/" + coverage.getIdElement().getIdPart());
        }

        return bundle;
    }
}
