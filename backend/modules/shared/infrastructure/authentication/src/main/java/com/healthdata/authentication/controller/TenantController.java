package com.healthdata.authentication.controller;

import com.healthdata.authentication.dto.TenantRegistrationRequest;
import com.healthdata.authentication.dto.TenantRegistrationResponse;
import com.healthdata.authentication.exception.TenantAlreadyExistsException;
import com.healthdata.authentication.exception.UserAlreadyExistsException;
import com.healthdata.authentication.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for tenant management operations.
 * Provides endpoints for tenant registration and lifecycle management.
 *
 * CONDITIONAL LOADING:
 * This controller is conditionally loaded by AuthenticationControllerAutoConfiguration
 * only when authentication.controller.enabled=true in application properties.
 * By default, this is disabled in all services except the Gateway.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tenants")
@Validated
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "authentication.controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class TenantController {

    private final TenantService tenantService;

    /**
     * Register a new tenant with an admin user.
     *
     * PUBLIC ENDPOINT: Allows self-service tenant registration.
     * For restricted registration, change @PreAuthorize to hasRole('SUPER_ADMIN').
     *
     * @param request Tenant registration details
     * @return TenantRegistrationResponse with tenant and admin user details
     * @throws ResponseStatusException 409 if tenant already exists
     * @throws ResponseStatusException 400 if validation fails
     */
    @PostMapping(
        value = "/register",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("permitAll()")  // Public registration - change to hasRole('SUPER_ADMIN') for restricted
    public ResponseEntity<TenantRegistrationResponse> registerTenant(
            @Valid @RequestBody TenantRegistrationRequest request) {

        log.info("Tenant registration request received: {}", request.getTenantId());

        try {
            TenantRegistrationResponse response = tenantService.registerTenant(request);
            log.info("Tenant registration successful: {}", response.getTenantId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (TenantAlreadyExistsException e) {
            log.warn("Tenant registration failed - tenant already exists: {}", request.getTenantId());
            throw e;
        } catch (UserAlreadyExistsException e) {
            log.warn("Tenant registration failed - duplicate user: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Tenant registration failed for {}: {}",
                request.getTenantId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Activate a suspended tenant.
     * Only SUPER_ADMIN can activate tenants.
     *
     * @param tenantId Tenant ID to activate
     * @return 204 No Content on success
     */
    @PostMapping("/{tenantId}/activate")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<Void> activateTenant(@PathVariable String tenantId) {
        log.info("Tenant activation request: {}", tenantId);
        tenantService.activateTenant(tenantId);
        log.info("Tenant activated: {}", tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Suspend a tenant.
     * Only SUPER_ADMIN can suspend tenants.
     *
     * @param tenantId Tenant ID to suspend
     * @return 204 No Content on success
     */
    @PostMapping("/{tenantId}/suspend")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<Void> suspendTenant(@PathVariable String tenantId) {
        log.info("Tenant suspension request: {}", tenantId);
        tenantService.suspendTenant(tenantId);
        log.info("Tenant suspended: {}", tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deactivate a tenant permanently.
     * Only SUPER_ADMIN can deactivate tenants.
     *
     * @param tenantId Tenant ID to deactivate
     * @return 204 No Content on success
     */
    @PostMapping("/{tenantId}/deactivate")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<Void> deactivateTenant(@PathVariable String tenantId) {
        log.info("Tenant deactivation request: {}", tenantId);
        tenantService.deactivateTenant(tenantId);
        log.info("Tenant deactivated: {}", tenantId);
        return ResponseEntity.noContent().build();
    }
}
