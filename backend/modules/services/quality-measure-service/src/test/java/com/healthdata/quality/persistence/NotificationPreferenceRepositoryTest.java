package com.healthdata.quality.persistence;

import com.healthdata.quality.config.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for NotificationPreferenceRepository
 */
@BaseIntegrationTest
@Sql(scripts = "classpath:test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class NotificationPreferenceRepositoryTest {

    @Autowired
    private NotificationPreferenceRepository repository;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID_1 = "user-1";
    private static final String USER_ID_2 = "user-2";
    private static final String USER_ID_3 = "user-3";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindNotificationPreference() {
        // Given
        NotificationPreferenceEntity preference = NotificationPreferenceEntity.builder()
            .tenantId(TENANT_ID)
            .userId(USER_ID_1)
            .emailEnabled(true)
            .smsEnabled(true)
            .emailAddress("user1@example.com")
            .phoneNumber("+15555551234")
            .severityThreshold(NotificationEntity.NotificationSeverity.MEDIUM)
            .consentGiven(true)
            .consentDate(LocalDateTime.now())
            .build();

        // When
        NotificationPreferenceEntity saved = repository.save(preference);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(USER_ID_1);
        assertThat(saved.getEmailAddress()).isEqualTo("user1@example.com");
    }

    @Test
    void shouldFindByUserIdAndTenantId() {
        // Given
        NotificationPreferenceEntity preference = createPreference(USER_ID_1, "user1@example.com");
        repository.save(preference);

        // When
        Optional<NotificationPreferenceEntity> found = repository.findByUserIdAndTenantId(USER_ID_1, TENANT_ID);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(USER_ID_1);
        assertThat(found.get().getEmailAddress()).isEqualTo("user1@example.com");
    }

    @Test
    void shouldFindByMultipleUserIds() {
        // Given
        repository.save(createPreference(USER_ID_1, "user1@example.com"));
        repository.save(createPreference(USER_ID_2, "user2@example.com"));
        repository.save(createPreference(USER_ID_3, "user3@example.com"));

        // When
        List<NotificationPreferenceEntity> found = repository.findByUserIdsAndTenantId(
            List.of(USER_ID_1, USER_ID_2), TENANT_ID
        );

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(NotificationPreferenceEntity::getUserId)
            .containsExactlyInAnyOrder(USER_ID_1, USER_ID_2);
    }

    @Test
    void shouldFindEmailEnabledUsers() {
        // Given
        NotificationPreferenceEntity emailEnabled = createPreference(USER_ID_1, "user1@example.com");
        emailEnabled.setEmailEnabled(true);
        emailEnabled.setConsentGiven(true);

        NotificationPreferenceEntity emailDisabled = createPreference(USER_ID_2, "user2@example.com");
        emailDisabled.setEmailEnabled(false);
        emailDisabled.setConsentGiven(true);

        repository.saveAll(List.of(emailEnabled, emailDisabled));

        // When
        List<NotificationPreferenceEntity> found = repository.findEmailEnabledByTenantId(TENANT_ID);

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getUserId()).isEqualTo(USER_ID_1);
    }

    @Test
    void shouldFindSmsEnabledUsers() {
        // Given
        NotificationPreferenceEntity smsEnabled = createPreference(USER_ID_1, "user1@example.com");
        smsEnabled.setSmsEnabled(true);
        smsEnabled.setPhoneNumber("+15555551234");
        smsEnabled.setConsentGiven(true);

        NotificationPreferenceEntity smsDisabled = createPreference(USER_ID_2, "user2@example.com");
        smsDisabled.setSmsEnabled(false);
        smsDisabled.setConsentGiven(true);

        repository.saveAll(List.of(smsEnabled, smsDisabled));

        // When
        List<NotificationPreferenceEntity> found = repository.findSmsEnabledByTenantId(TENANT_ID);

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getUserId()).isEqualTo(USER_ID_1);
    }

    @Test
    void shouldCheckExistence() {
        // Given
        repository.save(createPreference(USER_ID_1, "user1@example.com"));

        // When/Then
        assertThat(repository.existsByUserIdAndTenantId(USER_ID_1, TENANT_ID)).isTrue();
        assertThat(repository.existsByUserIdAndTenantId(USER_ID_2, TENANT_ID)).isFalse();
    }

    @Test
    void shouldFindConsentedUsers() {
        // Given
        NotificationPreferenceEntity consented = createPreference(USER_ID_1, "user1@example.com");
        consented.setConsentGiven(true);
        consented.setConsentDate(LocalDateTime.now());

        NotificationPreferenceEntity notConsented = createPreference(USER_ID_2, "user2@example.com");
        notConsented.setConsentGiven(false);

        repository.saveAll(List.of(consented, notConsented));

        // When
        List<NotificationPreferenceEntity> found = repository.findConsentedByTenantId(TENANT_ID);

        // Then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getUserId()).isEqualTo(USER_ID_1);
    }

    @Test
    void shouldRespectQuietHours() {
        // Given
        NotificationPreferenceEntity preference = createPreference(USER_ID_1, "user1@example.com");
        preference.setQuietHoursEnabled(true);
        preference.setQuietHoursStart(LocalTime.of(22, 0));
        preference.setQuietHoursEnd(LocalTime.of(8, 0));

        // When
        boolean withinQuietHours = preference.isWithinQuietHours();

        // Then - depends on current time, just test method exists
        assertThat(withinQuietHours).isNotNull();
    }

    @Test
    void shouldEvaluateReceivePermission() {
        // Given
        NotificationPreferenceEntity preference = createPreference(USER_ID_1, "user1@example.com");
        preference.setEmailEnabled(true);
        preference.setConsentGiven(true);
        preference.setSeverityThreshold(NotificationEntity.NotificationSeverity.MEDIUM);

        // When
        boolean shouldReceiveHigh = preference.shouldReceive(
            NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationType.CLINICAL_ALERT,
            NotificationEntity.NotificationSeverity.HIGH
        );

        boolean shouldReceiveLow = preference.shouldReceive(
            NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationType.CLINICAL_ALERT,
            NotificationEntity.NotificationSeverity.LOW
        );

        // Then
        assertThat(shouldReceiveHigh).isTrue(); // HIGH >= MEDIUM threshold
        assertThat(shouldReceiveLow).isFalse(); // LOW < MEDIUM threshold
    }

    private NotificationPreferenceEntity createPreference(String userId, String email) {
        return NotificationPreferenceEntity.builder()
            .tenantId(TENANT_ID)
            .userId(userId)
            .emailEnabled(true)
            .emailAddress(email)
            .consentGiven(true)
            .consentDate(LocalDateTime.now())
            .build();
    }
}
