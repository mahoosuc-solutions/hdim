package com.healthdata.quality.service;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.ClinicalAlertEntity;
import com.healthdata.quality.persistence.ClinicalAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for Alert Escalation Service (Phase 5)
 *
 * Tests automatic alert escalation logic:
 * - Escalate CRITICAL alerts not acknowledged within 15 minutes
 * - Escalate HIGH alerts not acknowledged within 30 minutes
 * - Escalate MEDIUM alerts not acknowledged within 2 hours
 * - Do not escalate LOW severity alerts
 * - Do not re-escalate already escalated alerts
 * - Publish escalation events to Kafka
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Alert Escalation Service Tests")
class AlertEscalationServiceTest {

    @Mock
    private ClinicalAlertRepository alertRepository;

    @Mock
    private AlertRoutingService alertRoutingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AlertEscalationService escalationService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        reset(alertRepository, alertRoutingService, notificationService, kafkaTemplate);
    }

    @Test
    @DisplayName("Should escalate CRITICAL alerts not acknowledged within 15 minutes")
    void shouldEscalateCriticalAlertsAfter15Minutes() {
        // Given: CRITICAL alert triggered 20 minutes ago, not acknowledged
        Instant triggeredAt = Instant.now().minus(20, ChronoUnit.MINUTES);
        ClinicalAlertEntity alert = createAlertEntity(
            "CRITICAL",
            "ACTIVE",
            triggeredAt,
            null,
            false
        );

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(List.of(alert));
        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRoutingService.getEscalationRecipients(eq(TENANT_ID), any(ClinicalAlertDTO.class)))
            .thenReturn(List.of("on-call-provider", "care-team-lead"));

        // When: Processing escalations
        int escalated = escalationService.processEscalations();

        // Then: Alert should be escalated
        assertThat(escalated).isEqualTo(1);
        verify(alertRepository).save(argThat(entity ->
            entity.isEscalated() &&
            entity.getEscalatedAt() != null
        ));
        verify(kafkaTemplate).send(eq("clinical-alert.escalated"), any(Map.class));
        verify(notificationService).sendNotification(any());
    }

    @Test
    @DisplayName("Should escalate HIGH alerts not acknowledged within 30 minutes")
    void shouldEscalateHighAlertsAfter30Minutes() {
        // Given: HIGH alert triggered 35 minutes ago, not acknowledged
        Instant triggeredAt = Instant.now().minus(35, ChronoUnit.MINUTES);
        ClinicalAlertEntity alert = createAlertEntity(
            "HIGH",
            "ACTIVE",
            triggeredAt,
            null,
            false
        );

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(List.of(alert));
        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRoutingService.getEscalationRecipients(eq(TENANT_ID), any(ClinicalAlertDTO.class)))
            .thenReturn(List.of("care-coordinator", "care-team-lead"));

        // When: Processing escalations
        int escalated = escalationService.processEscalations();

        // Then: Alert should be escalated
        assertThat(escalated).isEqualTo(1);
        verify(alertRepository).save(argThat(entity -> entity.isEscalated()));
    }

    @Test
    @DisplayName("Should escalate MEDIUM alerts not acknowledged within 2 hours")
    void shouldEscalateMediumAlertsAfter2Hours() {
        // Given: MEDIUM alert triggered 3 hours ago, not acknowledged
        Instant triggeredAt = Instant.now().minus(3, ChronoUnit.HOURS);
        ClinicalAlertEntity alert = createAlertEntity(
            "MEDIUM",
            "ACTIVE",
            triggeredAt,
            null,
            false
        );

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(List.of(alert));
        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRoutingService.getEscalationRecipients(eq(TENANT_ID), any(ClinicalAlertDTO.class)))
            .thenReturn(List.of("primary-care-provider"));

        // When: Processing escalations
        int escalated = escalationService.processEscalations();

        // Then: Alert should be escalated
        assertThat(escalated).isEqualTo(1);
        verify(alertRepository).save(argThat(entity -> entity.isEscalated()));
    }

    @Test
    @DisplayName("Should NOT escalate alerts that are still within threshold")
    void shouldNotEscalateAlertsWithinThreshold() {
        // Given: CRITICAL alert triggered 10 minutes ago (within 15 min threshold)
        Instant triggeredAt = Instant.now().minus(10, ChronoUnit.MINUTES);
        ClinicalAlertEntity alert = createAlertEntity(
            "CRITICAL",
            "ACTIVE",
            triggeredAt,
            null,
            false
        );

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(List.of(alert));

        // When: Processing escalations
        int escalated = escalationService.processEscalations();

        // Then: No alerts should be escalated
        assertThat(escalated).isEqualTo(0);
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should NOT escalate LOW severity alerts")
    void shouldNotEscalateLowSeverityAlerts() {
        // Given: LOW alert triggered 5 hours ago, not acknowledged
        Instant triggeredAt = Instant.now().minus(5, ChronoUnit.HOURS);
        ClinicalAlertEntity alert = createAlertEntity(
            "LOW",
            "ACTIVE",
            triggeredAt,
            null,
            false
        );

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(List.of(alert));

        // When: Processing escalations
        int escalated = escalationService.processEscalations();

        // Then: No escalation for LOW severity
        assertThat(escalated).isEqualTo(0);
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should NOT re-escalate already escalated alerts")
    void shouldNotReEscalateAlreadyEscalatedAlerts() {
        // Given: CRITICAL alert already escalated
        Instant triggeredAt = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant escalatedAt = Instant.now().minus(10, ChronoUnit.MINUTES);
        ClinicalAlertEntity alert = createAlertEntity(
            "CRITICAL",
            "ACTIVE",
            triggeredAt,
            null,
            true // Already escalated
        );
        alert.setEscalatedAt(escalatedAt);

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(List.of(alert));

        // When: Processing escalations
        int escalated = escalationService.processEscalations();

        // Then: Should not re-escalate
        assertThat(escalated).isEqualTo(0);
        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should NOT escalate acknowledged alerts")
    void shouldNotEscalateAcknowledgedAlerts() {
        // Given: CRITICAL alert that has been acknowledged
        Instant triggeredAt = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant acknowledgedAt = Instant.now().minus(5, ChronoUnit.MINUTES);
        ClinicalAlertEntity alert = createAlertEntity(
            "CRITICAL",
            "ACKNOWLEDGED",
            triggeredAt,
            acknowledgedAt,
            false
        );

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(Collections.emptyList()); // Query should not return acknowledged alerts

        // When: Processing escalations
        int escalated = escalationService.processEscalations();

        // Then: No escalation
        assertThat(escalated).isEqualTo(0);
    }

    @Test
    @DisplayName("Should publish escalation events to Kafka")
    void shouldPublishEscalationEventsToKafka() {
        // Given: CRITICAL alert ready for escalation
        Instant triggeredAt = Instant.now().minus(20, ChronoUnit.MINUTES);
        ClinicalAlertEntity alert = createAlertEntity(
            "CRITICAL",
            "ACTIVE",
            triggeredAt,
            null,
            false
        );

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(List.of(alert));
        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRoutingService.getEscalationRecipients(eq(TENANT_ID), any(ClinicalAlertDTO.class)))
            .thenReturn(List.of("on-call-provider"));

        // When: Processing escalations
        escalationService.processEscalations();

        // Then: Should publish escalation event
        verify(kafkaTemplate).send(eq("clinical-alert.escalated"), argThat(event -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventMap = (Map<String, Object>) event;
            return eventMap.containsKey("alertId") &&
                   eventMap.containsKey("patientId") &&
                   eventMap.containsKey("escalatedAt");
        }));
    }

    @Test
    @DisplayName("Should batch process multiple alerts for escalation")
    void shouldBatchProcessMultipleAlertsForEscalation() {
        // Given: Multiple alerts ready for escalation
        Instant triggeredAt1 = Instant.now().minus(20, ChronoUnit.MINUTES);
        Instant triggeredAt2 = Instant.now().minus(40, ChronoUnit.MINUTES);
        Instant triggeredAt3 = Instant.now().minus(3, ChronoUnit.HOURS);

        ClinicalAlertEntity alert1 = createAlertEntity("CRITICAL", "ACTIVE", triggeredAt1, null, false);
        ClinicalAlertEntity alert2 = createAlertEntity("HIGH", "ACTIVE", triggeredAt2, null, false);
        ClinicalAlertEntity alert3 = createAlertEntity("MEDIUM", "ACTIVE", triggeredAt3, null, false);

        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenReturn(List.of(alert1, alert2, alert3));
        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRoutingService.getEscalationRecipients(anyString(), any(ClinicalAlertDTO.class)))
            .thenReturn(List.of("escalation-team"));

        // When: Processing escalations
        int escalated = escalationService.processEscalations();

        // Then: All 3 alerts should be escalated
        assertThat(escalated).isEqualTo(3);
        verify(alertRepository, times(3)).save(any(ClinicalAlertEntity.class));
        verify(kafkaTemplate, times(3)).send(eq("clinical-alert.escalated"), any());
    }

    @Test
    @DisplayName("Should return zero when escalation processing fails")
    void shouldReturnZeroWhenEscalationProcessingFails() {
        when(alertRepository.findUnacknowledgedAlerts(any(Instant.class)))
            .thenThrow(new RuntimeException("db error"));

        int escalated = escalationService.processEscalations();

        assertThat(escalated).isEqualTo(0);
        verifyNoInteractions(alertRoutingService, notificationService, kafkaTemplate);
    }

    @Test
    @DisplayName("Should publish escalation event even if notification fails")
    void shouldPublishEventEvenIfNotificationFails() {
        ClinicalAlertEntity alert = createAlertEntity(
            "CRITICAL",
            "ACTIVE",
            Instant.now().minus(20, ChronoUnit.MINUTES),
            null,
            false
        );

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(alertRoutingService.getEscalationRecipients(eq(TENANT_ID), any(ClinicalAlertDTO.class)))
            .thenReturn(List.of("on-call-provider"));
        doThrow(new RuntimeException("notify down")).when(notificationService).sendNotification(any());

        escalationService.escalateAlert(alert);

        verify(kafkaTemplate).send(eq("clinical-alert.escalated"), any(Map.class));
    }

    // Helper methods
    private ClinicalAlertEntity createAlertEntity(
        String severity,
        String status,
        Instant triggeredAt,
        Instant acknowledgedAt,
        boolean escalated
    ) {
        ClinicalAlertEntity alert = ClinicalAlertEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .alertType(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)
            .severity(ClinicalAlertEntity.AlertSeverity.valueOf(severity))
            .title("Test Alert")
            .message("Test message")
            .status(ClinicalAlertEntity.AlertStatus.valueOf(status))
            .triggeredAt(triggeredAt)
            .acknowledgedAt(acknowledgedAt)
            .escalated(escalated)
            .build();

        return alert;
    }
}
