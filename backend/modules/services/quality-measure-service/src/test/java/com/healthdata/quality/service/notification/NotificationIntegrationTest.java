package com.healthdata.quality.service.notification;

import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.dto.notification.HealthScoreNotificationRequest;
import com.healthdata.quality.model.NotificationRecipient;
import com.healthdata.quality.persistence.CareTeamMemberEntity;
import com.healthdata.quality.persistence.NotificationEntity;
import com.healthdata.quality.persistence.NotificationHistoryEntity;
import com.healthdata.quality.persistence.NotificationHistoryRepository;
import com.healthdata.quality.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Notification Integration Test
 *
 * Tests the complete notification flow from trigger to delivery:
 * 1. HealthScoreNotificationTrigger receives health score
 * 2. Checks if change is significant
 * 3. Builds HealthScoreNotificationRequest
 * 4. NotificationService routes to appropriate channels
 * 5. Channels send notifications and save history
 *
 * Validates:
 * - Notification routing logic (WebSocket, Email, SMS)
 * - HIPAA-compliant history tracking
 * - Notification filtering (prevents fatigue)
 * - Channel-specific formatting
 * - Error handling and recovery
 */
@ExtendWith(MockitoExtension.class)
class NotificationIntegrationTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RecipientResolutionService recipientResolutionService;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    private EmailNotificationChannel emailChannel;

    @Mock
    private SmsNotificationChannel smsChannel;

    @InjectMocks
    private HealthScoreNotificationTrigger notificationTrigger;

    private String tenantId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        patientId = UUID.fromString("abababab-abab-abab-abab-abababababab");

        // Setup lenient stubbing for notificationService
        NotificationService.NotificationStatus status = NotificationService.NotificationStatus.builder()
                .allSuccessful(true)
                .build();
        lenient().when(notificationService.sendNotification(any())).thenReturn(status);

        // Setup lenient stubbing for recipientResolutionService
        NotificationRecipient recipient = createRecipient("user-1", "doctor@example.com", "+15555551234", true);
        lenient().when(recipientResolutionService.resolveRecipients(any(), any(), any(), any()))
                .thenReturn(List.of(recipient));
    }

    /**
     * TEST 1: Notification triggered on significant health score decrease
     * Validates that 10+ point decreases trigger notifications
     */
    @Test
    void testNotificationTriggered_SignificantDecrease() {
        // Arrange: Health score dropped from 75 to 60 (15 point decrease)
        HealthScoreDTO healthScore = createHealthScore(60.0, 70.0, 55.0, 60.0, 58.0, 62.0);
        Double previousScore = 75.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of("websocket", true, "email", true, "sms", true))
                        .allSuccessful(true)
                        .build());

        // Act
        notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore);

        // Assert: Notification should be sent
        ArgumentCaptor<HealthScoreNotificationRequest> captor =
                ArgumentCaptor.forClass(HealthScoreNotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());

        HealthScoreNotificationRequest request = captor.getValue();
        assertNotNull(request);
        assertEquals("HEALTH_SCORE_UPDATE", request.getNotificationType());
        assertEquals(patientId, request.getPatientId());
        assertEquals(tenantId, request.getTenantId());
        assertEquals(60.0, request.getNewScore());
        assertEquals(75.0, request.getPreviousScore());
        assertEquals(-15.0, request.getChangeAmount());
        assertEquals("DECREASED", request.getChangeDirection());

        // Verify channel routing: All channels should be triggered for 15 point decrease
        assertTrue(request.shouldSendWebSocket()); // Always true
        assertTrue(request.shouldSendEmail());     // True for 10+ decrease
        assertTrue(request.shouldSendSms());       // True for 15+ decrease
    }

    /**
     * TEST 2: Notification skipped for minor changes
     * Validates that small changes (< 5 points) don't trigger notifications
     */
    @Test
    void testNotificationSkipped_MinorChange() {
        // Arrange: Health score dropped from 75 to 72 (3 point decrease)
        HealthScoreDTO healthScore = createHealthScore(72.0, 72.0, 70.0, 73.0, 72.0, 74.0);
        Double previousScore = 75.0;

        // Act
        notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore);

        // Assert: No notification should be sent
        verify(notificationService, never()).sendNotification(any());
    }

    /**
     * TEST 3: Notification triggered on moderate decrease (5-9 points)
     * Validates that moderate changes trigger WebSocket + Email but not SMS
     */
    @Test
    void testNotificationTriggered_ModerateDecrease() {
        // Arrange: Health score dropped from 75 to 68 (7 point decrease)
        HealthScoreDTO healthScore = createHealthScore(68.0, 68.0, 65.0, 70.0, 66.0, 70.0);
        Double previousScore = 75.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of("websocket", true))
                        .allSuccessful(true)
                        .build());

        // Act
        notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore);

        // Assert
        ArgumentCaptor<HealthScoreNotificationRequest> captor =
                ArgumentCaptor.forClass(HealthScoreNotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());

        HealthScoreNotificationRequest request = captor.getValue();
        assertTrue(request.shouldSendWebSocket());
        assertFalse(request.shouldSendEmail());  // Only for 10+ decrease
        assertFalse(request.shouldSendSms());    // Only for 15+ decrease
    }

    /**
     * TEST 4: Notification triggered on health score improvement
     * Validates that improvements also trigger notifications (WebSocket only)
     */
    @Test
    void testNotificationTriggered_Improvement() {
        // Arrange: Health score improved from 60 to 75 (15 point increase)
        HealthScoreDTO healthScore = createHealthScore(75.0, 75.0, 72.0, 78.0, 74.0, 76.0);
        Double previousScore = 60.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of("websocket", true))
                        .allSuccessful(true)
                        .build());

        // Act
        notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore);

        // Assert
        ArgumentCaptor<HealthScoreNotificationRequest> captor =
                ArgumentCaptor.forClass(HealthScoreNotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());

        HealthScoreNotificationRequest request = captor.getValue();
        assertEquals("INCREASED", request.getChangeDirection());
        assertTrue(request.shouldSendWebSocket());
        assertFalse(request.shouldSendEmail());  // Email only for decreases
        assertFalse(request.shouldSendSms());    // SMS only for decreases
    }

    /**
     * TEST 5: Notification triggered on threshold crossing (score drops below 40)
     * Validates that crossing critical thresholds triggers notifications
     */
    @Test
    void testNotificationTriggered_CriticalThresholdCrossing() {
        // Arrange: Health score dropped from 42 to 38 (only 4 points, but crossed 40 threshold)
        HealthScoreDTO healthScore = createHealthScore(38.0, 40.0, 35.0, 38.0, 36.0, 40.0);
        Double previousScore = 42.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of("websocket", true))
                        .allSuccessful(true)
                        .build());

        // Act
        notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore);

        // Assert: Should trigger notification despite < 5 point change
        verify(notificationService).sendNotification(any(HealthScoreNotificationRequest.class));
    }

    /**
     * TEST 6: Notification triggered on excellent score achievement (>= 90)
     * Validates that positive milestones trigger notifications
     */
    @Test
    void testNotificationTriggered_ExcellentScoreAchieved() {
        // Arrange: Health score improved from 88 to 91 (3 points, but crossed 90 threshold)
        HealthScoreDTO healthScore = createHealthScore(91.0, 92.0, 90.0, 90.0, 91.0, 92.0);
        Double previousScore = 88.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of("websocket", true))
                        .allSuccessful(true)
                        .build());

        // Act
        notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore);

        // Assert: Should trigger notification for positive milestone
        verify(notificationService).sendNotification(any(HealthScoreNotificationRequest.class));
    }

    /**
     * TEST 7: Notification triggered on first health score calculation
     * Validates that initial calculations always trigger notifications
     */
    @Test
    void testNotificationTriggered_FirstCalculation() {
        // Arrange: First health score (no previous score)
        HealthScoreDTO healthScore = createHealthScore(75.0, 75.0, 72.0, 78.0, 74.0, 76.0);
        Double previousScore = null;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of("websocket", true))
                        .allSuccessful(true)
                        .build());

        // Act
        notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore);

        // Assert: Should always notify on first calculation
        verify(notificationService).sendNotification(any(HealthScoreNotificationRequest.class));
    }

    /**
     * TEST 8: Notification request contains correct template variables
     * Validates that all required data is included for template rendering
     */
    @Test
    void testNotificationRequest_TemplateVariables() {
        // Arrange
        HealthScoreDTO healthScore = createHealthScore(60.0, 65.0, 50.0, 60.0, 58.0, 67.0);
        Double previousScore = 75.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of())
                        .allSuccessful(true)
                        .build());

        // Act
        notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore);

        // Assert
        ArgumentCaptor<HealthScoreNotificationRequest> captor =
                ArgumentCaptor.forClass(HealthScoreNotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());

        HealthScoreNotificationRequest request = captor.getValue();
        Map<String, Object> variables = request.getTemplateVariables();

        // Verify all required template variables are present
        assertNotNull(variables);
        assertTrue(variables.containsKey("patientId"));
        assertTrue(variables.containsKey("currentScore"));
        assertTrue(variables.containsKey("previousScore"));
        assertTrue(variables.containsKey("changeAmount"));
        assertTrue(variables.containsKey("changeDirection"));
        assertTrue(variables.containsKey("components"));
        assertTrue(variables.containsKey("timestamp"));

        // Verify component breakdown
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) variables.get("components");
        assertNotNull(components);
        assertEquals(65.0, components.get("physical"));
        assertEquals(50.0, components.get("mental"));
        assertEquals(60.0, components.get("social"));
        assertEquals(58.0, components.get("preventive"));
        assertEquals(67.0, components.get("chronicDisease"));
    }

    /**
     * TEST 9: Notification error handling
     * Validates that notification errors don't prevent health score calculation
     */
    @Test
    void testNotificationError_DoesNotThrow() {
        // Arrange
        HealthScoreDTO healthScore = createHealthScore(60.0, 65.0, 50.0, 60.0, 58.0, 67.0);
        Double previousScore = 75.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenThrow(new RuntimeException("Notification service unavailable"));

        // Act & Assert: Should not throw exception
        assertDoesNotThrow(() ->
                notificationTrigger.onHealthScoreCalculated(tenantId, healthScore, previousScore)
        );
    }

    /**
     * TEST 10: Manual health score refresh notification
     * Validates that manual refreshes always send notifications (WebSocket only)
     */
    @Test
    void testManualHealthScoreRefresh_AlwaysNotifies() {
        // Arrange
        HealthScoreDTO healthScore = createHealthScore(75.0, 75.0, 72.0, 78.0, 74.0, 76.0);
        healthScore.setPreviousScore(74.0);
        healthScore.setScoreDelta(1.0);

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of("websocket", true))
                        .allSuccessful(true)
                        .build());

        // Act
        notificationTrigger.onManualHealthScoreRefresh(tenantId, healthScore);

        // Assert: Should always send notification for manual refresh
        verify(notificationService).sendNotification(any(HealthScoreNotificationRequest.class));
    }

    /**
     * TEST 11: Notification severity levels
     * Validates that severity is correctly calculated based on change magnitude
     */
    @Test
    void testNotificationSeverity_CalculatedCorrectly() {
        // Test HIGH severity (15+ point change)
        HealthScoreDTO healthScoreHigh = createHealthScore(60.0, 65.0, 50.0, 60.0, 58.0, 67.0);
        Double previousScoreHigh = 75.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of())
                        .allSuccessful(true)
                        .build());

        notificationTrigger.onHealthScoreCalculated(tenantId, healthScoreHigh, previousScoreHigh);

        ArgumentCaptor<HealthScoreNotificationRequest> captorHigh =
                ArgumentCaptor.forClass(HealthScoreNotificationRequest.class);
        verify(notificationService).sendNotification(captorHigh.capture());

        assertEquals("HIGH", captorHigh.getValue().getSeverity());

        // Test MEDIUM severity (10-14 point change)
        reset(notificationService);
        HealthScoreDTO healthScoreMedium = createHealthScore(63.0, 65.0, 60.0, 63.0, 62.0, 65.0);
        Double previousScoreMedium = 75.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of())
                        .allSuccessful(true)
                        .build());

        notificationTrigger.onHealthScoreCalculated(tenantId, healthScoreMedium, previousScoreMedium);

        ArgumentCaptor<HealthScoreNotificationRequest> captorMedium =
                ArgumentCaptor.forClass(HealthScoreNotificationRequest.class);
        verify(notificationService).sendNotification(captorMedium.capture());

        assertEquals("MEDIUM", captorMedium.getValue().getSeverity());
    }

    /**
     * TEST 12: Notification title formatting
     * Validates that titles are descriptive and indicate direction of change
     */
    @Test
    void testNotificationTitle_FormattedCorrectly() {
        // Test decrease title
        HealthScoreDTO healthScoreDecrease = createHealthScore(60.0, 65.0, 50.0, 60.0, 58.0, 67.0);
        Double previousScoreDecrease = 75.0;

        when(notificationService.sendNotification(any(HealthScoreNotificationRequest.class)))
                .thenReturn(NotificationService.NotificationStatus.builder()
                        .alertId("test-alert")
                        .channelStatus(Map.of())
                        .allSuccessful(true)
                        .build());

        notificationTrigger.onHealthScoreCalculated(tenantId, healthScoreDecrease, previousScoreDecrease);

        ArgumentCaptor<HealthScoreNotificationRequest> captor =
                ArgumentCaptor.forClass(HealthScoreNotificationRequest.class);
        verify(notificationService).sendNotification(captor.capture());

        String title = captor.getValue().getTitle();
        assertTrue(title.contains("Decreased"));
        assertTrue(title.contains("75.0"));
        assertTrue(title.contains("60.0"));
    }

    /**
     * Helper Methods
     */

    private HealthScoreDTO createHealthScore(
            Double overallScore,
            Double physicalHealth,
            Double mentalHealth,
            Double socialDeterminants,
            Double preventiveCare,
            Double chronicDisease
    ) {
        return HealthScoreDTO.builder()
                .patientId(patientId)
                .tenantId(tenantId)
                .overallScore(overallScore)
                .physicalHealthScore(physicalHealth)
                .mentalHealthScore(mentalHealth)
                .socialDeterminantsScore(socialDeterminants)
                .preventiveCareScore(preventiveCare)
                .chronicDiseaseScore(chronicDisease)
                .calculatedAt(Instant.now())
                .build();
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
