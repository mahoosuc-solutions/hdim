package com.healthdata.fhir.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
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

import com.healthdata.fhir.service.DocumentReferenceService;

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
 * REST Controller for FHIR DocumentReference resource.
 * Provides CRUD operations and search functionality for clinical document references.
 */
@RestController
@RequestMapping("/DocumentReference")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DocumentReference", description = "FHIR DocumentReference resource operations")
public class DocumentReferenceController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final DocumentReferenceService documentReferenceService;

    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Create a new DocumentReference resource")
    public ResponseEntity<String> createDocumentReference(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String docRefJson) {

        log.debug("Creating document reference for tenant: {}", tenantId);
        DocumentReference docRef = JSON_PARSER.parseResource(DocumentReference.class, docRefJson);
        DocumentReference created = documentReferenceService.createDocumentReference(tenantId, docRef, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSON_PARSER.encodeResourceToString(created));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get a DocumentReference resource by ID")
    public ResponseEntity<String> getDocumentReference(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.debug("Fetching document reference: tenant={}, id={}", tenantId, id);
        return documentReferenceService.getDocumentReference(tenantId, id)
                .map(docRef -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(docRef)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Update an existing DocumentReference resource")
    public ResponseEntity<String> updateDocumentReference(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody String docRefJson) {

        log.debug("Updating document reference: tenant={}, id={}", tenantId, id);
        DocumentReference docRef = JSON_PARSER.parseResource(DocumentReference.class, docRefJson);
        DocumentReference updated = documentReferenceService.updateDocumentReference(tenantId, id, docRef, userId);
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
    }

    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a DocumentReference resource")
    public ResponseEntity<Void> deleteDocumentReference(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {

        log.debug("Deleting document reference: tenant={}, id={}", tenantId, id);
        documentReferenceService.deleteDocumentReference(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search Endpoints ====================

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search DocumentReference resources")
    public ResponseEntity<String> searchDocuments(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient reference (e.g., Patient/uuid)")
            @RequestParam(required = false) String patient,
            @Parameter(description = "Encounter reference (e.g., Encounter/uuid)")
            @RequestParam(required = false) String encounter,
            @Parameter(description = "Document status (current, superseded)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Document type code (LOINC)")
            @RequestParam(required = false) String type,
            @Parameter(description = "Document category")
            @RequestParam(required = false) String category,
            @Parameter(description = "Content MIME type")
            @RequestParam(name = "contenttype", required = false) String contentType,
            @RequestParam(defaultValue = "0") int _page,
            @RequestParam(defaultValue = "20") int _count) {

        log.debug("Searching documents: tenant={}, patient={}", tenantId, patient);

        UUID patientId = extractUuidFromReference(patient, "Patient/");
        UUID encounterId = extractUuidFromReference(encounter, "Encounter/");

        Page<DocumentReference> documents = documentReferenceService.searchDocuments(
                tenantId, patientId, encounterId, status, type, category, contentType,
                PageRequest.of(_page, Math.min(_count, 100)));

        Bundle bundle = toSearchBundle(documents.getContent(), documents.getTotalElements());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get all document references for a patient")
    public ResponseEntity<String> getDocumentsByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching documents for patient: tenant={}, patient={}", tenantId, patientId);
        List<DocumentReference> documents = documentReferenceService.getDocumentsByPatient(tenantId, patientId);
        Bundle bundle = toSearchBundle(documents, documents.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/current", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get current document references for a patient")
    public ResponseEntity<String> getCurrentDocuments(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching current documents: tenant={}, patient={}", tenantId, patientId);
        List<DocumentReference> documents = documentReferenceService.getCurrentDocuments(tenantId, patientId);
        Bundle bundle = toSearchBundle(documents, documents.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/encounter/{encounterId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get document references for an encounter")
    public ResponseEntity<String> getDocumentsByEncounter(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID encounterId) {

        log.debug("Fetching documents for encounter: tenant={}, encounter={}", tenantId, encounterId);
        List<DocumentReference> documents = documentReferenceService.getDocumentsByEncounter(tenantId, encounterId);
        Bundle bundle = toSearchBundle(documents, documents.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/type/{typeCode}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get document references by type for a patient")
    public ResponseEntity<String> getDocumentsByType(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String typeCode) {

        log.debug("Fetching documents by type: tenant={}, patient={}, type={}", tenantId, patientId, typeCode);
        List<DocumentReference> documents = documentReferenceService.getDocumentsByType(tenantId, patientId, typeCode);
        Bundle bundle = toSearchBundle(documents, documents.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/type/{typeCode}/latest", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get latest document of a specific type for a patient")
    public ResponseEntity<String> getLatestDocumentByType(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String typeCode) {

        log.debug("Fetching latest document of type: tenant={}, patient={}, type={}", tenantId, patientId, typeCode);
        return documentReferenceService.getLatestDocumentByType(tenantId, patientId, typeCode)
                .map(docRef -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(docRef)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/search", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search documents by description text")
    public ResponseEntity<String> searchByDescription(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @Parameter(description = "Search term")
            @RequestParam String q) {

        log.debug("Searching documents by description: tenant={}, patient={}, term={}", tenantId, patientId, q);
        List<DocumentReference> documents = documentReferenceService.searchByDescription(tenantId, patientId, q);
        Bundle bundle = toSearchBundle(documents, documents.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/date-range", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get documents created within a date range")
    public ResponseEntity<String> getDocumentsByDateRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @Parameter(description = "Start date (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End date (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {

        log.debug("Fetching documents by date range: tenant={}, patient={}", tenantId, patientId);
        List<DocumentReference> documents = documentReferenceService.getDocumentsByDateRange(tenantId, patientId, start, end);
        Bundle bundle = toSearchBundle(documents, documents.size());
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

    private Bundle toSearchBundle(List<DocumentReference> documents, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        for (DocumentReference docRef : documents) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(docRef);
            entry.setFullUrl("DocumentReference/" + docRef.getIdElement().getIdPart());
        }

        return bundle;
    }
}
