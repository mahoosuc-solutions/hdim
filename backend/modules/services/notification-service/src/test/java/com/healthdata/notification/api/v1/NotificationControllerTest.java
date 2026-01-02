package com.healthdata.notification.api.v1;

import com.healthdata.notification.api.v1.dto.*;
import com.healthdata.notification.application.NotificationService;
import com.healthdata.notification.domain.model.Notification;
import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPriority;
import com.healthdata.notification.domain.model.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Tests")
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Authentication authentication;

    private NotificationController controller;

    private static final String TENANT_ID = "TENANT001";
    private static final String USER_NAME = "testuser";

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
        lenient().when(authentication.getName()).thenReturn(USER_NAME);
    }

    @Nested
    @DisplayName("Send Notification Tests")
    class SendNotificationTests {

        @Test
        @DisplayName("should send email notification successfully")
        void sendEmailNotificationSuccessfully() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest();
            request.setRecipientId("user123");
            request.setRecipientEmail("user@example.com");
            request.setChannel(NotificationChannel.EMAIL);
            request.setSubject("Test Subject");
            request.setBody("Test Body");
            request.setPriority(NotificationPriority.NORMAL);

            Notification notification = createNotification(NotificationChannel.EMAIL, NotificationStatus.SENT);
            when(notificationService.sendNotification(any(NotificationService.SendNotificationRequest.class)))
                .thenReturn(notification);

            // When
            ResponseEntity<NotificationResponse> response = controller.sendNotification(
                request, TENANT_ID, authentication);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getChannel()).isEqualTo(NotificationChannel.EMAIL);

            ArgumentCaptor<NotificationService.SendNotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationService.SendNotificationRequest.class);
            verify(notificationService).sendNotification(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_NAME);
        }

        @Test
        @DisplayName("should send SMS notification successfully")
        void sendSmsNotificationSuccessfully() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest();
            request.setRecipientId("user123");
            request.setRecipientPhone("+15551234567");
            request.setChannel(NotificationChannel.SMS);
            request.setBody("Test SMS");

            Notification notification = createNotification(NotificationChannel.SMS, NotificationStatus.SENT);
            when(notificationService.sendNotification(any())).thenReturn(notification);

            // When
            ResponseEntity<NotificationResponse> response = controller.sendNotification(
                request, TENANT_ID, authentication);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getChannel()).isEqualTo(NotificationChannel.SMS);
        }

        @Test
        @DisplayName("should send in-app notification successfully")
        void sendInAppNotificationSuccessfully() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest();
            request.setRecipientId("user123");
            request.setChannel(NotificationChannel.IN_APP);
            request.setSubject("Alert");
            request.setBody("Important notification");

            Notification notification = createNotification(NotificationChannel.IN_APP, NotificationStatus.DELIVERED);
            when(notificationService.sendNotification(any())).thenReturn(notification);

            // When
            ResponseEntity<NotificationResponse> response = controller.sendNotification(
                request, TENANT_ID, authentication);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getChannel()).isEqualTo(NotificationChannel.IN_APP);
        }

        @Test
        @DisplayName("should pass template code to service")
        void passTemplateCodeToService() {
            // Given
            SendNotificationRequest request = new SendNotificationRequest();
            request.setRecipientId("user123");
            request.setRecipientEmail("user@example.com");
            request.setChannel(NotificationChannel.EMAIL);
            request.setTemplateCode("care-gap-alert");
            request.setVariables(Map.of("patientName", "John Doe", "measureName", "HbA1c"));

            Notification notification = createNotification(NotificationChannel.EMAIL, NotificationStatus.SENT);
            when(notificationService.sendNotification(any())).thenReturn(notification);

            // When
            controller.sendNotification(request, TENANT_ID, authentication);

            // Then
            ArgumentCaptor<NotificationService.SendNotificationRequest> captor =
                ArgumentCaptor.forClass(NotificationService.SendNotificationRequest.class);
            verify(notificationService).sendNotification(captor.capture());
            assertThat(captor.getValue().getTemplateCode()).isEqualTo("care-gap-alert");
            assertThat(captor.getValue().getVariables()).containsEntry("patientName", "John Doe");
        }
    }

    @Nested
    @DisplayName("Bulk Notification Tests")
    class BulkNotificationTests {

        @Test
        @DisplayName("should send bulk notifications successfully")
        void sendBulkNotificationsSuccessfully() {
            // Given
            SendNotificationRequest req1 = new SendNotificationRequest();
            req1.setRecipientId("user1");
            req1.setRecipientEmail("user1@example.com");
            req1.setChannel(NotificationChannel.EMAIL);
            req1.setSubject("Test 1");
            req1.setBody("Body 1");

            SendNotificationRequest req2 = new SendNotificationRequest();
            req2.setRecipientId("user2");
            req2.setRecipientEmail("user2@example.com");
            req2.setChannel(NotificationChannel.EMAIL);
            req2.setSubject("Test 2");
            req2.setBody("Body 2");

            BulkNotificationRequest bulkRequest = new BulkNotificationRequest();
            bulkRequest.setNotifications(List.of(req1, req2));

            List<Notification> notifications = List.of(
                createNotification(NotificationChannel.EMAIL, NotificationStatus.SENT),
                createNotification(NotificationChannel.EMAIL, NotificationStatus.SENT)
            );
            when(notificationService.sendBulkNotifications(anyList())).thenReturn(notifications);

            // When
            ResponseEntity<BulkNotificationResponse> response = controller.sendBulkNotifications(
                bulkRequest, TENANT_ID, authentication);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTotalRequested()).isEqualTo(2);
            assertThat(response.getBody().getSuccessCount()).isEqualTo(2);
            assertThat(response.getBody().getFailedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should report failed notifications in bulk response")
        void reportFailedNotificationsInBulkResponse() {
            // Given
            SendNotificationRequest req1 = new SendNotificationRequest();
            req1.setRecipientId("user1");
            req1.setChannel(NotificationChannel.EMAIL);

            SendNotificationRequest req2 = new SendNotificationRequest();
            req2.setRecipientId("user2");
            req2.setChannel(NotificationChannel.EMAIL);

            BulkNotificationRequest bulkRequest = new BulkNotificationRequest();
            bulkRequest.setNotifications(List.of(req1, req2));

            Notification successNotification = createNotification(NotificationChannel.EMAIL, NotificationStatus.SENT);
            Notification failedNotification = createNotification(NotificationChannel.EMAIL, NotificationStatus.FAILED);

            when(notificationService.sendBulkNotifications(anyList()))
                .thenReturn(List.of(successNotification, failedNotification));

            // When
            ResponseEntity<BulkNotificationResponse> response = controller.sendBulkNotifications(
                bulkRequest, TENANT_ID, authentication);

            // Then
            assertThat(response.getBody().getSuccessCount()).isEqualTo(1);
            assertThat(response.getBody().getFailedCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get Notification Tests")
    class GetNotificationTests {

        @Test
        @DisplayName("should return notification when found")
        void returnNotificationWhenFound() {
            // Given
            UUID notificationId = UUID.randomUUID();
            Notification notification = createNotification(NotificationChannel.EMAIL, NotificationStatus.SENT);
            notification.setId(notificationId);

            when(notificationService.getNotification(notificationId, TENANT_ID))
                .thenReturn(Optional.of(notification));

            // When
            ResponseEntity<NotificationResponse> response = controller.getNotification(notificationId, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isEqualTo(notificationId);
        }

        @Test
        @DisplayName("should return 404 when notification not found")
        void return404WhenNotFound() {
            // Given
            UUID notificationId = UUID.randomUUID();
            when(notificationService.getNotification(notificationId, TENANT_ID))
                .thenReturn(Optional.empty());

            // When
            ResponseEntity<NotificationResponse> response = controller.getNotification(notificationId, TENANT_ID);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("List Notifications Tests")
    class ListNotificationsTests {

        @Test
        @DisplayName("should return paginated notifications")
        void returnPaginatedNotifications() {
            // Given
            PageRequest pageable = PageRequest.of(0, 10);
            List<Notification> notifications = List.of(
                createNotification(NotificationChannel.EMAIL, NotificationStatus.SENT),
                createNotification(NotificationChannel.SMS, NotificationStatus.DELIVERED)
            );
            Page<Notification> page = new PageImpl<>(notifications, pageable, 2);

            when(notificationService.getNotifications(TENANT_ID, pageable)).thenReturn(page);

            // When
            ResponseEntity<Page<NotificationResponse>> response = controller.getNotifications(
                TENANT_ID, null, null, null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(2);
            assertThat(response.getBody().getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("should filter notifications by recipient")
        void filterNotificationsByRecipient() {
            // Given
            String recipientId = "user123";
            PageRequest pageable = PageRequest.of(0, 10);
            List<Notification> notifications = List.of(
                createNotification(NotificationChannel.EMAIL, NotificationStatus.SENT)
            );
            Page<Notification> page = new PageImpl<>(notifications, pageable, 1);

            when(notificationService.getNotificationsForRecipient(TENANT_ID, recipientId, pageable))
                .thenReturn(page);

            // When
            ResponseEntity<Page<NotificationResponse>> response = controller.getNotifications(
                TENANT_ID, recipientId, null, null, pageable);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(notificationService).getNotificationsForRecipient(TENANT_ID, recipientId, pageable);
        }
    }

    private Notification createNotification(NotificationChannel channel, NotificationStatus status) {
        return Notification.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .recipientId("user123")
            .recipientEmail("user@example.com")
            .channel(channel)
            .subject("Test Subject")
            .body("Test Body")
            .status(status)
            .priority(NotificationPriority.NORMAL)
            .build();
    }
}
