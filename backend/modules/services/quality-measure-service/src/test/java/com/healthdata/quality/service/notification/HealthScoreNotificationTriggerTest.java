package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.CareTeamMemberEntity;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests for HealthScoreNotificationTrigger
 */
@ExtendWith(MockitoExtension.class)
class HealthScoreNotificationTriggerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RecipientResolutionService recipientResolutionService;

    @InjectMocks
    private HealthScoreNotificationTrigger trigger;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";

    @BeforeEach
    void setUp() {
        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .allSuccessful(true)
            .build();
        lenient().when(notificationService.sendNotification(any())).thenReturn(status);
    }

    @Test
    void shouldSendNotificationForSignificantScoreChange() {
        // Given
        HealthScoreDTO healthScore = createHealthScore(75.0);
        Double previousScore = 85.0; // 10 point decrease

        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of(recipient));

        // When
        trigger.onHealthScoreCalculated(TENANT_ID, healthScore, previousScore);

        // Then
        verify(recipientResolutionService, times(2)).resolveRecipients(
            eq(TENANT_ID), eq(PATIENT_ID), any(), any()
        );
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldNotSendNotificationForMinorChange() {
        // Given
        HealthScoreDTO healthScore = createHealthScore(75.0);
        Double previousScore = 77.0; // Only 2 point change

        // When
        trigger.onHealthScoreCalculated(TENANT_ID, healthScore, previousScore);

        // Then
        verify(recipientResolutionService, never()).resolveRecipients(any(), any(), any(), any());
        verify(notificationService, never()).sendNotification(any());
    }

    @Test
    void shouldSendNotificationForFirstCalculation() {
        // Given
        HealthScoreDTO healthScore = createHealthScore(80.0);
        Double previousScore = null; // First calculation

        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of(recipient));

        // When
        trigger.onHealthScoreCalculated(TENANT_ID, healthScore, previousScore);

        // Then
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldSendNotificationWhenScoreCrossesThreshold() {
        // Given
        HealthScoreDTO healthScore = createHealthScore(38.0); // Below 40 threshold
        Double previousScore = 42.0; // Was above 40

        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of(recipient));

        // When
        trigger.onHealthScoreCalculated(TENANT_ID, healthScore, previousScore);

        // Then
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldUsePrimaryCareProviderContact() {
        // Given
        HealthScoreDTO healthScore = createHealthScore(70.0);
        Double previousScore = 80.0;

        NotificationRecipient primary = createRecipient("user-1", "primary@example.com", "+15555551111", true);
        NotificationRecipient secondary = createRecipient("user-2", "secondary@example.com", "+15555552222", false);

        when(recipientResolutionService.resolveRecipients(
            eq(TENANT_ID), eq(PATIENT_ID),
            eq(NotificationEntity.NotificationChannel.EMAIL),
            any()
        )).thenReturn(List.of(primary, secondary));

        // When
        trigger.onHealthScoreCalculated(TENANT_ID, healthScore, previousScore);

        // Then - should use primary's email
        verify(notificationService).sendNotification(argThat(request ->
            request.getRecipients().get("EMAIL").equals("primary@example.com")
        ));
    }

    @Test
    void shouldHandleManualRefresh() {
        // Given
        HealthScoreDTO healthScore = createHealthScore(85.0);
        healthScore.setPreviousScore(80.0);
        healthScore.setScoreDelta(5.0);

        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of(recipient));

        // When
        trigger.onManualHealthScoreRefresh(TENANT_ID, healthScore);

        // Then
        verify(recipientResolutionService, times(2)).resolveRecipients(any(), any(), any(), any());
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldHandleNoRecipientsFound() {
        // Given
        HealthScoreDTO healthScore = createHealthScore(70.0);
        Double previousScore = 80.0;

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of()); // No recipients

        // When
        trigger.onHealthScoreCalculated(TENANT_ID, healthScore, previousScore);

        // Then
        verify(recipientResolutionService, times(2)).resolveRecipients(any(), any(), any(), any());
        verify(notificationService).sendNotification(any());
    }

    private HealthScoreDTO createHealthScore(Double score) {
        HealthScoreDTO healthScore = new HealthScoreDTO();
        healthScore.setPatientId(PATIENT_ID);
        healthScore.setOverallScore(score);
        healthScore.setPhysicalHealthScore(score);
        healthScore.setMentalHealthScore(score);
        healthScore.setPreventiveCareScore(score);
        return healthScore;
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
