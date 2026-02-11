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
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Email Notification Channel Tests")
class EmailNotificationChannelTest {

    @Test
    @DisplayName("Should send notification request and save history")
    void shouldSendNotificationRequestAndSaveHistory() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("EMAIL", "doc@test.com"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of("patientName", "Test"); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of("key", "value"); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
            @Override
            public String getRelatedEntityId() { return "alert-1"; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
        verify(mailSender).send(any(MimeMessage.class));

        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SENT");
        assertThat(captor.getValue().getRecipientId()).isEqualTo("doc@test.com");
    }

    @Test
    @DisplayName("Should save failed history when rendering throws")
    void shouldSaveFailedHistoryWhenRenderThrows() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any()))
            .thenThrow(new RuntimeException("render failed"));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return null; }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of(); }
            @Override
            public Map<String, Object> getMetadata() { return null; }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isFalse();

        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).contains("render failed");
    }

    @Test
    @DisplayName("Should send deprecated alert and save history")
    void shouldSendDeprecatedAlertAndSaveHistory() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(patientNameService.getPatientName(any(UUID.class))).thenReturn("Test Patient");

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-1")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .alertType("RISK_ESCALATION")
            .title("Suicide Risk Alert")
            .message("Immediate intervention required")
            .triggeredAt(Instant.now())
            .build();

        boolean sent = channel.send("tenant-1", alert);

        assertThat(sent).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
        verify(historyRepository).save(any(NotificationHistoryEntity.class));
    }

    @Test
    @DisplayName("Should save failed history when deprecated send throws")
    void shouldSaveFailedHistoryWhenDeprecatedSendThrows() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any()))
            .thenThrow(new RuntimeException("render failed"));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-30")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .alertType("CUSTOM_ALERT")
            .title("Critical Alert")
            .message("Alert message")
            .build();

        boolean sent = channel.send("tenant-1", alert);

        assertThat(sent).isFalse();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).contains("render failed");
    }

    @Test
    @DisplayName("Should use default recipient when missing")
    void shouldUseDefaultRecipientWhenMissing() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return null; }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of("patientName", "Test"); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of("key", "value"); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo("default@test.com");
    }

    @Test
    @DisplayName("Should use default recipient when recipients map is empty")
    void shouldUseDefaultRecipientWhenRecipientsEmpty() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of(); }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of("patientName", "Test"); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo("default@test.com");
    }

    @Test
    @DisplayName("Should format subjects and guidance for critical alerts")
    void shouldFormatSubjectsAndGuidanceForCriticalAlerts() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-10")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .alertType("CUSTOM_ALERT")
            .title("Critical Alert")
            .message("Immediate attention")
            .build();

        String subject = ReflectionTestUtils.invokeMethod(channel, "formatSubject", alert);
        assertThat(subject).contains("[URGENT]");

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
            channel, "buildTemplateVariables", alert);

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) variables.get("recommendedActions");
        assertThat(actions).anyMatch(action -> action.contains("Contact patient within 24 hours"));
        // The alertType is formatted for display in the template
        assertThat(variables.get("alertType")).isEqualTo("Custom alert");
    }
    @Test
    @DisplayName("Should capture suicide risk guidance for critical alerts")
    void shouldCaptureSuicideRiskGuidance() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(patientNameService.getPatientName(any(UUID.class))).thenReturn("Test Patient");

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-2")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .alertType("MENTAL_HEALTH_CRISIS")
            .title("Suicide Risk Alert")
            .message("Immediate intervention required")
            .escalated(true)
            .triggeredAt(null)
            .build();

        channel.send("tenant-1", alert);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(templateRenderer).render(eq("critical-alert"), captor.capture());
        Map<String, Object> variables = captor.getValue();
        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) variables.get("recommendedActions");
        assertThat(actions.get(0)).contains("Contact patient IMMEDIATELY");
    }

    @Test
    @DisplayName("Should record failure when mail sender throws")
    void shouldRecordFailureWhenMailSenderThrows() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return null; }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of("patientName", "Test"); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isFalse();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).contains("smtp down");
    }

    @Test
    @DisplayName("Should record failure when mime message creation throws")
    void shouldRecordFailureWhenMimeMessageCreationThrows() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        doThrow(new RuntimeException("mime fail")).when(mailSender).createMimeMessage();

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("EMAIL", "doc@test.com"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of("patientName", "Test"); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isFalse();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).contains("mime fail");
    }

    @Test
    @DisplayName("Should prefix HIGH severity subjects")
    void shouldPrefixHighSeveritySubject() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(patientNameService.getPatientName(any(UUID.class))).thenReturn("Test Patient");

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-5")
            .patientId(UUID.randomUUID())
            .severity("HIGH")
            .alertType("RISK_ESCALATION")
            .title("Risk Escalation")
            .message("High risk escalation")
            .triggeredAt(Instant.now())
            .build();

        channel.send("tenant-1", alert);

        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).contains("[HIGH PRIORITY]");
    }

    @Test
    @DisplayName("Should use default recipient when EMAIL key missing")
    void shouldUseDefaultRecipientWhenEmailKeyMissing() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("SMS", "+15551234567"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of("patientName", "Test"); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of("key", "value"); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
        ArgumentCaptor<NotificationHistoryEntity> captor =
            ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo("default@test.com");
    }

    @Test
    @DisplayName("Should swallow history save errors")
    void shouldSwallowHistorySaveErrors() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new RuntimeException("db down")).when(historyRepository).save(any(NotificationHistoryEntity.class));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("EMAIL", "doc@test.com"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return Map.of("patientName", "Test"); }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
    }

    @Test
    @DisplayName("Should format known alert types")
    void shouldFormatKnownAlertTypes() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-20")
            .patientId(UUID.randomUUID())
            .severity("MEDIUM")
            .alertType("MENTAL_HEALTH_CRISIS")
            .title("Crisis")
            .message("Alert")
            .build();

        String formatted = ReflectionTestUtils.invokeMethod(channel, "formatAlertType", alert.getAlertType());
        assertThat(formatted).isEqualTo("Mental Health Crisis");
    }

    @Test
    @DisplayName("Should format additional alert types")
    void shouldFormatAdditionalAlertTypes() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        String healthDecline = ReflectionTestUtils.invokeMethod(
            channel, "formatAlertType", "HEALTH_DECLINE");
        String chronic = ReflectionTestUtils.invokeMethod(
            channel, "formatAlertType", "CHRONIC_DETERIORATION");

        assertThat(healthDecline).isEqualTo("Health Score Decline");
        assertThat(chronic).isEqualTo("Chronic Disease Deterioration");
    }

    @Test
    @DisplayName("Should format risk escalation alert type")
    void shouldFormatRiskEscalationAlertType() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        String formatted = ReflectionTestUtils.invokeMethod(
            channel, "formatAlertType", "RISK_ESCALATION");

        assertThat(formatted).isEqualTo("Risk Escalation");
    }

    @Test
    @DisplayName("Should mark escalated alerts in template details")
    void shouldMarkEscalatedAlertsInTemplateDetails() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-99")
            .patientId(UUID.randomUUID())
            .severity("CRITICAL")
            .alertType("MENTAL_HEALTH_CRISIS")
            .title("Suicide Risk")
            .message("Immediate intervention required")
            .escalated(true)
            .triggeredAt(null)
            .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
            channel, "buildTemplateVariables", alert);

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) variables.get("details");

        assertThat(details.get("Escalated")).contains("IMMEDIATE ATTENTION");
    }

    @Test
    @DisplayName("Should handle null template variables")
    void shouldHandleNullTemplateVariables() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        TemplateRenderer templateRenderer = Mockito.mock(TemplateRenderer.class);
        NotificationHistoryRepository historyRepository = Mockito.mock(NotificationHistoryRepository.class);
        PatientNameService patientNameService = Mockito.mock(PatientNameService.class);

        when(templateRenderer.render(eq("critical-alert"), eq(null))).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailNotificationChannel channel = new EmailNotificationChannel(
            mailSender,
            templateRenderer,
            historyRepository,
            patientNameService
        );
        ReflectionTestUtils.setField(channel, "defaultRecipient", "default@test.com");

        NotificationRequest request = new NotificationRequest() {
            @Override
            public String getNotificationType() { return "CRITICAL_ALERT"; }
            @Override
            public String getTemplateId() { return "critical-alert"; }
            @Override
            public String getTenantId() { return "tenant-1"; }
            @Override
            public UUID getPatientId() { return UUID.randomUUID(); }
            @Override
            public String getTitle() { return "Critical Alert"; }
            @Override
            public String getMessage() { return "Alert message"; }
            @Override
            public String getSeverity() { return "CRITICAL"; }
            @Override
            public Instant getTimestamp() { return Instant.now(); }
            @Override
            public Map<String, String> getRecipients() { return Map.of("EMAIL", "doc@test.com"); }
            @Override
            public Map<String, Object> getTemplateVariables() { return null; }
            @Override
            public Map<String, Object> getMetadata() { return Map.of(); }
            @Override
            public boolean shouldSendEmail() { return true; }
            @Override
            public boolean shouldSendSms() { return false; }
            @Override
            public boolean shouldSendWebSocket() { return false; }
        };

        boolean sent = channel.send(request);

        assertThat(sent).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }
    @Test
    @DisplayName("Should include timestamp and alert details for non-escalated alerts")
    void shouldIncludeTimestampAndDetailsForNonEscalatedAlerts() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        Instant triggeredAt = Instant.parse("2024-05-01T15:30:00Z");
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-100")
            .patientId(UUID.randomUUID())
            .severity("HIGH")
            .alertType("RISK_ESCALATION")
            .title("Risk Escalation")
            .message("Review needed")
            .escalated(false)
            .triggeredAt(triggeredAt)
            .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
            channel, "buildTemplateVariables", alert);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String expectedTimestamp = LocalDateTime.ofInstant(triggeredAt, ZoneId.systemDefault())
            .format(formatter);

        assertThat(variables.get("timestamp")).isEqualTo(expectedTimestamp);

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) variables.get("details");
        assertThat(details.get("Alert ID")).isEqualTo("alert-100");
        assertThat(details.get("Escalated")).isEqualTo("No");
    }

    @Test
    @DisplayName("Should return action guidance for high severity")
    void shouldReturnActionGuidanceForHighSeverity() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-21")
            .patientId(UUID.randomUUID())
            .severity("HIGH")
            .alertType("HEALTH_DECLINE")
            .title("Decline")
            .message("Alert")
            .build();

        String guidance = ReflectionTestUtils.invokeMethod(channel, "getActionGuidance", alert);

        assertThat(guidance).contains("Review alert details");
    }

    @Test
    @DisplayName("Should return default action guidance for low severity")
    void shouldReturnDefaultActionGuidanceForLowSeverity() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-22")
            .patientId(UUID.randomUUID())
            .severity("LOW")
            .alertType("RISK_ESCALATION")
            .title("Risk")
            .message("Alert")
            .build();

        String guidance = ReflectionTestUtils.invokeMethod(channel, "getActionGuidance", alert);

        assertThat(guidance).contains("Review alert during next scheduled patient contact");
    }

    @Test
    @DisplayName("Should leave subject unprefixed for non-critical alerts")
    void shouldLeaveSubjectUnprefixedForNonCriticalAlerts() {
        EmailNotificationChannel channel = new EmailNotificationChannel(
            Mockito.mock(JavaMailSender.class),
            Mockito.mock(TemplateRenderer.class),
            Mockito.mock(NotificationHistoryRepository.class),
            Mockito.mock(PatientNameService.class)
        );

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-23")
            .patientId(UUID.randomUUID())
            .severity("LOW")
            .alertType("CUSTOM_ALERT")
            .title("Routine Alert")
            .message("Alert")
            .build();

        String subject = ReflectionTestUtils.invokeMethod(channel, "formatSubject", alert);

        assertThat(subject).isEqualTo("Routine Alert");
    }
}
