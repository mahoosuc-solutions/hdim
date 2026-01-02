package com.healthdata.authentication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email.
     */
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    /**
     * Check if username exists.
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists.
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users.
     */
    List<User> findByActiveTrue();

    /**
     * Find users by role.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.active = true")
    List<User> findByRole(@Param("role") UserRole role);

    /**
     * Find users by tenant ID.
     */
    @Query("SELECT u FROM User u JOIN u.tenantIds t WHERE t = :tenantId AND u.active = true")
    List<User> findByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find users with any of the specified roles.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r IN :roles AND u.active = true")
    List<User> findByRoles(@Param("roles") Set<UserRole> roles);

    /**
     * Find all non-deleted users.
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllNonDeleted();

    /**
     * Count active users by tenant.
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.tenantIds t WHERE t = :tenantId AND u.active = true")
    Long countActiveUsersByTenantId(@Param("tenantId") String tenantId);
}
