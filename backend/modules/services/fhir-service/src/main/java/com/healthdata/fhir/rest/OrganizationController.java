package com.healthdata.fhir.rest;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
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
import com.healthdata.fhir.service.OrganizationService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for FHIR Organization resource.
 * Provides CRUD operations and search for healthcare organizations.
 */
@RestController
@RequestMapping("/Organization")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization", description = "FHIR Organization resource operations")
public class OrganizationController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final OrganizationService organizationService;

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Create a new Organization resource")
    public ResponseEntity<String> createOrganization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String organizationJson) {

        log.debug("Creating organization for tenant: {}", tenantId);
        Organization organization = JSON_PARSER.parseResource(Organization.class, organizationJson);
        Organization created = organizationService.createOrganization(tenantId, organization, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSON_PARSER.encodeResourceToString(created));
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get an Organization resource by ID")
    public ResponseEntity<String> getOrganization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.debug("Fetching organization: tenant={}, id={}", tenantId, id);
        return organizationService.getOrganization(tenantId, id)
                .map(org -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(org)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Update an existing Organization resource")
    public ResponseEntity<String> updateOrganization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody String organizationJson) {

        log.debug("Updating organization: tenant={}, id={}", tenantId, id);
        Organization organization = JSON_PARSER.parseResource(Organization.class, organizationJson);
        Organization updated = organizationService.updateOrganization(tenantId, id, organization, userId);
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
    }

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an Organization resource")
    public ResponseEntity<Void> deleteOrganization(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {

        log.debug("Deleting organization: tenant={}, id={}", tenantId, id);
        organizationService.deleteOrganization(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search Endpoints ====================

    @PreAuthorize("@hdimPermissionEvaluator.hasPermission(authentication, null, 'PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search Organization resources")
    public ResponseEntity<String> searchOrganizations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Search by name")
            @RequestParam(required = false) String name,
            @Parameter(description = "Search by identifier (system|value format)")
            @RequestParam(required = false) String identifier,
            @RequestParam(defaultValue = "0") int _page,
            @RequestParam(defaultValue = "20") int _count) {

        log.debug("Searching organizations: tenant={}, name={}, identifier={}", tenantId, name, identifier);

        // Identifier search (used by seed script ensure_resource)
        if (identifier != null && !identifier.isEmpty()) {
            String identifierValue = identifier.contains("|")
                    ? identifier.substring(identifier.indexOf("|") + 1)
                    : identifier;
            return organizationService.findByIdentifier(tenantId, identifierValue)
                    .map(o -> {
                        Bundle bundle = toSearchBundle(List.of(o), 1);
                        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
                    })
                    .orElseGet(() -> {
                        Bundle bundle = toSearchBundle(List.of(), 0);
                        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
                    });
        }

        // Name search
        if (name != null && !name.isEmpty()) {
            List<Organization> results = organizationService.findByName(tenantId, name);
            Bundle bundle = toSearchBundle(results, results.size());
            return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
        }

        // Default: paginated list
        Page<Organization> results = organizationService.searchOrganizations(
                tenantId, PageRequest.of(_page, Math.min(_count, 100)));
        Bundle bundle = toSearchBundle(results.getContent(), results.getTotalElements());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    // ==================== Helper Methods ====================

    private Bundle toSearchBundle(List<Organization> organizations, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        for (Organization organization : organizations) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(organization);
            entry.setFullUrl("Organization/" + organization.getIdElement().getIdPart());
        }

        return bundle;
    }
}
