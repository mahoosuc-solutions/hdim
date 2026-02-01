package com.healthdata.shared.security.service;

import com.healthdata.shared.security.model.Role;
import com.healthdata.shared.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Role Service - Role and permission management business logic
 *
 * Responsibilities:
 * - Role CRUD operations
 * - Permission management
 * - Role assignment and validation
 * - Multi-tenant role isolation
 * - System role initialization
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    // ============ System Role Initialization ============

    /**
     * Initialize default system roles
     * Called during application startup
     */
    public void initializeSystemRoles() {
        log.info("Initializing system roles");

        for (Role.RoleType roleType : Role.RoleType.values()) {
            initializeSystemRole(roleType);
        }

        log.info("System roles initialized successfully");
    }

    /**
     * Initialize a specific system role
     *
     * @param roleType Role type to initialize
     */
    public void initializeSystemRole(Role.RoleType roleType) {
        String roleName = roleType.getRoleName();

        // Check if role already exists
        if (roleRepository.existsSystemRole(roleName)) {
            log.debug("System role {} already exists, skipping initialization", roleName);
            return;
        }

        Role role = Role.createSystemRole(roleType);

        // Set default permissions based on role type
        Set<String> permissions = getDefaultPermissionsForRole(roleType);
        if (!permissions.isEmpty()) {
            role.setPermissions(String.join(",", permissions));
        }

        roleRepository.save(role);
        log.info("System role {} initialized", roleName);
    }

    /**
     * Get default permissions for a role type
     *
     * @param roleType Role type
     * @return Set of default permissions
     */
    private Set<String> getDefaultPermissionsForRole(Role.RoleType roleType) {
        Set<String> permissions = new HashSet<>();

        switch (roleType) {
            case ADMIN:
                permissions.addAll(List.of(
                    "user:create", "user:read", "user:update", "user:delete",
                    "role:create", "role:read", "role:update", "role:delete",
                    "tenant:read", "tenant:update",
                    "patient:read", "patient:write",
                    "report:generate", "report:export",
                    "measure:create", "measure:read", "measure:update", "measure:delete",
                    "audit:read"
                ));
                break;

            case PROVIDER:
                permissions.addAll(List.of(
                    "user:read",
                    "patient:read", "patient:write",
                    "report:generate", "report:export",
                    "measure:read",
                    "appointment:read", "appointment:write",
                    "diagnosis:read", "diagnosis:write"
                ));
                break;

            case CARE_MANAGER:
                permissions.addAll(List.of(
                    "patient:read", "patient:write",
                    "report:generate",
                    "measure:read",
                    "appointment:read", "appointment:write",
                    "care-gap:read", "care-gap:update"
                ));
                break;

            case PATIENT:
                permissions.addAll(List.of(
                    "patient:read:own",
                    "report:read:own",
                    "appointment:read:own"
                ));
                break;

            case ANALYST:
                permissions.addAll(List.of(
                    "patient:read",
                    "report:read", "report:generate", "report:export",
                    "measure:read",
                    "audit:read"
                ));
                break;

            case QUALITY_OFFICER:
                permissions.addAll(List.of(
                    "patient:read",
                    "measure:create", "measure:read", "measure:update", "measure:delete",
                    "report:generate", "report:export",
                    "care-gap:read", "care-gap:update",
                    "audit:read"
                ));
                break;

            case QA_ANALYST:
                permissions.addAll(List.of(
                    "patient:read",
                    "measure:read",
                    "report:read", "report:generate",
                    "audit:read", "audit:query",
                    "ai-decision:review",
                    "quality-metric:validate",
                    "care-gap:review"
                ));
                break;

            case MPI_ADMIN:
                permissions.addAll(List.of(
                    "patient:read", "patient:merge", "patient:unmerge",
                    "mpi:read", "mpi:write", "mpi:resolve",
                    "identity:match", "identity:review",
                    "audit:read", "audit:query",
                    "data-quality:review"
                ));
                break;

            case CLINICAL_NURSE:
                permissions.addAll(List.of(
                    "patient:read", "patient:write",
                    "appointment:read", "appointment:write",
                    "diagnosis:read",
                    "care-gap:read", "care-gap:update",
                    "audit:read",
                    "vitals:read", "vitals:write",
                    "medication:read"
                ));
                break;

            case CLINICAL_PHYSICIAN:
                permissions.addAll(List.of(
                    "patient:read", "patient:write",
                    "appointment:read", "appointment:write", "appointment:delete",
                    "diagnosis:read", "diagnosis:write", "diagnosis:delete",
                    "care-gap:read", "care-gap:update", "care-gap:close",
                    "audit:read", "audit:query",
                    "ai-decision:review", "ai-decision:approve", "ai-decision:reject",
                    "report:generate", "report:export",
                    "medication:read", "medication:prescribe",
                    "order:read", "order:write"
                ));
                break;
        }

        return permissions;
    }

    // ============ Role CRUD Operations ============

    /**
     * Create a new role in a tenant
     *
     * @param name Role name (must be unique within tenant)
     * @param description Role description
     * @param tenantId Tenant ID
     * @return Created Role entity
     * @throws IllegalArgumentException if role name already exists
     */
    public Role createRole(String name, String description, String tenantId) {
        log.info("Creating role: {} in tenant: {}", name, tenantId);

        // Check if role already exists
        if (roleRepository.existsByNameInTenant(name, tenantId)) {
            throw new IllegalArgumentException("Role '" + name + "' already exists in tenant");
        }

        Role role = Role.createTenantRole(name, description, tenantId);
        role = roleRepository.save(role);

        log.info("Role created successfully: {} (ID: {})", name, role.getId());
        return role;
    }

    /**
     * Find role by ID
     *
     * @param roleId Role ID
     * @return Role if found
     * @throws IllegalArgumentException if role not found
     */
    public Role getRoleById(String roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));
    }

    /**
     * Find role by name
     *
     * @param name Role name
     * @return Role if found
     * @throws IllegalArgumentException if role not found
     */
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with name: " + name));
    }

    /**
     * Find role by name within a tenant (includes system roles)
     *
     * @param name Role name
     * @param tenantId Tenant ID
     * @return Role if found
     * @throws IllegalArgumentException if role not found
     */
    public Role getRoleByNameInTenant(String name, String tenantId) {
        return roleRepository.findByNameInTenant(name, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role not found with name: " + name + " in tenant: " + tenantId));
    }

    /**
     * Update role details
     *
     * @param roleId Role ID
     * @param description New description
     * @param permissions New permissions (comma-separated)
     * @return Updated Role entity
     */
    public Role updateRole(String roleId, String description, String permissions) {
        log.info("Updating role: {}", roleId);

        Role role = getRoleById(roleId);

        // System roles cannot be modified
        if (role.isSystemRole()) {
            log.warn("Attempted to modify system role: {}", roleId);
            throw new IllegalArgumentException("System roles cannot be modified");
        }

        if (description != null) {
            role.setDescription(description);
        }

        if (permissions != null) {
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        log.info("Role updated successfully");
        return role;
    }

    /**
     * Get all roles in a tenant (includes system roles)
     *
     * @param tenantId Tenant ID
     * @return List of roles
     */
    public List<Role> getRolesByTenant(String tenantId) {
        return roleRepository.findByTenant(tenantId);
    }

    /**
     * Get all active roles in a tenant
     *
     * @param tenantId Tenant ID
     * @return List of active roles
     */
    public List<Role> getActiveRolesByTenant(String tenantId) {
        return roleRepository.findActiveRolesByTenant(tenantId);
    }

    /**
     * Get all roles in a tenant (paginated)
     *
     * @param tenantId Tenant ID
     * @param pageable Pagination parameters
     * @return Page of roles
     */
    public Page<Role> getRolesByTenant(String tenantId, Pageable pageable) {
        return roleRepository.findByTenant(tenantId, pageable);
    }

    /**
     * Get all tenant-specific roles (excluding system roles)
     *
     * @param tenantId Tenant ID
     * @return List of tenant-specific roles
     */
    public List<Role> getTenantSpecificRoles(String tenantId) {
        return roleRepository.findTenantSpecificRoles(tenantId);
    }

    /**
     * Get all system roles
     *
     * @return List of system roles
     */
    public List<Role> getSystemRoles() {
        return roleRepository.findAllSystemRoles();
    }

    /**
     * Search roles in a tenant
     *
     * @param tenantId Tenant ID
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching roles
     */
    public Page<Role> searchRoles(String tenantId, String searchTerm, Pageable pageable) {
        return roleRepository.searchRolesByTenant(tenantId, searchTerm, pageable);
    }

    // ============ Permission Management ============

    /**
     * Add a permission to a role
     *
     * @param roleId Role ID
     * @param permission Permission to add
     * @return Updated Role entity
     */
    public Role addPermissionToRole(String roleId, String permission) {
        log.info("Adding permission {} to role: {}", permission, roleId);

        Role role = getRoleById(roleId);

        // System roles cannot be modified
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot modify system roles");
        }

        role.addPermission(permission);
        role = roleRepository.save(role);

        log.info("Permission added successfully");
        return role;
    }

    /**
     * Remove a permission from a role
     *
     * @param roleId Role ID
     * @param permission Permission to remove
     * @return Updated Role entity
     */
    public Role removePermissionFromRole(String roleId, String permission) {
        log.info("Removing permission {} from role: {}", permission, roleId);

        Role role = getRoleById(roleId);

        // System roles cannot be modified
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("Cannot modify system roles");
        }

        role.removePermission(permission);
        role = roleRepository.save(role);

        log.info("Permission removed successfully");
        return role;
    }

    /**
     * Check if a role has a specific permission
     *
     * @param roleId Role ID
     * @param permission Permission to check
     * @return true if role has permission
     */
    public boolean roleHasPermission(String roleId, String permission) {
        Role role = getRoleById(roleId);
        return role.hasPermission(permission);
    }

    /**
     * Get all roles with a specific permission
     *
     * @param permission Permission to search for
     * @return List of roles with the permission
     */
    public List<Role> getRolesByPermission(String permission) {
        return roleRepository.findRolesByPermission(permission);
    }

    /**
     * Get all active roles in a tenant with a specific permission
     *
     * @param tenantId Tenant ID
     * @param permission Permission to search for
     * @return List of roles with the permission
     */
    public List<Role> getRolesByPermissionInTenant(String tenantId, String permission) {
        return roleRepository.findRolesByPermissionInTenant(tenantId, permission);
    }

    // ============ Role Status Management ============

    /**
     * Activate a role
     *
     * @param roleId Role ID
     * @return Updated Role entity
     */
    public Role activateRole(String roleId) {
        log.info("Activating role: {}", roleId);

        Role role = getRoleById(roleId);
        role.setActive(true);

        role = roleRepository.save(role);
        log.info("Role activated successfully");
        return role;
    }

    /**
     * Deactivate a role
     *
     * @param roleId Role ID
     * @return Updated Role entity
     */
    public Role deactivateRole(String roleId) {
        log.info("Deactivating role: {}", roleId);

        Role role = getRoleById(roleId);

        // System roles cannot be deactivated
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("System roles cannot be deactivated");
        }

        role.setActive(false);
        role = roleRepository.save(role);

        log.info("Role deactivated successfully");
        return role;
    }

    /**
     * Delete a role (only tenant-specific roles with no users)
     *
     * @param roleId Role ID
     */
    public void deleteRole(String roleId) {
        log.warn("Deleting role: {}", roleId);

        Role role = getRoleById(roleId);

        // System roles cannot be deleted
        if (role.isSystemRole()) {
            throw new IllegalArgumentException("System roles cannot be deleted");
        }

        // Check if role has users assigned
        if (role.getUserCount() > 0) {
            throw new IllegalArgumentException("Cannot delete role with assigned users");
        }

        roleRepository.deleteById(roleId);
        log.info("Role deleted successfully");
    }

    // ============ Count Operations ============

    /**
     * Count total roles in a tenant
     *
     * @param tenantId Tenant ID
     * @return Number of roles
     */
    public long countRolesByTenant(String tenantId) {
        return roleRepository.countByTenant(tenantId);
    }

    /**
     * Count active roles in a tenant
     *
     * @param tenantId Tenant ID
     * @return Number of active roles
     */
    public long countActiveRolesByTenant(String tenantId) {
        return roleRepository.countActiveByTenant(tenantId);
    }

    /**
     * Count tenant-specific roles
     *
     * @param tenantId Tenant ID
     * @return Number of tenant-specific roles
     */
    public long countTenantSpecificRoles(String tenantId) {
        return roleRepository.countTenantSpecificRoles(tenantId);
    }

    // ============ Validation ============

    /**
     * Check if a role name is available in a tenant
     *
     * @param name Role name to check
     * @param tenantId Tenant ID
     * @return true if name is available
     */
    public boolean isRoleNameAvailable(String name, String tenantId) {
        return !roleRepository.existsByNameInTenant(name, tenantId);
    }

    /**
     * Validate that a role exists and is active
     *
     * @param roleId Role ID
     * @return true if role exists and is active
     */
    public boolean isRoleActiveAndExists(String roleId) {
        return roleRepository.findById(roleId)
                .map(Role::isActive)
                .orElse(false);
    }
}
