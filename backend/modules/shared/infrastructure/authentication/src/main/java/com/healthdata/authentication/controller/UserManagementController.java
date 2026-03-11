package com.healthdata.authentication.controller;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.TempPasswordResponse;
import com.healthdata.authentication.dto.UpdateUserRequest;
import com.healthdata.authentication.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@ConditionalOnProperty(
    prefix = "authentication.controller",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('USER_READ')")
    public ResponseEntity<List<User>> listUsers(
            @RequestParam(required = false) String tenantId) {
        List<User> users = (tenantId != null)
            ? userManagementService.getUsersByTenant(tenantId)
            : userManagementService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('USER_READ')")
    public ResponseEntity<User> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userManagementService.getUser(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<User> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userManagementService.updateUser(id, request));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<User> deactivateUser(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Auth-User-ID", required = false) String actorIdStr) {
        UUID actorId = (actorIdStr != null) ? UUID.fromString(actorIdStr) : null;
        return ResponseEntity.ok(userManagementService.deactivateUser(id, actorId));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<User> reactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userManagementService.reactivateUser(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasPermission('USER_MANAGE_ROLES')")
    public ResponseEntity<User> updateRoles(
            @PathVariable UUID id,
            @RequestBody Set<UserRole> roles) {
        return ResponseEntity.ok(userManagementService.updateRoles(id, roles));
    }

    @PutMapping("/{id}/tenants")
    @PreAuthorize("hasPermission('USER_MANAGE_ROLES')")
    public ResponseEntity<User> updateTenants(
            @PathVariable UUID id,
            @RequestBody Set<String> tenantIds) {
        return ResponseEntity.ok(userManagementService.updateTenants(id, tenantIds));
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<User> unlockAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(userManagementService.unlockAccount(id));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasPermission('USER_WRITE')")
    public ResponseEntity<TempPasswordResponse> resetPassword(@PathVariable UUID id) {
        String tempPassword = userManagementService.resetPassword(id);
        return ResponseEntity.ok(new TempPasswordResponse(tempPassword));
    }
}
