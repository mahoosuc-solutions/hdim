package com.healthdata.quality.service;

import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.*;
import com.healthdata.quality.service.notification.ClinicalAlertNotificationTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * TDD Tests for Clinical Alert Service
 *
 * Tests mental health crisis detection, risk escalation alerts,
 * health score decline alerts, and alert deduplication logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Clinical Alert Service Tests")
class ClinicalAlertServiceTest {

    @Mock
    private ClinicalAlertRepository alertRepository;

    @Mock
    private AlertEvaluationService alertEvaluationService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ClinicalAlertNotificationTrigger notificationTrigger;

    @InjectMocks
    private ClinicalAlertService clinicalAlertService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    private static final String PROVIDER_ID = "provider-001";

    @BeforeEach
    void setUp() {
        // Setup common test state
        // Configure notification trigger to not throw exceptions (it's called but errors are caught)
        // Use lenient() to avoid UnnecessaryStubbingException for tests that don't trigger notifications
        lenient().doNothing().when(notificationTrigger).onAlertTriggered(anyString(), any(ClinicalAlertDTO.class));
        lenient().doNothing().when(notificationTrigger).onAlertAcknowledged(anyString(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should create CRITICAL alert for PHQ-9 score >= 20 (severe depression)")
    void shouldCreateCriticalAlertForSevereDepression() {
        // Given: PHQ-9 assessment with score >= 20
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessment(
            "PHQ-9", 22, "severe"
        );

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Processing mental health assessment
        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        // Then: CRITICAL alert should be created
        assertThat(alert).isNotNull();
        assertThat(alert.getAlertType()).isEqualTo("MENTAL_HEALTH_CRISIS");
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
        assertThat(alert.getTitle()).contains("Severe Depression Detected");
        assertThat(alert.getMessage()).contains("PHQ-9 score: 22/27");

        // Verify alert was saved and event published
        verify(alertRepository).save(any(ClinicalAlertEntity.class));
        verify(kafkaTemplate).send(eq("clinical-alert.triggered"), any());
    }

    @Test
    @DisplayName("Should create CRITICAL alert for suicide risk (PHQ-9 item 9 > 0)")
    void shouldCreateCriticalAlertForSuicideRisk() {
        // Given: PHQ-9 with item 9 (suicidal ideation) > 0
        Map<String, Integer> responses = new HashMap<>();
        responses.put("item_9", 2); // Positive suicide risk indicator

        MentalHealthAssessmentEntity assessment = createMentalHealthAssessmentWithResponses(
            "PHQ-9", 15, "moderately-severe", responses
        );

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Evaluating assessment
        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        // Then: CRITICAL suicide risk alert created
        assertThat(alert).isNotNull();
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
        assertThat(alert.getTitle()).contains("Suicide Risk");
        assertThat(alert.getMessage()).contains("suicidal ideation");
        assertThat(alert.isEscalated()).isTrue();

        verify(alertRepository).save(argThat(entity ->
            entity.getSeverity() == ClinicalAlertEntity.AlertSeverity.CRITICAL &&
            entity.isEscalated()
        ));
    }

    @Test
    @DisplayName("Should create HIGH alert for severe anxiety (GAD-7 >= 15)")
    void shouldCreateHighAlertForSevereAnxiety() {
        // Given: GAD-7 assessment with score >= 15
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessment(
            "GAD-7", 17, "severe"
        );

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Processing assessment
        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        // Then: HIGH severity alert created
        assertThat(alert).isNotNull();
        assertThat(alert.getAlertType()).isEqualTo("MENTAL_HEALTH_CRISIS");
        assertThat(alert.getSeverity()).isEqualTo("HIGH");
        assertThat(alert.getTitle()).contains("Severe Anxiety");
        assertThat(alert.getMessage()).contains("GAD-7 score: 17/21");

        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should create HIGH alert for risk level escalation to VERY_HIGH")
    void shouldCreateHighAlertForRiskEscalation() {
        // Given: Risk assessment changing to VERY_HIGH
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(85)
            .riskLevel(RiskAssessmentEntity.RiskLevel.VERY_HIGH)
            .assessmentDate(Instant.now())
            .build();

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.RISK_ESCALATION),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Processing risk assessment
        ClinicalAlertDTO alert = clinicalAlertService.evaluateRiskAssessment(
            TENANT_ID, riskAssessment
        );

        // Then: HIGH alert for risk escalation
        assertThat(alert).isNotNull();
        assertThat(alert.getAlertType()).isEqualTo("RISK_ESCALATION");
        assertThat(alert.getSeverity()).isEqualTo("HIGH");
        assertThat(alert.getTitle()).contains("Patient Risk Level: Very High");
        assertThat(alert.getMessage()).contains("Risk score: 85");

        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should not create alert when risk level is HIGH or below")
    void shouldSkipRiskAlertWhenNotVeryHigh() {
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(60)
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .assessmentDate(Instant.now())
            .build();

        ClinicalAlertDTO alert = clinicalAlertService.evaluateRiskAssessment(
            TENANT_ID, riskAssessment
        );

        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should create MEDIUM alert for health score decline >= 15 points")
    void shouldCreateMediumAlertForHealthScoreDecline() {
        // Given: Health score declined by 15+ points
        int previousScore = 75;
        int currentScore = 58;
        int decline = previousScore - currentScore; // 17 points

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.HEALTH_DECLINE),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        // When: Evaluating health score change
        ClinicalAlertDTO alert = clinicalAlertService.evaluateHealthScoreChange(
            TENANT_ID, PATIENT_ID, previousScore, currentScore
        );

        // Then: MEDIUM alert for health decline
        assertThat(alert).isNotNull();
        assertThat(alert.getAlertType()).isEqualTo("HEALTH_DECLINE");
        assertThat(alert.getSeverity()).isEqualTo("MEDIUM");
        assertThat(alert.getTitle()).contains("Health Score Decline");
        assertThat(alert.getMessage()).contains("declined by 17 points");

        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should NOT create duplicate alert within 24 hour window")
    void shouldNotCreateDuplicateAlertWithin24Hours() {
        // Given: Recent alert exists (within 24 hours)
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessment(
            "PHQ-9", 22, "severe"
        );

        ClinicalAlertEntity existingAlert = ClinicalAlertEntity.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .alertType(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)
            .severity(ClinicalAlertEntity.AlertSeverity.CRITICAL)
            .triggeredAt(Instant.now().minus(12, ChronoUnit.HOURS))
            .build();

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS),
            any(Instant.class)
        )).thenReturn(List.of(existingAlert));

        // When: Attempting to create duplicate
        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        // Then: No new alert created (returns existing)
        assertThat(alert).isNull(); // Or returns existing alert

        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should prioritize CRITICAL alerts over HIGH and MEDIUM")
    void shouldPrioritizeAlertsBySeverity() {
        // Given: Multiple alerts of different severities (repository returns them sorted)
        when(alertRepository.findActiveAlertsForPatient(eq(TENANT_ID), eq(PATIENT_ID)))
            .thenReturn(List.of(
                createAlertEntity("CRITICAL", "MENTAL_HEALTH_CRISIS", "Suicide risk"),
                createAlertEntity("HIGH", "RISK_ESCALATION", "High risk"),
                createAlertEntity("MEDIUM", "HEALTH_DECLINE", "Health decline")
            ));

        // When: Getting prioritized alerts
        List<ClinicalAlertDTO> alerts = clinicalAlertService.getActiveAlerts(
            TENANT_ID, PATIENT_ID
        );

        // Then: Alerts returned in priority order (CRITICAL, HIGH, MEDIUM)
        assertThat(alerts).hasSize(3);
        assertThat(alerts.get(0).getSeverity()).isEqualTo("CRITICAL");
        assertThat(alerts.get(1).getSeverity()).isEqualTo("HIGH");
        assertThat(alerts.get(2).getSeverity()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Should isolate alerts by tenant (multi-tenant)")
    void shouldIsolateAlertsByTenant() {
        // Given: Alerts for different tenants
        String otherTenant = "other-tenant";

        when(alertRepository.findActiveAlertsForPatient(eq(TENANT_ID), eq(PATIENT_ID)))
            .thenReturn(List.of(
                createAlertEntityForTenant(TENANT_ID, "Alert 1"),
                createAlertEntityForTenant(TENANT_ID, "Alert 2")
            ));

        // When: Fetching alerts for test tenant
        List<ClinicalAlertDTO> alerts = clinicalAlertService.getActiveAlerts(
            TENANT_ID, PATIENT_ID
        );

        // Then: Only tenant's alerts returned
        assertThat(alerts).hasSize(2);
        alerts.forEach(alert ->
            assertThat(alert.getTenantId()).isEqualTo(TENANT_ID)
        );

        // Verify query filtered by tenant
        verify(alertRepository).findActiveAlertsForPatient(eq(TENANT_ID), eq(PATIENT_ID));
    }

    @Test
    @DisplayName("Should acknowledge alert and update status")
    void shouldAcknowledgeAlert() {
        // Given: Active alert
        UUID alertId = UUID.randomUUID();
        ClinicalAlertEntity alert = createAlertEntityWithId(alertId);

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: Acknowledging alert
        ClinicalAlertDTO acknowledged = clinicalAlertService.acknowledgeAlert(
            TENANT_ID, alertId.toString(), PROVIDER_ID
        );

        // Then: Alert marked as acknowledged
        assertThat(acknowledged).isNotNull();
        assertThat(acknowledged.getStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(acknowledged.getAcknowledgedBy()).isEqualTo(PROVIDER_ID);
        assertThat(acknowledged.getAcknowledgedAt()).isNotNull();

        verify(alertRepository).save(argThat(entity ->
            entity.getStatus() == ClinicalAlertEntity.AlertStatus.ACKNOWLEDGED &&
            entity.getAcknowledgedBy().equals(PROVIDER_ID) &&
            entity.getAcknowledgedAt() != null
        ));
    }

    @Test
    @DisplayName("Should swallow notification errors on acknowledgment")
    void shouldHandleNotificationFailureOnAcknowledge() {
        UUID alertId = UUID.randomUUID();
        ClinicalAlertEntity alert = createAlertEntityWithId(alertId);

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("notify")).when(notificationTrigger)
            .onAlertAcknowledged(eq(TENANT_ID), any(ClinicalAlertDTO.class));

        ClinicalAlertDTO acknowledged = clinicalAlertService.acknowledgeAlert(
            TENANT_ID, alertId.toString(), PROVIDER_ID
        );

        assertThat(acknowledged.getStatus()).isEqualTo("ACKNOWLEDGED");
    }

    @Test
    @DisplayName("Should resolve alert and update status")
    void shouldResolveAlert() {
        // Given: Acknowledged alert
        UUID alertId = UUID.randomUUID();
        ClinicalAlertEntity alert = createAlertEntityWithId(alertId);
        alert.setStatus(ClinicalAlertEntity.AlertStatus.ACKNOWLEDGED);

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: Resolving alert
        ClinicalAlertDTO resolved = clinicalAlertService.resolveAlert(
            TENANT_ID, alertId.toString(), PROVIDER_ID
        );

        // Then: Alert marked as resolved
        assertThat(resolved).isNotNull();
        assertThat(resolved.getStatus()).isEqualTo("RESOLVED");

        verify(alertRepository).save(argThat(entity ->
            entity.getStatus() == ClinicalAlertEntity.AlertStatus.RESOLVED
        ));
    }

    @Test
    @DisplayName("Should reject resolve when alert not found")
    void shouldRejectResolveWhenAlertNotFound() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicalAlertService.resolveAlert(
            TENANT_ID, alertId.toString(), PROVIDER_ID
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Alert not found");
    }

    @Test
    @DisplayName("Should reject resolve when alert belongs to another tenant")
    void shouldRejectResolveWhenTenantMismatch() {
        UUID alertId = UUID.randomUUID();
        ClinicalAlertEntity alert = createAlertEntityWithId(alertId);
        alert.setTenantId("other-tenant");

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> clinicalAlertService.resolveAlert(
            TENANT_ID, alertId.toString(), PROVIDER_ID
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to tenant");
    }

    @Test
    @DisplayName("Should NOT create alert for moderate depression (PHQ-9 10-14)")
    void shouldNotCreateAlertForModerateDepression() {
        // Given: PHQ-9 with moderate score (not critical)
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessment(
            "PHQ-9", 12, "moderate"
        );

        // When: Evaluating assessment
        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        // Then: No alert created (below threshold)
        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should create HIGH alert for severe chronic deterioration")
    void shouldCreateChronicDeteriorationAlert() {
        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.CHRONIC_DETERIORATION),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        ClinicalAlertDTO alert = clinicalAlertService.evaluateChronicDiseaseDeterioration(
            TENANT_ID, PATIENT_ID, "COPD", "FEV1", "SEVERE"
        );

        assertThat(alert).isNotNull();
        assertThat(alert.getAlertType()).isEqualTo("CHRONIC_DETERIORATION");
        assertThat(alert.getSeverity()).isEqualTo("HIGH");
        verify(kafkaTemplate).send(eq("clinical-alert.triggered"), any());
    }

    @Test
    @DisplayName("Should create MEDIUM alert for moderate chronic deterioration")
    void shouldCreateModerateChronicDeteriorationAlert() {
        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.CHRONIC_DETERIORATION),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        ClinicalAlertDTO alert = clinicalAlertService.evaluateChronicDiseaseDeterioration(
            TENANT_ID, PATIENT_ID, "Diabetes", "HbA1c", "MODERATE"
        );

        assertThat(alert).isNotNull();
        assertThat(alert.getSeverity()).isEqualTo("MEDIUM");
        assertThat(alert.isEscalated()).isFalse();
    }

    @Test
    @DisplayName("Should not create alert for mild chronic deterioration")
    void shouldSkipChronicDeteriorationWhenMild() {
        ClinicalAlertDTO alert = clinicalAlertService.evaluateChronicDiseaseDeterioration(
            TENANT_ID, PATIENT_ID, "COPD", "FEV1", "MILD"
        );

        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should not create alert for low chronic deterioration")
    void shouldSkipChronicDeteriorationWhenLow() {
        ClinicalAlertDTO alert = clinicalAlertService.evaluateChronicDiseaseDeterioration(
            TENANT_ID, PATIENT_ID, "COPD", "FEV1", "LOW"
        );

        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should not create alert when health score decline below threshold")
    void shouldSkipHealthScoreDeclineBelowThreshold() {
        ClinicalAlertDTO alert = clinicalAlertService.evaluateHealthScoreChange(
            TENANT_ID, PATIENT_ID, 80, 70
        );

        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should suppress duplicate risk escalation alerts")
    void shouldSuppressDuplicateRiskEscalationAlerts() {
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(90)
            .riskLevel(RiskAssessmentEntity.RiskLevel.VERY_HIGH)
            .assessmentDate(Instant.now())
            .build();

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.RISK_ESCALATION),
            any(Instant.class)
        )).thenReturn(List.of(ClinicalAlertEntity.builder().build()));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateRiskAssessment(
            TENANT_ID, riskAssessment
        );

        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should suppress duplicate health score decline alerts")
    void shouldSuppressDuplicateHealthScoreDeclineAlerts() {
        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.HEALTH_DECLINE),
            any(Instant.class)
        )).thenReturn(List.of(ClinicalAlertEntity.builder().build()));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateHealthScoreChange(
            TENANT_ID, PATIENT_ID, 80, 60
        );

        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should suppress duplicate chronic deterioration alerts")
    void shouldSuppressDuplicateChronicDeteriorationAlerts() {
        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.CHRONIC_DETERIORATION),
            any(Instant.class)
        )).thenReturn(List.of(ClinicalAlertEntity.builder().build()));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateChronicDiseaseDeterioration(
            TENANT_ID, PATIENT_ID, "CHF", "Ejection Fraction", "SEVERE"
        );

        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should swallow notification errors on alert trigger")
    void shouldHandleNotificationFailureOnTrigger() {
        RiskAssessmentEntity riskAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(85)
            .riskLevel(RiskAssessmentEntity.RiskLevel.VERY_HIGH)
            .assessmentDate(Instant.now())
            .build();

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.RISK_ESCALATION),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        doThrow(new RuntimeException("notify")).when(notificationTrigger)
            .onAlertTriggered(eq(TENANT_ID), any(ClinicalAlertDTO.class));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateRiskAssessment(
            TENANT_ID, riskAssessment
        );

        assertThat(alert).isNotNull();
        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should swallow notification errors for suicide risk alerts")
    void shouldHandleNotificationFailureForSuicideRisk() {
        Map<String, Integer> responses = new HashMap<>();
        responses.put("item_9", 1);
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessmentWithResponses(
            "PHQ-9", 12, "moderate", responses
        );

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        doThrow(new RuntimeException("notify")).when(notificationTrigger)
            .onAlertTriggered(eq(TENANT_ID), any(ClinicalAlertDTO.class));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        assertThat(alert).isNotNull();
        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should swallow notification errors for severe depression alerts")
    void shouldHandleNotificationFailureForSevereDepression() {
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessment(
            "PHQ-9", 22, "severe"
        );

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        doThrow(new RuntimeException("notify")).when(notificationTrigger)
            .onAlertTriggered(eq(TENANT_ID), any(ClinicalAlertDTO.class));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        assertThat(alert).isNotNull();
        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should swallow notification errors for severe anxiety alerts")
    void shouldHandleNotificationFailureForSevereAnxiety() {
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessment(
            "GAD-7", 18, "severe"
        );

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        doThrow(new RuntimeException("notify")).when(notificationTrigger)
            .onAlertTriggered(eq(TENANT_ID), any(ClinicalAlertDTO.class));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        assertThat(alert).isNotNull();
        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should swallow notification errors for health score decline alerts")
    void shouldHandleNotificationFailureForHealthScoreDecline() {
        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.HEALTH_DECLINE),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        doThrow(new RuntimeException("notify")).when(notificationTrigger)
            .onAlertTriggered(eq(TENANT_ID), any(ClinicalAlertDTO.class));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateHealthScoreChange(
            TENANT_ID, PATIENT_ID, 90, 70
        );

        assertThat(alert).isNotNull();
        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should swallow notification errors for chronic deterioration alerts")
    void shouldHandleNotificationFailureForChronicDeterioration() {
        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.CHRONIC_DETERIORATION),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        doThrow(new RuntimeException("notify")).when(notificationTrigger)
            .onAlertTriggered(eq(TENANT_ID), any(ClinicalAlertDTO.class));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateChronicDiseaseDeterioration(
            TENANT_ID, PATIENT_ID, "CHF", "Ejection Fraction", "SEVERE"
        );

        assertThat(alert).isNotNull();
        verify(alertRepository).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should suppress duplicate suicide risk alerts")
    void shouldSuppressDuplicateSuicideRiskAlerts() {
        Map<String, Integer> responses = new HashMap<>();
        responses.put("item_9", 1);
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessmentWithResponses(
            "PHQ-9", 15, "moderate", responses
        );

        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS),
            any(Instant.class)
        )).thenReturn(List.of(ClinicalAlertEntity.builder().build()));

        ClinicalAlertDTO alert = clinicalAlertService.evaluateMentalHealthAssessment(
            TENANT_ID, assessment
        );

        assertThat(alert).isNull();
        verify(alertRepository, never()).save(any(ClinicalAlertEntity.class));
    }

    @Test
    @DisplayName("Should publish alert event with expected payload")
    @SuppressWarnings("unchecked")
    void shouldPublishAlertEventWithExpectedPayload() {
        when(alertRepository.findRecentDuplicates(
            eq(TENANT_ID),
            eq(PATIENT_ID),
            eq(ClinicalAlertEntity.AlertType.HEALTH_DECLINE),
            any(Instant.class)
        )).thenReturn(Collections.emptyList());

        when(alertRepository.save(any(ClinicalAlertEntity.class)))
            .thenAnswer(invocation -> {
                ClinicalAlertEntity entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                return entity;
            });

        clinicalAlertService.evaluateHealthScoreChange(
            TENANT_ID, PATIENT_ID, 90, 70
        );

        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq("clinical-alert.triggered"), eventCaptor.capture());

        Map<String, Object> payload = eventCaptor.getValue();
        assertThat(payload.get("patientId")).isEqualTo(PATIENT_ID);
        assertThat(payload.get("tenantId")).isEqualTo(TENANT_ID);
        assertThat(payload.get("alertType")).isEqualTo("HEALTH_DECLINE");
        assertThat(payload.get("severity")).isEqualTo("MEDIUM");
        assertThat(payload.get("alertId")).isNotNull();
    }

    @Test
    @DisplayName("Should reject acknowledging alert from another tenant")
    void shouldRejectAcknowledgeWhenTenantMismatch() {
        UUID alertId = UUID.randomUUID();
        ClinicalAlertEntity alert = createAlertEntityWithId(alertId);
        alert.setTenantId("other-tenant");

        when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));

        assertThatThrownBy(() -> clinicalAlertService.acknowledgeAlert(
            TENANT_ID, alertId.toString(), PROVIDER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong to tenant");
    }

    @Test
    @DisplayName("Should reject acknowledging when alert not found")
    void shouldRejectAcknowledgeWhenAlertNotFound() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findById(alertId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicalAlertService.acknowledgeAlert(
            TENANT_ID, alertId.toString(), PROVIDER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Alert not found");
    }

    // Helper methods
    private MentalHealthAssessmentEntity createMentalHealthAssessment(
        String type, int score, String severity
    ) {
        return MentalHealthAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .type(MentalHealthAssessmentEntity.AssessmentType.valueOf(type.replace("-", "_")))
            .score(score)
            .severity(severity)
            .assessmentDate(Instant.now())
            .assessedBy(PROVIDER_ID)
            .responses(new HashMap<>())
            .build();
    }

    private MentalHealthAssessmentEntity createMentalHealthAssessmentWithResponses(
        String type, int score, String severity, Map<String, Integer> responses
    ) {
        MentalHealthAssessmentEntity assessment = createMentalHealthAssessment(type, score, severity);
        assessment.setResponses(responses);
        return assessment;
    }

    private ClinicalAlertEntity createAlertEntity(
        String severity, String type, String title
    ) {
        return ClinicalAlertEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .alertType(ClinicalAlertEntity.AlertType.valueOf(type))
            .severity(ClinicalAlertEntity.AlertSeverity.valueOf(severity))
            .title(title)
            .status(ClinicalAlertEntity.AlertStatus.ACTIVE)
            .triggeredAt(Instant.now())
            .build();
    }

    private ClinicalAlertEntity createAlertEntityForTenant(String tenantId, String title) {
        return ClinicalAlertEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(PATIENT_ID)
            .alertType(ClinicalAlertEntity.AlertType.MENTAL_HEALTH_CRISIS)
            .severity(ClinicalAlertEntity.AlertSeverity.HIGH)
            .title(title)
            .status(ClinicalAlertEntity.AlertStatus.ACTIVE)
            .triggeredAt(Instant.now())
            .build();
    }

    private ClinicalAlertEntity createAlertEntityWithId(UUID id) {
        ClinicalAlertEntity alert = createAlertEntity("CRITICAL", "MENTAL_HEALTH_CRISIS", "Test Alert");
        alert.setId(id);
        return alert;
    }
}
