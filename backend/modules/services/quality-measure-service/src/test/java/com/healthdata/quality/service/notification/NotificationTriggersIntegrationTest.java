package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.CareGapDTO;
import com.healthdata.quality.dto.ClinicalAlertDTO;
import com.healthdata.quality.dto.MentalHealthAssessmentDTO;
import com.healthdata.quality.dto.notification.GenericNotificationRequest;
import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.CareTeamMemberEntity;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.service.NotificationService;
import com.healthdata.quality.service.PatientNameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Comprehensive Integration Tests for Notification Triggers
 *
 * Tests all three notification triggers:
 * - CareGapNotificationTrigger
 * - ClinicalAlertNotificationTrigger
 * - MentalHealthNotificationTrigger
 *
 * Validates:
 * - Notifications are sent at appropriate times
 * - Channel routing based on severity/priority
 * - Smart filtering to prevent notification fatigue
 * - Template variable construction
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Triggers Integration Tests")
class NotificationTriggersIntegrationTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RecipientResolutionService recipientResolutionService;

    @Mock
    private PatientNameService patientNameService;

    @InjectMocks
    private CareGapNotificationTrigger careGapTrigger;

    @InjectMocks
    private ClinicalAlertNotificationTrigger clinicalAlertTrigger;

    @InjectMocks
    private MentalHealthNotificationTrigger mentalHealthTrigger;

    @Captor
    private ArgumentCaptor<GenericNotificationRequest> requestCaptor;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("55555555-6666-7777-8888-999999999999");

    @BeforeEach
    void setup() {
        // Mock successful notification status (lenient for tests where notification is skipped)
        NotificationService.NotificationStatus successStatus =
                NotificationService.NotificationStatus.builder()
                        .allSuccessful(true)
                        .channelStatus(Map.of(
                                "WEBSOCKET", true,
                                "EMAIL", true,
                                "SMS", true
                        ))
                        .build();

        lenient().when(notificationService.sendNotification(any(GenericNotificationRequest.class)))
                .thenReturn(successStatus);

        // Mock PatientNameService (lenient for all tests)
        lenient().when(patientNameService.getPatientName(any(UUID.class)))
                .thenReturn("John Doe");

        // Mock RecipientResolutionService (lenient for all tests)
        NotificationRecipient mockRecipient = NotificationRecipient.builder()
                .userId("user-1")
                .emailAddress("doctor@example.com")
                .phoneNumber("+15555551234")
                .isPrimary(true)
                .careTeamRole(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN)
                .enabledChannels(Set.of(
                        NotificationEntity.NotificationChannel.EMAIL,
                        NotificationEntity.NotificationChannel.SMS
                ))
                .severityThreshold(NotificationEntity.NotificationSeverity.MEDIUM)
                .build();

        lenient().when(recipientResolutionService.resolveRecipients(
                anyString(), any(UUID.class), any(NotificationEntity.NotificationChannel.class),
                any(NotificationEntity.NotificationSeverity.class)))
                .thenReturn(List.of(mockRecipient));
    }

    // ============================================================
    // CARE GAP NOTIFICATION TRIGGER TESTS
    // ============================================================

    @Test
    @DisplayName("CareGapTrigger: Send notification for HIGH priority gap identified")
    void testCareGapIdentified_HighPriority() {
        // Given: HIGH priority care gap
        CareGapDTO careGap = CareGapDTO.builder()
                .id("gap-123")
                .patientId(PATIENT_ID)
                .category("mental-health")
                .gapType("mental-health-followup-phq9")
                .title("PHQ-9 Positive Screen - Follow-up Required")
                .description("Patient screened positive on PHQ-9")
                .priority("HIGH")
                .status("open")
                .dueDate(Instant.now().plus(14, ChronoUnit.DAYS))
                .identifiedDate(Instant.now())
                .build();

        // When: Trigger notification
        careGapTrigger.onCareGapIdentified(TENANT_ID, careGap);

        // Then: Notification sent
        verify(notificationService, times(1)).sendNotification(requestCaptor.capture());

        GenericNotificationRequest request = requestCaptor.getValue();
        assertThat(request.getNotificationType()).isEqualTo("CARE_GAP_IDENTIFIED");
        assertThat(request.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(request.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(request.getSendWebSocket()).isTrue();
        assertThat(request.getSendEmail()).isTrue();  // HIGH priority → email
        assertThat(request.getSendSms()).isFalse();   // Not CRITICAL
    }

    @Test
    @DisplayName("CareGapTrigger: Send SMS for CRITICAL priority gap")
    void testCareGapIdentified_CriticalPriority() {
        // Given: CRITICAL priority care gap
        CareGapDTO careGap = CareGapDTO.builder()
                .id("gap-456")
                .patientId(PATIENT_ID)
                .category("mental-health")
                .priority("CRITICAL")
                .title("Critical Care Gap")
                .dueDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        // When
        careGapTrigger.onCareGapIdentified(TENANT_ID, careGap);

        // Then: SMS sent for CRITICAL
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getSendWebSocket()).isTrue();
        assertThat(request.getSendEmail()).isTrue();
        assertThat(request.getSendSms()).isTrue();  // CRITICAL → SMS
    }

    @Test
    @DisplayName("CareGapTrigger: Skip notification for LOW priority gap not due soon")
    void testCareGapIdentified_LowPriorityNotDueSoon() {
        // Given: LOW priority gap due in 60 days
        CareGapDTO careGap = CareGapDTO.builder()
                .id("gap-789")
                .patientId(PATIENT_ID)
                .priority("LOW")
                .title("Low Priority Gap")
                .dueDate(Instant.now().plus(60, ChronoUnit.DAYS))
                .build();

        // When
        careGapTrigger.onCareGapIdentified(TENANT_ID, careGap);

        // Then: No notification sent (smart filtering)
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("CareGapTrigger: Send notification when gap addressed")
    void testCareGapAddressed() {
        // Given: Addressed care gap
        CareGapDTO careGap = CareGapDTO.builder()
                .id("gap-101")
                .patientId(PATIENT_ID)
                .title("Gap Addressed")
                .status("addressed")
                .addressedBy("Dr. Smith")
                .addressedDate(Instant.now())
                .priority("HIGH")
                .build();

        // When
        careGapTrigger.onCareGapAddressed(TENANT_ID, careGap);

        // Then: Notification sent with documentation
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getNotificationType()).isEqualTo("CARE_GAP_ADDRESSED");
        assertThat(request.getSeverity()).isEqualTo("LOW");  // Positive event
        assertThat(request.getSendEmail()).isTrue();  // For documentation
        assertThat(request.getSendSms()).isFalse();   // Not needed for addressed
    }

    // ============================================================
    // CLINICAL ALERT NOTIFICATION TRIGGER TESTS
    // ============================================================

    @Test
    @DisplayName("ClinicalAlertTrigger: Send notification for CRITICAL alert")
    void testClinicalAlert_Critical() {
        // Given: CRITICAL severity alert (suicide risk)
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
                .id("alert-001")
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .alertType("MENTAL_HEALTH_CRISIS")
                .severity("CRITICAL")
                .title("URGENT: Suicide Risk Detected")
                .message("Patient reported suicidal ideation on PHQ-9")
                .triggeredAt(Instant.now())
                .escalated(true)
                .escalatedAt(Instant.now())
                .status("ACTIVE")
                .build();

        // When
        clinicalAlertTrigger.onAlertTriggered(TENANT_ID, alert);

        // Then: All channels used for CRITICAL
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getNotificationType()).isEqualTo("CLINICAL_ALERT_TRIGGERED");
        assertThat(request.getTemplateId()).isEqualTo("critical-alert");
        assertThat(request.getSeverity()).isEqualTo("HIGH");
        assertThat(request.getSendWebSocket()).isTrue();
        assertThat(request.getSendEmail()).isTrue();
        assertThat(request.getSendSms()).isTrue();  // CRITICAL → SMS
    }

    @Test
    @DisplayName("ClinicalAlertTrigger: HIGH severity gets email but not SMS")
    void testClinicalAlert_HighSeverity() {
        // Given: HIGH severity alert
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
                .id("alert-002")
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .alertType("RISK_ESCALATION")
                .severity("HIGH")
                .title("Patient Risk Level: Very High")
                .message("Risk score escalated to VERY_HIGH")
                .triggeredAt(Instant.now())
                .status("ACTIVE")
                .build();

        // When
        clinicalAlertTrigger.onAlertTriggered(TENANT_ID, alert);

        // Then: Email but no SMS for HIGH
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getSendWebSocket()).isTrue();
        assertThat(request.getSendEmail()).isTrue();   // HIGH → email
        assertThat(request.getSendSms()).isFalse();    // Only CRITICAL gets SMS
    }

    @Test
    @DisplayName("ClinicalAlertTrigger: MEDIUM severity gets email")
    void testClinicalAlert_MediumSeverity() {
        // Given: MEDIUM severity alert
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
                .id("alert-003")
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .alertType("HEALTH_DECLINE")
                .severity("MEDIUM")
                .title("Health Score Decline Detected")
                .message("Overall health score declined by 15 points")
                .triggeredAt(Instant.now())
                .status("ACTIVE")
                .build();

        // When
        clinicalAlertTrigger.onAlertTriggered(TENANT_ID, alert);

        // Then: Email sent for MEDIUM
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getSendWebSocket()).isTrue();
        assertThat(request.getSendEmail()).isTrue();   // MEDIUM → email
        assertThat(request.getSendSms()).isFalse();
    }

    @Test
    @DisplayName("ClinicalAlertTrigger: Send notification when CRITICAL alert acknowledged")
    void testClinicalAlert_Acknowledged() {
        // Given: Acknowledged CRITICAL alert
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
                .id("alert-004")
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .alertType("MENTAL_HEALTH_CRISIS")
                .severity("CRITICAL")
                .title("URGENT: Suicide Risk Detected")
                .status("ACKNOWLEDGED")
                .acknowledgedBy("Dr. Johnson")
                .acknowledgedAt(Instant.now())
                .build();

        // When
        clinicalAlertTrigger.onAlertAcknowledged(TENANT_ID, alert);

        // Then: Acknowledgment notification sent
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getNotificationType()).isEqualTo("CLINICAL_ALERT_ACKNOWLEDGED");
        assertThat(request.getSeverity()).isEqualTo("LOW");  // Informational
        assertThat(request.getSendEmail()).isTrue();   // For documentation
        assertThat(request.getSendSms()).isFalse();
    }

    @Test
    @DisplayName("ClinicalAlertTrigger: Skip acknowledgment notification for MEDIUM severity")
    void testClinicalAlert_AcknowledgedMedium_Skipped() {
        // Given: Acknowledged MEDIUM alert
        ClinicalAlertDTO alert = ClinicalAlertDTO.builder()
                .id("alert-005")
                .patientId(PATIENT_ID)
                .severity("MEDIUM")
                .status("ACKNOWLEDGED")
                .build();

        // When
        clinicalAlertTrigger.onAlertAcknowledged(TENANT_ID, alert);

        // Then: No notification for MEDIUM acknowledgments
        verify(notificationService, never()).sendNotification(any());
    }

    // ============================================================
    // MENTAL HEALTH ASSESSMENT NOTIFICATION TRIGGER TESTS
    // ============================================================

    @Test
    @DisplayName("MentalHealthTrigger: Send notification for severe PHQ-9 assessment")
    void testMentalHealthAssessment_Severe() {
        // Given: Severe PHQ-9 assessment
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
                .id("assessment-001")
                .patientId(PATIENT_ID)
                .type("PHQ_9")
                .name("Patient Health Questionnaire-9")
                .score(22)
                .maxScore(27)
                .severity("severe")
                .interpretation("Severe depression")
                .positiveScreen(true)
                .thresholdScore(10)
                .requiresFollowup(true)
                .assessedBy("Dr. Williams")
                .assessmentDate(Instant.now())
                .build();

        // When
        mentalHealthTrigger.onAssessmentCompleted(TENANT_ID, assessment);

        // Then: SMS sent for severe assessment
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getNotificationType()).isEqualTo("MENTAL_HEALTH_ASSESSMENT_COMPLETED");
        assertThat(request.getTemplateId()).isEqualTo("care-gap");
        assertThat(request.getSeverity()).isEqualTo("HIGH");
        assertThat(request.getSendWebSocket()).isTrue();
        assertThat(request.getSendEmail()).isTrue();
        assertThat(request.getSendSms()).isTrue();  // Severe → SMS
    }

    @Test
    @DisplayName("MentalHealthTrigger: Send notification for moderate GAD-7 assessment")
    void testMentalHealthAssessment_Moderate() {
        // Given: Moderate GAD-7 assessment
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
                .id("assessment-002")
                .patientId(PATIENT_ID)
                .type("GAD_7")
                .name("Generalized Anxiety Disorder-7")
                .score(12)
                .maxScore(21)
                .severity("moderate")
                .interpretation("Moderate anxiety")
                .positiveScreen(true)
                .thresholdScore(10)
                .requiresFollowup(true)
                .assessmentDate(Instant.now())
                .build();

        // When
        mentalHealthTrigger.onAssessmentCompleted(TENANT_ID, assessment);

        // Then: Email sent for moderate, but not SMS
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getSeverity()).isEqualTo("MEDIUM");
        assertThat(request.getSendEmail()).isTrue();
        assertThat(request.getSendSms()).isFalse();  // Only severe gets SMS
    }

    @Test
    @DisplayName("MentalHealthTrigger: Skip notification for negative screen")
    void testMentalHealthAssessment_Negative() {
        // Given: Negative PHQ-2 screen
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
                .id("assessment-003")
                .patientId(PATIENT_ID)
                .type("PHQ_2")
                .name("Patient Health Questionnaire-2")
                .score(1)
                .maxScore(6)
                .severity("negative")
                .interpretation("Negative screen for depression")
                .positiveScreen(false)
                .thresholdScore(3)
                .requiresFollowup(false)
                .assessmentDate(Instant.now())
                .build();

        // When
        mentalHealthTrigger.onAssessmentCompleted(TENANT_ID, assessment);

        // Then: No notification for routine negative screen
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    @DisplayName("MentalHealthTrigger: Send notification for mild assessment")
    void testMentalHealthAssessment_Mild() {
        // Given: Mild PHQ-9 assessment
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
                .id("assessment-004")
                .patientId(PATIENT_ID)
                .type("PHQ_9")
                .name("Patient Health Questionnaire-9")
                .score(7)
                .maxScore(27)
                .severity("mild")
                .interpretation("Mild depression")
                .positiveScreen(false)
                .thresholdScore(10)
                .requiresFollowup(false)
                .assessmentDate(Instant.now())
                .build();

        // When
        mentalHealthTrigger.onAssessmentCompleted(TENANT_ID, assessment);

        // Then: Email sent for tracking
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        assertThat(request.getSeverity()).isEqualTo("LOW");
        assertThat(request.getSendEmail()).isTrue();
        assertThat(request.getSendSms()).isFalse();
    }

    // ============================================================
    // ERROR HANDLING TESTS
    // ============================================================

    @Test
    @DisplayName("Triggers: Handle notification service failure gracefully")
    void testNotificationFailure_GracefulHandling() {
        // Given: Notification service throws exception
        when(notificationService.sendNotification(any()))
                .thenThrow(new RuntimeException("Notification service unavailable"));

        CareGapDTO careGap = CareGapDTO.builder()
                .id("gap-error")
                .patientId(PATIENT_ID)
                .priority("HIGH")
                .title("Test Gap")
                .build();

        // When: Trigger notification (should not throw)
        careGapTrigger.onCareGapIdentified(TENANT_ID, careGap);

        // Then: Exception handled gracefully
        verify(notificationService).sendNotification(any());
        // No exception propagated - test passes if no exception thrown
    }

    // ============================================================
    // TEMPLATE VARIABLE TESTS
    // ============================================================

    @Test
    @DisplayName("Triggers: Verify template variables are populated correctly")
    void testTemplateVariablesPopulated() {
        // Given: Care gap with full details
        CareGapDTO careGap = CareGapDTO.builder()
                .id("gap-template")
                .patientId(PATIENT_ID)
                .category("mental-health")
                .gapType("mental-health-followup-phq9")
                .title("PHQ-9 Positive Screen")
                .description("Patient screened positive")
                .priority("HIGH")
                .status("open")
                .qualityMeasure("CMS2")
                .dueDate(Instant.now().plus(14, ChronoUnit.DAYS))
                .identifiedDate(Instant.now())
                .build();

        // When
        careGapTrigger.onCareGapIdentified(TENANT_ID, careGap);

        // Then: Template variables populated
        verify(notificationService).sendNotification(requestCaptor.capture());
        GenericNotificationRequest request = requestCaptor.getValue();

        Map<String, Object> variables = request.getTemplateVariables();
        assertThat(variables).isNotNull();
        assertThat(variables).containsKeys(
                "patientId", "patientName", "gapTitle", "gapDescription",
                "priority", "status", "qualityMeasure", "actionUrl"
        );
        assertThat(variables.get("patientId")).isEqualTo(PATIENT_ID);
        assertThat(variables.get("gapTitle")).isEqualTo("PHQ-9 Positive Screen");
    }
}
