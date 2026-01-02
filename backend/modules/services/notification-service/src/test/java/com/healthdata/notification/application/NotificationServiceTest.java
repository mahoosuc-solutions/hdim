package com.healthdata.notification.application;

import com.healthdata.notification.domain.model.*;
import com.healthdata.notification.domain.repository.NotificationPreferenceRepository;
import com.healthdata.notification.domain.repository.NotificationRepository;
import com.healthdata.notification.infrastructure.providers.NotificationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private TemplateService templateService;

    @Mock
    private ChannelRouter channelRouter;

    @Mock
    private NotificationProvider emailProvider;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private NotificationService notificationService;

    private static final String TENANT_ID = "TENANT001";

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationRepository,
            preferenceRepository,
            templateService,
            channelRouter
        );
    }

    @Nested
    @DisplayName("Send Notification")
    class SendNotificationTests {

        @Test
        @DisplayName("should create notification with EMAIL channel")
        void shouldCreateNotificationWithEmailChannel() {
            // Given
            NotificationService.SendNotificationRequest request = NotificationService.SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId("user123")
                .recipientEmail("user@example.com")
                .channel(NotificationChannel.EMAIL)
                .subject("Test Subject")
                .body("Test Body")
                .build();

            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(TENANT_ID, "user123", NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
            when(channelRouter.getProvider(NotificationChannel.EMAIL)).thenReturn(emailProvider);
            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            Notification result = notificationService.sendNotification(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getRecipientEmail()).isEqualTo("user@example.com");

            verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());
        }

        @Test
        @DisplayName("should create notification with IN_APP channel")
        void shouldCreateNotificationWithInAppChannel() {
            // Given
            NotificationService.SendNotificationRequest request = NotificationService.SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId("user123")
                .channel(NotificationChannel.IN_APP)
                .subject("In-App Title")
                .body("In-App Body")
                .build();

            NotificationProvider inAppProvider = mock(NotificationProvider.class);
            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(TENANT_ID, "user123", NotificationChannel.IN_APP))
                .thenReturn(Optional.empty());
            when(channelRouter.getProvider(NotificationChannel.IN_APP)).thenReturn(inAppProvider);
            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            Notification result = notificationService.sendNotification(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        }

        @Test
        @DisplayName("should block notification when user preference disables channel")
        void shouldBlockNotificationWhenPreferenceDisabled() {
            // Given
            NotificationService.SendNotificationRequest request = NotificationService.SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId("user123")
                .recipientEmail("user@example.com")
                .channel(NotificationChannel.EMAIL)
                .subject("Test Subject")
                .body("Test Body")
                .build();

            NotificationPreference disabledPreference = NotificationPreference.builder()
                .tenantId(TENANT_ID)
                .userId("user123")
                .channel(NotificationChannel.EMAIL)
                .enabled(false)
                .build();

            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(TENANT_ID, "user123", NotificationChannel.EMAIL))
                .thenReturn(Optional.of(disabledPreference));

            // When
            Notification result = notificationService.sendNotification(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
            assertThat(result.getErrorMessage()).contains("preferences");
        }

        @Test
        @DisplayName("should schedule notification for future delivery")
        void shouldScheduleNotificationForFutureDelivery() {
            // Given
            Instant futureTime = Instant.now().plusSeconds(3600);
            NotificationService.SendNotificationRequest request = NotificationService.SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId("user123")
                .recipientEmail("user@example.com")
                .channel(NotificationChannel.EMAIL)
                .subject("Scheduled Subject")
                .body("Scheduled Body")
                .scheduledAt(futureTime)
                .build();

            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(TENANT_ID, "user123", NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            Notification result = notificationService.sendNotification(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getScheduledAt()).isEqualTo(futureTime);
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.PENDING);

            // Should NOT trigger async send for future-scheduled notifications
            verify(channelRouter, never()).getProvider(any());
        }
    }

    @Nested
    @DisplayName("Get Notification")
    class GetNotificationTests {

        @Test
        @DisplayName("should return notification when found")
        void shouldReturnNotificationWhenFound() {
            // Given
            UUID notificationId = UUID.randomUUID();
            Notification notification = Notification.builder()
                .id(notificationId)
                .tenantId(TENANT_ID)
                .recipientId("user123")
                .channel(NotificationChannel.EMAIL)
                .subject("Test")
                .body("Test body")
                .status(NotificationStatus.SENT)
                .build();

            when(notificationRepository.findByIdAndTenantId(notificationId, TENANT_ID))
                .thenReturn(Optional.of(notification));

            // When
            Optional<Notification> result = notificationService.getNotification(notificationId, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(notificationId);
            assertThat(result.get().getStatus()).isEqualTo(NotificationStatus.SENT);
        }

        @Test
        @DisplayName("should return empty when notification not found")
        void shouldReturnEmptyWhenNotificationNotFound() {
            // Given
            UUID notificationId = UUID.randomUUID();
            when(notificationRepository.findByIdAndTenantId(notificationId, TENANT_ID))
                .thenReturn(Optional.empty());

            // When
            Optional<Notification> result = notificationService.getNotification(notificationId, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Bulk Notifications")
    class BulkNotificationTests {

        @Test
        @DisplayName("should send multiple notifications")
        void shouldSendMultipleNotifications() {
            // Given
            NotificationService.SendNotificationRequest request1 = NotificationService.SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId("user1")
                .recipientEmail("user1@example.com")
                .channel(NotificationChannel.EMAIL)
                .subject("Test 1")
                .body("Body 1")
                .build();

            NotificationService.SendNotificationRequest request2 = NotificationService.SendNotificationRequest.builder()
                .tenantId(TENANT_ID)
                .recipientId("user2")
                .recipientEmail("user2@example.com")
                .channel(NotificationChannel.EMAIL)
                .subject("Test 2")
                .body("Body 2")
                .build();

            when(preferenceRepository.findByTenantIdAndUserIdAndChannel(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());
            when(channelRouter.getProvider(NotificationChannel.EMAIL)).thenReturn(emailProvider);
            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            var results = notificationService.sendBulkNotifications(java.util.List.of(request1, request2));

            // Then
            assertThat(results).hasSize(2);
            verify(notificationRepository, atLeast(2)).save(any(Notification.class));
        }
    }
}
