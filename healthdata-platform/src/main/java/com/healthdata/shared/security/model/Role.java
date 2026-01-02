package com.healthdata.shared.security.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Role Entity - Represents user roles/authorities in the system
 *
 * Supports multi-tenant role management with tenant isolation.
 * Standard roles: ADMIN, PROVIDER, CARE_MANAGER, PATIENT
 * Permissions/authorities are stored as a comma-separated string for flexibility.
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Entity
@Table(name = "roles", indexes = {
    @Index(name = "idx_role_name", columnList = "name"),
    @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"users"})
@EqualsAndHashCode(exclude = {"users"})
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique role identifier (UUID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private String id;

    /**
     * Role name (e.g., ADMIN, PROVIDER, CARE_MANAGER, PATIENT)
     * Must be unique within a tenant
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Human-readable description of the role
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Comma-separated list of permissions/authorities granted to this role
     * Examples: "patient:read", "patient:write", "report:generate", etc.
     */
    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;

    /**
     * Tenant ID for multi-tenant support
     * Null means role is shared across all tenants (system role)
     */
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /**
     * Whether this is a system role (cannot be deleted)
     */
    @Column(name = "is_system_role", nullable = false)
    @Builder.Default
    private boolean systemRole = false;

    /**
     * Whether this role is active
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Number of users assigned to this role (for reference)
     */
    @Column(name = "user_count", nullable = false)
    @Builder.Default
    private int userCount = 0;

    /**
     * Users assigned to this role
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    /**
     * Timestamp when role was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when role was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Pre-defined role enum for standard roles
     */
    public enum RoleType {
        ADMIN("ADMIN", "System Administrator - Full access to all features"),
        PROVIDER("PROVIDER", "Healthcare Provider - Access to patient data and clinical tools"),
        CARE_MANAGER("CARE_MANAGER", "Care Manager - Manages patient care coordination"),
        PATIENT("PATIENT", "Patient - Limited access to own health information"),
        ANALYST("ANALYST", "Data Analyst - Read-only access to reports and analytics"),
        QUALITY_OFFICER("QUALITY_OFFICER", "Quality Officer - Manages quality measures and reports");

        private final String roleName;
        private final String description;

        RoleType(String roleName, String description) {
            this.roleName = roleName;
            this.description = description;
        }

        public String getRoleName() {
            return roleName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Check if this role has a specific permission
     *
     * @param permission Permission to check
     * @return true if role has the permission
     */
    public boolean hasPermission(String permission) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        return permissions.contains(permission);
    }

    /**
     * Add a permission to this role
     *
     * @param permission Permission to add
     */
    public void addPermission(String permission) {
        if (permissions == null || permissions.isEmpty()) {
            permissions = permission;
        } else if (!hasPermission(permission)) {
            permissions = permissions + "," + permission;
        }
    }

    /**
     * Remove a permission from this role
     *
     * @param permission Permission to remove
     */
    public void removePermission(String permission) {
        if (permissions != null && !permissions.isEmpty()) {
            String[] perms = permissions.split(",");
            StringBuilder newPermissions = new StringBuilder();
            for (String p : perms) {
                if (!p.trim().equals(permission)) {
                    if (newPermissions.length() > 0) {
                        newPermissions.append(",");
                    }
                    newPermissions.append(p.trim());
                }
            }
            permissions = newPermissions.toString();
        }
    }

    /**
     * Get all permissions as a Set
     *
     * @return Set of permissions
     */
    public Set<String> getPermissionsAsSet() {
        Set<String> permSet = new HashSet<>();
        if (permissions != null && !permissions.isEmpty()) {
            String[] perms = permissions.split(",");
            for (String p : perms) {
                permSet.add(p.trim());
            }
        }
        return permSet;
    }

    /**
     * Create a default system role
     *
     * @param roleType Role type enum
     * @return Role instance
     */
    public static Role createSystemRole(RoleType roleType) {
        return Role.builder()
                .name(roleType.getRoleName())
                .description(roleType.getDescription())
                .systemRole(true)
                .active(true)
                .tenantId(null) // System role, available to all tenants
                .build();
    }

    /**
     * Create a tenant-specific role
     *
     * @param name Role name
     * @param description Role description
     * @param tenantId Tenant ID
     * @return Role instance
     */
    public static Role createTenantRole(String name, String description, String tenantId) {
        return Role.builder()
                .name(name)
                .description(description)
                .systemRole(false)
                .active(true)
                .tenantId(tenantId)
                .build();
    }
}
