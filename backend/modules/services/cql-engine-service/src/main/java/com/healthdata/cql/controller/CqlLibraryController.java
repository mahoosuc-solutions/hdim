package com.healthdata.cql.controller;

import com.healthdata.cql.dto.CqlLibraryRequest;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.service.CqlLibraryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * REST Controller for CQL Library Management
 *
 * Provides HTTP endpoints for managing Clinical Quality Language libraries.
 * All endpoints are multi-tenant aware via X-Tenant-ID header.
 *
 * Authorization:
 * - Create/Update/Delete operations require ADMIN or SUPER_ADMIN role
 * - Read operations require VIEWER, ANALYST, EVALUATOR, ADMIN, or SUPER_ADMIN role
 * - Administrative operations (activate, retire, compile) require ADMIN or SUPER_ADMIN role
 */
@RestController
@RequestMapping("/api/v1/cql/libraries")
public class CqlLibraryController {

    private static final Logger logger = LoggerFactory.getLogger(CqlLibraryController.class);

    private final CqlLibraryService libraryService;

    public CqlLibraryController(CqlLibraryService libraryService) {
        this.libraryService = libraryService;
    }

    /**
     * Create a new CQL library
     * POST /api/v1/cql/libraries
     * Requires ADMIN or SUPER_ADMIN role
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping
    public ResponseEntity<CqlLibrary> createLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CqlLibraryRequest request) {
        logger.info("Creating CQL library for tenant: {}", tenantId);

        // Convert DTO to entity
        CqlLibrary library = new CqlLibrary();
        library.setTenantId(tenantId);
        library.setName(request.getName());
        library.setLibraryName(request.getName());
        library.setVersion(request.getVersion());
        library.setCqlContent(request.getCqlContent());
        library.setStatus(request.getStatus() != null ? request.getStatus() : "DRAFT");
        library.setDescription(request.getDescription());
        library.setPublisher(request.getPublisher());

        CqlLibrary created = libraryService.createLibrary(library);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a library by ID
     * GET /api/v1/cql/libraries/{id}
     * Requires VIEWER, ANALYST, EVALUATOR, ADMIN, or SUPER_ADMIN role
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/{id}")
    public ResponseEntity<CqlLibrary> getLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.debug("Getting CQL library: {} for tenant: {}", id, tenantId);

        return libraryService.getLibraryByIdAndTenant(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all libraries for a tenant
     * GET /api/v1/cql/libraries
     * Requires VIEWER, ANALYST, EVALUATOR, ADMIN, or SUPER_ADMIN role
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping
    public ResponseEntity<Page<CqlLibrary>> getAllLibraries(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        logger.debug("Getting all CQL libraries for tenant: {}", tenantId);

        Page<CqlLibrary> libraries = libraryService.getAllLibraries(tenantId, pageable);
        return ResponseEntity.ok(libraries);
    }

    /**
     * Get a specific library by name and version
     * GET /api/v1/cql/libraries/by-name/{name}/version/{version}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-name/{name}/version/{version}")
    public ResponseEntity<CqlLibrary> getLibraryByNameAndVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String name,
            @PathVariable String version) {
        logger.debug("Getting CQL library: {} v{} for tenant: {}", name, version, tenantId);

        return libraryService.getLibraryByNameAndVersion(tenantId, name, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get the latest version of a library
     * GET /api/v1/cql/libraries/by-name/{name}/latest
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-name/{name}/latest")
    public ResponseEntity<CqlLibrary> getLatestLibraryVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String name) {
        logger.debug("Getting latest version of CQL library: {} for tenant: {}", name, tenantId);

        return libraryService.getLatestLibraryVersion(tenantId, name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all versions of a library
     * GET /api/v1/cql/libraries/by-name/{name}/versions
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-name/{name}/versions")
    public ResponseEntity<List<CqlLibrary>> getAllLibraryVersions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String name) {
        logger.debug("Getting all versions of CQL library: {} for tenant: {}", name, tenantId);

        List<CqlLibrary> versions = libraryService.getAllLibraryVersions(tenantId, name);
        return ResponseEntity.ok(versions);
    }

    /**
     * Get libraries by status
     * GET /api/v1/cql/libraries/by-status/{status}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<CqlLibrary>> getLibrariesByStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String status) {
        logger.debug("Getting CQL libraries with status: {} for tenant: {}", status, tenantId);

        List<CqlLibrary> libraries = libraryService.getLibrariesByStatus(tenantId, status);
        return ResponseEntity.ok(libraries);
    }

    /**
     * Get all ACTIVE libraries
     * GET /api/v1/cql/libraries/active
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/active")
    public ResponseEntity<List<CqlLibrary>> getActiveLibraries(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting active CQL libraries for tenant: {}", tenantId);

        List<CqlLibrary> libraries = libraryService.getActiveLibraries(tenantId);
        return ResponseEntity.ok(libraries);
    }

    /**
     * Search libraries by name
     * GET /api/v1/cql/libraries/search?q={searchTerm}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/search")
    public ResponseEntity<List<CqlLibrary>> searchLibraries(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("q") String searchTerm) {
        logger.debug("Searching CQL libraries for: {} in tenant: {}", searchTerm, tenantId);

        List<CqlLibrary> libraries = libraryService.searchLibrariesByName(tenantId, searchTerm);
        return ResponseEntity.ok(libraries);
    }

    /**
     * Update a library
     * PUT /api/v1/cql/libraries/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping("/{id}")
    public ResponseEntity<CqlLibrary> updateLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody CqlLibraryRequest request) {
        logger.info("Updating CQL library: {} for tenant: {}", id, tenantId);

        // Convert DTO to entity
        CqlLibrary updates = new CqlLibrary();
        updates.setTenantId(tenantId);
        updates.setName(request.getName());
        updates.setLibraryName(request.getName());
        updates.setVersion(request.getVersion());
        updates.setCqlContent(request.getCqlContent());
        if (request.getStatus() != null) {
            updates.setStatus(request.getStatus());
        }
        updates.setDescription(request.getDescription());
        updates.setPublisher(request.getPublisher());

        CqlLibrary updated = libraryService.updateLibrary(id, updates);

        return ResponseEntity.ok(updated);
    }

    /**
     * Activate a library
     * POST /api/v1/cql/libraries/{id}/activate
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/activate")
    public ResponseEntity<CqlLibrary> activateLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Activating CQL library: {} for tenant: {}", id, tenantId);

        CqlLibrary activated = libraryService.activateLibrary(id, tenantId);
        return ResponseEntity.ok(activated);
    }

    /**
     * Retire a library
     * POST /api/v1/cql/libraries/{id}/retire
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/retire")
    public ResponseEntity<CqlLibrary> retireLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Retiring CQL library: {} for tenant: {}", id, tenantId);

        CqlLibrary retired = libraryService.retireLibrary(id, tenantId);
        return ResponseEntity.ok(retired);
    }

    /**
     * Compile CQL to ELM
     * POST /api/v1/cql/libraries/{id}/compile
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/compile")
    public ResponseEntity<CqlLibrary> compileLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Compiling CQL library: {} for tenant: {}", id, tenantId);

        CqlLibrary compiled = libraryService.compileLibrary(id, tenantId);
        return ResponseEntity.ok(compiled);
    }

    /**
     * Validate CQL syntax
     * POST /api/v1/cql/libraries/{id}/validate
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/validate")
    public ResponseEntity<Boolean> validateLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Validating CQL library: {} for tenant: {}", id, tenantId);

        boolean isValid = libraryService.validateLibrary(id, tenantId);
        return ResponseEntity.ok(isValid);
    }

    /**
     * Delete a library (soft delete)
     * DELETE /api/v1/cql/libraries/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLibrary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Deleting CQL library: {} for tenant: {}", id, tenantId);

        libraryService.deleteLibrary(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get library count
     * GET /api/v1/cql/libraries/count
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count")
    public ResponseEntity<Long> countLibraries(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Counting CQL libraries for tenant: {}", tenantId);

        long count = libraryService.countLibraries(tenantId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get library count by status
     * GET /api/v1/cql/libraries/count/by-status/{status}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count/by-status/{status}")
    public ResponseEntity<Long> countLibrariesByStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String status) {
        logger.debug("Counting CQL libraries with status: {} for tenant: {}", status, tenantId);

        long count = libraryService.countLibrariesByStatus(tenantId, status);
        return ResponseEntity.ok(count);
    }

    /**
     * Check if library exists
     * GET /api/v1/cql/libraries/exists?name={name}&version={version}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/exists")
    public ResponseEntity<Boolean> libraryExists(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String name,
            @RequestParam String version) {
        logger.debug("Checking if CQL library exists: {} v{} for tenant: {}",
                name, version, tenantId);

        boolean exists = libraryService.libraryExists(tenantId, name, version);
        return ResponseEntity.ok(exists);
    }
}
