package com.healthdata.notification.integration;

import com.healthdata.notification.config.BaseIntegrationTest;
import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPreference;
import com.healthdata.notification.domain.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notification Preference Repository Integration Tests
 *
 * Tests database operations for notification preference persistence with real PostgreSQL
 * via Testcontainers. Covers:
 * - Basic CRUD operations
 * - User channel preference management
 * - Quiet hours functionality
 * - Enabled/disabled channel filtering
 * - Multi-tenant data isolation
 */
@BaseIntegrationTest
@DisplayName("NotificationPreferenceRepository Integration Tests")
class NotificationPreferenceRepositoryIntegrationTest {

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final String USER_ID = "user-001";

    private NotificationPreference emailPreference;
    private NotificationPreference smsPreference;
    private NotificationPreference disabledPushPreference;

    @BeforeEach
    void setUp() {
        emailPreference = createPreference(TENANT_ID, USER_ID, NotificationChannel.EMAIL, true);
        emailPreference.setEmail("user@example.com");

        smsPreference = createPreference(TENANT_ID, USER_ID, NotificationChannel.SMS, true);
        smsPreference.setPhone("+1234567890");

        disabledPushPreference = createPreference(TENANT_ID, USER_ID, NotificationChannel.PUSH, false);

        emailPreference = preferenceRepository.save(emailPreference);
        smsPreference = preferenceRepository.save(smsPreference);
        disabledPushPreference = preferenceRepository.save(disabledPushPreference);
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCrudTests {

        @Test
        @DisplayName("Should save and retrieve preference by ID")
        void shouldSaveAndRetrieve() {
            Optional<NotificationPreference> found = preferenceRepository.findById(emailPreference.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(USER_ID);
            assertThat(found.get().getChannel()).isEqualTo(NotificationChannel.EMAIL);
        }

        @Test
        @DisplayName("Should update preference")
        void shouldUpdate() {
            emailPreference.setEnabled(false);
            preferenceRepository.save(emailPreference);

            Optional<NotificationPreference> found = preferenceRepository.findById(emailPreference.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should delete preference")
        void shouldDelete() {
            UUID id = emailPreference.getId();
            preferenceRepository.delete(emailPreference);

            Optional<NotificationPreference> found = preferenceRepository.findById(id);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should auto-generate timestamp on create")
        void shouldAutoGenerateTimestamp() {
            NotificationPreference newPreference = createPreference(TENANT_ID, "user-002",
                    NotificationChannel.IN_APP, true);
            newPreference.setCreatedAt(null);

            NotificationPreference saved = preferenceRepository.save(newPreference);

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("User Preference Queries")
    class UserPreferenceQueryTests {

        @Test
        @DisplayName("Should find preference by tenant, user, and channel")
        void shouldFindByTenantUserAndChannel() {
            Optional<NotificationPreference> found = preferenceRepository.findByTenantIdAndUserIdAndChannel(
                    TENANT_ID, USER_ID, NotificationChannel.EMAIL);

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Should find all user preferences")
        void shouldFindAllUserPreferences() {
            List<NotificationPreference> preferences = preferenceRepository.findByTenantIdAndUserId(
                    TENANT_ID, USER_ID);

            assertThat(preferences).hasSize(3);
            assertThat(preferences).extracting(NotificationPreference::getChannel)
                    .containsExactlyInAnyOrder(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.PUSH);
        }

        @Test
        @DisplayName("Should find only enabled user preferences")
        void shouldFindEnabledUserPreferences() {
            List<NotificationPreference> preferences = preferenceRepository.findByTenantIdAndUserIdAndEnabledTrue(
                    TENANT_ID, USER_ID);

            assertThat(preferences).hasSize(2);
            assertThat(preferences).allMatch(p -> p.getEnabled());
        }

        @Test
        @DisplayName("Should check if preference exists")
        void shouldCheckPreferenceExists() {
            boolean exists = preferenceRepository.existsByTenantIdAndUserIdAndChannel(
                    TENANT_ID, USER_ID, NotificationChannel.EMAIL);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existent preference")
        void shouldReturnFalseForNonExistent() {
            boolean exists = preferenceRepository.existsByTenantIdAndUserIdAndChannel(
                    TENANT_ID, USER_ID, NotificationChannel.IN_APP);

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Quiet Hours Functionality")
    class QuietHoursTests {

        @Test
        @DisplayName("Should store and retrieve quiet hours settings")
        void shouldStoreQuietHours() {
            emailPreference.setQuietHoursEnabled(true);
            emailPreference.setQuietHoursStart(LocalTime.of(22, 0));
            emailPreference.setQuietHoursEnd(LocalTime.of(7, 0));
            emailPreference.setTimezone("America/New_York");
            preferenceRepository.save(emailPreference);

            Optional<NotificationPreference> found = preferenceRepository.findById(emailPreference.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getQuietHoursEnabled()).isTrue();
            assertThat(found.get().getQuietHoursStart()).isEqualTo(LocalTime.of(22, 0));
            assertThat(found.get().getQuietHoursEnd()).isEqualTo(LocalTime.of(7, 0));
            assertThat(found.get().getTimezone()).isEqualTo("America/New_York");
        }

        @Test
        @DisplayName("Should detect when in quiet hours (same day)")
        void shouldDetectQuietHoursSameDay() {
            emailPreference.setQuietHoursEnabled(true);
            emailPreference.setQuietHoursStart(LocalTime.of(9, 0));
            emailPreference.setQuietHoursEnd(LocalTime.of(17, 0));
            preferenceRepository.save(emailPreference);

            Optional<NotificationPreference> found = preferenceRepository.findById(emailPreference.getId());
            assertThat(found).isPresent();

            NotificationPreference pref = found.get();
            assertThat(pref.isInQuietHours(LocalTime.of(12, 0))).isTrue();
            assertThat(pref.isInQuietHours(LocalTime.of(8, 0))).isFalse();
            assertThat(pref.isInQuietHours(LocalTime.of(18, 0))).isFalse();
        }

        @Test
        @DisplayName("Should detect when in quiet hours (spanning midnight)")
        void shouldDetectQuietHoursSpanningMidnight() {
            emailPreference.setQuietHoursEnabled(true);
            emailPreference.setQuietHoursStart(LocalTime.of(22, 0));
            emailPreference.setQuietHoursEnd(LocalTime.of(7, 0));
            preferenceRepository.save(emailPreference);

            Optional<NotificationPreference> found = preferenceRepository.findById(emailPreference.getId());
            assertThat(found).isPresent();

            NotificationPreference pref = found.get();
            assertThat(pref.isInQuietHours(LocalTime.of(23, 0))).isTrue();
            assertThat(pref.isInQuietHours(LocalTime.of(3, 0))).isTrue();
            assertThat(pref.isInQuietHours(LocalTime.of(12, 0))).isFalse();
        }

        @Test
        @DisplayName("Should return false when quiet hours disabled")
        void shouldReturnFalseWhenQuietHoursDisabled() {
            emailPreference.setQuietHoursEnabled(false);
            emailPreference.setQuietHoursStart(LocalTime.of(22, 0));
            emailPreference.setQuietHoursEnd(LocalTime.of(7, 0));
            preferenceRepository.save(emailPreference);

            Optional<NotificationPreference> found = preferenceRepository.findById(emailPreference.getId());
            assertThat(found).isPresent();

            assertThat(found.get().isInQuietHours(LocalTime.of(23, 0))).isFalse();
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperationsTests {

        @Test
        @DisplayName("Should delete all user preferences")
        void shouldDeleteAllUserPreferences() {
            preferenceRepository.deleteByTenantIdAndUserId(TENANT_ID, USER_ID);

            List<NotificationPreference> remaining = preferenceRepository.findByTenantIdAndUserId(TENANT_ID, USER_ID);

            assertThat(remaining).isEmpty();
        }

        @Test
        @DisplayName("Should not delete other tenant's preferences")
        void shouldNotDeleteOtherTenantPreferences() {
            NotificationPreference otherTenantPref = createPreference(OTHER_TENANT, USER_ID,
                    NotificationChannel.EMAIL, true);
            preferenceRepository.save(otherTenantPref);

            preferenceRepository.deleteByTenantIdAndUserId(TENANT_ID, USER_ID);

            List<NotificationPreference> otherTenantPrefs = preferenceRepository.findByTenantIdAndUserId(
                    OTHER_TENANT, USER_ID);
            assertThat(otherTenantPrefs).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation (HIPAA Compliance)")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate preferences between tenants")
        void shouldIsolatePreferencesBetweenTenants() {
            NotificationPreference otherTenantPref = createPreference(OTHER_TENANT, USER_ID,
                    NotificationChannel.EMAIL, true);
            preferenceRepository.save(otherTenantPref);

            List<NotificationPreference> tenant1Prefs = preferenceRepository.findByTenantIdAndUserId(
                    TENANT_ID, USER_ID);
            List<NotificationPreference> tenant2Prefs = preferenceRepository.findByTenantIdAndUserId(
                    OTHER_TENANT, USER_ID);

            assertThat(tenant1Prefs).noneMatch(p -> p.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Prefs).noneMatch(p -> p.getTenantId().equals(TENANT_ID));
        }

        @Test
        @DisplayName("Should allow same user-channel in different tenants")
        void shouldAllowSameUserChannelInDifferentTenants() {
            NotificationPreference otherTenantPref = createPreference(OTHER_TENANT, USER_ID,
                    NotificationChannel.EMAIL, true);
            otherTenantPref.setEmail("different@example.com");
            preferenceRepository.save(otherTenantPref);

            Optional<NotificationPreference> tenant1Pref = preferenceRepository.findByTenantIdAndUserIdAndChannel(
                    TENANT_ID, USER_ID, NotificationChannel.EMAIL);
            Optional<NotificationPreference> tenant2Pref = preferenceRepository.findByTenantIdAndUserIdAndChannel(
                    OTHER_TENANT, USER_ID, NotificationChannel.EMAIL);

            assertThat(tenant1Pref).isPresent();
            assertThat(tenant2Pref).isPresent();
            assertThat(tenant1Pref.get().getEmail()).isNotEqualTo(tenant2Pref.get().getEmail());
        }

        @Test
        @DisplayName("All preferences should have non-null tenant IDs")
        void shouldHaveNonNullTenantIds() {
            List<NotificationPreference> all = preferenceRepository.findAll();

            assertThat(all).allMatch(p -> p.getTenantId() != null && !p.getTenantId().isEmpty());
        }

        @Test
        @DisplayName("Should not expose contact info across tenants")
        void shouldNotExposeContactInfoAcrossTenants() {
            emailPreference.setEmail("sensitive-email@example.com");
            emailPreference.setPhone("+1-555-123-4567");
            preferenceRepository.save(emailPreference);

            List<NotificationPreference> otherTenantPrefs = preferenceRepository.findByTenantIdAndUserId(
                    OTHER_TENANT, USER_ID);

            assertThat(otherTenantPrefs).noneMatch(p ->
                    "sensitive-email@example.com".equals(p.getEmail()));
        }
    }

    // Helper method
    private NotificationPreference createPreference(String tenantId, String userId,
                                                    NotificationChannel channel, boolean enabled) {
        return NotificationPreference.builder()
                .tenantId(tenantId)
                .userId(userId)
                .channel(channel)
                .enabled(enabled)
                .quietHoursEnabled(false)
                .timezone("UTC")
                .build();
    }
}
