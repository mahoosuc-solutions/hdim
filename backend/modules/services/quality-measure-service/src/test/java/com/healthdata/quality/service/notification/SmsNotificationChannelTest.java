package com.healthdata.quality.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.dto.notification.NotificationRequest;
import com.healthdata.quality.persistence.NotificationHistoryEntity;
import com.healthdata.quality.persistence.NotificationHistoryRepository;
import com.healthdata.quality.service.PatientNameService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("SMS Notification Channel Tests")
class SmsNotificationChannelTest {

    @Test
    @DisplayName("Should send SMS notification request and save history")
    void shouldSendSmsNotificationRequestAndSaveHistory() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenReturn("sms text");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15550001111"); }
            @Override
            public Map<String, Object> getTemplateVariables() {
                return new java.util.HashMap<>(Map.of("key", "value"));
            }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();

        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SENT");
        assertThat(captor.getValue().getRecipientId()).isEqualTo("+15550001111");
    }

    @Test
    @DisplayName("Should record failure when template rendering fails")
    void shouldRecordFailureWhenRenderingFails() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenThrow(new RuntimeException("render fail"));

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of(); }
            @Override
            public Map<String, Object> getTemplateVariables() {
                return new java.util.HashMap<>();
            }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isFalse();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).contains("render fail");
    }

    @Test
    @DisplayName("Should send deprecated alert SMS and save history")
    void shouldSendDeprecatedAlertSmsAndSaveHistory() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("alert sms");
        when(patientNameService.getPatientName(any(UUID.class))).thenReturn("Jane Doe");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-1")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .alertType("RISK_ESCALATION")
            .message("Alert message")
            .triggeredAt(Instant.now())
            .build();

        boolean sent = channel.send("tenant-1", alert);

        assertThat(sent).isTrue();
        verify(historyRepository).save(any(NotificationHistoryEntity.class));
    }

    @Test
    @DisplayName("Should default recipient to fallback phone when none provided")
    void shouldDefaultRecipientWhenMissing() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenReturn("sms text");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return null; }
            @Override
            public Map<String, Object> getTemplateVariables() { return new java.util.HashMap<>(); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo("+12345678900");
    }

    @Test
    @DisplayName("Should include channel in template variables")
    void shouldInjectChannelInTemplateVariables() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenReturn("sms text");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15550001111"); }
            @Override
            public Map<String, Object> getTemplateVariables() {
                return new java.util.HashMap<>(Map.of("key", "value"));
            }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        channel.send(request);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(templateRenderer).render(eq("lab-result"), captor.capture());
        assertThat(captor.getValue()).containsEntry("channel", "SMS");
    }

    @Test
    @DisplayName("Should continue when history save fails")
    void shouldContinueWhenHistorySaveFails() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenReturn("sms text");
        when(historyRepository.save(any(NotificationHistoryEntity.class)))
            .thenThrow(new RuntimeException("save fail"));

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15550001111"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return new java.util.HashMap<>(); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
    }

    @Test
    @DisplayName("Should add SMS channel to template variables")
    void shouldAddSmsChannelToTemplateVariables() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenReturn("sms text");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        Map<String, Object> vars = new HashMap<>();
        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15550002222"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return vars; }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        channel.send(request);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(templateRenderer).render(eq("lab-result"), captor.capture());
        assertThat(captor.getValue()).containsEntry("channel", "SMS");
    }

    @Test
    @DisplayName("Should ignore history save failures")
    void shouldIgnoreHistorySaveFailures() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenReturn("sms text");
        doThrow(new RuntimeException("db down")).when(historyRepository).save(any());

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15550003333"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return new HashMap<>(); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
    }

    @Test
    @DisplayName("Should default alert type formatting and timestamp when missing")
    void shouldDefaultAlertTypeAndTimestampWhenMissing() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("alert sms");
        when(patientNameService.getPatientName(any(UUID.class))).thenReturn("Unknown Patient");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-2")
            .patientId(UUID.randomUUID())
            .severity("HIGH")
            .alertType("UNKNOWN_TYPE")
            .message("Alert message")
            .triggeredAt(null)
            .build();

        channel.send("tenant-1", alert);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(templateRenderer).render(eq("critical-alert"), captor.capture());
        assertThat(captor.getValue()).containsEntry("alertType", "Unknown type"); // Formatted from UNKNOWN_TYPE
        assertThat(captor.getValue().get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("Should not initialize Twilio when credentials missing")
    void shouldNotInitializeTwilioWhenCredentialsMissing() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        ReflectionTestUtils.setField(channel, "twilioEnabled", true);
        ReflectionTestUtils.setField(channel, "accountSid", " ");
        ReflectionTestUtils.setField(channel, "authToken", " ");

        channel.initTwilio();

        boolean initialized = (boolean) ReflectionTestUtils.getField(channel, "twilioInitialized");
        assertThat(initialized).isFalse();
    }

    @Test
    @DisplayName("Should fall back to mock when fromPhone missing")
    void shouldFallbackToMockWhenFromPhoneMissing() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenReturn("sms text");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        ReflectionTestUtils.setField(channel, "twilioInitialized", true);
        ReflectionTestUtils.setField(channel, "fromPhone", " ");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15550004444"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return new HashMap<>(); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
    }

    @Test
    @DisplayName("Should build alert template variables with formatted alert type")
    void shouldBuildAlertVariablesWithFormattedAlertType() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("alert sms");
        when(patientNameService.getPatientName(any(UUID.class))).thenReturn("Jane Doe");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-2")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .alertType("MENTAL_HEALTH_CRISIS")
            .message("Alert message")
            .triggeredAt(null)
            .build();

        channel.send("tenant-1", alert);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(templateRenderer).render(eq("critical-alert"), captor.capture());
        assertThat(captor.getValue()).containsEntry("alertType", "Mental Health Crisis");
        assertThat(captor.getValue()).containsEntry("channel", "SMS");
    }

    @Test
    @DisplayName("Should not initialize Twilio when disabled")
    void shouldNotInitializeTwilioWhenDisabled() {
        SmsNotificationChannel channel = new SmsNotificationChannel(
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        channel.initTwilio();

        Object initialized = ReflectionTestUtils.getField(channel, "twilioInitialized");
        assertThat(initialized).isEqualTo(false);
    }

    @Test
    @DisplayName("Should initialize Twilio when enabled and credentials provided")
    void shouldInitializeTwilioWhenEnabled() {
        SmsNotificationChannel channel = new SmsNotificationChannel(
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        ReflectionTestUtils.setField(channel, "twilioEnabled", true);
        ReflectionTestUtils.setField(channel, "accountSid", "sid");
        ReflectionTestUtils.setField(channel, "authToken", "token");

        channel.initTwilio();

        Object initialized = ReflectionTestUtils.getField(channel, "twilioInitialized");
        assertThat(initialized).isEqualTo(true);
    }

    @Test
    @DisplayName("Should send via Twilio when initialized and fromPhone is set")
    void shouldSendViaTwilioWhenInitialized() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("lab-result"), any())).thenReturn("sms text");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        ReflectionTestUtils.setField(channel, "twilioInitialized", true);
        ReflectionTestUtils.setField(channel, "fromPhone", "+15550000000");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15550007777"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return new HashMap<>(); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        MessageCreator creator = Mockito.mock(MessageCreator.class);
        Message message = Mockito.mock(Message.class);
        when(message.getSid()).thenReturn("sid-123");
        when(creator.create()).thenReturn(message);

        try (MockedStatic<Message> messageStatic = Mockito.mockStatic(Message.class)) {
            messageStatic.when(() ->
                Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), any(String.class)))
                .thenReturn(creator);

            boolean sent = channel.send(request);

            assertThat(sent).isTrue();
            verify(creator).create();
        }
    }

    @Test
    @DisplayName("Should not initialize Twilio when init fails")
    void shouldNotInitializeTwilioWhenInitFails() {
        SmsNotificationChannel channel = new SmsNotificationChannel(
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        ReflectionTestUtils.setField(channel, "twilioEnabled", true);
        ReflectionTestUtils.setField(channel, "accountSid", "sid");
        ReflectionTestUtils.setField(channel, "authToken", "token");

        try (MockedStatic<Twilio> twilioStatic = Mockito.mockStatic(Twilio.class)) {
            twilioStatic.when(() -> Twilio.init("sid", "token"))
                .thenThrow(new RuntimeException("init fail"));

            channel.initTwilio();
        }

        Object initialized = ReflectionTestUtils.getField(channel, "twilioInitialized");
        assertThat(initialized).isEqualTo(false);
    }

    @Test
    @DisplayName("Should record failure when template variables missing")
    void shouldRecordFailureWhenTemplateVariablesMissing() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "LAB_RESULT"; }
            @Override
            public String getTemplateId() { return "lab-result"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Lab Result"; }
            @Override
            public String getMessage() { return "Message"; }
            @Override
            public String getSeverity() { return "MEDIUM"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15550005555"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return null; }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return false; }
            @Override
            public boolean shouldSendSms() { return true; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isFalse();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Should send deprecated alert via Twilio when configured")
    void shouldSendDeprecatedAlertViaTwilioWhenConfigured() {
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("alert sms");
        when(patientNameService.getPatientName(any(UUID.class))).thenReturn("Jane Doe");

        SmsNotificationChannel channel = new SmsNotificationChannel(
            templateRenderer,
            historyRepository,
            patientNameService
        );

        ReflectionTestUtils.setField(channel, "twilioInitialized", true);
        ReflectionTestUtils.setField(channel, "fromPhone", "+15550000000");

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-99")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .alertType("MENTAL_HEALTH_CRISIS")
            .message("Alert message")
            .triggeredAt(Instant.now())
            .build();

        MessageCreator creator = Mockito.mock(MessageCreator.class);
        Message message = Mockito.mock(Message.class);
        when(message.getSid()).thenReturn("sid-999");
        when(creator.create()).thenReturn(message);

        try (MockedStatic<Message> messageStatic = Mockito.mockStatic(Message.class)) {
            messageStatic.when(() ->
                Message.creator(any(PhoneNumber.class), any(PhoneNumber.class), any(String.class)))
                .thenReturn(creator);

            boolean sent = channel.send("tenant-1", alert);

            assertThat(sent).isTrue();
            verify(creator).create();
        }
    }
}
