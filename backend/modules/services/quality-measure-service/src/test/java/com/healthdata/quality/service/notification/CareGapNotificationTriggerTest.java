package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.CareGapDTO;
import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.CareTeamMemberEntity;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.service.NotificationService;
import com.healthdata.quality.service.PatientNameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for CareGapNotificationTrigger
 */
@ExtendWith(MockitoExtension.class)
class CareGapNotificationTriggerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RecipientResolutionService recipientResolutionService;

    @Mock
    private PatientNameService patientNameService;

    @InjectMocks
    private CareGapNotificationTrigger trigger;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void setUp() {
        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .allSuccessful(true)
            .build();
        lenient().when(notificationService.sendNotification(any())).thenReturn(status);
        lenient().when(patientNameService.getPatientName(any(UUID.class))).thenReturn("Jane Smith");
    }

    @Test
    void shouldSendHighPriorityCareGapWithResolvedRecipients() {
        // Given
        CareGapDTO careGap = createCareGap("HIGH");

        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of(recipient));

        // When
        trigger.onCareGapIdentified(TENANT_ID, careGap);

        // Then
        verify(recipientResolutionService, times(2)).resolveRecipients(
            eq(TENANT_ID), eq(PATIENT_ID), any(), any()
        );
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldNotSendLowPriorityGapWithDistantDueDate() {
        // Given
        CareGapDTO careGap = createCareGap("LOW");
        careGap.setDueDate(Instant.now().plusSeconds(30 * 24 * 60 * 60)); // 30 days away

        // When
        trigger.onCareGapIdentified(TENANT_ID, careGap);

        // Then
        verify(recipientResolutionService, never()).resolveRecipients(any(), any(), any(), any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void shouldSendCareGapAddressedNotification() {
        // Given
        CareGapDTO careGap = createCareGap("MEDIUM");
        careGap.setStatus("ADDRESSED");
        careGap.setAddressedBy("Dr. Jones");
        careGap.setAddressedDate(Instant.now());

        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of(recipient));

        // When
        trigger.onCareGapAddressed(TENANT_ID, careGap);

        // Then
        verify(recipientResolutionService, times(2)).resolveRecipients(any(), any(), any(), any());
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldHandleMultipleRecipients() {
        // Given
        CareGapDTO careGap = createCareGap("CRITICAL");

        NotificationRecipient primary = createRecipient("user-1", "primary@example.com", "+15555551111", true);
        NotificationRecipient secondary = createRecipient("user-2", "secondary@example.com", "+15555552222", false);

        when(recipientResolutionService.resolveRecipients(
            eq(TENANT_ID), eq(PATIENT_ID),
            eq(NotificationEntity.NotificationChannel.EMAIL),
            any()
        )).thenReturn(List.of(primary, secondary));

        // When
        trigger.onCareGapIdentified(TENANT_ID, careGap);

        // Then - should use primary's email
        verify(notificationService).sendNotification(argThat(request ->
            request.getRecipients().get("EMAIL").equals("primary@example.com")
        ));
    }

    @Test
    void shouldHandlePartialFailureOnIdentification() {
        CareGapDTO careGap = createCareGap("HIGH");
        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .channelStatus(java.util.Map.of("websocket", true, "email", false))
            .allSuccessful(false)
            .build();
        when(notificationService.sendNotification(any())).thenReturn(status);
        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of());

        trigger.onCareGapIdentified(TENANT_ID, careGap);

        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldHandleExceptionOnIdentification() {
        CareGapDTO careGap = createCareGap("HIGH");
        when(notificationService.sendNotification(any()))
            .thenThrow(new RuntimeException("notify failed"));
        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of());

        trigger.onCareGapIdentified(TENANT_ID, careGap);

        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldNotifyLowPriorityGapDueSoon() {
        CareGapDTO careGap = createCareGap("LOW");
        careGap.setDueDate(Instant.now().plusSeconds(2 * 24 * 60 * 60));

        boolean shouldNotify = (boolean) ReflectionTestUtils.invokeMethod(
            trigger, "shouldNotifyOnIdentification", careGap);

        assertThat(shouldNotify).isTrue();
    }

    @Test
    void shouldSkipLowPriorityGapWithoutDueDate() {
        CareGapDTO careGap = createCareGap("LOW");
        careGap.setDueDate(null);

        boolean shouldNotify = (boolean) ReflectionTestUtils.invokeMethod(
            trigger, "shouldNotifyOnIdentification", careGap);

        assertThat(shouldNotify).isFalse();
    }

    @Test
    void shouldSkipMediumPriorityGapWithDistantDueDate() {
        CareGapDTO careGap = createCareGap("MEDIUM");
        careGap.setDueDate(Instant.now().plusSeconds(40L * 24 * 60 * 60));

        boolean shouldNotify = (boolean) ReflectionTestUtils.invokeMethod(
            trigger, "shouldNotifyOnIdentification", careGap);

        assertThat(shouldNotify).isFalse();
    }

    @Test
    void shouldNotifyMediumPriorityGapWithoutDueDate() {
        CareGapDTO careGap = createCareGap("MEDIUM");
        careGap.setDueDate(null);

        boolean shouldNotify = (boolean) ReflectionTestUtils.invokeMethod(
            trigger, "shouldNotifyOnIdentification", careGap);

        assertThat(shouldNotify).isTrue();
    }

    private CareGapDTO createCareGap(String priority) {
        CareGapDTO careGap = new CareGapDTO();
        careGap.setId("gap-001");
        careGap.setPatientId(PATIENT_ID);
        careGap.setPriority(priority);
        careGap.setTitle("Diabetes Annual Eye Exam");
        careGap.setDescription("Patient is due for annual diabetic eye exam");
        careGap.setCategory("PREVENTIVE_CARE");
        careGap.setGapType("SCREENING");
        careGap.setStatus("OPEN");
        careGap.setQualityMeasure("CMS131");
        careGap.setIdentifiedDate(Instant.now());
        return careGap;
    }

    private NotificationRecipient createRecipient(String userId, String email, String phone, boolean isPrimary) {
        return NotificationRecipient.builder()
            .userId(userId)
            .emailAddress(email)
            .phoneNumber(phone)
            .isPrimary(isPrimary)
            .careTeamRole(CareTeamMemberEntity.CareTeamRole.PRIMARY_CARE_PHYSICIAN)
            .enabledChannels(Set.of(
                NotificationEntity.NotificationChannel.EMAIL,
                NotificationEntity.NotificationChannel.SMS
            ))
            .severityThreshold(NotificationEntity.NotificationSeverity.MEDIUM)
            .build();
    }
}
