package com.healthdata.shared.security.repository;

import com.healthdata.shared.security.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Repository - Spring Data JPA repository for User entity
 *
 * Provides:
 * - Basic CRUD operations (inherited from JpaRepository)
 * - Tenant-isolated queries for multi-tenant support
 * - Custom queries for specific business logic
 * - Bulk operations for efficiency
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // ============ Find by Username/Email ============

    /**
     * Find user by username (case-sensitive)
     * Username must be unique across the system
     *
     * @param username User's username
     * @return User if found, empty Optional otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by username within a specific tenant
     * Provides tenant isolation for username lookups
     *
     * @param username User's username
     * @param tenantId Tenant ID for isolation
     * @return User if found, empty Optional otherwise
     */
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.tenantId = :tenantId")
    Optional<User> findByUsernameAndTenant(@Param("username") String username,
                                           @Param("tenantId") String tenantId);

    /**
     * Find user by email (case-insensitive)
     * Email must be unique across the system
     *
     * @param email User's email address
     * @return User if found, empty Optional otherwise
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Find user by email within a specific tenant
     * Provides tenant isolation for email lookups
     *
     * @param email User's email address
     * @param tenantId Tenant ID for isolation
     * @return User if found, empty Optional otherwise
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.tenantId = :tenantId")
    Optional<User> findByEmailAndTenant(@Param("email") String email,
                                        @Param("tenantId") String tenantId);

    // ============ Tenant-Isolated Queries ============

    /**
     * Find all active users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return List of active users in the tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.active = true AND u.deletedAt IS NULL")
    List<User> findActiveUsersByTenant(@Param("tenantId") String tenantId);

    /**
     * Find all users in a tenant with pagination
     *
     * @param tenantId Tenant ID for isolation
     * @param pageable Pagination parameters
     * @return Page of users in the tenant
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL")
    Page<User> findByTenant(@Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Find all users in a tenant by role name
     *
     * @param tenantId Tenant ID for isolation
     * @param roleName Role name to filter by
     * @return List of users with the specified role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.tenantId = :tenantId " +
           "AND r.name = :roleName AND u.deletedAt IS NULL")
    List<User> findUsersByTenantAndRole(@Param("tenantId") String tenantId,
                                        @Param("roleName") String roleName);

    /**
     * Find all users in a tenant by role name with pagination
     *
     * @param tenantId Tenant ID for isolation
     * @param roleName Role name to filter by
     * @param pageable Pagination parameters
     * @return Page of users with the specified role
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE u.tenantId = :tenantId " +
           "AND r.name = :roleName AND u.deletedAt IS NULL")
    Page<User> findUsersByTenantAndRole(@Param("tenantId") String tenantId,
                                        @Param("roleName") String roleName,
                                        Pageable pageable);

    /**
     * Find all locked users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return List of locked users
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.locked = true " +
           "AND u.deletedAt IS NULL")
    List<User> findLockedUsersByTenant(@Param("tenantId") String tenantId);

    /**
     * Find all inactive users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return List of inactive users
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.active = false " +
           "AND u.deletedAt IS NULL")
    List<User> findInactiveUsersByTenant(@Param("tenantId") String tenantId);

    // ============ Search Queries ============

    /**
     * Search users by name or email in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @param searchTerm Search term (matched against first name, last name, email)
     * @param pageable Pagination parameters
     * @return Page of matching users
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> searchUsersByTenant(@Param("tenantId") String tenantId,
                                    @Param("searchTerm") String searchTerm,
                                    Pageable pageable);

    // ============ Activity Tracking ============

    /**
     * Find users who logged in within a specific time period
     *
     * @param tenantId Tenant ID for isolation
     * @param since Minimum last login time
     * @return List of active users
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.lastLogin >= :since " +
           "AND u.deletedAt IS NULL ORDER BY u.lastLogin DESC")
    List<User> findRecentlyActiveUsers(@Param("tenantId") String tenantId,
                                       @Param("since") LocalDateTime since);

    /**
     * Find users who have never logged in
     *
     * @param tenantId Tenant ID for isolation
     * @return List of users with no login activity
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.lastLogin IS NULL " +
           "AND u.deletedAt IS NULL")
    List<User> findUsersWithNoLoginActivity(@Param("tenantId") String tenantId);

    /**
     * Find users whose passwords have expired
     *
     * @param tenantId Tenant ID for isolation
     * @return List of users with expired passwords
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.passwordExpiryDays IS NOT NULL " +
           "AND u.passwordExpiryDays > 0 AND u.passwordChangedAt IS NOT NULL AND " +
           "DATE_ADD(u.passwordChangedAt, u.passwordExpiryDays DAY) < NOW() AND u.deletedAt IS NULL")
    List<User> findUsersWithExpiredPasswords(@Param("tenantId") String tenantId);

    /**
     * Find users with email not verified
     *
     * @param tenantId Tenant ID for isolation
     * @return List of users with unverified email
     */
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.emailVerified = false " +
           "AND u.deletedAt IS NULL")
    List<User> findUsersWithUnverifiedEmail(@Param("tenantId") String tenantId);

    // ============ Update Operations ============

    /**
     * Update user's last login timestamp
     *
     * @param userId User ID
     * @param lastLogin Last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") String userId,
                         @Param("lastLogin") LocalDateTime lastLogin);

    /**
     * Update user's password
     *
     * @param userId User ID
     * @param newPassword New password hash
     * @param passwordChangedAt Password change timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.password = :newPassword, u.passwordChangedAt = :passwordChangedAt " +
           "WHERE u.id = :userId")
    void updatePassword(@Param("userId") String userId,
                        @Param("newPassword") String newPassword,
                        @Param("passwordChangedAt") LocalDateTime passwordChangedAt);

    /**
     * Lock a user account
     *
     * @param userId User ID
     */
    @Modifying
    @Query("UPDATE User u SET u.locked = true WHERE u.id = :userId")
    void lockUser(@Param("userId") String userId);

    /**
     * Unlock a user account
     *
     * @param userId User ID
     */
    @Modifying
    @Query("UPDATE User u SET u.locked = false, u.failedLoginAttempts = 0, u.lastFailedLogin = NULL " +
           "WHERE u.id = :userId")
    void unlockUser(@Param("userId") String userId);

    /**
     * Increment failed login attempts
     *
     * @param userId User ID
     * @param failedAttempts Current number of failed attempts
     * @param lastFailedLogin Last failed login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :failedAttempts, " +
           "u.lastFailedLogin = :lastFailedLogin WHERE u.id = :userId")
    void updateFailedLoginAttempts(@Param("userId") String userId,
                                    @Param("failedAttempts") int failedAttempts,
                                    @Param("lastFailedLogin") LocalDateTime lastFailedLogin);

    /**
     * Reset failed login attempts after successful login
     *
     * @param userId User ID
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lastFailedLogin = NULL, u.locked = false " +
           "WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") String userId);

    /**
     * Soft delete a user
     *
     * @param userId User ID
     * @param deletedAt Deletion timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.deletedAt = :deletedAt, u.active = false WHERE u.id = :userId")
    void softDelete(@Param("userId") String userId,
                    @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * Restore a soft-deleted user
     *
     * @param userId User ID
     */
    @Modifying
    @Query("UPDATE User u SET u.deletedAt = NULL, u.active = true WHERE u.id = :userId")
    void restore(@Param("userId") String userId);

    // ============ Count Operations ============

    /**
     * Count total users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return Number of users in tenant
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.deletedAt IS NULL")
    long countByTenant(@Param("tenantId") String tenantId);

    /**
     * Count active users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return Number of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.active = true " +
           "AND u.deletedAt IS NULL")
    long countActiveByTenant(@Param("tenantId") String tenantId);

    /**
     * Check if username exists in a tenant
     *
     * @param username Username to check
     * @param tenantId Tenant ID
     * @return true if username exists
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username AND u.tenantId = :tenantId")
    boolean existsByUsernameInTenant(@Param("username") String username,
                                     @Param("tenantId") String tenantId);

    /**
     * Check if email exists in a tenant
     *
     * @param email Email to check
     * @param tenantId Tenant ID
     * @return true if email exists
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email) " +
           "AND u.tenantId = :tenantId")
    boolean existsByEmailInTenant(@Param("email") String email,
                                   @Param("tenantId") String tenantId);
}
