package com.healthdata.shared.security.service;

import com.healthdata.shared.security.model.Role;
import com.healthdata.shared.security.model.User;
import com.healthdata.shared.security.repository.RoleRepository;
import com.healthdata.shared.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * User Service - Core user management business logic
 *
 * Responsibilities:
 * - User CRUD operations (Create, Read, Update, Delete)
 * - Password encoding and validation with BCrypt
 * - User authentication and authorization
 * - Role management and assignment
 * - Account lock/unlock functionality
 * - Multi-tenant user isolation
 * - Implements Spring Security UserDetailsService
 *
 * Spring Boot 3.3.5 compatible - Uses Jakarta EE (jakarta.*)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ============ User CRUD Operations ============

    /**
     * Create a new user with the specified details
     *
     * @param username Username (must be unique)
     * @param email Email (must be unique)
     * @param password Plain text password (will be encrypted)
     * @param firstName First name
     * @param lastName Last name
     * @param tenantId Tenant ID for multi-tenant isolation
     * @return Created User entity
     * @throws IllegalArgumentException if username or email already exists
     */
    public User createUser(String username, String email, String password,
                          String firstName, String lastName, String tenantId) {
        log.info("Creating new user: {} in tenant: {}", username, tenantId);

        // Validate username uniqueness
        if (userRepository.findByUsernameAndTenant(username, tenantId).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' already exists in tenant");
        }

        // Validate email uniqueness
        if (userRepository.findByEmailAndTenant(email, tenantId).isPresent()) {
            throw new IllegalArgumentException("Email '" + email + "' already exists in tenant");
        }

        // Create user entity
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .tenantId(tenantId)
                .active(true)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("User created successfully: {} (ID: {})", username, user.getId());
        return user;
    }

    /**
     * Find user by ID
     *
     * @param userId User ID to find
     * @return User if found
     * @throws UsernameNotFoundException if user not found
     */
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Find user by username
     *
     * @param username Username to find
     * @return User if found
     * @throws UsernameNotFoundException if user not found
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    /**
     * Find user by username within a specific tenant
     *
     * @param username Username to find
     * @param tenantId Tenant ID for isolation
     * @return User if found
     * @throws UsernameNotFoundException if user not found
     */
    public User getUserByUsernameAndTenant(String username, String tenantId) {
        return userRepository.findByUsernameAndTenant(username, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username + " in tenant: " + tenantId));
    }

    /**
     * Find user by email
     *
     * @param email Email to find
     * @return User if found
     * @throws UsernameNotFoundException if user not found
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    /**
     * Find user by email within a specific tenant
     *
     * @param email Email to find
     * @param tenantId Tenant ID for isolation
     * @return User if found
     * @throws UsernameNotFoundException if user not found
     */
    public User getUserByEmailAndTenant(String email, String tenantId) {
        return userRepository.findByEmailAndTenant(email, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email + " in tenant: " + tenantId));
    }

    /**
     * Update user details
     *
     * @param userId User ID to update
     * @param firstName New first name
     * @param lastName New last name
     * @param email New email
     * @param phoneNumber New phone number
     * @return Updated User entity
     */
    public User updateUser(String userId, String firstName, String lastName,
                          String email, String phoneNumber) {
        log.info("Updating user: {}", userId);

        User user = getUserById(userId);

        // Validate email uniqueness if email is changed
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
                throw new IllegalArgumentException("Email '" + email + "' already exists");
            }
            user.setEmail(email);
        }

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }

        user = userRepository.save(user);
        log.info("User updated successfully: {}", userId);
        return user;
    }

    /**
     * Get all users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @param pageable Pagination parameters
     * @return Page of users in the tenant
     */
    public Page<User> getUsersByTenant(String tenantId, Pageable pageable) {
        return userRepository.findByTenant(tenantId, pageable);
    }

    /**
     * Get all active users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return List of active users
     */
    public List<User> getActiveUsersByTenant(String tenantId) {
        return userRepository.findActiveUsersByTenant(tenantId);
    }

    /**
     * Search users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @param searchTerm Search term (matches name or email)
     * @param pageable Pagination parameters
     * @return Page of matching users
     */
    public Page<User> searchUsers(String tenantId, String searchTerm, Pageable pageable) {
        return userRepository.searchUsersByTenant(tenantId, searchTerm, pageable);
    }

    /**
     * Count total users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return Number of users
     */
    public long countUsersByTenant(String tenantId) {
        return userRepository.countByTenant(tenantId);
    }

    /**
     * Count active users in a tenant
     *
     * @param tenantId Tenant ID for isolation
     * @return Number of active users
     */
    public long countActiveUsersByTenant(String tenantId) {
        return userRepository.countActiveByTenant(tenantId);
    }

    // ============ Password Management ============

    /**
     * Change user's password
     *
     * @param userId User ID
     * @param currentPassword Current password (must be correct)
     * @param newPassword New password to set
     * @throws IllegalArgumentException if current password is incorrect
     */
    public void changePassword(String userId, String currentPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = getUserById(userId);

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Password change failed - incorrect current password for user: {}", userId);
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Reset user's password (admin operation)
     * Requires admin privileges
     *
     * @param userId User ID
     * @param newPassword New password to set
     * @return Updated User entity
     */
    public User resetPassword(String userId, String newPassword) {
        log.warn("Resetting password for user: {} (admin operation)", userId);

        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());

        user = userRepository.save(user);
        log.info("Password reset successfully for user: {}", userId);
        return user;
    }

    /**
     * Check if user's password is correct
     *
     * @param user User entity
     * @param rawPassword Plain text password to check
     * @return true if password matches
     */
    public boolean validatePassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    // ============ Role Management ============

    /**
     * Assign a role to a user
     *
     * @param userId User ID
     * @param roleId Role ID
     * @return Updated User entity
     */
    public User assignRoleToUser(String userId, String roleId) {
        log.info("Assigning role {} to user: {}", roleId, userId);

        User user = getUserById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        user.addRole(role);
        user = userRepository.save(user);

        log.info("Role assigned successfully");
        return user;
    }

    /**
     * Assign multiple roles to a user
     *
     * @param userId User ID
     * @param roleNames List of role names to assign
     * @return Updated User entity
     */
    public User assignRolesToUser(String userId, List<String> roleNames) {
        log.info("Assigning {} roles to user: {}", roleNames.size(), userId);

        User user = getUserById(userId);
        List<Role> roles = roleRepository.findByNames(roleNames);

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("No valid roles found");
        }

        for (Role role : roles) {
            user.addRole(role);
        }

        user = userRepository.save(user);
        log.info("Roles assigned successfully");
        return user;
    }

    /**
     * Remove a role from a user
     *
     * @param userId User ID
     * @param roleId Role ID
     * @return Updated User entity
     */
    public User removeRoleFromUser(String userId, String roleId) {
        log.info("Removing role {} from user: {}", roleId, userId);

        User user = getUserById(userId);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));

        user.removeRole(role);
        user = userRepository.save(user);

        log.info("Role removed successfully");
        return user;
    }

    /**
     * Get all roles assigned to a user
     *
     * @param userId User ID
     * @return Set of roles
     */
    public Set<Role> getUserRoles(String userId) {
        User user = getUserById(userId);
        return user.getRoles();
    }

    /**
     * Get all users with a specific role in a tenant
     *
     * @param tenantId Tenant ID
     * @param roleName Role name
     * @return List of users with the role
     */
    public List<User> getUsersByRole(String tenantId, String roleName) {
        return userRepository.findUsersByTenantAndRole(tenantId, roleName);
    }

    /**
     * Get all users with a specific role in a tenant (paginated)
     *
     * @param tenantId Tenant ID
     * @param roleName Role name
     * @param pageable Pagination parameters
     * @return Page of users with the role
     */
    public Page<User> getUsersByRole(String tenantId, String roleName, Pageable pageable) {
        return userRepository.findUsersByTenantAndRole(tenantId, roleName, pageable);
    }

    // ============ Account Status Management ============

    /**
     * Activate a user account
     *
     * @param userId User ID
     * @return Updated User entity
     */
    public User activateUser(String userId) {
        log.info("Activating user: {}", userId);

        User user = getUserById(userId);
        user.setActive(true);

        user = userRepository.save(user);
        log.info("User activated successfully");
        return user;
    }

    /**
     * Deactivate a user account
     *
     * @param userId User ID
     * @return Updated User entity
     */
    public User deactivateUser(String userId) {
        log.info("Deactivating user: {}", userId);

        User user = getUserById(userId);
        user.setActive(false);

        user = userRepository.save(user);
        log.info("User deactivated successfully");
        return user;
    }

    /**
     * Lock a user account (prevents login)
     *
     * @param userId User ID
     */
    public void lockUser(String userId) {
        log.warn("Locking user account: {}", userId);
        userRepository.lockUser(userId);
    }

    /**
     * Unlock a user account
     *
     * @param userId User ID
     */
    public void unlockUser(String userId) {
        log.info("Unlocking user account: {}", userId);
        userRepository.unlockUser(userId);
    }

    /**
     * Check if user account is locked
     *
     * @param userId User ID
     * @return true if account is locked
     */
    public boolean isUserLocked(String userId) {
        User user = getUserById(userId);
        return user.isLocked();
    }

    /**
     * Soft delete a user (data retained but marked as deleted)
     *
     * @param userId User ID
     */
    public void deleteUser(String userId) {
        log.warn("Deleting user: {} (soft delete)", userId);
        userRepository.softDelete(userId, LocalDateTime.now());
    }

    /**
     * Restore a soft-deleted user
     *
     * @param userId User ID
     */
    public void restoreUser(String userId) {
        log.info("Restoring user: {}", userId);
        userRepository.restore(userId);
    }

    // ============ Login Activity Tracking ============

    /**
     * Record successful user login
     *
     * @param userId User ID
     */
    public void recordLogin(String userId) {
        log.info("Recording login for user: {}", userId);
        User user = getUserById(userId);
        user.recordSuccessfulLogin();
        userRepository.save(user);
    }

    /**
     * Record failed login attempt
     * Automatically locks account after configurable number of failed attempts
     *
     * @param userId User ID
     */
    public void recordFailedLogin(String userId) {
        log.warn("Recording failed login attempt for user: {}", userId);
        User user = getUserById(userId);
        user.recordFailedLogin();
        userRepository.save(user);
    }

    /**
     * Get users who logged in recently
     *
     * @param tenantId Tenant ID
     * @param since Time threshold for recent activity
     * @return List of recently active users
     */
    public List<User> getRecentlyActiveUsers(String tenantId, LocalDateTime since) {
        return userRepository.findRecentlyActiveUsers(tenantId, since);
    }

    /**
     * Get users who have never logged in
     *
     * @param tenantId Tenant ID
     * @return List of users with no login activity
     */
    public List<User> getUsersWithNoLoginActivity(String tenantId) {
        return userRepository.findUsersWithNoLoginActivity(tenantId);
    }

    // ============ Spring Security UserDetailsService Implementation ============

    /**
     * Load user by username for Spring Security authentication
     * Required by UserDetailsService interface
     *
     * @param username Username to load
     * @return UserDetails for authentication
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Validate user can authenticate
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User account is disabled: " + username);
        }

        if (user.isLocked()) {
            throw new UsernameNotFoundException("User account is locked: " + username);
        }

        return user;
    }

    /**
     * Load user by username and tenant ID for multi-tenant support
     *
     * @param username Username to load
     * @param tenantId Tenant ID
     * @return UserDetails for authentication
     * @throws UsernameNotFoundException if user not found
     */
    public UserDetails loadUserByUsernameAndTenant(String username, String tenantId)
            throws UsernameNotFoundException {
        log.debug("Loading user by username and tenant: {}", username);

        User user = userRepository.findByUsernameAndTenant(username, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username + " in tenant: " + tenantId));

        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User account is disabled: " + username);
        }

        if (user.isLocked()) {
            throw new UsernameNotFoundException("User account is locked: " + username);
        }

        return user;
    }

    /**
     * Get user's authorities/permissions
     *
     * @param userId User ID
     * @return List of permission strings
     */
    public List<String> getUserAuthorities(String userId) {
        User user = getUserById(userId);
        return user.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());
    }

    /**
     * Check if user has a specific permission
     *
     * @param userId User ID
     * @param permission Permission to check
     * @return true if user has the permission
     */
    public boolean userHasPermission(String userId, String permission) {
        User user = getUserById(userId);
        return user.hasPermission(permission);
    }
}
