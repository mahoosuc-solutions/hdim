package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.ClinicalAlertDTO;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Tests for ClinicalAlertNotificationTrigger
 */
@ExtendWith(MockitoExtension.class)
class ClinicalAlertNotificationTriggerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RecipientResolutionService recipientResolutionService;

    @Mock
    private PatientNameService patientNameService;

    @InjectMocks
    private ClinicalAlertNotificationTrigger trigger;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";
    private static final String ALERT_ID = "alert-789";

    @BeforeEach
    void setUp() {
        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
            .allSuccessful(true)
            .build();
        lenient().when(notificationService.sendNotification(any())).thenReturn(status);
        lenient().when(patientNameService.getPatientName(anyString())).thenReturn("John Doe");
    }

    @Test
    void shouldSendCriticalAlertWithResolvedRecipients() {
        // Given
        ClinicalAlertDTO alert = createClinicalAlert("CRITICAL");

        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);

        when(recipientResolutionService.resolveRecipients(
            eq(TENANT_ID), eq(PATIENT_ID),
            eq(NotificationEntity.NotificationChannel.EMAIL),
            any()
        )).thenReturn(List.of(recipient));

        when(recipientResolutionService.resolveRecipients(
            eq(TENANT_ID), eq(PATIENT_ID),
            eq(NotificationEntity.NotificationChannel.SMS),
            any()
        )).thenReturn(List.of(recipient));

        // When
        trigger.onAlertTriggered(TENANT_ID, alert);

        // Then
        verify(recipientResolutionService, times(2)).resolveRecipients(
            eq(TENANT_ID), eq(PATIENT_ID), any(), any()
        );
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldHandleNoRecipientsFound() {
        // Given
        ClinicalAlertDTO alert = createClinicalAlert("HIGH");

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of()); // No recipients

        // When
        trigger.onAlertTriggered(TENANT_ID, alert);

        // Then
        verify(recipientResolutionService, times(2)).resolveRecipients(any(), any(), any(), any());
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldUsePrimaryCareProviderEmail() {
        // Given
        ClinicalAlertDTO alert = createClinicalAlert("HIGH");

        NotificationRecipient primary = createRecipient("user-1", "primary@example.com", "+15555551111", true);
        NotificationRecipient secondary = createRecipient("user-2", "secondary@example.com", "+15555552222", false);

        when(recipientResolutionService.resolveRecipients(
            eq(TENANT_ID), eq(PATIENT_ID),
            eq(NotificationEntity.NotificationChannel.EMAIL),
            any()
        )).thenReturn(List.of(primary, secondary));

        // When
        trigger.onAlertTriggered(TENANT_ID, alert);

        // Then
        verify(notificationService).sendNotification(argThat(request ->
            request.getRecipients().get("EMAIL").equals("primary@example.com")
        ));
    }

    @Test
    void shouldSendAcknowledgmentForHighSeverity() {
        // Given
        ClinicalAlertDTO alert = createClinicalAlert("HIGH");
        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy("Dr. Smith");
        alert.setAcknowledgedAt(Instant.now());

        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);

        when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
            .thenReturn(List.of(recipient));

        // When
        trigger.onAlertAcknowledged(TENANT_ID, alert);

        // Then
        verify(notificationService).sendNotification(any());
    }

    @Test
    void shouldNotSendAcknowledgmentForMediumSeverity() {
        // Given
        ClinicalAlertDTO alert = createClinicalAlert("MEDIUM");
        alert.setStatus("ACKNOWLEDGED");

        // When
        trigger.onAlertAcknowledged(TENANT_ID, alert);

        // Then
        verify(notificationService, never()).sendNotification(any());
        verify(recipientResolutionService, never()).resolveRecipients(any(), any(), any(), any());
    }

    private ClinicalAlertDTO createClinicalAlert(String severity) {
        ClinicalAlertDTO alert = new ClinicalAlertDTO();
        alert.setId(ALERT_ID);
        alert.setPatientId(PATIENT_ID);
        alert.setSeverity(severity);
        alert.setTitle("Test Alert");
        alert.setMessage("Test alert message");
        alert.setAlertType("MENTAL_HEALTH_CRISIS");
        alert.setStatus("ACTIVE");
        alert.setTriggeredAt(Instant.now());
        return alert;
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
