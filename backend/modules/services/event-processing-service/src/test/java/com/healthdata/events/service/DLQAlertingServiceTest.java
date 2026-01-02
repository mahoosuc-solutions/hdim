package com.healthdata.events.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.healthdata.events.model.DLQExhaustionAlert;

/**
 * TDD Test Suite for DLQ Alerting Service
 *
 * Tests the alerting functionality when DLQ events exhaust all retry attempts.
 * Follows Test-Driven Development approach.
 */
@ExtendWith(MockitoExtension.class)
class DLQAlertingServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private Logger logger;

    private DLQAlertingService alertingService;

    @BeforeEach
    void setUp() {
        alertingService = new DLQAlertingService(mailSender);
    }

    @Nested
    @DisplayName("Alert Sending Tests")
    class AlertSendingTests {

        @Test
        @DisplayName("Should send email alert when DLQ event is exhausted")
        void shouldSendEmailAlertOnExhaustion() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("PATIENT_REGISTERED", false);

            // When
            alertingService.sendExhaustionAlert(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, times(1)).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertNotNull(sentMessage);
            assertNotNull(sentMessage.getTo());
            assertTrue(sentMessage.getTo().length > 0);
            assertTrue(sentMessage.getSubject().contains("DLQ Event Exhausted"));
            assertTrue(sentMessage.getText().contains(alert.getEventType()));
            assertTrue(sentMessage.getText().contains(alert.getTenantId()));
        }

        @Test
        @DisplayName("Should include patient ID in alert when available")
        void shouldIncludePatientIdInAlert() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("HEALTH_RECORD_UPDATED", false);
            alert.setAffectedPatientId("PATIENT-12345");

            // When
            alertingService.sendExhaustionAlert(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertTrue(sentMessage.getText().contains("PATIENT-12345"));
        }

        @Test
        @DisplayName("Should include error details in alert message")
        void shouldIncludeErrorDetailsInAlert() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("ORDER_PROCESSING", false);
            String errorMessage = "Database connection timeout after 30 seconds";
            alert.setOriginalErrorMessage(errorMessage);

            // When
            alertingService.sendExhaustionAlert(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertTrue(sentMessage.getText().contains(errorMessage));
        }

        @Test
        @DisplayName("Should include retry count and timestamps in alert")
        void shouldIncludeRetryCountAndTimestamps() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("PAYMENT_PROCESSED", false);
            alert.setRetryCount(5);

            // When
            alertingService.sendExhaustionAlert(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertTrue(sentMessage.getText().contains("5"));
            assertTrue(sentMessage.getText().contains("First Failure"));
            assertTrue(sentMessage.getText().contains("Last Failure"));
        }

        @Test
        @DisplayName("Should not throw exception if email sending fails")
        void shouldHandleEmailFailureGracefully() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("TEST_EVENT", false);
            doThrow(new RuntimeException("SMTP server unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

            // When & Then - should not throw exception
            assertDoesNotThrow(() -> alertingService.sendExhaustionAlert(alert));
        }
    }

    @Nested
    @DisplayName("Critical Event Escalation Tests")
    class CriticalEventEscalationTests {

        @Test
        @DisplayName("Should escalate critical events with high priority")
        void shouldEscalateCriticalEvents() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("PATIENT_REGISTERED", true);

            // When
            alertingService.escalateCriticalFailure(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender, times(1)).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertTrue(sentMessage.getSubject().contains("CRITICAL"));
            assertTrue(sentMessage.getSubject().contains("URGENT"));
        }

        @Test
        @DisplayName("Should identify medication events as critical")
        void shouldIdentifyMedicationEventsAsCritical() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("MEDICATION_ADMINISTERED", true);
            alert.setAffectedPatientId("PATIENT-999");

            // When
            alertingService.escalateCriticalFailure(alert);

            // Then
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("Should identify clinical alert events as critical")
        void shouldIdentifyClinicalAlertEventsAsCritical() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("CLINICAL_ALERT_TRIGGERED", true);

            // When
            alertingService.escalateCriticalFailure(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertTrue(sentMessage.getText().contains("IMMEDIATE ACTION REQUIRED"));
        }

        @Test
        @DisplayName("Should send to multiple recipients for critical events")
        void shouldSendToMultipleRecipientsForCritical() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("EMERGENCY_ALERT", true);

            // When
            alertingService.escalateCriticalFailure(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertNotNull(sentMessage.getTo());
            assertTrue(sentMessage.getTo().length >= 2, "Critical alerts should go to multiple recipients");
        }
    }

    @Nested
    @DisplayName("Dashboard Entry Creation Tests")
    class DashboardEntryTests {

        @Test
        @DisplayName("Should create dashboard entry for exhausted event")
        void shouldCreateDashboardEntry() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("ORDER_PROCESSING", false);

            // When
            String dashboardEntry = alertingService.createDashboardEntry(alert);

            // Then
            assertNotNull(dashboardEntry);
            assertTrue(dashboardEntry.contains(alert.getEventType()));
            assertTrue(dashboardEntry.contains(alert.getTenantId()));
            assertTrue(dashboardEntry.contains(alert.getEventId().toString()));
        }

        @Test
        @DisplayName("Should include severity level in dashboard entry")
        void shouldIncludeSeverityInDashboard() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("PATIENT_REGISTERED", true);

            // When
            String dashboardEntry = alertingService.createDashboardEntry(alert);

            // Then
            assertTrue(dashboardEntry.contains("CRITICAL") || dashboardEntry.contains("HIGH"));
        }

        @Test
        @DisplayName("Should include timestamp in dashboard entry")
        void shouldIncludeTimestampInDashboard() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("DATA_SYNC", false);

            // When
            String dashboardEntry = alertingService.createDashboardEntry(alert);

            // Then
            assertTrue(dashboardEntry.contains("timestamp"));
        }

        @Test
        @DisplayName("Should format dashboard entry as JSON")
        void shouldFormatDashboardEntryAsJson() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("REPORT_GENERATION", false);

            // When
            String dashboardEntry = alertingService.createDashboardEntry(alert);

            // Then
            assertTrue(dashboardEntry.startsWith("{"));
            assertTrue(dashboardEntry.endsWith("}"));
            assertTrue(dashboardEntry.contains("\"eventId\""));
            assertTrue(dashboardEntry.contains("\"eventType\""));
        }
    }

    @Nested
    @DisplayName("Alert Content Formatting Tests")
    class AlertContentFormattingTests {

        @Test
        @DisplayName("Should format alert with proper structure")
        void shouldFormatAlertWithProperStructure() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("USER_AUTHENTICATION", false);

            // When
            alertingService.sendExhaustionAlert(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            String text = sentMessage.getText();

            // Verify structure
            assertTrue(text.contains("Event ID:"));
            assertTrue(text.contains("Event Type:"));
            assertTrue(text.contains("Tenant:"));
            assertTrue(text.contains("Error:"));
        }

        @Test
        @DisplayName("Should include DLQ ID for reference")
        void shouldIncludeDlqIdForReference() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("NOTIFICATION_SENT", false);
            UUID dlqId = UUID.randomUUID();
            alert.setDlqId(dlqId);

            // When
            alertingService.sendExhaustionAlert(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertTrue(sentMessage.getText().contains(dlqId.toString()));
        }

        @Test
        @DisplayName("Should include topic information in alert")
        void shouldIncludeTopicInAlert() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("DATA_EXPORT", false);
            alert.setTopic("health.data.export");

            // When
            alertingService.sendExhaustionAlert(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertTrue(sentMessage.getText().contains("health.data.export"));
        }

        @Test
        @DisplayName("Should handle null patient ID gracefully")
        void shouldHandleNullPatientIdGracefully() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("SYSTEM_EVENT", false);
            alert.setAffectedPatientId(null);

            // When
            alertingService.sendExhaustionAlert(alert);

            // Then
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertTrue(sentMessage.getText().contains("N/A") || sentMessage.getText().contains("Not Applicable"));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should send both regular alert and escalation for critical events")
        void shouldSendBothAlertTypesForCritical() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("MEDICATION_ORDER", true);

            // When
            alertingService.sendExhaustionAlert(alert);
            if (alert.isCritical()) {
                alertingService.escalateCriticalFailure(alert);
            }

            // Then - should send 2 emails (one regular, one escalation)
            verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("Should create dashboard entry along with sending alert")
        void shouldCreateDashboardEntryWithAlert() {
            // Given
            DLQExhaustionAlert alert = createTestAlert("PAYMENT_FAILED", false);

            // When
            alertingService.sendExhaustionAlert(alert);
            String dashboardEntry = alertingService.createDashboardEntry(alert);

            // Then
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
            assertNotNull(dashboardEntry);
            assertFalse(dashboardEntry.isEmpty());
        }
    }

    // Helper methods

    private DLQExhaustionAlert createTestAlert(String eventType, boolean critical) {
        Instant now = Instant.now();
        return DLQExhaustionAlert.builder()
            .eventId(UUID.randomUUID())
            .dlqId(UUID.randomUUID())
            .eventType(eventType)
            .tenantId("test-tenant-001")
            .originalErrorMessage("Test error message")
            .retryCount(3)
            .firstFailureTimestamp(now.minus(2, ChronoUnit.HOURS))
            .lastFailureTimestamp(now)
            .affectedPatientId(critical ? "PATIENT-123" : null)
            .topic("test.topic.events")
            .stackTrace("java.lang.RuntimeException: Test exception\n\tat com.test.Service.method()")
            .build();
    }
}
