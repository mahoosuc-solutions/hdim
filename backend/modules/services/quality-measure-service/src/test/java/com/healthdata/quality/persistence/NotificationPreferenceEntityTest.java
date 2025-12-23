package com.healthdata.quality.persistence;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPreferenceEntityTest {

    @Test
    void shouldBlockWithoutConsent() {
        NotificationPreferenceEntity entity = NotificationPreferenceEntity.builder()
            .consentGiven(false)
            .build();

        boolean result = entity.shouldReceive(
            NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationType.CLINICAL_ALERT,
            NotificationEntity.NotificationSeverity.HIGH
        );

        assertThat(result).isFalse();
    }

    @Test
    void shouldRespectChannelEnablement() {
        NotificationPreferenceEntity entity = NotificationPreferenceEntity.builder()
            .consentGiven(true)
            .emailEnabled(false)
            .build();

        boolean result = entity.shouldReceive(
            NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationType.CLINICAL_ALERT,
            NotificationEntity.NotificationSeverity.HIGH
        );

        assertThat(result).isFalse();
    }

    @Test
    void shouldRespectSeverityThreshold() {
        NotificationPreferenceEntity entity = NotificationPreferenceEntity.builder()
            .consentGiven(true)
            .severityThreshold(NotificationEntity.NotificationSeverity.HIGH)
            .build();

        boolean result = entity.shouldReceive(
            NotificationEntity.NotificationChannel.PUSH,
            NotificationEntity.NotificationType.CLINICAL_ALERT,
            NotificationEntity.NotificationSeverity.LOW
        );

        assertThat(result).isFalse();
    }

    @Test
    void shouldSuppressDuringQuietHours() {
        LocalTime now = LocalTime.now();
        NotificationPreferenceEntity entity = NotificationPreferenceEntity.builder()
            .consentGiven(true)
            .quietHoursEnabled(true)
            .quietHoursStart(now.minusHours(1))
            .quietHoursEnd(now.plusHours(1))
            .quietHoursOverrideCritical(false)
            .build();

        boolean result = entity.shouldReceive(
            NotificationEntity.NotificationChannel.PUSH,
            NotificationEntity.NotificationType.CLINICAL_ALERT,
            NotificationEntity.NotificationSeverity.MEDIUM
        );

        assertThat(result).isFalse();
    }

    @Test
    void shouldAllowCriticalDuringQuietHoursWithOverride() {
        LocalTime now = LocalTime.now();
        NotificationPreferenceEntity entity = NotificationPreferenceEntity.builder()
            .consentGiven(true)
            .quietHoursEnabled(true)
            .quietHoursStart(now.minusHours(1))
            .quietHoursEnd(now.plusHours(1))
            .quietHoursOverrideCritical(true)
            .build();

        boolean result = entity.shouldReceive(
            NotificationEntity.NotificationChannel.PUSH,
            NotificationEntity.NotificationType.CLINICAL_ALERT,
            NotificationEntity.NotificationSeverity.CRITICAL
        );

        assertThat(result).isTrue();
    }

    @Test
    void shouldFilterByEnabledTypes() {
        NotificationPreferenceEntity entity = NotificationPreferenceEntity.builder()
            .consentGiven(true)
            .enabledTypes(java.util.Set.of("LAB_RESULT"))
            .build();

        boolean result = entity.shouldReceive(
            NotificationEntity.NotificationChannel.WEBSOCKET,
            NotificationEntity.NotificationType.CLINICAL_ALERT,
            NotificationEntity.NotificationSeverity.HIGH
        );

        assertThat(result).isFalse();
    }

    @Test
    void shouldAllowWhenAllCriteriaMet() {
        NotificationPreferenceEntity entity = NotificationPreferenceEntity.builder()
            .consentGiven(true)
            .emailEnabled(true)
            .severityThreshold(NotificationEntity.NotificationSeverity.LOW)
            .build();

        boolean result = entity.shouldReceive(
            NotificationEntity.NotificationChannel.EMAIL,
            NotificationEntity.NotificationType.SYSTEM_NOTIFICATION,
            NotificationEntity.NotificationSeverity.MEDIUM
        );

        assertThat(result).isTrue();
    }
}
