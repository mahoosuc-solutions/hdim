package com.healthdata.admin.controller;

import com.healthdata.admin.dto.AlertConfigRequest;
import com.healthdata.admin.dto.AlertConfigResponse;
import com.healthdata.admin.dto.AlertConfigUpdateRequest;
import com.healthdata.admin.service.AlertConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Alert Configuration REST Controller
 *
 * Provides CRUD operations for alert configurations.
 * Requires ADMIN or SUPER_ADMIN role for all operations.
 *
 * Multi-tenant: Tenant ID extracted from X-Tenant-ID header
 * Security: All endpoints protected with role-based access control
 */
@RestController
@RequestMapping("/api/v1/admin/alerts/configs")
@RequiredArgsConstructor
@Slf4j
public class AlertConfigController {

    private final AlertConfigService alertConfigService;

    /**
     * Get all alert configurations for the current tenant
     *
     * @param tenantId Tenant identifier (from header)
     * @return List of alert configurations
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<AlertConfigResponse>> getAllAlertConfigs(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("GET /api/v1/admin/alerts/configs - tenant: {}", tenantId);

        List<AlertConfigResponse> configs = alertConfigService.getAllAlertConfigs(tenantId);

        return ResponseEntity.ok(configs);
    }

    /**
     * Get alert configuration by ID
     *
     * @param tenantId Tenant identifier (from header)
     * @param id       Alert configuration ID
     * @return Alert configuration
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AlertConfigResponse> getAlertConfig(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.info("GET /api/v1/admin/alerts/configs/{} - tenant: {}", id, tenantId);

        AlertConfigResponse config = alertConfigService.getAlertConfig(tenantId, id);

        return ResponseEntity.ok(config);
    }

    /**
     * Create a new alert configuration
     *
     * @param tenantId  Tenant identifier (from header)
     * @param request   Alert configuration request
     * @param principal Authenticated user
     * @return Created alert configuration
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AlertConfigResponse> createAlertConfig(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody @Valid AlertConfigRequest request,
            Principal principal) {

        log.info("POST /api/v1/admin/alerts/configs - tenant: {}, service: {}, type: {}",
                tenantId, request.getServiceName(), request.getAlertType());

        String username = principal != null ? principal.getName() : "system";

        AlertConfigResponse created = alertConfigService.createAlertConfig(
                tenantId, request, username);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing alert configuration
     *
     * @param tenantId Tenant identifier (from header)
     * @param id       Alert configuration ID
     * @param request  Update request
     * @return Updated alert configuration
     */
    @PutMapping(
        value = "/{id}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<AlertConfigResponse> updateAlertConfig(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id,
            @RequestBody @Valid AlertConfigUpdateRequest request) {

        log.info("PUT /api/v1/admin/alerts/configs/{} - tenant: {}", id, tenantId);

        AlertConfigResponse updated = alertConfigService.updateAlertConfig(
                tenantId, id, request);

        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an alert configuration
     *
     * @param tenantId Tenant identifier (from header)
     * @param id       Alert configuration ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteAlertConfig(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.info("DELETE /api/v1/admin/alerts/configs/{} - tenant: {}", id, tenantId);

        alertConfigService.deleteAlertConfig(tenantId, id);

        return ResponseEntity.noContent().build();
    }
}
