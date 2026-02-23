package com.healthdata.fhir.rest;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.PractitionerRole;
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
import com.healthdata.fhir.service.PractitionerRoleService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for FHIR PractitionerRole resource.
 * Links practitioners to organizations with specific roles (doctor, nurse, assistant).
 */
@RestController
@RequestMapping("/PractitionerRole")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "PractitionerRole", description = "FHIR PractitionerRole resource operations")
public class PractitionerRoleController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final PractitionerRoleService practitionerRoleService;

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Create a new PractitionerRole resource")
    public ResponseEntity<String> createPractitionerRole(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String roleJson) {

        log.debug("Creating practitioner role for tenant: {}", tenantId);
        PractitionerRole role = JSON_PARSER.parseResource(PractitionerRole.class, roleJson);
        PractitionerRole created = practitionerRoleService.createPractitionerRole(tenantId, role, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSON_PARSER.encodeResourceToString(created));
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get a PractitionerRole resource by ID")
    public ResponseEntity<String> getPractitionerRole(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.debug("Fetching practitioner role: tenant={}, id={}", tenantId, id);
        return practitionerRoleService.getPractitionerRole(tenantId, id)
                .map(role -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(role)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Update an existing PractitionerRole resource")
    public ResponseEntity<String> updatePractitionerRole(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody String roleJson) {

        log.debug("Updating practitioner role: tenant={}, id={}", tenantId, id);
        PractitionerRole role = JSON_PARSER.parseResource(PractitionerRole.class, roleJson);
        PractitionerRole updated = practitionerRoleService.updatePractitionerRole(tenantId, id, role, userId);
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a PractitionerRole resource")
    public ResponseEntity<Void> deletePractitionerRole(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {

        log.debug("Deleting practitioner role: tenant={}, id={}", tenantId, id);
        practitionerRoleService.deletePractitionerRole(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search Endpoints ====================

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search PractitionerRole resources")
    public ResponseEntity<String> searchPractitionerRoles(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Practitioner reference (e.g., Practitioner/uuid)")
            @RequestParam(required = false) String practitioner,
            @Parameter(description = "Role code (doctor, nurse, assistant)")
            @RequestParam(required = false) String role,
            @Parameter(description = "Search by identifier (system|value format)")
            @RequestParam(required = false) String identifier,
            @RequestParam(defaultValue = "0") int _page,
            @RequestParam(defaultValue = "20") int _count) {

        log.debug("Searching practitioner roles: tenant={}, practitioner={}, role={}, identifier={}",
                tenantId, practitioner, role, identifier);

        // Identifier search (used by seed script ensure_resource)
        if (identifier != null && !identifier.isEmpty()) {
            String identifierValue = identifier.contains("|")
                    ? identifier.substring(identifier.indexOf("|") + 1)
                    : identifier;
            return practitionerRoleService.findByIdentifier(tenantId, identifierValue)
                    .map(r -> {
                        Bundle bundle = toSearchBundle(List.of(r), 1);
                        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
                    })
                    .orElseGet(() -> {
                        Bundle bundle = toSearchBundle(List.of(), 0);
                        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
                    });
        }

        // Practitioner reference search
        if (practitioner != null && !practitioner.isEmpty()) {
            String practitionerId = practitioner.startsWith("Practitioner/")
                    ? practitioner.substring(13) : practitioner;
            List<PractitionerRole> results = practitionerRoleService.findByPractitioner(tenantId, practitionerId);
            Bundle bundle = toSearchBundle(results, results.size());
            return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
        }

        // Role code search
        if (role != null && !role.isEmpty()) {
            List<PractitionerRole> results = practitionerRoleService.findByRoleCode(tenantId, role);
            Bundle bundle = toSearchBundle(results, results.size());
            return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
        }

        // Default: paginated list
        Page<PractitionerRole> results = practitionerRoleService.searchPractitionerRoles(
                tenantId, PageRequest.of(_page, Math.min(_count, 100)));
        Bundle bundle = toSearchBundle(results.getContent(), results.getTotalElements());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    // ==================== Helper Methods ====================

    private Bundle toSearchBundle(List<PractitionerRole> roles, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        for (PractitionerRole role : roles) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(role);
            entry.setFullUrl("PractitionerRole/" + role.getIdElement().getIdPart());
        }

        return bundle;
    }
}
