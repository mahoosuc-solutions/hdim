package com.healthdata.shared.security.repository;

import com.healthdata.shared.security.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Role Repository - Spring Data JPA repository for Role entity
 *
 * Provides:
 * - Basic CRUD operations (inherited from JpaRepository)
 * - Tenant-isolated queries for multi-tenant support
 * - Custom queries for role management
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    // ============ Find by Name ============

    /**
     * Find role by name
     * Role names must be unique within a tenant (or globally for system roles)
     *
     * @param name Role name to find
     * @return Role if found, empty Optional otherwise
     */
    Optional<Role> findByName(String name);

    /**
     * Find role by name within a specific tenant
     *
     * @param name Role name
     * @param tenantId Tenant ID for isolation
     * @return Role if found, empty Optional otherwise
     */
    @Query("SELECT r FROM Role r WHERE r.name = :name AND (r.tenantId = :tenantId OR r.tenantId IS NULL)")
    Optional<Role> findByNameInTenant(@Param("name") String name,
                                      @Param("tenantId") String tenantId);

    /**
     * Check if a role name exists in a tenant
     *
     * @param name Role name to check
     * @param tenantId Tenant ID
     * @return true if role exists
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.name = :name AND r.tenantId = :tenantId")
    boolean existsByNameInTenant(@Param("name") String name,
                                 @Param("tenantId") String tenantId);

    // ============ System Roles ============

    /**
     * Find all system roles (available to all tenants)
     *
     * @return List of system roles
     */
    @Query("SELECT r FROM Role r WHERE r.systemRole = true ORDER BY r.name")
    List<Role> findAllSystemRoles();

    /**
     * Find all active system roles
     *
     * @return List of active system roles
     */
    @Query("SELECT r FROM Role r WHERE r.systemRole = true AND r.active = true ORDER BY r.name")
    List<Role> findActiveSystemRoles();

    /**
     * Check if a system role exists by name
     *
     * @param name Role name to check
     * @return true if system role exists
     */
    @Query("SELECT COUNT(r) > 0 FROM Role r WHERE r.systemRole = true AND r.name = :name")
    boolean existsSystemRole(@Param("name") String name);

    // ============ Tenant-Isolated Queries ============

    /**
     * Find all roles for a specific tenant (including system roles)
     *
     * @param tenantId Tenant ID for isolation
     * @return List of roles available to the tenant
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId OR r.systemRole = true ORDER BY r.name")
    List<Role> findByTenant(@Param("tenantId") String tenantId);

    /**
     * Find all active roles for a specific tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return List of active roles
     */
    @Query("SELECT r FROM Role r WHERE r.active = true AND (r.tenantId = :tenantId OR r.systemRole = true) " +
           "ORDER BY r.name")
    List<Role> findActiveRolesByTenant(@Param("tenantId") String tenantId);

    /**
     * Find all roles for a specific tenant with pagination
     *
     * @param tenantId Tenant ID for isolation
     * @param pageable Pagination parameters
     * @return Page of roles
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId OR r.systemRole = true")
    Page<Role> findByTenant(@Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Find tenant-specific roles (excluding system roles)
     *
     * @param tenantId Tenant ID for isolation
     * @return List of tenant-specific roles
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND r.systemRole = false ORDER BY r.name")
    List<Role> findTenantSpecificRoles(@Param("tenantId") String tenantId);

    // ============ Find Multiple Roles ============

    /**
     * Find multiple roles by their names
     *
     * @param names List of role names to find
     * @return List of found roles
     */
    @Query("SELECT r FROM Role r WHERE r.name IN :names")
    List<Role> findByNames(@Param("names") List<String> names);

    /**
     * Find multiple roles by IDs
     *
     * @param ids Set of role IDs
     * @return List of found roles
     */
    @Query("SELECT r FROM Role r WHERE r.id IN :ids")
    List<Role> findByIds(@Param("ids") Set<String> ids);

    // ============ Search Queries ============

    /**
     * Search roles by name or description in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching roles
     */
    @Query("SELECT r FROM Role r WHERE (r.tenantId = :tenantId OR r.systemRole = true) AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Role> searchRolesByTenant(@Param("tenantId") String tenantId,
                                    @Param("searchTerm") String searchTerm,
                                    Pageable pageable);

    // ============ Permission-Based Queries ============

    /**
     * Find all roles with a specific permission
     *
     * @param permission Permission to search for
     * @return List of roles with the permission
     */
    @Query("SELECT r FROM Role r WHERE r.active = true AND r.permissions LIKE CONCAT('%', :permission, '%')")
    List<Role> findRolesByPermission(@Param("permission") String permission);

    /**
     * Find all active roles in a tenant with a specific permission
     *
     * @param tenantId Tenant ID for isolation
     * @param permission Permission to search for
     * @return List of roles with the permission
     */
    @Query("SELECT r FROM Role r WHERE r.active = true AND " +
           "(r.tenantId = :tenantId OR r.systemRole = true) AND " +
           "r.permissions LIKE CONCAT('%', :permission, '%')")
    List<Role> findRolesByPermissionInTenant(@Param("tenantId") String tenantId,
                                              @Param("permission") String permission);

    // ============ Count Operations ============

    /**
     * Count total roles in a tenant (including system roles)
     *
     * @param tenantId Tenant ID for isolation
     * @return Number of roles
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.tenantId = :tenantId OR r.systemRole = true")
    long countByTenant(@Param("tenantId") String tenantId);

    /**
     * Count active roles in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return Number of active roles
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.active = true AND " +
           "(r.tenantId = :tenantId OR r.systemRole = true)")
    long countActiveByTenant(@Param("tenantId") String tenantId);

    /**
     * Count tenant-specific roles (excluding system roles)
     *
     * @param tenantId Tenant ID for isolation
     * @return Number of tenant-specific roles
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.tenantId = :tenantId AND r.systemRole = false")
    long countTenantSpecificRoles(@Param("tenantId") String tenantId);

    // ============ Delete Operations ============

    /**
     * Find deletable roles in a tenant (non-system roles with no users)
     *
     * @param tenantId Tenant ID for isolation
     * @return List of deletable roles
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND r.systemRole = false AND r.userCount = 0")
    List<Role> findDeletableRolesByTenant(@Param("tenantId") String tenantId);
}
