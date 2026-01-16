package com.healthdata.quality.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.dto.versioning.CreateVersionRequest;
import com.healthdata.quality.dto.versioning.MeasureVersionDTO;
import com.healthdata.quality.dto.versioning.VersionComparisonDTO;
import com.healthdata.quality.persistence.MeasureVersionEntity;
import com.healthdata.quality.service.MeasureVersionService;
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
public class MeasureVersionController {

    private final MeasureVersionService versionService;

    /**
     * Create a new version for a measure.
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> createVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader("X-Auth-User-Id") @NotBlank(message = "User ID is required") String userId,
            @RequestHeader(value = "X-Auth-Username", required = false) String username,
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
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions/initial", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> createInitialVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader("X-Auth-User-Id") @NotBlank(message = "User ID is required") String userId,
            @RequestHeader(value = "X-Auth-Username", required = false) String username,
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
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{measureId}/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MeasureVersionDTO>> getVersionHistory(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page,
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
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @GetMapping(value = "/{measureId}/versions/summaries", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> getVersionSummaries(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId) {

        log.info("GET /api/v1/measures/{}/versions/summaries - tenant: {}", measureId, tenantId);

        List<Map<String, Object>> summaries = versionService.getVersionSummaries(tenantId, measureId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Get a specific version.
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{measureId}/versions/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> getVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @PathVariable @NotBlank(message = "Version is required") String version) {

        log.info("GET /api/v1/measures/{}/versions/{} - tenant: {}", measureId, version, tenantId);

        MeasureVersionEntity entity = versionService.getVersion(tenantId, measureId, version);
        return ResponseEntity.ok(MeasureVersionDTO.fromEntity(entity));
    }

    /**
     * Get the current active version.
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @GetMapping(value = "/{measureId}/versions/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> getCurrentVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId) {

        log.info("GET /api/v1/measures/{}/versions/current - tenant: {}", measureId, tenantId);

        return versionService.getCurrentVersion(tenantId, measureId)
                .map(v -> ResponseEntity.ok(MeasureVersionDTO.fromEntity(v)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Set a version as the current active version (rollback support).
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions/{version}/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> setCurrentVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @PathVariable @NotBlank(message = "Version is required") String version) {

        log.info("POST /api/v1/measures/{}/versions/{}/current - tenant: {}", measureId, version, tenantId);

        MeasureVersionEntity entity = versionService.setCurrentVersion(tenantId, measureId, version);
        return ResponseEntity.ok(MeasureVersionDTO.fromEntity(entity));
    }

    /**
     * Publish a version to production.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(value = "/{measureId}/versions/{version}/publish", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MeasureVersionDTO> publishVersion(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestHeader("X-Auth-User-Id") @NotBlank(message = "User ID is required") String userId,
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @PathVariable @NotBlank(message = "Version is required") String version) {

        log.info("POST /api/v1/measures/{}/versions/{}/publish - tenant: {}", measureId, version, tenantId);

        MeasureVersionEntity entity = versionService.publishVersion(
                tenantId, measureId, version, UUID.fromString(userId));
        return ResponseEntity.ok(MeasureVersionDTO.fromEntity(entity));
    }

    /**
     * Get all published versions for a measure.
     */
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @GetMapping(value = "/{measureId}/versions/published", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MeasureVersionDTO>> getPublishedVersions(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
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
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{measureId}/versions/compare", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VersionComparisonDTO> compareVersions(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId,
            @RequestParam("v1") @NotBlank(message = "Version 1 is required") String version1,
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
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN', 'MEASURE_DEVELOPER')")
    @GetMapping(value = "/{measureId}/versions/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> getVersionCount(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @PathVariable @NotNull(message = "Measure ID is required") UUID measureId) {

        log.info("GET /api/v1/measures/{}/versions/count - tenant: {}", measureId, tenantId);

        long count = versionService.getVersionCount(tenantId, measureId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get all versions by user (audit trail).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/versions/by-user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MeasureVersionDTO>> getVersionsByUser(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
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
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/versions/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<MeasureVersionDTO>> getAllVersions(
            @RequestHeader("X-Tenant-ID") @NotBlank(message = "Tenant ID is required") String tenantId,
            @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(value = "size", defaultValue = "50") @PositiveOrZero int size) {

        log.info("GET /api/v1/measures/versions/audit - tenant: {}, page: {}, size: {}", tenantId, page, size);

        Page<MeasureVersionEntity> versions = versionService.getAllVersions(tenantId, page, size);
        Page<MeasureVersionDTO> dtos = versions.map(MeasureVersionDTO::fromEntitySummary);

        return ResponseEntity.ok(dtos);
    }
}
