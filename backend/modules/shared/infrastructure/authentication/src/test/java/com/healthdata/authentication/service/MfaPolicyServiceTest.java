package com.healthdata.authentication.service;

import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MfaPolicyService.
 *
 * Tests MFA policy enforcement logic including:
 * - Role-based MFA requirements
 * - Grace period calculations
 * - Login blocking logic
 */
@DisplayName("MfaPolicyService Unit Tests")
class MfaPolicyServiceTest {

    private MfaPolicyService mfaPolicyService;

    @BeforeEach
    void setUp() {
        mfaPolicyService = new MfaPolicyService();
    }

    @Test
    @DisplayName("MFA should be required for ADMIN role")
    void shouldRequireMfaForAdmin() {
        // Given
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, Instant.now());

        // When
        boolean required = mfaPolicyService.isMfaRequired(admin);

        // Then
        assertThat(required).isTrue();
    }

    @Test
    @DisplayName("MFA should be required for SUPER_ADMIN role")
    void shouldRequireMfaForSuperAdmin() {
        // Given
        User superAdmin = createUser(Set.of(UserRole.SUPER_ADMIN), false, null, Instant.now());

        // When
        boolean required = mfaPolicyService.isMfaRequired(superAdmin);

        // Then
        assertThat(required).isTrue();
    }

    @Test
    @DisplayName("MFA should be required for user with both ADMIN and EVALUATOR roles")
    void shouldRequireMfaForAdminWithMultipleRoles() {
        // Given
        User admin = createUser(Set.of(UserRole.ADMIN, UserRole.EVALUATOR), false, null, Instant.now());

        // When
        boolean required = mfaPolicyService.isMfaRequired(admin);

        // Then
        assertThat(required).isTrue();
    }

    @Test
    @DisplayName("MFA should NOT be required for EVALUATOR role")
    void shouldNotRequireMfaForEvaluator() {
        // Given
        User evaluator = createUser(Set.of(UserRole.EVALUATOR), false, null, Instant.now());

        // When
        boolean required = mfaPolicyService.isMfaRequired(evaluator);

        // Then
        assertThat(required).isFalse();
    }

    @Test
    @DisplayName("MFA should NOT be required for ANALYST role")
    void shouldNotRequireMfaForAnalyst() {
        // Given
        User analyst = createUser(Set.of(UserRole.ANALYST), false, null, Instant.now());

        // When
        boolean required = mfaPolicyService.isMfaRequired(analyst);

        // Then
        assertThat(required).isFalse();
    }

    @Test
    @DisplayName("MFA should NOT be required for VIEWER role")
    void shouldNotRequireMfaForViewer() {
        // Given
        User viewer = createUser(Set.of(UserRole.VIEWER), false, null, Instant.now());

        // When
        boolean required = mfaPolicyService.isMfaRequired(viewer);

        // Then
        assertThat(required).isFalse();
    }

    @Test
    @DisplayName("Should NOT block login during grace period (3 days old)")
    void shouldNotBlockDuringGracePeriod() {
        // Given - Admin account created 3 days ago
        Instant createdAt = Instant.now().minus(3, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, createdAt);

        // When
        boolean shouldBlock = mfaPolicyService.shouldBlockLogin(admin);
        long daysRemaining = mfaPolicyService.getGracePeriodRemainingDays(admin);

        // Then
        assertThat(shouldBlock).isFalse();
        assertThat(daysRemaining).isGreaterThanOrEqualTo(3); // At least 3 days remaining (truncates partial days)
        assertThat(daysRemaining).isLessThanOrEqualTo(4); // At most 4 days
    }

    @Test
    @DisplayName("Should NOT block login on last day of grace period (6 days old)")
    void shouldNotBlockOnLastDayOfGracePeriod() {
        // Given - Admin account created 6 days ago
        Instant createdAt = Instant.now().minus(6, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, createdAt);

        // When
        boolean shouldBlock = mfaPolicyService.shouldBlockLogin(admin);
        long daysRemaining = mfaPolicyService.getGracePeriodRemainingDays(admin);

        // Then
        assertThat(shouldBlock).isFalse();
        assertThat(daysRemaining).isGreaterThanOrEqualTo(0); // At least 0 days (truncates partial days)
        assertThat(daysRemaining).isLessThanOrEqualTo(1); // At most 1 day
    }

    @Test
    @DisplayName("Should block login after grace period expires (8 days old)")
    void shouldBlockAfterGracePeriodExpires() {
        // Given - Admin account created 8 days ago
        Instant createdAt = Instant.now().minus(8, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, createdAt);

        // When
        boolean shouldBlock = mfaPolicyService.shouldBlockLogin(admin);
        long daysRemaining = mfaPolicyService.getGracePeriodRemainingDays(admin);

        // Then
        assertThat(shouldBlock).isTrue();
        assertThat(daysRemaining).isEqualTo(0); // Grace period expired
    }

    @Test
    @DisplayName("Should block login well after grace period expires (30 days old)")
    void shouldBlockWellAfterGracePeriodExpires() {
        // Given - Admin account created 30 days ago
        Instant createdAt = Instant.now().minus(30, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, createdAt);

        // When
        boolean shouldBlock = mfaPolicyService.shouldBlockLogin(admin);
        long daysRemaining = mfaPolicyService.getGracePeriodRemainingDays(admin);

        // Then
        assertThat(shouldBlock).isTrue();
        assertThat(daysRemaining).isEqualTo(0); // No negative days
    }

    @Test
    @DisplayName("Should NOT block login if MFA is configured (even after grace period)")
    void shouldNotBlockIfMfaConfigured() {
        // Given - Admin account created 10 days ago WITH MFA enabled
        Instant createdAt = Instant.now().minus(10, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), true, "encrypted-secret", createdAt);

        // When
        boolean shouldBlock = mfaPolicyService.shouldBlockLogin(admin);

        // Then
        assertThat(shouldBlock).isFalse();
    }

    @Test
    @DisplayName("Should NOT block non-admin users regardless of grace period")
    void shouldNotBlockNonAdminUsers() {
        // Given - Evaluator account created 30 days ago without MFA
        Instant createdAt = Instant.now().minus(30, ChronoUnit.DAYS);
        User evaluator = createUser(Set.of(UserRole.EVALUATOR), false, null, createdAt);

        // When
        boolean shouldBlock = mfaPolicyService.shouldBlockLogin(evaluator);

        // Then
        assertThat(shouldBlock).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify user in grace period")
    void shouldIdentifyUserInGracePeriod() {
        // Given - Admin created 3 days ago without MFA
        Instant createdAt = Instant.now().minus(3, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, createdAt);

        // When
        boolean inGracePeriod = mfaPolicyService.isInGracePeriod(admin);

        // Then
        assertThat(inGracePeriod).isTrue();
    }

    @Test
    @DisplayName("Should correctly identify user NOT in grace period (MFA configured)")
    void shouldIdentifyUserNotInGracePeriodWithMfa() {
        // Given - Admin with MFA enabled
        Instant createdAt = Instant.now().minus(3, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), true, "encrypted-secret", createdAt);

        // When
        boolean inGracePeriod = mfaPolicyService.isInGracePeriod(admin);

        // Then
        assertThat(inGracePeriod).isFalse();
    }

    @Test
    @DisplayName("Should correctly identify user NOT in grace period (expired)")
    void shouldIdentifyUserNotInGracePeriodExpired() {
        // Given - Admin created 10 days ago without MFA
        Instant createdAt = Instant.now().minus(10, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, createdAt);

        // When
        boolean inGracePeriod = mfaPolicyService.isInGracePeriod(admin);

        // Then
        assertThat(inGracePeriod).isFalse();
    }

    @Test
    @DisplayName("Should provide correct status message for admin with MFA")
    void shouldProvideCorrectStatusMessageForAdminWithMfa() {
        // Given
        Instant createdAt = Instant.now().minus(3, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), true, "encrypted-secret", createdAt);

        // When
        String message = mfaPolicyService.getMfaStatusMessage(admin);

        // Then
        assertThat(message).isEqualTo("MFA enabled and required for your role");
    }

    @Test
    @DisplayName("Should provide correct status message for admin in grace period")
    void shouldProvideCorrectStatusMessageForAdminInGracePeriod() {
        // Given
        Instant createdAt = Instant.now().minus(3, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, createdAt);

        // When
        String message = mfaPolicyService.getMfaStatusMessage(admin);
        long daysRemaining = mfaPolicyService.getGracePeriodRemainingDays(admin);

        // Then
        assertThat(message).contains("MFA setup required within");
        assertThat(message).contains(daysRemaining + " days"); // Use actual calculated days
    }

    @Test
    @DisplayName("Should provide correct status message for admin after grace period")
    void shouldProvideCorrectStatusMessageForAdminAfterGracePeriod() {
        // Given
        Instant createdAt = Instant.now().minus(10, ChronoUnit.DAYS);
        User admin = createUser(Set.of(UserRole.ADMIN), false, null, createdAt);

        // When
        String message = mfaPolicyService.getMfaStatusMessage(admin);

        // Then
        assertThat(message).contains("MFA setup overdue");
        assertThat(message).contains("contact your administrator");
    }

    @Test
    @DisplayName("Should provide correct status message for non-admin without MFA")
    void shouldProvideCorrectStatusMessageForNonAdminWithoutMfa() {
        // Given
        Instant createdAt = Instant.now();
        User evaluator = createUser(Set.of(UserRole.EVALUATOR), false, null, createdAt);

        // When
        String message = mfaPolicyService.getMfaStatusMessage(evaluator);

        // Then
        assertThat(message).isEqualTo("MFA not required for your role");
    }

    @Test
    @DisplayName("Should provide correct status message for non-admin with MFA")
    void shouldProvideCorrectStatusMessageForNonAdminWithMfa() {
        // Given
        Instant createdAt = Instant.now();
        User evaluator = createUser(Set.of(UserRole.EVALUATOR), true, "encrypted-secret", createdAt);

        // When
        String message = mfaPolicyService.getMfaStatusMessage(evaluator);

        // Then
        assertThat(message).isEqualTo("MFA enabled (optional for your role)");
    }

    // Helper method to create test users
    private User createUser(Set<UserRole> roles, boolean mfaEnabled, String mfaSecret, Instant createdAt) {
        return User.builder()
            .id(UUID.randomUUID())
            .username("testuser")
            .email("test@example.com")
            .roles(roles)
            .mfaEnabled(mfaEnabled)
            .mfaSecret(mfaSecret)
            .createdAt(createdAt)
            .build();
    }
}
