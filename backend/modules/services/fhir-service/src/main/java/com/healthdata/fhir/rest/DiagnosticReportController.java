package com.healthdata.fhir.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.healthdata.fhir.service.DiagnosticReportService;

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
 * REST Controller for FHIR DiagnosticReport resource.
 * Provides CRUD operations and search functionality for diagnostic reports.
 */
@RestController
@RequestMapping("/DiagnosticReport")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DiagnosticReport", description = "FHIR DiagnosticReport resource operations")
public class DiagnosticReportController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final DiagnosticReportService diagnosticReportService;

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Create a new DiagnosticReport resource")
    public ResponseEntity<String> createDiagnosticReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String reportJson) {

        log.debug("Creating diagnostic report for tenant: {}", tenantId);
        DiagnosticReport report = JSON_PARSER.parseResource(DiagnosticReport.class, reportJson);
        DiagnosticReport created = diagnosticReportService.createDiagnosticReport(tenantId, report, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSON_PARSER.encodeResourceToString(created));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get a DiagnosticReport resource by ID")
    public ResponseEntity<String> getDiagnosticReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.debug("Fetching diagnostic report: tenant={}, id={}", tenantId, id);
        return diagnosticReportService.getDiagnosticReport(tenantId, id)
                .map(report -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(report)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Update an existing DiagnosticReport resource")
    public ResponseEntity<String> updateDiagnosticReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody String reportJson) {

        log.debug("Updating diagnostic report: tenant={}, id={}", tenantId, id);
        DiagnosticReport report = JSON_PARSER.parseResource(DiagnosticReport.class, reportJson);
        DiagnosticReport updated = diagnosticReportService.updateDiagnosticReport(tenantId, id, report, userId);
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
    }

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a DiagnosticReport resource")
    public ResponseEntity<Void> deleteDiagnosticReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {

        log.debug("Deleting diagnostic report: tenant={}, id={}", tenantId, id);
        diagnosticReportService.deleteDiagnosticReport(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search Endpoints ====================

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search DiagnosticReport resources")
    public ResponseEntity<String> searchReports(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient reference (e.g., Patient/uuid)")
            @RequestParam(required = false) String patient,
            @Parameter(description = "Encounter reference (e.g., Encounter/uuid)")
            @RequestParam(required = false) String encounter,
            @Parameter(description = "Report status")
            @RequestParam(required = false) String status,
            @Parameter(description = "Report type code (LOINC)")
            @RequestParam(required = false) String code,
            @Parameter(description = "Report category (LAB, RAD)")
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int _page,
            @RequestParam(defaultValue = "20") int _count) {

        log.debug("Searching diagnostic reports: tenant={}, patient={}", tenantId, patient);

        UUID patientId = extractUuidFromReference(patient, "Patient/");
        UUID encounterId = extractUuidFromReference(encounter, "Encounter/");

        Page<DiagnosticReport> reports = diagnosticReportService.searchReports(
                tenantId, patientId, encounterId, status, code, category,
                PageRequest.of(_page, Math.min(_count, 100)));

        Bundle bundle = toSearchBundle(reports.getContent(), reports.getTotalElements());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get all diagnostic reports for a patient")
    public ResponseEntity<String> getReportsByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching reports for patient: tenant={}, patient={}", tenantId, patientId);
        List<DiagnosticReport> reports = diagnosticReportService.getReportsByPatient(tenantId, patientId);
        Bundle bundle = toSearchBundle(reports, reports.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/final", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get final diagnostic reports for a patient")
    public ResponseEntity<String> getFinalReports(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching final reports: tenant={}, patient={}", tenantId, patientId);
        List<DiagnosticReport> reports = diagnosticReportService.getFinalReports(tenantId, patientId);
        Bundle bundle = toSearchBundle(reports, reports.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/pending", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get pending diagnostic reports for a patient")
    public ResponseEntity<String> getPendingReports(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching pending reports: tenant={}, patient={}", tenantId, patientId);
        List<DiagnosticReport> reports = diagnosticReportService.getPendingReports(tenantId, patientId);
        Bundle bundle = toSearchBundle(reports, reports.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/lab", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get lab reports for a patient")
    public ResponseEntity<String> getLabReports(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching lab reports: tenant={}, patient={}", tenantId, patientId);
        List<DiagnosticReport> reports = diagnosticReportService.getLabReports(tenantId, patientId);
        Bundle bundle = toSearchBundle(reports, reports.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/imaging", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get imaging reports for a patient")
    public ResponseEntity<String> getImagingReports(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching imaging reports: tenant={}, patient={}", tenantId, patientId);
        List<DiagnosticReport> reports = diagnosticReportService.getImagingReports(tenantId, patientId);
        Bundle bundle = toSearchBundle(reports, reports.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/encounter/{encounterId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get diagnostic reports for an encounter")
    public ResponseEntity<String> getReportsByEncounter(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID encounterId) {

        log.debug("Fetching reports for encounter: tenant={}, encounter={}", tenantId, encounterId);
        List<DiagnosticReport> reports = diagnosticReportService.getReportsByEncounter(tenantId, encounterId);
        Bundle bundle = toSearchBundle(reports, reports.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/code/{code}/latest", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get latest report of a specific type for a patient")
    public ResponseEntity<String> getLatestReportByCode(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String code) {

        log.debug("Fetching latest report: tenant={}, patient={}, code={}", tenantId, patientId, code);
        return diagnosticReportService.getLatestReportByCode(tenantId, patientId, code)
                .map(report -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(report)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/date-range", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get diagnostic reports within a date range")
    public ResponseEntity<String> getReportsByDateRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @Parameter(description = "Start date (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End date (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {

        log.debug("Fetching reports by date range: tenant={}, patient={}", tenantId, patientId);
        List<DiagnosticReport> reports = diagnosticReportService.getReportsByDateRange(tenantId, patientId, start, end);
        Bundle bundle = toSearchBundle(reports, reports.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    // ==================== Helper Methods ====================

    private UUID extractUuidFromReference(String reference, String prefix) {
        if (reference != null && reference.startsWith(prefix)) {
            try {
                return UUID.fromString(reference.substring(prefix.length()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in reference: {}", reference);
            }
        }
        return null;
    }

    private Bundle toSearchBundle(List<DiagnosticReport> reports, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        for (DiagnosticReport report : reports) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(report);
            entry.setFullUrl("DiagnosticReport/" + report.getIdElement().getIdPart());
        }

        return bundle;
    }
}
