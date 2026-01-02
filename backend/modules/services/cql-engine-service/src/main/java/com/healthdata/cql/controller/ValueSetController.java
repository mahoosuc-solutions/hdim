package com.healthdata.cql.controller;

import com.healthdata.cql.dto.ValueSetRequest;
import com.healthdata.cql.entity.ValueSet;
import com.healthdata.cql.service.ValueSetService;
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

/**
 * REST Controller for ValueSet Management
 *
 * Provides HTTP endpoints for managing SNOMED, LOINC, RxNorm, and other
 * code system value sets used in CQL expression evaluation.
 * All endpoints are multi-tenant aware via X-Tenant-ID header.
 *
 * Authorization:
 * - Create/Update/Delete operations require ADMIN or SUPER_ADMIN role
 * - Read operations require ANALYST, EVALUATOR, ADMIN, or SUPER_ADMIN role
 */
@RestController
@RequestMapping("/api/v1/cql/valuesets")
public class ValueSetController {

    private static final Logger logger = LoggerFactory.getLogger(ValueSetController.class);

    private final ValueSetService valueSetService;

    public ValueSetController(ValueSetService valueSetService) {
        this.valueSetService = valueSetService;
    }

    /**
     * Create a new value set
     * POST /api/v1/cql/valuesets
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<ValueSet> createValueSet(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody ValueSetRequest request) {
        logger.info("Creating value set for tenant: {}", tenantId);

        // Convert DTO to entity
        ValueSet valueSet = new ValueSet();
        valueSet.setTenantId(tenantId);
        valueSet.setOid(request.getOid());
        valueSet.setName(request.getName());
        valueSet.setVersion(request.getVersion());
        valueSet.setCodeSystem(request.getCodeSystem());
        valueSet.setCodes(request.getCodes());
        valueSet.setDescription(request.getDescription());
        valueSet.setPublisher(request.getPublisher());
        valueSet.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");

        ValueSet created = valueSetService.createValueSet(valueSet);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a value set by ID
     * GET /api/v1/cql/valuesets/{id}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ValueSet> getValueSet(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.debug("Getting value set: {} for tenant: {}", id, tenantId);

        return valueSetService.getValueSetByIdAndTenant(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all value sets for a tenant
     * GET /api/v1/cql/valuesets
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ValueSet>> getAllValueSets(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        logger.debug("Getting all value sets for tenant: {}", tenantId);

        Page<ValueSet> valueSets = valueSetService.getAllValueSets(tenantId, pageable);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get value set by OID
     * GET /api/v1/cql/valuesets/by-oid/{oid}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/by-oid/{oid}")
    public ResponseEntity<ValueSet> getValueSetByOid(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String oid) {
        logger.debug("Getting value set with OID: {} for tenant: {}", oid, tenantId);

        return valueSetService.getValueSetByOid(tenantId, oid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get value set by OID and version
     * GET /api/v1/cql/valuesets/by-oid/{oid}/version/{version}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/by-oid/{oid}/version/{version}")
    public ResponseEntity<ValueSet> getValueSetByOidAndVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String oid,
            @PathVariable String version) {
        logger.debug("Getting value set: {} v{} for tenant: {}", oid, version, tenantId);

        return valueSetService.getValueSetByOidAndVersion(tenantId, oid, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get the latest version of a value set by OID
     * GET /api/v1/cql/valuesets/by-oid/{oid}/latest
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/by-oid/{oid}/latest")
    public ResponseEntity<ValueSet> getLatestValueSetVersion(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String oid) {
        logger.debug("Getting latest version of value set: {} for tenant: {}", oid, tenantId);

        return valueSetService.getLatestValueSetVersion(tenantId, oid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all versions of a value set by OID
     * GET /api/v1/cql/valuesets/by-oid/{oid}/versions
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/by-oid/{oid}/versions")
    public ResponseEntity<List<ValueSet>> getAllValueSetVersions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String oid) {
        logger.debug("Getting all versions of value set: {} for tenant: {}", oid, tenantId);

        List<ValueSet> versions = valueSetService.getAllValueSetVersions(tenantId, oid);
        return ResponseEntity.ok(versions);
    }

    /**
     * Get value set by name
     * GET /api/v1/cql/valuesets/by-name/{name}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/by-name/{name}")
    public ResponseEntity<ValueSet> getValueSetByName(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String name) {
        logger.debug("Getting value set with name: {} for tenant: {}", name, tenantId);

        return valueSetService.getValueSetByName(tenantId, name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get value sets by code system
     * GET /api/v1/cql/valuesets/by-code-system/{codeSystem}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/by-code-system/{codeSystem}")
    public ResponseEntity<Page<ValueSet>> getValueSetsByCodeSystem(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String codeSystem,
            Pageable pageable) {
        logger.debug("Getting value sets for code system: {} in tenant: {}", codeSystem, tenantId);

        Page<ValueSet> valueSets = valueSetService.getValueSetsByCodeSystem(
                tenantId, codeSystem, pageable);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get ACTIVE value sets (status = ACTIVE)
     * GET /api/v1/cql/valuesets/active
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/active")
    public ResponseEntity<List<ValueSet>> getActiveValueSets(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting active value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getActiveValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get SNOMED value sets
     * GET /api/v1/cql/valuesets/snomed
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/snomed")
    public ResponseEntity<List<ValueSet>> getSnomedValueSets(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting SNOMED value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getSnomedValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get LOINC value sets
     * GET /api/v1/cql/valuesets/loinc
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/loinc")
    public ResponseEntity<List<ValueSet>> getLoincValueSets(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting LOINC value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getLoincValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get RxNorm value sets
     * GET /api/v1/cql/valuesets/rxnorm
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/rxnorm")
    public ResponseEntity<List<ValueSet>> getRxNormValueSets(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting RxNorm value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getRxNormValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get common code system value sets (SNOMED, LOINC, RxNorm)
     * GET /api/v1/cql/valuesets/common
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/common")
    public ResponseEntity<List<ValueSet>> getCommonCodeSystemValueSets(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting common code system value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getCommonCodeSystemValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Search value sets by name
     * GET /api/v1/cql/valuesets/search?q={searchTerm}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<ValueSet>> searchValueSets(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("q") String searchTerm) {
        logger.debug("Searching value sets for: {} in tenant: {}", searchTerm, tenantId);

        List<ValueSet> valueSets = valueSetService.searchValueSetsByName(tenantId, searchTerm);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get value sets by OID prefix
     * GET /api/v1/cql/valuesets/by-oid-prefix/{oidPrefix}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/by-oid-prefix/{oidPrefix}")
    public ResponseEntity<List<ValueSet>> getValueSetsByOidPrefix(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String oidPrefix) {
        logger.debug("Getting value sets with OID prefix: {} for tenant: {}", oidPrefix, tenantId);

        List<ValueSet> valueSets = valueSetService.getValueSetsByOidPrefix(tenantId, oidPrefix);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Update a value set
     * PUT /api/v1/cql/valuesets/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ValueSet> updateValueSet(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody ValueSetRequest request) {
        logger.info("Updating value set: {} for tenant: {}", id, tenantId);

        // Convert DTO to entity
        ValueSet updates = new ValueSet();
        updates.setTenantId(tenantId);
        updates.setOid(request.getOid());
        updates.setName(request.getName());
        updates.setVersion(request.getVersion());
        updates.setCodeSystem(request.getCodeSystem());
        updates.setCodes(request.getCodes());
        updates.setDescription(request.getDescription());
        updates.setPublisher(request.getPublisher());
        if (request.getStatus() != null) {
            updates.setStatus(request.getStatus());
        }

        ValueSet updated = valueSetService.updateValueSet(id, updates);

        return ResponseEntity.ok(updated);
    }

    /**
     * Activate a value set
     * POST /api/v1/cql/valuesets/{id}/activate
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/{id}/activate")
    public ResponseEntity<ValueSet> activateValueSet(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Activating value set: {} for tenant: {}", id, tenantId);

        ValueSet activated = valueSetService.activateValueSet(id, tenantId);
        return ResponseEntity.ok(activated);
    }

    /**
     * Retire a value set
     * POST /api/v1/cql/valuesets/{id}/retire
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/{id}/retire")
    public ResponseEntity<ValueSet> retireValueSet(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Retiring value set: {} for tenant: {}", id, tenantId);

        ValueSet retired = valueSetService.retireValueSet(id, tenantId);
        return ResponseEntity.ok(retired);
    }

    /**
     * Delete a value set (soft delete)
     * DELETE /api/v1/cql/valuesets/{id}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteValueSet(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        logger.info("Deleting value set: {} for tenant: {}", id, tenantId);

        valueSetService.deleteValueSet(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get value set count
     * GET /api/v1/cql/valuesets/count
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/count")
    public ResponseEntity<Long> countValueSets(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Counting value sets for tenant: {}", tenantId);

        long count = valueSetService.countValueSets(tenantId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get value set count by code system
     * GET /api/v1/cql/valuesets/count/by-code-system/{codeSystem}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/count/by-code-system/{codeSystem}")
    public ResponseEntity<Long> countValueSetsByCodeSystem(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String codeSystem) {
        logger.debug("Counting value sets for code system: {} in tenant: {}",
                codeSystem, tenantId);

        long count = valueSetService.countValueSetsByCodeSystem(tenantId, codeSystem);
        return ResponseEntity.ok(count);
    }

    /**
     * Check if value set exists by OID
     * GET /api/v1/cql/valuesets/exists/by-oid/{oid}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/exists/by-oid/{oid}")
    public ResponseEntity<Boolean> valueSetExistsByOid(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String oid) {
        logger.debug("Checking if value set exists with OID: {} for tenant: {}", oid, tenantId);

        boolean exists = valueSetService.valueSetExistsByOid(tenantId, oid);
        return ResponseEntity.ok(exists);
    }

    /**
     * Check if a code exists in a value set
     * GET /api/v1/cql/valuesets/{id}/contains-code/{code}
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/{id}/contains-code/{code}")
    public ResponseEntity<Boolean> codeExistsInValueSet(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id,
            @PathVariable String code) {
        logger.debug("Checking if code {} exists in value set: {} for tenant: {}",
                code, id, tenantId);

        boolean exists = valueSetService.codeExistsInValueSet(id, tenantId, code);
        return ResponseEntity.ok(exists);
    }
}
