package com.healthdata.quality.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.dto.versioning.CreateVersionRequest;
import com.healthdata.quality.dto.versioning.MeasureVersionDTO;
import com.healthdata.quality.dto.versioning.VersionComparisonDTO;
import com.healthdata.quality.persistence.MeasureVersionEntity;
import com.healthdata.quality.service.MeasureVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Measure Version Controller - REST API for measure versioning and audit trail.
 *
 * Endpoints:
 * - POST   /api/v1/measures/{measureId}/versions     - Create new version
 * - GET    /api/v1/measures/{measureId}/versions     - Get version history
 * - GET    /api/v1/measures/{measureId}/versions/{version} - Get specific version
 * - POST   /api/v1/measures/{measureId}/versions/{version}/current - Set as current
 * - POST   /api/v1/measures/{measureId}/versions/{version}/publish - Publish version
 * - GET    /api/v1/measures/{measureId}/versions/compare - Compare two versions
 *
 * Issue #152: Measure Versioning and Audit Trail
 */
@RestController
@RequestMapping("/api/v1/measures")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(
    name = "Measure Versioning",
    description = "APIs for HEDIS quality measure version management, audit trail, and publishing workflow."
)
public class MeasureVersionController {

    private final MeasureVersionService versionService;

    /**
     * Create a new version for a measure.
     */
    @Operation(
        summary = "Create new measure version",
        description = "Creates a new HEDIS quality measure version using semantic versioning (MAJOR/MINOR/PATCH). "
            + "MAJOR versions indicate breaking CQL logic changes, MINOR versions add new value sets or criteria, "
            + "and PATCH versions fix defects without changing measure intent. Each version captures a complete "
            + "snapshot of CQL text and value set bindings for auditability.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Measure version created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MeasureVersionDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid version request — missing CQL text or invalid version type"),
        @ApiResponse(responseCode = "404", description = "Measure not found"),
        @ApiResponse(responseCode = "403", description = "Access denied — requires EVALUATOR, ADMIN, or SUPER_ADMIN role"),
        @ApiResponse(responseCode = "409", description = "Version conflict — concurrent version creation detected")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> createVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader("X-Auth-User-Id") @NotBlank(message = "User ID is required") String userId,
            @RequestHeader(value = "X-Auth-Username", required = false) String username,
            @Parameter(description = "UUID of the HEDIS quality measure to version", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @Valid @RequestBody CreateVersionRequest request) {

        log.info("POST /api/v1/measures/{}/versions - tenant: {}, versionType: {}",
                measureId, tenantId, request.getVersionType());

        MeasureVersionEntity version = versionService.createVersion(
                tenantId,
                measureId,
                request.getVersionType(),
                request.getCqlText(),
                request.getValueSets(),
                request.getChangeSummary(),
                UUID.fromString(userId),
                username);

        return ResponseEntity.status(HttpStatus.CREATED).body(MeasureVersionDTO.fromEntity(version));
    }

    /**
     * Create initial version for a new measure (1.0.0).
     */
    @Operation(
        summary = "Create initial measure version (1.0.0)",
        description = "Creates the initial 1.0.0 version for a newly defined HEDIS quality measure. "
            + "This must be called once before any subsequent version increments. The initial version "
            + "establishes the baseline CQL logic and value set bindings for the measure.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Initial version 1.0.0 created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MeasureVersionDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request — CQL text is required"),
        @ApiResponse(responseCode = "404", description = "Measure not found"),
        @ApiResponse(responseCode = "403", description = "Access denied — requires EVALUATOR, ADMIN, or SUPER_ADMIN role"),
        @ApiResponse(responseCode = "409", description = "Initial version already exists for this measure")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions/initial", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> createInitialVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader("X-Auth-User-Id") @NotBlank(message = "User ID is required") String userId,
            @RequestHeader(value = "X-Auth-Username", required = false) String username,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @RequestBody Map<String, String> body) {

        log.info("POST /api/v1/measures/{}/versions/initial - tenant: {}", measureId, tenantId);

        String cqlText = body.get("cqlText");
        String valueSets = body.get("valueSets");

        if (cqlText == null || cqlText.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        MeasureVersionEntity version = versionService.createInitialVersion(
                tenantId,
                measureId,
                cqlText,
                valueSets,
                UUID.fromString(userId),
                username);

        return ResponseEntity.status(HttpStatus.CREATED).body(MeasureVersionDTO.fromEntity(version));
    }

    /**
     * Get version history for a measure.
     */
    @Operation(
        summary = "Get measure version history",
        description = "Retrieves the paginated version history for a HEDIS quality measure, ordered by creation date "
            + "descending. Each entry includes version number, status (DRAFT/PUBLISHED/RETIRED), author, "
            + "and change summary. Useful for auditing measure evolution across HEDIS measurement years.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version history retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Measure not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{measureId}/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MeasureVersionDTO>> getVersionHistory(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @Parameter(description = "Page number (zero-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(value = "size", defaultValue = "20") @PositiveOrZero int size) {

        log.info("GET /api/v1/measures/{}/versions - tenant: {}, page: {}, size: {}",
                measureId, tenantId, page, size);

        Page<MeasureVersionEntity> versions = versionService.getVersionHistory(tenantId, measureId, page, size);
        Page<MeasureVersionDTO> dtos = versions.map(MeasureVersionDTO::fromEntitySummary);

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get version summaries (lightweight for dropdowns).
     */
    @Operation(
        summary = "Get lightweight version summaries",
        description = "Returns a lightweight list of version summaries suitable for UI dropdowns and selectors. "
            + "Includes version number, status, and creation date without full CQL text or value set details. "
            + "Optimized for the Clinical Portal measure version selector.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version summaries retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Measure not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @GetMapping(value = "/{measureId}/versions/summaries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getVersionSummaries(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId) {

        log.info("GET /api/v1/measures/{}/versions/summaries - tenant: {}", measureId, tenantId);

        List<Map<String, Object>> summaries = versionService.getVersionSummaries(tenantId, measureId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Get a specific version.
     */
    @Operation(
        summary = "Get specific measure version",
        description = "Retrieves the full details of a specific measure version including CQL text, value set bindings, "
            + "change summary, status, and authorship metadata. Used for reviewing CQL logic at a specific "
            + "point in the measure's lifecycle.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version details retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MeasureVersionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Measure or version not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{measureId}/versions/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> getVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @Parameter(description = "Semantic version string (e.g., 1.0.0, 2.1.0)", required = true, example = "1.0.0")
            @PathVariable @NotBlank(message = "Version is required") String version) {

        log.info("GET /api/v1/measures/{}/versions/{} - tenant: {}", measureId, version, tenantId);

        MeasureVersionEntity entity = versionService.getVersion(tenantId, measureId, version);
        return ResponseEntity.ok(MeasureVersionDTO.fromEntity(entity));
    }

    /**
     * Get the current active version.
     */
    @Operation(
        summary = "Get current active measure version",
        description = "Retrieves the currently active version of a HEDIS quality measure. The current version is the one "
            + "used by the CQL evaluation engine for measure calculations. Returns 404 if no version has been "
            + "set as current.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current version retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MeasureVersionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Measure not found or no current version set"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @GetMapping(value = "/{measureId}/versions/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> getCurrentVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId) {

        log.info("GET /api/v1/measures/{}/versions/current - tenant: {}", measureId, tenantId);

        return versionService.getCurrentVersion(tenantId, measureId)
                .map(v -> ResponseEntity.ok(MeasureVersionDTO.fromEntity(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Set a version as the current active version (rollback support).
     */
    @Operation(
        summary = "Set version as current (rollback support)",
        description = "Designates a specific version as the current active version for CQL evaluation. "
            + "Supports rollback by allowing any previously published version to be re-activated. "
            + "The previously current version is automatically demoted. This is critical for "
            + "mid-measurement-year corrections to HEDIS logic.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version set as current successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MeasureVersionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Measure or version not found"),
        @ApiResponse(responseCode = "403", description = "Access denied — requires EVALUATOR, ADMIN, or SUPER_ADMIN role")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions/{version}/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> setCurrentVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @Parameter(description = "Semantic version to activate (e.g., 1.0.0)", required = true, example = "1.0.0")
            @PathVariable @NotBlank(message = "Version is required") String version) {

        log.info("POST /api/v1/measures/{}/versions/{}/current - tenant: {}", measureId, version, tenantId);

        MeasureVersionEntity entity = versionService.setCurrentVersion(tenantId, measureId, version);
        return ResponseEntity.ok(MeasureVersionDTO.fromEntity(entity));
    }

    /**
     * Publish a version to production.
     */
    @Operation(
        summary = "Publish measure version to production",
        description = "Transitions a DRAFT measure version to PUBLISHED status, making it available for production "
            + "CQL evaluation. Published versions are immutable — CQL text and value sets cannot be modified "
            + "after publishing. Only ADMIN and SUPER_ADMIN roles can publish, enforcing separation of duties "
            + "in the HEDIS measure development workflow.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version published successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MeasureVersionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Measure or version not found"),
        @ApiResponse(responseCode = "400", description = "Version is not in DRAFT status and cannot be published"),
        @ApiResponse(responseCode = "403", description = "Access denied — requires ADMIN or SUPER_ADMIN role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions/{version}/publish", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> publishVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader("X-Auth-User-Id") @NotBlank(message = "User ID is required") String userId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @Parameter(description = "Semantic version to publish (e.g., 2.0.0)", required = true, example = "2.0.0")
            @PathVariable @NotBlank(message = "Version is required") String version) {

        log.info("POST /api/v1/measures/{}/versions/{}/publish - tenant: {}", measureId, version, tenantId);

        MeasureVersionEntity entity = versionService.publishVersion(
                tenantId, measureId, version, UUID.fromString(userId));
        return ResponseEntity.ok(MeasureVersionDTO.fromEntity(entity));
    }

    /**
     * Retire a version (mark as no longer active).
     */
    @Operation(
        summary = "Retire a measure version",
        description = "Transitions a measure version to RETIRED status, indicating it is no longer valid for "
            + "CQL evaluation. Retired versions remain in the system for audit trail purposes but cannot "
            + "be set as current. Typically used when a HEDIS measurement year ends or when a measure "
            + "specification is superseded by NCQA updates.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version retired successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MeasureVersionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Measure or version not found"),
        @ApiResponse(responseCode = "400", description = "Version cannot be retired — may be in an invalid state"),
        @ApiResponse(responseCode = "403", description = "Access denied — requires ADMIN or SUPER_ADMIN role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions/{version}/retire", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> retireVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader("X-Auth-User-Id") @NotBlank(message = "User ID is required") String userId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @Parameter(description = "Semantic version to retire (e.g., 1.0.0)", required = true, example = "1.0.0")
            @PathVariable @NotBlank(message = "Version is required") String version) {

        log.info("POST /api/v1/measures/{}/versions/{}/retire - tenant: {}", measureId, version, tenantId);

        MeasureVersionEntity entity = versionService.retireVersion(
                tenantId, measureId, version, UUID.fromString(userId));
        return ResponseEntity.ok(MeasureVersionDTO.fromEntity(entity));
    }

    /**
     * Get all published versions for a measure.
     */
    @Operation(
        summary = "List published measure versions",
        description = "Retrieves all versions with PUBLISHED status for a HEDIS quality measure. Published versions "
            + "represent production-ready CQL logic that has been reviewed and approved. Useful for identifying "
            + "which versions are eligible for CQL evaluation or rollback.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Published versions retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Measure not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @GetMapping(value = "/{measureId}/versions/published", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MeasureVersionDTO>> getPublishedVersions(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId) {

        log.info("GET /api/v1/measures/{}/versions/published - tenant: {}", measureId, tenantId);

        List<MeasureVersionEntity> versions = versionService.getPublishedVersions(tenantId, measureId);
        List<MeasureVersionDTO> dtos = versions.stream()
                .map(MeasureVersionDTO::fromEntitySummary)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Compare two versions.
     */
    @Operation(
        summary = "Compare two measure versions (CQL diff)",
        description = "Performs a side-by-side comparison of two measure versions, highlighting differences in CQL text, "
            + "value set bindings, and metadata. Returns a structured diff showing additions, deletions, and "
            + "modifications. Essential for HEDIS measure stewards reviewing CQL logic changes between "
            + "measurement years or patch releases.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version comparison completed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = VersionComparisonDTO.class))),
        @ApiResponse(responseCode = "404", description = "Measure or one of the specified versions not found"),
        @ApiResponse(responseCode = "400", description = "Invalid version parameters — both v1 and v2 are required"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{measureId}/versions/compare", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VersionComparisonDTO> compareVersions(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @Parameter(description = "First version for comparison (e.g., 1.0.0)", required = true, example = "1.0.0")
            @RequestParam("v1") @NotBlank(message = "Version 1 is required") String version1,
            @Parameter(description = "Second version for comparison (e.g., 2.0.0)", required = true, example = "2.0.0")
            @RequestParam("v2") @NotBlank(message = "Version 2 is required") String version2) {

        log.info("GET /api/v1/measures/{}/versions/compare - tenant: {}, v1: {}, v2: {}",
                measureId, tenantId, version1, version2);

        MeasureVersionService.VersionComparisonResult result =
                versionService.compareVersions(tenantId, measureId, version1, version2);

        return ResponseEntity.ok(VersionComparisonDTO.fromResult(result));
    }

    /**
     * Get version count for a measure.
     */
    @Operation(
        summary = "Get version count for a measure",
        description = "Returns the total number of versions (all statuses) for a HEDIS quality measure. "
            + "Useful for dashboard widgets and measure maturity indicators.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version count retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Measure not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @GetMapping(value = "/{measureId}/versions/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getVersionCount(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the HEDIS quality measure", required = true)
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId) {

        log.info("GET /api/v1/measures/{}/versions/count - tenant: {}", measureId, tenantId);

        long count = versionService.getVersionCount(tenantId, measureId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get all versions by user (audit trail).
     */
    @Operation(
        summary = "Get versions by user (audit trail)",
        description = "Retrieves all measure versions created by a specific user across all measures in the tenant. "
            + "Provides an audit trail of measure authorship for compliance reviews and HEDIS measure "
            + "development accountability tracking.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User's version history retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Access denied — requires ADMIN or SUPER_ADMIN role")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/versions/by-user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MeasureVersionDTO>> getVersionsByUser(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "UUID of the user whose version history to retrieve", required = true)
            @PathVariable @NotNull(message = "User ID is required") UUID userId) {

        log.info("GET /api/v1/measures/versions/by-user/{} - tenant: {}", userId, tenantId);

        List<MeasureVersionEntity> versions = versionService.getVersionsByUser(tenantId, userId);
        List<MeasureVersionDTO> dtos = versions.stream()
                .map(MeasureVersionDTO::fromEntitySummary)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get all versions in tenant (admin audit view).
     */
    @Operation(
        summary = "Get all versions in tenant (admin audit view)",
        description = "Retrieves a paginated list of all measure versions across all measures within the tenant. "
            + "Designed for SUPER_ADMIN audit dashboards to provide a comprehensive view of measure "
            + "development activity, version status distribution, and authorship patterns across "
            + "the entire HEDIS measure portfolio.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tenant-wide version audit data retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Access denied — requires SUPER_ADMIN role")
    })
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/versions/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MeasureVersionDTO>> getAllVersions(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @Parameter(description = "Page number (zero-based)", example = "0")
            @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page,
            @Parameter(description = "Page size", example = "50")
            @RequestParam(value = "size", defaultValue = "50") @PositiveOrZero int size) {

        log.info("GET /api/v1/measures/versions/audit - tenant: {}, page: {}, size: {}", tenantId, page, size);

        Page<MeasureVersionEntity> versions = versionService.getAllVersions(tenantId, page, size);
        Page<MeasureVersionDTO> dtos = versions.map(MeasureVersionDTO::fromEntitySummary);

        return ResponseEntity.ok(dtos);
    }
}
