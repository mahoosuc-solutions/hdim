package com.healthdata.fhir.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.fhir.service.PractitionerService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for FHIR Practitioner resource.
 * Provides CRUD operations and search functionality for healthcare providers.
 */
@RestController
@RequestMapping("/Practitioner")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Practitioner", description = "FHIR Practitioner resource operations")
public class PractitionerController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final PractitionerService practitionerService;

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Create a new Practitioner resource")
    public ResponseEntity<String> createPractitioner(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String practitionerJson) {

        log.debug("Creating practitioner for tenant: {}", tenantId);
        Practitioner practitioner = JSON_PARSER.parseResource(Practitioner.class, practitionerJson);
        Practitioner created = practitionerService.createPractitioner(tenantId, practitioner, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSON_PARSER.encodeResourceToString(created));
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get a Practitioner resource by ID")
    public ResponseEntity<String> getPractitioner(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.debug("Fetching practitioner: tenant={}, id={}", tenantId, id);
        return practitionerService.getPractitioner(tenantId, id)
                .map(practitioner -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(practitioner)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Update an existing Practitioner resource")
    public ResponseEntity<String> updatePractitioner(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody String practitionerJson) {

        log.debug("Updating practitioner: tenant={}, id={}", tenantId, id);
        Practitioner practitioner = JSON_PARSER.parseResource(Practitioner.class, practitionerJson);
        Practitioner updated = practitionerService.updatePractitioner(tenantId, id, practitioner, userId);
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Practitioner resource")
    public ResponseEntity<Void> deletePractitioner(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {

        log.debug("Deleting practitioner: tenant={}, id={}", tenantId, id);
        practitionerService.deletePractitioner(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search Endpoints ====================

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search Practitioner resources")
    public ResponseEntity<String> searchPractitioners(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Search by name")
            @RequestParam(required = false) String name,
            @Parameter(description = "Search by identifier (system|value format)")
            @RequestParam(required = false) String identifier,
            @RequestParam(defaultValue = "0") int _page,
            @RequestParam(defaultValue = "20") int _count) {

        log.debug("Searching practitioners: tenant={}, name={}, identifier={}", tenantId, name, identifier);

        // Identifier search (used by seed script ensure_resource)
        if (identifier != null && !identifier.isEmpty()) {
            String identifierValue = identifier.contains("|")
                    ? identifier.substring(identifier.indexOf("|") + 1)
                    : identifier;
            return practitionerService.findByIdentifier(tenantId, identifierValue)
                    .map(p -> {
                        Bundle bundle = toSearchBundle(List.of(p), 1);
                        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
                    })
                    .orElseGet(() -> {
                        Bundle bundle = toSearchBundle(List.of(), 0);
                        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
                    });
        }

        // Name search
        if (name != null && !name.isEmpty()) {
            List<Practitioner> results = practitionerService.findByName(tenantId, name);
            Bundle bundle = toSearchBundle(results, results.size());
            return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
        }

        // Default: paginated list
        Page<Practitioner> results = practitionerService.searchPractitioners(
                tenantId, PageRequest.of(_page, Math.min(_count, 100)));
        Bundle bundle = toSearchBundle(results.getContent(), results.getTotalElements());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    // ==================== Helper Methods ====================

    private Bundle toSearchBundle(List<Practitioner> practitioners, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        for (Practitioner practitioner : practitioners) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(practitioner);
            entry.setFullUrl("Practitioner/" + practitioner.getIdElement().getIdPart());
        }

        return bundle;
    }
}
