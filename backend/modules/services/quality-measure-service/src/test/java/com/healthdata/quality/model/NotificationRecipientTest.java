package com.healthdata.quality.model;

import com.healthdata.quality.persistence.NotificationEntity;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRecipientTest {

    @Test
    void supportsChannelReturnsTrueForEnabledChannels() {
        NotificationRecipient recipient = NotificationRecipient.builder()
            .enabledChannels(EnumSet.of(NotificationEntity.NotificationChannel.EMAIL, NotificationEntity.NotificationChannel.SMS))
            .build();

        assertThat(recipient.supportsChannel(NotificationEntity.NotificationChannel.EMAIL)).isTrue();
        assertThat(recipient.supportsChannel(NotificationEntity.NotificationChannel.WEBSOCKET)).isFalse();
    }

    @Test
    void supportsChannelHandlesMissingChannels() {
        NotificationRecipient recipient = new NotificationRecipient();

        assertThat(recipient.supportsChannel(NotificationEntity.NotificationChannel.EMAIL)).isFalse();
    }

    @Test
    void meetsThresholdUsesSeverityLevels() {
        NotificationRecipient recipient = NotificationRecipient.builder()
            .severityThreshold(NotificationEntity.NotificationSeverity.HIGH)
            .build();

        assertThat(recipient.meetsThreshold(NotificationEntity.NotificationSeverity.MEDIUM)).isFalse();
        assertThat(recipient.meetsThreshold(NotificationEntity.NotificationSeverity.HIGH)).isTrue();
        assertThat(recipient.meetsThreshold(NotificationEntity.NotificationSeverity.CRITICAL)).isTrue();
        assertThat(recipient.meetsThreshold(null)).isTrue();
    }

    @Test
    void getContactForChannelUsesChannelDefaults() {
        NotificationRecipient recipient = NotificationRecipient.builder()
            .userId("user-1")
            .emailAddress("user@example.com")
            .phoneNumber("555-0100")
            .build();

        assertThat(recipient.getContactForChannel(NotificationEntity.NotificationChannel.EMAIL)).isEqualTo("user@example.com");
        assertThat(recipient.getContactForChannel(NotificationEntity.NotificationChannel.SMS)).isEqualTo("555-0100");
        assertThat(recipient.getContactForChannel(NotificationEntity.NotificationChannel.WEBSOCKET)).isEqualTo("user-1");
    }
}
