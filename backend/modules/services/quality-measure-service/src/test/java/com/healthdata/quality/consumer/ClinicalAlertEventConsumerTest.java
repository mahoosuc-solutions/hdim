package com.healthdata.quality.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthdata.audit.service.AuditService;
import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity;
import com.healthdata.quality.persistence.MentalHealthAssessmentRepository;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import com.healthdata.quality.service.ClinicalAlertService;
import com.healthdata.quality.service.NotificationService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Clinical Alert Event Consumer Tests")
class ClinicalAlertEventConsumerTest {

    @Mock
    private ClinicalAlertService clinicalAlertService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MentalHealthAssessmentRepository mentalHealthAssessmentRepository;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ClinicalAlertEventConsumer consumer;

    @Test
    @DisplayName("Should send notification for mental health assessment alert")
    void shouldSendNotificationForMentalHealthAssessmentAlert() {
        UUID assessmentId = UUID.randomUUID();
        MentalHealthAssessmentEntity assessment = new MentalHealthAssessmentEntity();
        assessment.setId(assessmentId);
        when(mentalHealthAssessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder().id("alert-1").build();
        when(clinicalAlertService.evaluateMentalHealthAssessment(eq("tenant-1"), eq(assessment)))
            .thenReturn(alert);

        consumer.handleMentalHealthAssessment(Map.of(
            "tenantId", "tenant-1",
            "assessmentId", assessmentId.toString()
        ));

        verify(notificationService).sendNotification("tenant-1", alert);
    }

    @Test
    @DisplayName("Should skip notification when no alert created for assessment")
    void shouldSkipNotificationWhenNoAlertCreatedForAssessment() {
        UUID assessmentId = UUID.randomUUID();
        MentalHealthAssessmentEntity assessment = new MentalHealthAssessmentEntity();
        when(mentalHealthAssessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(clinicalAlertService.evaluateMentalHealthAssessment(eq("tenant-1"), eq(assessment)))
            .thenReturn(null);

        consumer.handleMentalHealthAssessment(Map.of(
            "tenantId", "tenant-1",
            "assessmentId", assessmentId.toString()
        ));

        verify(notificationService, never()).sendNotification(any(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should throw when assessment not found for DLT routing")
    void shouldHandleMissingAssessment() {
        UUID assessmentId = UUID.randomUUID();
        when(mentalHealthAssessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            consumer.handleMentalHealthAssessment(Map.of(
                "tenantId", "tenant-1",
                "assessmentId", assessmentId.toString()
            ))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Failed to process mental health assessment event");

        verify(notificationService, never()).sendNotification(any(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should skip risk assessment alerts when not VERY_HIGH")
    void shouldSkipRiskAssessmentWhenNotVeryHigh() {
        consumer.handleRiskAssessmentUpdate(Map.of(
            "tenantId", "tenant-1",
            "assessmentId", UUID.randomUUID().toString(),
            "riskLevel", "HIGH"
        ));

        verify(riskAssessmentRepository, never()).findById(any());
        verify(notificationService, never()).sendNotification(any(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should send notification for VERY_HIGH risk assessment")
    void shouldSendNotificationForVeryHighRisk() {
        UUID assessmentId = UUID.randomUUID();
        RiskAssessmentEntity assessment = RiskAssessmentEntity.builder()
            .patientId(UUID.randomUUID())
            .build();
        when(riskAssessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        ClinicalAlertDTO alert = ClinicalAlertDTO.builder().id("alert-2").build();
        when(clinicalAlertService.evaluateRiskAssessment(eq("tenant-1"), eq(assessment)))
            .thenReturn(alert);

        consumer.handleRiskAssessmentUpdate(Map.of(
            "tenantId", "tenant-1",
            "assessmentId", assessmentId.toString(),
            "riskLevel", "VERY_HIGH"
        ));

        verify(notificationService).sendNotification("tenant-1", alert);
    }

    @Test
    @DisplayName("Should throw when risk assessment not found for DLT routing")
    void shouldHandleMissingRiskAssessment() {
        UUID assessmentId = UUID.randomUUID();
        when(riskAssessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            consumer.handleRiskAssessmentUpdate(Map.of(
                "tenantId", "tenant-1",
                "assessmentId", assessmentId.toString(),
                "riskLevel", "VERY_HIGH"
            ))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Failed to process risk assessment event");

        verify(notificationService, never()).sendNotification(any(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should skip health score change when not declining")
    void shouldSkipHealthScoreWhenNotDeclining() {
        consumer.handleHealthScoreChange(Map.of(
            "tenantId", "tenant-1",
            "patientId", UUID.randomUUID().toString(),
            "previousScore", 70,
            "currentScore", 72
        ));

        verify(notificationService, never()).sendNotification(any(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should send notification on health score decline")
    void shouldSendNotificationOnHealthScoreDecline() {
        UUID patientId = UUID.randomUUID();
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder().id("alert-3").build();
        when(clinicalAlertService.evaluateHealthScoreChange(eq("tenant-1"), eq(patientId), eq(80), eq(60)))
            .thenReturn(alert);

        consumer.handleHealthScoreChange(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "previousScore", 80,
            "currentScore", 60
        ));

        verify(notificationService).sendNotification("tenant-1", alert);
    }

    @Test
    @DisplayName("Should handle health score change when patientId missing")
    void shouldHandleHealthScoreChangeWhenPatientIdMissing() {
        consumer.handleHealthScoreChange(Map.of(
            "tenantId", "tenant-1",
            "previousScore", 80,
            "currentScore", 60
        ));

        verify(notificationService, never()).sendNotification(any(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should handle alert triggered cascade notification")
    void shouldHandleAlertTriggeredCascadeNotification() {
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
            .id("alert-4")
            .patientId(UUID.randomUUID())
            .severity("HIGH")
            .build();
        when(notificationService.sendNotificationWithStatus(eq("tenant-1"), any(ClinicalAlertDTO.class)))
            .thenReturn(NotificationService.NotificationStatus.builder().build());

        consumer.handleAlertTriggered(Map.of(
            "alertId", "alert-4",
            "tenantId", "tenant-1",
            "patientId", alert.getPatientId().toString(),
            "alertType", "RISK_ESCALATION",
            "severity", "HIGH",
            "triggeredAt", Instant.now().toString()
        ));

        verify(notificationService).sendNotificationWithStatus(eq("tenant-1"), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should skip chronic deterioration notification when no alert returned")
    void shouldSkipChronicDeteriorationWhenNoAlert() {
        UUID patientId = UUID.randomUUID();
        when(clinicalAlertService.evaluateChronicDiseaseDeterioration(
            eq("tenant-1"), eq(patientId), eq("COPD"), eq("FEV1"), eq("MILD")))
            .thenReturn(null);

        consumer.handleChronicDiseaseDeterioration(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "condition", "COPD",
            "metric", "FEV1",
            "severity", "MILD"
        ));

        verify(notificationService, never()).sendNotification(any(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should send notification for chronic deterioration alert")
    void shouldSendNotificationForChronicDeteriorationAlert() {
        UUID patientId = UUID.randomUUID();
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder().id("alert-6").build();
        when(clinicalAlertService.evaluateChronicDiseaseDeterioration(
            eq("tenant-1"), eq(patientId), eq("COPD"), eq("FEV1"), eq("SEVERE")))
            .thenReturn(alert);

        consumer.handleChronicDiseaseDeterioration(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId,
            "condition", "COPD",
            "metric", "FEV1",
            "severity", "SEVERE"
        ));

        verify(notificationService).sendNotification("tenant-1", alert);
    }

    @Test
    @DisplayName("Should ignore health score change when patientId invalid")
    void shouldIgnoreHealthScoreChangeWhenPatientIdInvalid() {
        consumer.handleHealthScoreChange(Map.of(
            "tenantId", "tenant-1",
            "patientId", "not-a-uuid",
            "previousScore", 80,
            "currentScore", 60
        ));

        verify(notificationService, never()).sendNotification(any(), any(ClinicalAlertDTO.class));
    }

    @Test
    @DisplayName("Should handle alert triggered even when notification fails")
    void shouldHandleAlertTriggeredWhenNotificationFails() {
        UUID patientId = UUID.randomUUID();
        when(notificationService.sendNotificationWithStatus(eq("tenant-1"), any(ClinicalAlertDTO.class)))
            .thenThrow(new RuntimeException("notify fail"));

        consumer.handleAlertTriggered(Map.of(
            "alertId", "alert-5",
            "tenantId", "tenant-1",
            "patientId", patientId,
            "alertType", "HEALTH_DECLINE",
            "severity", "MEDIUM",
            "triggeredAt", Instant.now().toString()
        ));

        verify(notificationService).sendNotificationWithStatus(eq("tenant-1"), any(ClinicalAlertDTO.class));
    }
}
