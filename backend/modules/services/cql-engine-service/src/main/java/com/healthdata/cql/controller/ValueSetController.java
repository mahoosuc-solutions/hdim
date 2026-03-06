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
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Value Set Management", description = "APIs for managing FHIR value sets (SNOMED, LOINC, RxNorm, ICD-10) used in CQL expression evaluation. Value sets define the clinical codes referenced by quality measures.")
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
    @Operation(
        summary = "Create a new value set",
        description = "Creates a new FHIR value set containing clinical terminology codes (SNOMED CT, LOINC, RxNorm, ICD-10). "
            + "Value sets are referenced by CQL libraries to define the clinical concepts used in HEDIS quality measure evaluation.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Value set created successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid value set data"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_WRITE permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping
    public ResponseEntity<ValueSet> createValueSet(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
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
    @Operation(
        summary = "Get a value set by ID",
        description = "Retrieves a specific FHIR value set by its unique identifier. Returns the value set metadata, "
            + "OID, code system, version, and the list of clinical codes it contains.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set found and returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission"),
        @ApiResponse(responseCode = "404", description = "Value set not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/{id}")
    public ResponseEntity<ValueSet> getValueSet(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the value set to retrieve", required = true) @PathVariable UUID id) {
        logger.debug("Getting value set: {} for tenant: {}", id, tenantId);

        return valueSetService.getValueSetByIdAndTenant(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all value sets for a tenant
     * GET /api/v1/cql/valuesets
     */
    @Operation(
        summary = "Get all value sets for a tenant",
        description = "Returns a paginated list of all FHIR value sets within the tenant. Includes value sets across all "
            + "code systems (SNOMED CT, LOINC, RxNorm, ICD-10) and statuses.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of value sets returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping
    public ResponseEntity<Page<ValueSet>> getAllValueSets(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        logger.debug("Getting all value sets for tenant: {}", tenantId);

        Page<ValueSet> valueSets = valueSetService.getAllValueSets(tenantId, pageable);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get value set by OID
     * GET /api/v1/cql/valuesets/by-oid/{oid}
     */
    @Operation(
        summary = "Get value set by OID",
        description = "Retrieves a FHIR value set by its Object Identifier (OID). OIDs are the standard identifiers used "
            + "by VSAC (Value Set Authority Center) and are referenced in CQL libraries for HEDIS measure definitions.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set found and returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission"),
        @ApiResponse(responseCode = "404", description = "Value set with specified OID not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-oid/{oid}")
    public ResponseEntity<ValueSet> getValueSetByOid(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Value set OID (e.g., 2.16.840.1.113883.3.464.1003.101.12.1001)", required = true) @PathVariable String oid) {
        logger.debug("Getting value set with OID: {} for tenant: {}", oid, tenantId);

        return valueSetService.getValueSetByOid(tenantId, oid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get value set by OID and version
     * GET /api/v1/cql/valuesets/by-oid/{oid}/version/{version}
     */
    @Operation(
        summary = "Get value set by OID and version",
        description = "Retrieves a specific version of a FHIR value set by its OID and version string. "
            + "Supports version-pinned CQL libraries that reference a specific value set release for reproducible HEDIS evaluations.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set version found and returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission"),
        @ApiResponse(responseCode = "404", description = "Value set with specified OID and version not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-oid/{oid}/version/{version}")
    public ResponseEntity<ValueSet> getValueSetByOidAndVersion(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Value set OID", required = true) @PathVariable String oid,
            @Parameter(description = "Value set version string (e.g., 20230101)", required = true) @PathVariable String version) {
        logger.debug("Getting value set: {} v{} for tenant: {}", oid, version, tenantId);

        return valueSetService.getValueSetByOidAndVersion(tenantId, oid, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get the latest version of a value set by OID
     * GET /api/v1/cql/valuesets/by-oid/{oid}/latest
     */
    @Operation(
        summary = "Get the latest version of a value set by OID",
        description = "Retrieves the most recent version of a FHIR value set identified by its OID. "
            + "Used when CQL libraries reference a value set without specifying a version, defaulting to the latest available release.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Latest value set version returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission"),
        @ApiResponse(responseCode = "404", description = "No versions found for the specified OID")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-oid/{oid}/latest")
    public ResponseEntity<ValueSet> getLatestValueSetVersion(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Value set OID to find the latest version for", required = true) @PathVariable String oid) {
        logger.debug("Getting latest version of value set: {} for tenant: {}", oid, tenantId);

        return valueSetService.getLatestValueSetVersion(tenantId, oid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all versions of a value set by OID
     * GET /api/v1/cql/valuesets/by-oid/{oid}/versions
     */
    @Operation(
        summary = "Get all versions of a value set by OID",
        description = "Returns all available versions of a FHIR value set identified by its OID. "
            + "Enables version comparison and audit of how value set content has changed across HEDIS measurement years.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "All value set versions returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-oid/{oid}/versions")
    public ResponseEntity<List<ValueSet>> getAllValueSetVersions(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Value set OID to list all versions for", required = true) @PathVariable String oid) {
        logger.debug("Getting all versions of value set: {} for tenant: {}", oid, tenantId);

        List<ValueSet> versions = valueSetService.getAllValueSetVersions(tenantId, oid);
        return ResponseEntity.ok(versions);
    }

    /**
     * Get value set by name
     * GET /api/v1/cql/valuesets/by-name/{name}
     */
    @Operation(
        summary = "Get value set by name",
        description = "Retrieves a FHIR value set by its human-readable name. "
            + "Useful for looking up value sets by clinical terminology name (e.g., 'Diabetes', 'HbA1c Lab Test').",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set found and returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission"),
        @ApiResponse(responseCode = "404", description = "Value set with specified name not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-name/{name}")
    public ResponseEntity<ValueSet> getValueSetByName(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Human-readable name of the value set", required = true) @PathVariable String name) {
        logger.debug("Getting value set with name: {} for tenant: {}", name, tenantId);

        return valueSetService.getValueSetByName(tenantId, name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get value sets by code system
     * GET /api/v1/cql/valuesets/by-code-system/{codeSystem}
     */
    @Operation(
        summary = "Get value sets by code system",
        description = "Returns a paginated list of FHIR value sets filtered by code system (e.g., SNOMED CT, LOINC, RxNorm, ICD-10). "
            + "Useful for browsing all value sets from a specific clinical terminology standard.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated value sets for the code system returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-code-system/{codeSystem}")
    public ResponseEntity<Page<ValueSet>> getValueSetsByCodeSystem(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Code system identifier (e.g., SNOMED, LOINC, RXNORM, ICD10)", required = true) @PathVariable String codeSystem,
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
    @Operation(
        summary = "Get active value sets",
        description = "Returns all value sets with ACTIVE status for the tenant. Active value sets are available for use "
            + "in CQL library evaluation and represent the current clinical terminology definitions.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active value sets returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/active")
    public ResponseEntity<List<ValueSet>> getActiveValueSets(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting active value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getActiveValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get SNOMED value sets
     * GET /api/v1/cql/valuesets/snomed
     */
    @Operation(
        summary = "Get SNOMED CT value sets",
        description = "Returns all value sets based on the SNOMED CT clinical terminology system. SNOMED CT codes are used "
            + "in HEDIS measures for diagnoses, procedures, and clinical findings (e.g., diabetes diagnosis codes).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "SNOMED CT value sets returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/snomed")
    public ResponseEntity<List<ValueSet>> getSnomedValueSets(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting SNOMED value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getSnomedValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get LOINC value sets
     * GET /api/v1/cql/valuesets/loinc
     */
    @Operation(
        summary = "Get LOINC value sets",
        description = "Returns all value sets based on the LOINC (Logical Observation Identifiers Names and Codes) system. "
            + "LOINC codes are used in HEDIS measures for laboratory tests and clinical observations (e.g., HbA1c test codes).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "LOINC value sets returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/loinc")
    public ResponseEntity<List<ValueSet>> getLoincValueSets(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting LOINC value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getLoincValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get RxNorm value sets
     * GET /api/v1/cql/valuesets/rxnorm
     */
    @Operation(
        summary = "Get RxNorm value sets",
        description = "Returns all value sets based on the RxNorm medication terminology system. RxNorm codes are used "
            + "in HEDIS measures for medication-related quality measures (e.g., statin therapy, antidepressant medication management).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "RxNorm value sets returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/rxnorm")
    public ResponseEntity<List<ValueSet>> getRxNormValueSets(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting RxNorm value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getRxNormValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get common code system value sets (SNOMED, LOINC, RxNorm)
     * GET /api/v1/cql/valuesets/common
     */
    @Operation(
        summary = "Get common code system value sets",
        description = "Returns value sets from the three most commonly used clinical terminology systems: SNOMED CT, LOINC, and RxNorm. "
            + "Provides a consolidated view of the core value sets used across HEDIS quality measure evaluations.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Common code system value sets returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/common")
    public ResponseEntity<List<ValueSet>> getCommonCodeSystemValueSets(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Getting common code system value sets for tenant: {}", tenantId);

        List<ValueSet> valueSets = valueSetService.getCommonCodeSystemValueSets(tenantId);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Search value sets by name
     * GET /api/v1/cql/valuesets/search?q={searchTerm}
     */
    @Operation(
        summary = "Search value sets by name",
        description = "Searches for FHIR value sets whose name contains the specified search term. "
            + "Supports partial matching for discovering value sets by clinical concept (e.g., searching 'diabetes' returns all diabetes-related value sets).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matching value sets returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/search")
    public ResponseEntity<List<ValueSet>> searchValueSets(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Search term to match against value set names", required = true) @RequestParam("q") String searchTerm) {
        logger.debug("Searching value sets for: {} in tenant: {}", searchTerm, tenantId);

        List<ValueSet> valueSets = valueSetService.searchValueSetsByName(tenantId, searchTerm);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Get value sets by OID prefix
     * GET /api/v1/cql/valuesets/by-oid-prefix/{oidPrefix}
     */
    @Operation(
        summary = "Get value sets by OID prefix",
        description = "Returns all FHIR value sets whose OID starts with the specified prefix. "
            + "Useful for finding value sets within a specific OID namespace (e.g., all NCQA HEDIS value sets under 2.16.840.1.113883.3.464).",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value sets matching OID prefix returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/by-oid-prefix/{oidPrefix}")
    public ResponseEntity<List<ValueSet>> getValueSetsByOidPrefix(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "OID prefix to match (e.g., 2.16.840.1.113883.3.464)", required = true) @PathVariable String oidPrefix) {
        logger.debug("Getting value sets with OID prefix: {} for tenant: {}", oidPrefix, tenantId);

        List<ValueSet> valueSets = valueSetService.getValueSetsByOidPrefix(tenantId, oidPrefix);
        return ResponseEntity.ok(valueSets);
    }

    /**
     * Update a value set
     * PUT /api/v1/cql/valuesets/{id}
     */
    @Operation(
        summary = "Update a value set",
        description = "Updates an existing FHIR value set's metadata, codes, or status. "
            + "Used to maintain value set content as clinical terminology standards evolve across HEDIS measurement years.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set updated successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_WRITE permission"),
        @ApiResponse(responseCode = "404", description = "Value set not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping("/{id}")
    public ResponseEntity<ValueSet> updateValueSet(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the value set to update", required = true) @PathVariable UUID id,
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
    @Operation(
        summary = "Activate a value set",
        description = "Sets a value set's status to ACTIVE, making it available for use in CQL library evaluation. "
            + "Only active value sets are resolved during HEDIS quality measure execution.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set activated successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_WRITE permission"),
        @ApiResponse(responseCode = "404", description = "Value set not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/activate")
    public ResponseEntity<ValueSet> activateValueSet(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the value set to activate", required = true) @PathVariable UUID id) {
        logger.info("Activating value set: {} for tenant: {}", id, tenantId);

        ValueSet activated = valueSetService.activateValueSet(id, tenantId);
        return ResponseEntity.ok(activated);
    }

    /**
     * Retire a value set
     * POST /api/v1/cql/valuesets/{id}/retire
     */
    @Operation(
        summary = "Retire a value set",
        description = "Sets a value set's status to RETIRED, removing it from active use in CQL evaluations. "
            + "Retired value sets remain in the system for historical reference but are not resolved during new evaluations.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set retired successfully", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_WRITE permission"),
        @ApiResponse(responseCode = "404", description = "Value set not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/retire")
    public ResponseEntity<ValueSet> retireValueSet(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the value set to retire", required = true) @PathVariable UUID id) {
        logger.info("Retiring value set: {} for tenant: {}", id, tenantId);

        ValueSet retired = valueSetService.retireValueSet(id, tenantId);
        return ResponseEntity.ok(retired);
    }

    /**
     * Delete a value set (soft delete)
     * DELETE /api/v1/cql/valuesets/{id}
     */
    @Operation(
        summary = "Delete a value set",
        description = "Soft-deletes a FHIR value set, removing it from active queries. "
            + "The value set data is retained for audit purposes in compliance with HIPAA data retention requirements.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Value set deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_WRITE permission"),
        @ApiResponse(responseCode = "404", description = "Value set not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteValueSet(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the value set to delete", required = true) @PathVariable UUID id) {
        logger.info("Deleting value set: {} for tenant: {}", id, tenantId);

        valueSetService.deleteValueSet(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get value set count
     * GET /api/v1/cql/valuesets/count
     */
    @Operation(
        summary = "Get total value set count",
        description = "Returns the total number of FHIR value sets for the tenant. "
            + "Useful for dashboard metrics and monitoring the scope of clinical terminology coverage.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set count returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count")
    public ResponseEntity<Long> countValueSets(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId) {
        logger.debug("Counting value sets for tenant: {}", tenantId);

        long count = valueSetService.countValueSets(tenantId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get value set count by code system
     * GET /api/v1/cql/valuesets/count/by-code-system/{codeSystem}
     */
    @Operation(
        summary = "Get value set count by code system",
        description = "Returns the number of FHIR value sets for a specific code system within the tenant. "
            + "Useful for understanding the distribution of value sets across SNOMED CT, LOINC, RxNorm, and ICD-10.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Value set count for code system returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/count/by-code-system/{codeSystem}")
    public ResponseEntity<Long> countValueSetsByCodeSystem(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Code system to count value sets for (e.g., SNOMED, LOINC, RXNORM)", required = true) @PathVariable String codeSystem) {
        logger.debug("Counting value sets for code system: {} in tenant: {}",
                codeSystem, tenantId);

        long count = valueSetService.countValueSetsByCodeSystem(tenantId, codeSystem);
        return ResponseEntity.ok(count);
    }

    /**
     * Check if value set exists by OID
     * GET /api/v1/cql/valuesets/exists/by-oid/{oid}
     */
    @Operation(
        summary = "Check if value set exists by OID",
        description = "Returns a boolean indicating whether a FHIR value set with the specified OID exists in the tenant. "
            + "Used by CQL evaluation engines to verify value set availability before executing quality measure logic.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Existence check result returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/exists/by-oid/{oid}")
    public ResponseEntity<Boolean> valueSetExistsByOid(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Value set OID to check existence for", required = true) @PathVariable String oid) {
        logger.debug("Checking if value set exists with OID: {} for tenant: {}", oid, tenantId);

        boolean exists = valueSetService.valueSetExistsByOid(tenantId, oid);
        return ResponseEntity.ok(exists);
    }

    /**
     * Check if a code exists in a value set
     * GET /api/v1/cql/valuesets/{id}/contains-code/{code}
     */
    @Operation(
        summary = "Check if a code exists in a value set",
        description = "Returns a boolean indicating whether a specific clinical code exists within a FHIR value set. "
            + "Used during CQL evaluation to determine if a patient's clinical data (diagnoses, labs, medications) matches the value set criteria.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Code membership check result returned", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied - requires MEASURE_READ permission"),
        @ApiResponse(responseCode = "404", description = "Value set not found")
    })
    @PreAuthorize("hasAuthority('MEASURE_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/{id}/contains-code/{code}")
    public ResponseEntity<Boolean> codeExistsInValueSet(
            @Parameter(description = "Tenant identifier for multi-tenant isolation", required = true) @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "UUID of the value set to check", required = true) @PathVariable UUID id,
            @Parameter(description = "Clinical code to check membership for (e.g., SNOMED code, LOINC code)", required = true) @PathVariable String code) {
        logger.debug("Checking if code {} exists in value set: {} for tenant: {}",
                code, id, tenantId);

        boolean exists = valueSetService.codeExistsInValueSet(id, tenantId, code);
        return ResponseEntity.ok(exists);
    }
}
