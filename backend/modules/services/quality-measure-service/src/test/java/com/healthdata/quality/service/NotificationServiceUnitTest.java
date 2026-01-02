package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.healthdata.quality.dto.notification.NotificationRequest;
import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.service.notification.EmailNotificationChannel;
import com.healthdata.quality.service.notification.SmsNotificationChannel;
import com.healthdata.quality.websocket.HealthScoreWebSocketHandler;
import com.healthdata.quality.websocket.WebSocketBroadcastService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Notification Service Unit Tests")
class NotificationServiceUnitTest {

    @Test
    @DisplayName("Should send notifications through all configured channels")
    void shouldSendNotificationsThroughAllChannels() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        when(broadcastService.broadcastNotification(Mockito.eq("tenant-1"), Mockito.any()))
            .thenReturn(true);
        when(emailChannel.send(Mockito.any(NotificationRequest.class))).thenReturn(true);
        when(smsChannel.send(Mockito.any(NotificationRequest.class))).thenReturn(true);

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        NotificationRequest request = Mockito.mock(NotificationRequest.class);
        when(request.getNotificationType()).thenReturn("CRITICAL_ALERT");
        when(request.getPatientId()).thenReturn(UUID.randomUUID());
        when(request.getTenantId()).thenReturn("tenant-1");
        when(request.shouldSendWebSocket()).thenReturn(true);
        when(request.shouldSendEmail()).thenReturn(true);
        when(request.shouldSendSms()).thenReturn(true);

        NotificationService.NotificationStatus status = service.sendNotification(request);

        assertThat(status.isAllSuccessful()).isTrue();
        assertThat(status.getChannelStatus())
            .containsEntry("websocket", true)
            .containsEntry("email", true)
            .containsEntry("sms", true);
    }

    @Test
    @DisplayName("Should mark email failure when channel missing")
    void shouldMarkEmailFailureWhenChannelMissing() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        when(broadcastService.broadcastNotification(Mockito.eq("tenant-1"), Mockito.any()))
            .thenReturn(true);

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            null,
            smsChannel
        );

        NotificationRequest request = Mockito.mock(NotificationRequest.class);
        when(request.getNotificationType()).thenReturn("HEALTH_SCORE_UPDATE");
        when(request.getPatientId()).thenReturn(null);
        when(request.getTenantId()).thenReturn("tenant-1");
        when(request.shouldSendWebSocket()).thenReturn(true);
        when(request.shouldSendEmail()).thenReturn(true);
        when(request.shouldSendSms()).thenReturn(false);

        NotificationService.NotificationStatus status = service.sendNotification(request);

        assertThat(status.getChannelStatus()).containsEntry("email", false);
        assertThat(status.isAllSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Should handle channel exceptions and mark failures")
    void shouldHandleChannelExceptions() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        when(broadcastService.broadcastNotification(Mockito.eq("tenant-1"), Mockito.any()))
            .thenThrow(new RuntimeException("websocket down"));
        when(emailChannel.send(Mockito.any(NotificationRequest.class)))
            .thenThrow(new RuntimeException("email down"));
        when(smsChannel.send(Mockito.any(NotificationRequest.class)))
            .thenThrow(new RuntimeException("sms down"));

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        NotificationRequest request = Mockito.mock(NotificationRequest.class);
        when(request.getNotificationType()).thenReturn("LAB_RESULT");
        when(request.getPatientId()).thenReturn(UUID.randomUUID());
        when(request.getTenantId()).thenReturn("tenant-1");
        when(request.getNotificationId()).thenReturn("n-1");
        when(request.shouldSendWebSocket()).thenReturn(true);
        when(request.shouldSendEmail()).thenReturn(true);
        when(request.shouldSendSms()).thenReturn(true);

        NotificationService.NotificationStatus status = service.sendNotification(request);

        assertThat(status.getChannelStatus())
            .containsEntry("websocket", false)
            .containsEntry("email", false)
            .containsEntry("sms", false);
        assertThat(status.isAllSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Should send notification with status based on severity")
    void shouldSendNotificationWithStatus() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        // The deprecated method now delegates to the new API which uses these signatures
        when(broadcastService.broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class))).thenReturn(true);
        when(emailChannel.send(Mockito.any(NotificationRequest.class))).thenReturn(true);
        when(smsChannel.send(Mockito.any(NotificationRequest.class))).thenReturn(true);

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-1")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .build();

        NotificationService.NotificationStatus status =
            service.sendNotificationWithStatus("tenant-1", alert);

        assertThat(status.getChannelStatus())
            .containsEntry("websocket", true)
            .containsEntry("email", true)
            .containsEntry("sms", true);
        assertThat(status.isAllSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Should route legacy alert notifications based on severity")
    void shouldRouteLegacyNotifications() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-2")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .build();

        service.sendNotification("tenant-1", alert);

        // The deprecated method now delegates to the new API
        verify(broadcastService, times(1)).broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class));
        verify(emailChannel, times(1)).send(Mockito.any(NotificationRequest.class));
        verify(smsChannel, times(1)).send(Mockito.any(NotificationRequest.class));
    }

    @Test
    @DisplayName("Should report success when no channels requested")
    void shouldReportSuccessWhenNoChannelsRequested() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        NotificationRequest request = Mockito.mock(NotificationRequest.class);
        when(request.getNotificationType()).thenReturn("DAILY_DIGEST");
        when(request.getPatientId()).thenReturn(UUID.randomUUID());
        when(request.getTenantId()).thenReturn("tenant-1");
        when(request.shouldSendWebSocket()).thenReturn(false);
        when(request.shouldSendEmail()).thenReturn(false);
        when(request.shouldSendSms()).thenReturn(false);

        NotificationService.NotificationStatus status = service.sendNotification(request);

        assertThat(status.getChannelStatus()).isEmpty();
        assertThat(status.isAllSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Should route legacy HIGH severity without SMS")
    void shouldRouteLegacyHighSeverityWithoutSms() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-3")
            .patientId(UUID.randomUUID())
            .severity("HIGH")
            .build();

        service.sendNotification("tenant-1", alert);

        // The deprecated method delegates to new API - HIGH severity gets websocket + email but not SMS
        verify(broadcastService).broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class));
        verify(emailChannel).send(Mockito.any(NotificationRequest.class));
        verify(smsChannel, Mockito.never()).send(Mockito.any(NotificationRequest.class));
    }

    @Test
    @DisplayName("Should skip email when channel is unavailable for status tracking")
    void shouldSkipEmailWhenChannelUnavailableForStatusTracking() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        // The deprecated method delegates to new API which uses broadcastService
        when(broadcastService.broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class))).thenReturn(true);

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            null,  // No email channel
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-4")
            .patientId(UUID.randomUUID())
            .severity("HIGH")  // HIGH = websocket + email (but email unavailable)
            .build();

        NotificationService.NotificationStatus status =
            service.sendNotificationWithStatus("tenant-1", alert);

        assertThat(status.getChannelStatus()).containsEntry("websocket", true);
        // Email should be marked as false because channel is unavailable
        assertThat(status.getChannelStatus()).containsEntry("email", false);
        assertThat(status.isAllSuccessful()).isFalse();  // Not all successful since email failed
    }

    @Test
    @DisplayName("Should mark websocket failure when broadcast throws for status tracking")
    void shouldMarkWebsocketFailureWhenBroadcastThrows() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        // The deprecated method delegates to new API which uses broadcastService
        when(broadcastService.broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class)))
            .thenThrow(new RuntimeException("ws down"));

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-5")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .build();

        NotificationService.NotificationStatus status =
            service.sendNotificationWithStatus("tenant-1", alert);

        assertThat(status.getChannelStatus()).containsEntry("websocket", false);
        assertThat(status.isAllSuccessful()).isFalse();
    }

    @Test
    @DisplayName("Should skip email and SMS for non-critical severity")
    void shouldSkipEmailAndSmsForLowSeverity() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-low")
            .patientId(UUID.randomUUID())
            .severity("LOW")
            .build();

        service.sendNotificationWithStatus("tenant-1", alert);

        // LOW severity: websocket only, no email or SMS per ClinicalAlertNotificationRequest logic
        verify(emailChannel, times(0)).send(Mockito.any(NotificationRequest.class));
        verify(smsChannel, times(0)).send(Mockito.any(NotificationRequest.class));
        verify(broadcastService, times(1)).broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class));
    }

    @Test
    @DisplayName("Should send batch notifications for each alert")
    void shouldSendBatchNotifications() {
        NotificationService service = Mockito.spy(new NotificationService(
            Mockito.mock(HealthScoreWebSocketHandler.class),
            Mockito.mock(WebSocketBroadcastService.class),
            Mockito.mock(EmailNotificationChannel.class),
            Mockito.mock(SmsNotificationChannel.class)
        ));

        ClinicalAlertDTO alert1 = ClinicalAlertDTO.builder()
            .id("alert-1")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .build();
        ClinicalAlertDTO alert2 = ClinicalAlertDTO.builder()
            .id("alert-2")
            .patientId(UUID.randomUUID())
            .severity("HIGH")
            .build();

        Mockito.doNothing().when(service).sendNotification("tenant-1", alert1);
        Mockito.doNothing().when(service).sendNotification("tenant-1", alert2);

        service.sendBatchNotification("tenant-1", java.util.List.of(alert1, alert2));

        verify(service, times(1)).sendNotification("tenant-1", alert1);
        verify(service, times(1)).sendNotification("tenant-1", alert2);
    }

    @Test
    @DisplayName("Should handle outer exceptions during send notification")
    void shouldHandleOuterExceptionsInSendNotification() {
        NotificationService service = new NotificationService(
            Mockito.mock(HealthScoreWebSocketHandler.class),
            Mockito.mock(WebSocketBroadcastService.class),
            Mockito.mock(EmailNotificationChannel.class),
            Mockito.mock(SmsNotificationChannel.class)
        );

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "DAILY_DIGEST"; }
            @Override
            public String getTemplateId() { return "digest"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Digest"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "LOW"; }
            @Override
            public java.time.Instant getTimestamp() { return java.time.Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of(); }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of(); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { throw new RuntimeException("boom"); }
        };

        NotificationService.NotificationStatus status = service.sendNotification(request);

        assertThat(status.getChannelStatus()).isEmpty();
        assertThat(status.isAllSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Should handle deprecated notification channel failures")
    void shouldHandleDeprecatedNotificationChannelFailures() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        // The deprecated method delegates to new API which uses NotificationRequest signatures
        doThrow(new RuntimeException("email down")).when(emailChannel).send(Mockito.any(NotificationRequest.class));
        doThrow(new RuntimeException("sms down")).when(smsChannel).send(Mockito.any(NotificationRequest.class));

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-ex")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .build();

        service.sendNotification("tenant-1", alert);

        // Verify new API methods are called (deprecated method delegates to new API)
        verify(broadcastService).broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class));
        verify(emailChannel).send(Mockito.any(NotificationRequest.class));
        verify(smsChannel).send(Mockito.any(NotificationRequest.class));
    }

    @Test
    @DisplayName("Should handle deprecated notification when websocket fails")
    void shouldHandleDeprecatedNotificationWebSocketFailure() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        // The deprecated method delegates to new API which uses broadcastService
        doThrow(new RuntimeException("ws down")).when(broadcastService)
            .broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class));

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-ws")
            .patientId(UUID.randomUUID())
            .severity("HIGH")
            .build();

        service.sendNotification("tenant-1", alert);

        // Verify broadcastService was called (deprecated method delegates to new API)
        verify(broadcastService).broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class));
    }

    @Test
    @DisplayName("Should mark email and SMS failures in status tracking")
    void shouldMarkEmailAndSmsFailuresInStatusTracking() {
        HealthScoreWebSocketHandler webSocketHandler = Mockito.mock(HealthScoreWebSocketHandler.class);
        WebSocketBroadcastService broadcastService = Mockito.mock(WebSocketBroadcastService.class);
        EmailNotificationChannel emailChannel = Mockito.mock(EmailNotificationChannel.class);
        SmsNotificationChannel smsChannel = Mockito.mock(SmsNotificationChannel.class);

        // The deprecated method delegates to new API which uses NotificationRequest signatures
        when(broadcastService.broadcastNotification(Mockito.eq("tenant-1"), Mockito.any(NotificationRequest.class))).thenReturn(true);
        doThrow(new RuntimeException("email down")).when(emailChannel).send(Mockito.any(NotificationRequest.class));
        doThrow(new RuntimeException("sms down")).when(smsChannel).send(Mockito.any(NotificationRequest.class));

        NotificationService service = new NotificationService(
            webSocketHandler,
            broadcastService,
            emailChannel,
            smsChannel
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-status")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .build();

        NotificationService.NotificationStatus status =
            service.sendNotificationWithStatus("tenant-1", alert);

        assertThat(status.getChannelStatus()).containsEntry("email", false);
        assertThat(status.getChannelStatus()).containsEntry("sms", false);
        assertThat(status.isAllSuccessful()).isFalse();
    }
}
