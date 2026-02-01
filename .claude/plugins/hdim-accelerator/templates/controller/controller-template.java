package com.healthdata.{{SERVICE_NAME}}.api.v1;

import com.healthdata.{{SERVICE_NAME}}.dto.{{ENTITY_CLASS_NAME}}Request;
import com.healthdata.{{SERVICE_NAME}}.dto.{{ENTITY_CLASS_NAME}}Response;
import com.healthdata.{{SERVICE_NAME}}.service.{{ENTITY_CLASS_NAME}}Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API controller for {{ENTITY_CLASS_NAME}} management.
 *
 * Security: All endpoints require authentication via gateway trust headers.
 * Authorization: Role-based access control via @PreAuthorize annotations.
 * Multi-tenant: Tenant ID extracted from X-Tenant-ID header.
 * Audit: All PHI access methods should be @Audited (if applicable).
 */
@RestController
@RequestMapping("/api/v1/{{RESOURCE_PATH}}")
@RequiredArgsConstructor
@Validated
@Slf4j
public class {{ENTITY_CLASS_NAME}}Controller {

    private final {{ENTITY_CLASS_NAME}}Service service;

    /**
     * Get {{ENTITY_CLASS_NAME}} by ID with tenant isolation.
     *
     * @param id Entity ID
     * @param tenantId Tenant ID from gateway trust header
     * @return {{ENTITY_CLASS_NAME}} response
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    public ResponseEntity<{{ENTITY_CLASS_NAME}}Response> getById(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Getting {{ENTITY_CLASS_NAME}} with id={} for tenant={}", id, tenantId);

        {{ENTITY_CLASS_NAME}}Response response = service.getById(id, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all {{ENTITY_CLASS_NAME}}s for tenant with pagination.
     *
     * @param tenantId Tenant ID from gateway trust header
     * @param pageable Pagination parameters
     * @return Page of {{ENTITY_CLASS_NAME}} responses
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    public ResponseEntity<Page<{{ENTITY_CLASS_NAME}}Response>> getAll(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        log.debug("Getting all {{ENTITY_CLASS_NAME}}s for tenant={}", tenantId);

        Page<{{ENTITY_CLASS_NAME}}Response> responses = service.getAll(tenantId, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * Create new {{ENTITY_CLASS_NAME}} for tenant.
     *
     * @param request {{ENTITY_CLASS_NAME}} creation request
     * @param tenantId Tenant ID from gateway trust header
     * @param userId User ID from gateway trust header
     * @return Created {{ENTITY_CLASS_NAME}} response
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<{{ENTITY_CLASS_NAME}}Response> create(
            @Valid @RequestBody {{ENTITY_CLASS_NAME}}Request request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Creating {{ENTITY_CLASS_NAME}} for tenant={} by user={}", tenantId, userId);

        {{ENTITY_CLASS_NAME}}Response response = service.create(request, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update existing {{ENTITY_CLASS_NAME}} with tenant isolation.
     *
     * @param id Entity ID
     * @param request {{ENTITY_CLASS_NAME}} update request
     * @param tenantId Tenant ID from gateway trust header
     * @param userId User ID from gateway trust header
     * @return Updated {{ENTITY_CLASS_NAME}} response
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<{{ENTITY_CLASS_NAME}}Response> update(
            @PathVariable UUID id,
            @Valid @RequestBody {{ENTITY_CLASS_NAME}}Request request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Updating {{ENTITY_CLASS_NAME}} with id={} for tenant={} by user={}", id, tenantId, userId);

        {{ENTITY_CLASS_NAME}}Response response = service.update(id, request, tenantId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete {{ENTITY_CLASS_NAME}} with tenant isolation.
     *
     * @param id Entity ID
     * @param tenantId Tenant ID from gateway trust header
     * @return No content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Deleting {{ENTITY_CLASS_NAME}} with id={} for tenant={}", id, tenantId);

        service.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
