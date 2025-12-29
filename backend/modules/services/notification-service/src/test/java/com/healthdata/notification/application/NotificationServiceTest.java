package com.healthdata.notification.application;

import com.healthdata.notification.api.v1.dto.NotificationResponse;
import com.healthdata.notification.api.v1.dto.SendNotificationRequest;
import com.healthdata.notification.domain.model.Notification;
import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationStatus;
import com.healthdata.notification.domain.repository.NotificationPreferenceRepository;
import com.healthdata.notification.domain.repository.NotificationRepository;
import com.healthdata.notification.domain.repository.NotificationTemplateRepository;
import com.healthdata.notification.infrastructure.providers.EmailProvider;
import com.healthdata.notification.infrastructure.providers.PushProvider;
import com.healthdata.notification.infrastructure.providers.SmsProvider;
import com.healthdata.notification.infrastructure.websocket.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private EmailProvider emailProvider;

    @Mock
    private SmsProvider smsProvider;

    @Mock
    private PushProvider pushProvider;

    @Mock
    private WebSocketNotificationService webSocketService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private NotificationService notificationService;

    private static final String TENANT_ID = "TENANT001";

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationRepository,
            templateRepository,
            preferenceRepository,
            emailProvider,
            Optional.of(smsProvider),
            Optional.of(pushProvider),
            webSocketService
        );
    }

    @Nested
    @DisplayName("Send Notification")
    class SendNotificationTests {

        @Test
        @DisplayName("should create notification with EMAIL channel")
        void shouldCreateNotificationWithEmailChannel() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId("user123")
                .recipientEmail("user@example.com")
                .channel(NotificationChannel.EMAIL)
                .subject("Test Subject")
                .body("Test Body")
                .checkPreferences(false)
                .build();

            when(emailProvider.send(anyString(), anyString(), anyString())).thenReturn("email-123");
            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            NotificationResponse response = notificationService.sendNotification(request, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getChannel()).isEqualTo(NotificationChannel.EMAIL);

            verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());
            Notification saved = notificationCaptor.getAllValues().get(0);
            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getRecipientEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("should create notification with SMS channel")
        void shouldCreateNotificationWithSmsChannel() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId("user123")
                .recipientPhone("+15551234567")
                .channel(NotificationChannel.SMS)
                .body("Test SMS message")
                .checkPreferences(false)
                .build();

            when(smsProvider.isAvailable()).thenReturn(true);
            when(smsProvider.send(anyString(), anyString())).thenReturn("sms-123");
            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            NotificationResponse response = notificationService.sendNotification(request, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getChannel()).isEqualTo(NotificationChannel.SMS);

            verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());
            Notification saved = notificationCaptor.getAllValues().get(0);
            assertThat(saved.getRecipientPhone()).isEqualTo("+15551234567");
        }

        @Test
        @DisplayName("should create notification with PUSH channel")
        void shouldCreateNotificationWithPushChannel() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId("user123")
                .deviceToken("fcm-token-abc123")
                .channel(NotificationChannel.PUSH)
                .subject("Push Title")
                .body("Push Body")
                .checkPreferences(false)
                .build();

            when(pushProvider.isAvailable()).thenReturn(true);
            when(pushProvider.send(anyString(), anyString(), anyString())).thenReturn("push-123");
            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            NotificationResponse response = notificationService.sendNotification(request, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getChannel()).isEqualTo(NotificationChannel.PUSH);

            verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());
            Notification saved = notificationCaptor.getAllValues().get(0);
            assertThat(saved.getDeviceToken()).isEqualTo("fcm-token-abc123");
        }

        @Test
        @DisplayName("should create notification with IN_APP channel")
        void shouldCreateNotificationWithInAppChannel() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId("user123")
                .channel(NotificationChannel.IN_APP)
                .subject("In-App Title")
                .body("In-App Body")
                .checkPreferences(false)
                .build();

            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            NotificationResponse response = notificationService.sendNotification(request, TENANT_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getChannel()).isEqualTo(NotificationChannel.IN_APP);

            verify(notificationRepository, atLeastOnce()).save(notificationCaptor.capture());
        }

        @Test
        @DisplayName("should push IN_APP notification via WebSocket")
        void shouldPushInAppNotificationViaWebSocket() {
            // Given
            SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId("user123")
                .channel(NotificationChannel.IN_APP)
                .subject("WebSocket Test")
                .body("Real-time notification")
                .checkPreferences(false)
                .build();

            when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    if (n.getId() == null) {
                        n.setId(UUID.randomUUID());
                    }
                    return n;
                });

            // When
            notificationService.sendNotification(request, TENANT_ID);

            // Then - verify WebSocket push was attempted
            verify(webSocketService, atLeastOnce()).pushToUser(eq("user123"), any(NotificationResponse.class));
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
            Optional<NotificationResponse> result = notificationService.getNotification(notificationId, TENANT_ID);

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
            Optional<NotificationResponse> result = notificationService.getNotification(notificationId, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Provider Unavailable Handling")
    class ProviderUnavailableTests {

        @Test
        @DisplayName("should handle missing SMS provider gracefully")
        void shouldHandleMissingSmsProvider() {
            // Create service without SMS provider
            NotificationService serviceWithoutSms = new NotificationService(
                notificationRepository,
                templateRepository,
                preferenceRepository,
                emailProvider,
                Optional.empty(),  // No SMS provider
                Optional.of(pushProvider),
                webSocketService
            );

            // Service should still be instantiable - provider availability checked at send time
            assertThat(serviceWithoutSms).isNotNull();
        }

        @Test
        @DisplayName("should handle missing Push provider gracefully")
        void shouldHandleMissingPushProvider() {
            // Create service without Push provider
            NotificationService serviceWithoutPush = new NotificationService(
                notificationRepository,
                templateRepository,
                preferenceRepository,
                emailProvider,
                Optional.of(smsProvider),
                Optional.empty(),  // No Push provider
                webSocketService
            );

            // Service should still be instantiable - provider availability checked at send time
            assertThat(serviceWithoutPush).isNotNull();
        }
    }
}
