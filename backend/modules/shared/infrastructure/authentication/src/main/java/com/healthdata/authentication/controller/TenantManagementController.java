package com.healthdata.authentication.controller;

import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.dto.TenantDetailResponse;
import com.healthdata.authentication.dto.UpdateTenantRequest;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@ConditionalOnProperty(
    prefix = "authentication.controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class TenantManagementController {

    private final TenantRepository tenantRepository;
    private final UserManagementService userManagementService;

    public TenantManagementController(TenantRepository tenantRepository, UserManagementService userManagementService) {
        this.tenantRepository = tenantRepository;
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<List<TenantDetailResponse>> listTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<TenantDetailResponse> responses = tenants.stream().map(this::toDetailResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<TenantDetailResponse> getTenant(@PathVariable String id) {
        Tenant tenant = tenantRepository.findByIdIgnoreCase(id)
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + id));
        return ResponseEntity.ok(toDetailResponse(tenant));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<TenantDetailResponse> updateTenant(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findByIdIgnoreCase(id)
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + id));
        if (request.getName() != null) tenant.setName(request.getName());
        tenantRepository.save(tenant);
        return ResponseEntity.ok(toDetailResponse(tenant));
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasPermission('TENANT_MANAGE')")
    public ResponseEntity<List<User>> getTenantUsers(@PathVariable String id) {
        return ResponseEntity.ok(userManagementService.getUsersByTenant(id));
    }

    private TenantDetailResponse toDetailResponse(Tenant tenant) {
        return TenantDetailResponse.builder()
            .id(tenant.getId())
            .name(tenant.getName())
            .status(tenant.getStatus().name())
            .userCount(userManagementService.getUsersByTenant(tenant.getId()).size())
            .createdAt(tenant.getCreatedAt())
            .updatedAt(tenant.getUpdatedAt())
            .build();
    }
}
