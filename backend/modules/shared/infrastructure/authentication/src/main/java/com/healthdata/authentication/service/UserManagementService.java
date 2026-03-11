package com.healthdata.authentication.service;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.UpdateUserRequest;
import com.healthdata.authentication.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUser(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public List<User> getUsersByTenant(String tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    public List<User> getAllActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    public List<User> getAllUsers() {
        return userRepository.findAllNonDeleted();
    }

    @Transactional
    public User updateUser(UUID id, UpdateUserRequest request) {
        User user = getUser(id);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getNotes() != null) user.setNotes(request.getNotes());

        return userRepository.save(user);
    }

    @Transactional
    public User deactivateUser(UUID id, UUID actorId) {
        User user = getUser(id);
        user.setActive(false);
        user.setDeactivatedAt(Instant.now());
        user.setDeactivatedBy(actorId);
        return userRepository.save(user);
    }

    @Transactional
    public User reactivateUser(UUID id) {
        User user = getUser(id);
        user.setActive(true);
        user.setDeactivatedAt(null);
        user.setDeactivatedBy(null);
        return userRepository.save(user);
    }

    @Transactional
    public User updateRoles(UUID id, Set<UserRole> roles) {
        User user = getUser(id);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    @Transactional
    public User updateTenants(UUID id, Set<String> tenantIds) {
        User user = getUser(id);
        user.setTenantIds(tenantIds);
        return userRepository.save(user);
    }

    @Transactional
    public User unlockAccount(UUID id) {
        User user = getUser(id);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        return userRepository.save(user);
    }

    @Transactional
    public String resetPassword(UUID id) {
        User user = getUser(id);
        String tempPassword = generateTempPassword(12);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setForcePasswordChange(true);
        userRepository.save(user);
        return tempPassword;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    @Transactional
    public void forceChangePassword(UUID userId, String newPassword) {
        User user = getUser(userId);
        if (!Boolean.TRUE.equals(user.getForcePasswordChange())) {
            throw new RuntimeException("Force password change is not required for this user");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    private String generateTempPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
