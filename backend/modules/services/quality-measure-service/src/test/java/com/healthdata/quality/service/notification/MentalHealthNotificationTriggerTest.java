package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.MentalHealthAssessmentDTO;
import com.healthdata.quality.dto.notification.NotificationRequest;
import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.CareTeamMemberEntity;
import com.healthdata.quality.persistence.CareTeamMemberRepository;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.service.NotificationService;
import com.healthdata.quality.service.PatientNameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentalHealthNotificationTrigger Tests")
class MentalHealthNotificationTriggerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PatientNameService patientNameService;

    @Mock
    private RecipientResolutionService recipientResolutionService;

    @Mock
    private CareTeamMemberRepository careTeamMemberRepository;

    private MentalHealthNotificationTrigger trigger;

    @BeforeEach
    void setUp() {
        trigger = new MentalHealthNotificationTrigger(
            notificationService,
            patientNameService,
            recipientResolutionService,
            careTeamMemberRepository
        );

        ReflectionTestUtils.setField(trigger, "defaultEmail", "default@example.com");
        ReflectionTestUtils.setField(trigger, "defaultPhone", "+15550001111");
    }

    @Test
    @DisplayName("Should skip notification for minimal negative screen")
    void shouldSkipNotificationForMinimalNegative() {
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-1")
            .patientId(UUID.randomUUID())
            .type("phq-9")
            .name("PHQ-9")
            .score(2)
            .maxScore(27)
            .severity("minimal")
            .interpretation("Minimal or no depression")
            .positiveScreen(false)
            .requiresFollowup(false)
            .build();

        trigger.onAssessmentCompleted("tenant-1", assessment);

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should send severe assessment notification with defaults")
    void shouldSendSevereAssessmentWithDefaults() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-2")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(24)
            .maxScore(27)
            .severity("severe")
            .interpretation("Severe depression")
            .positiveScreen(true)
            .thresholdScore(10)
            .requiresFollowup(true)
            .assessedBy("Practitioner/Dr-Smith")
            .assessmentDate(Instant.now())
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of());
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());
        when(careTeamMemberRepository.findActiveByPatientIdAndTenantIdAndRole(
            eq(patientId), eq("tenant-1"), eq(CareTeamMemberEntity.CareTeamRole.MENTAL_HEALTH_COUNSELOR)))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true, "sms", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Jane Doe");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isTrue();
        assertThat(request.getRecipients()).containsEntry("EMAIL", "default@example.com");
        assertThat(request.getRecipients()).containsEntry("SMS", "+15550001111");
        assertThat(request.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should send moderate assessment via email only")
    void shouldSendModerateAssessmentViaEmailOnly() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-3")
            .patientId(patientId)
            .type("gad-7")
            .name("GAD-7")
            .score(12)
            .maxScore(21)
            .severity("moderate")
            .interpretation("Moderate anxiety")
            .positiveScreen(true)
            .thresholdScore(10)
            .requiresFollowup(true)
            .build();

        NotificationRecipient emailRecipient = NotificationRecipient.builder()
            .emailAddress("care@example.com")
            .build();
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of(emailRecipient));
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Jamie Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isFalse();
        assertThat(request.getRecipients()).containsEntry("EMAIL", "care@example.com");
        assertThat(request.getSeverity()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Should append counselor email for severe assessment")
    void shouldAppendCounselorEmailForSevereAssessment() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-4")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(21)
            .maxScore(27)
            .severity("severe")
            .interpretation("Severe depression")
            .positiveScreen(true)
            .requiresFollowup(true)
            .build();

        NotificationRecipient baseRecipient = NotificationRecipient.builder()
            .emailAddress("primary@example.com")
            .build();
        NotificationRecipient counselorRecipient = NotificationRecipient.builder()
            .userId("counselor-1")
            .emailAddress("counselor@example.com")
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of(baseRecipient, counselorRecipient));
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        CareTeamMemberEntity counselor = CareTeamMemberEntity.builder()
            .userId("counselor-1")
            .role(CareTeamMemberEntity.CareTeamRole.MENTAL_HEALTH_COUNSELOR)
            .build();
        when(careTeamMemberRepository.findActiveByPatientIdAndTenantIdAndRole(
            eq(patientId), eq("tenant-1"), eq(CareTeamMemberEntity.CareTeamRole.MENTAL_HEALTH_COUNSELOR)))
            .thenReturn(List.of(counselor));

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Casey Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.getRecipients().get("EMAIL"))
            .contains("primary@example.com")
            .contains("counselor@example.com");
    }

    @Test
    @DisplayName("Should not append counselor email when userId mismatches")
    void shouldNotAppendCounselorEmailWhenUserIdMismatches() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-4b")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(19)
            .maxScore(27)
            .severity("severe")
            .interpretation("Severe depression")
            .positiveScreen(true)
            .requiresFollowup(true)
            .build();

        NotificationRecipient baseRecipient = NotificationRecipient.builder()
            .emailAddress("primary@example.com")
            .build();
        NotificationRecipient otherRecipient = NotificationRecipient.builder()
            .userId("other-user")
            .emailAddress("counselor@example.com")
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of(baseRecipient))
            .thenReturn(List.of(otherRecipient));
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        CareTeamMemberEntity counselor = CareTeamMemberEntity.builder()
            .userId("counselor-1")
            .role(CareTeamMemberEntity.CareTeamRole.MENTAL_HEALTH_COUNSELOR)
            .build();
        when(careTeamMemberRepository.findActiveByPatientIdAndTenantIdAndRole(
            eq(patientId), eq("tenant-1"), eq(CareTeamMemberEntity.CareTeamRole.MENTAL_HEALTH_COUNSELOR)))
            .thenReturn(List.of(counselor));

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Casey Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.getRecipients().get("EMAIL"))
            .contains("primary@example.com")
            .doesNotContain("counselor@example.com");
    }

    @Test
    @DisplayName("Should fall back to default recipients on resolution error")
    void shouldFallbackToDefaultsOnResolutionError() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-5")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(6)
            .maxScore(27)
            .severity("mild")
            .interpretation("Mild depression")
            .positiveScreen(false)
            .requiresFollowup(false)
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenThrow(new RuntimeException("db down"));

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Jamie Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.getRecipients()).containsEntry("EMAIL", "default@example.com");
        assertThat(request.getRecipients()).containsEntry("SMS", "+15550001111");
        assertThat(request.shouldSendSms()).isFalse();
        assertThat(request.getSeverity()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should format hyphenated severity labels")
    void shouldFormatHyphenatedSeverityLabels() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-6")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(20)
            .maxScore(27)
            .severity("moderately-severe")
            .interpretation("Moderately severe depression")
            .positiveScreen(true)
            .requiresFollowup(true)
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of());
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true, "sms", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Jordan Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) captor.getValue().getTemplateVariables();
        assertThat(variables.get("severityLabel")).isEqualTo("Moderately-Severe");
    }

    @Test
    @DisplayName("Should include assessment metadata and action flags")
    void shouldIncludeAssessmentMetadataAndActionFlags() {
        UUID patientId = UUID.randomUUID();
        Instant assessmentDate = Instant.parse("2024-05-01T10:11:12Z");
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-6b")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(18)
            .maxScore(27)
            .severity("moderately-severe")
            .interpretation("Moderately severe depression")
            .positiveScreen(true)
            .requiresFollowup(true)
            .assessedBy("clinician-1")
            .assessmentDate(assessmentDate)
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of());
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true, "sms", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Jordan Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = (Map<String, Object>) captor.getValue().getTemplateVariables();

        assertThat(variables.get("assessedBy")).isEqualTo("clinician-1");
        assertThat(variables.get("assessmentDate")).isNotNull();
        assertThat(variables.get("requiresImmediateAction")).isEqualTo(true);
        assertThat(variables.get("actionUrl").toString()).contains(patientId.toString());
    }

    @Test
    @DisplayName("Should notify on mild severity even without positive screen")
    void shouldNotifyOnMildSeverityWithoutPositiveScreen() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-6c")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(4)
            .maxScore(27)
            .severity("mild")
            .interpretation("Mild depression")
            .positiveScreen(false)
            .requiresFollowup(false)
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of());
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Taylor Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isFalse();
    }

    @Test
    @DisplayName("Should join multiple email and SMS recipients")
    void shouldJoinMultipleEmailAndSmsRecipients() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-10")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(22)
            .maxScore(27)
            .severity("severe")
            .interpretation("Severe depression")
            .positiveScreen(true)
            .requiresFollowup(true)
            .build();

        NotificationRecipient emailOne = NotificationRecipient.builder()
            .emailAddress("a@example.com")
            .build();
        NotificationRecipient emailTwo = NotificationRecipient.builder()
            .emailAddress("b@example.com")
            .build();
        NotificationRecipient smsOne = NotificationRecipient.builder()
            .phoneNumber("+15550001")
            .build();
        NotificationRecipient smsTwo = NotificationRecipient.builder()
            .phoneNumber("+15550002")
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of(emailOne, emailTwo));
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of(smsOne, smsTwo));
        when(careTeamMemberRepository.findActiveByPatientIdAndTenantIdAndRole(
            eq(patientId), eq("tenant-1"), eq(CareTeamMemberEntity.CareTeamRole.MENTAL_HEALTH_COUNSELOR)))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true, "sms", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Sky Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.getRecipients().get("EMAIL")).isEqualTo("a@example.com,b@example.com");
        assertThat(request.getRecipients().get("SMS")).isEqualTo("+15550001,+15550002");
        assertThat(request.shouldSendSms()).isTrue();
    }

    @Test
    @DisplayName("Should notify on positive screen even if severity minimal")
    void shouldNotifyOnPositiveMinimalScreen() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-11")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(1)
            .maxScore(27)
            .severity("minimal")
            .interpretation("Minimal symptoms")
            .positiveScreen(true)
            .requiresFollowup(false)
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of());
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Parker Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        verify(notificationService).sendNotification(any(NotificationRequest.class));
    }

    @Test
    @DisplayName("Should map positive severity to medium notification level")
    void shouldMapPositiveSeverityToMedium() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-7")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(8)
            .maxScore(27)
            .severity("positive")
            .interpretation("Positive screen")
            .positiveScreen(true)
            .requiresFollowup(true)
            .build();

        NotificationRecipient emailRecipient = NotificationRecipient.builder()
            .emailAddress("team@example.com")
            .build();
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of(emailRecipient));
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Jordan Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.getSeverity()).isEqualTo("MEDIUM");
        assertThat(request.shouldSendEmail()).isTrue();
        assertThat(request.shouldSendSms()).isFalse();
    }

    @Test
    @DisplayName("Should fall back to default recipients when resolved contacts are blank")
    void shouldFallbackToDefaultsWhenResolvedContactsBlank() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-8")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(6)
            .maxScore(27)
            .severity("mild")
            .interpretation("Mild depression")
            .positiveScreen(false)
            .requiresFollowup(false)
            .build();

        NotificationRecipient blankEmail = NotificationRecipient.builder()
            .emailAddress(" ")
            .build();
        NotificationRecipient blankPhone = NotificationRecipient.builder()
            .phoneNumber(" ")
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of(blankEmail));
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of(blankPhone));

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Morgan Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.getRecipients()).containsEntry("EMAIL", "default@example.com");
        assertThat(request.getRecipients()).containsEntry("SMS", "+15550001111");
        assertThat(request.shouldSendSms()).isFalse();
    }

    @Test
    @DisplayName("Should map unknown severity to low notification level")
    void shouldMapUnknownSeverityToLow() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .id("a-9")
            .patientId(patientId)
            .type("phq-9")
            .name("PHQ-9")
            .score(9)
            .maxScore(27)
            .severity("unknown")
            .interpretation("Unknown severity")
            .positiveScreen(true)
            .requiresFollowup(true)
            .build();

        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.EMAIL), any()))
            .thenReturn(List.of());
        when(recipientResolutionService.resolveRecipients(
            any(), any(), eq(NotificationEntity.NotificationChannel.SMS), any()))
            .thenReturn(List.of());

        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(Map.of("websocket", true, "email", true))
            .allSuccessful(true)
            .build();
        when(notificationService.sendNotification(any(NotificationRequest.class))).thenReturn(status);
        when(patientNameService.getPatientName(patientId)).thenReturn("Quinn Patient");

        trigger.onAssessmentCompleted("tenant-1", assessment);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());
        NotificationRequest request = captor.getValue();

        assertThat(request.getSeverity()).isEqualTo("LOW");
        assertThat(request.shouldSendEmail()).isTrue();
    }
}
