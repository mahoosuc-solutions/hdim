package com.healthdata.approval.websocket;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import com.healthdata.approval.websocket.ApprovalNotificationService.ApprovalNotification;
import com.healthdata.approval.websocket.ApprovalNotificationService.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalNotificationService Tests")
class ApprovalNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ApprovalNotificationService notificationService;

    @Captor
    private ArgumentCaptor<ApprovalNotification> notificationCaptor;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String REVIEWER_ID = "reviewer-789";

    @Nested
    @DisplayName("Notify New Request")
    class NotifyNewRequestTests {

        @Test
        @DisplayName("should broadcast to tenant topic")
        void notifyNewRequest_BroadcastsToTenant() {
            // Given
            ApprovalRequest request = createRequest();

            // When
            notificationService.notifyNewRequest(request);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(
                eq("/topic/tenant/" + TENANT_ID + "/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should broadcast to role topic when role assigned")
        void notifyNewRequest_WithRole_BroadcastsToRoleTopic() {
            // Given
            ApprovalRequest request = createRequest();
            request.setAssignedRole("CLINICAL_REVIEWER");

            // When
            notificationService.notifyNewRequest(request);

            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/topic/tenant/" + TENANT_ID + "/role/CLINICAL_REVIEWER/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should not broadcast to role topic when role is null")
        void notifyNewRequest_NoRole_SkipsRoleTopic() {
            // Given
            ApprovalRequest request = createRequest();
            request.setAssignedRole(null);

            // When
            notificationService.notifyNewRequest(request);

            // Then
            verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(ApprovalNotification.class));
        }

        @Test
        @DisplayName("should include correct notification type")
        void notifyNewRequest_CorrectNotificationType() {
            // Given
            ApprovalRequest request = createRequest();

            // When
            notificationService.notifyNewRequest(request);

            // Then
            verify(messagingTemplate).convertAndSend(anyString(), notificationCaptor.capture());
            ApprovalNotification notification = notificationCaptor.getValue();
            assertThat(notification.type()).isEqualTo(NotificationType.CREATED);
        }

        @Test
        @DisplayName("should include all request details in notification")
        void notifyNewRequest_IncludesAllDetails() {
            // Given
            ApprovalRequest request = createRequest();

            // When
            notificationService.notifyNewRequest(request);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            ApprovalNotification notification = notificationCaptor.getValue();

            assertThat(notification.requestId()).isEqualTo(request.getId());
            assertThat(notification.tenantId()).isEqualTo(TENANT_ID);
            assertThat(notification.entityType()).isEqualTo(request.getEntityType());
            assertThat(notification.riskLevel()).isEqualTo(request.getRiskLevel());
            assertThat(notification.status()).isEqualTo(request.getStatus());
        }
    }

    @Nested
    @DisplayName("Notify Assigned")
    class NotifyAssignedTests {

        @Test
        @DisplayName("should broadcast to tenant topic")
        void notifyAssigned_BroadcastsToTenant() {
            // Given
            ApprovalRequest request = createAssignedRequest();

            // When
            notificationService.notifyAssigned(request);

            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/topic/tenant/" + TENANT_ID + "/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should send to assigned user queue")
        void notifyAssigned_SendsToUserQueue() {
            // Given
            ApprovalRequest request = createAssignedRequest();

            // When
            notificationService.notifyAssigned(request);

            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/queue/user/" + REVIEWER_ID + "/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should not send to user queue when assignee is null")
        void notifyAssigned_NoAssignee_SkipsUserQueue() {
            // Given
            ApprovalRequest request = createRequest();
            request.setAssignedTo(null);

            // When
            notificationService.notifyAssigned(request);

            // Then
            verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(ApprovalNotification.class));
        }

        @Test
        @DisplayName("should have ASSIGNED notification type")
        void notifyAssigned_CorrectType() {
            // Given
            ApprovalRequest request = createAssignedRequest();

            // When
            notificationService.notifyAssigned(request);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().type()).isEqualTo(NotificationType.ASSIGNED);
        }
    }

    @Nested
    @DisplayName("Notify Status Change")
    class NotifyStatusChangeTests {

        @Test
        @DisplayName("should notify approved status change")
        void notifyStatusChange_Approved_SendsCorrectMessage() {
            // Given
            ApprovalRequest request = createRequest();
            request.approve(REVIEWER_ID, "Looks good");

            // When
            notificationService.notifyStatusChange(request, REVIEWER_ID);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            ApprovalNotification notification = notificationCaptor.getValue();

            assertThat(notification.type()).isEqualTo(NotificationType.STATUS_CHANGED);
            assertThat(notification.status()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(notification.actor()).isEqualTo(REVIEWER_ID);
            assertThat(notification.message()).contains("approved");
        }

        @Test
        @DisplayName("should notify rejected status change")
        void notifyStatusChange_Rejected_SendsCorrectMessage() {
            // Given
            ApprovalRequest request = createRequest();
            request.reject(REVIEWER_ID, "Not appropriate");

            // When
            notificationService.notifyStatusChange(request, REVIEWER_ID);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            ApprovalNotification notification = notificationCaptor.getValue();

            assertThat(notification.status()).isEqualTo(ApprovalStatus.REJECTED);
            assertThat(notification.message()).contains("rejected");
        }

        @Test
        @DisplayName("should notify escalated status change")
        void notifyStatusChange_Escalated_SendsCorrectMessage() {
            // Given
            ApprovalRequest request = createRequest();
            request.escalate("supervisor-123", "Needs review");

            // When
            notificationService.notifyStatusChange(request, REVIEWER_ID);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            ApprovalNotification notification = notificationCaptor.getValue();

            assertThat(notification.status()).isEqualTo(ApprovalStatus.ESCALATED);
            assertThat(notification.message()).contains("escalated");
        }

        @Test
        @DisplayName("should send to requester queue")
        void notifyStatusChange_SendsToRequester() {
            // Given
            ApprovalRequest request = createRequest();
            request.approve(REVIEWER_ID, "Approved");

            // When
            notificationService.notifyStatusChange(request, REVIEWER_ID);

            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/queue/user/" + USER_ID + "/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should send to assignee when different from actor")
        void notifyStatusChange_DifferentAssignee_SendsToAssignee() {
            // Given
            ApprovalRequest request = createAssignedRequest();
            request.approve("admin-999", "Approved");

            // When
            notificationService.notifyStatusChange(request, "admin-999");

            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/queue/user/" + REVIEWER_ID + "/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should not send to assignee when same as actor")
        void notifyStatusChange_SameAssignee_SkipsAssigneeQueue() {
            // Given
            ApprovalRequest request = createAssignedRequest();
            request.approve(REVIEWER_ID, "Approved");

            // When
            notificationService.notifyStatusChange(request, REVIEWER_ID);

            // Then
            verify(messagingTemplate, never()).convertAndSend(
                eq("/queue/user/" + REVIEWER_ID + "/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should include decision reason in metadata")
        void notifyStatusChange_IncludesDecisionReason() {
            // Given
            ApprovalRequest request = createRequest();
            request.approve(REVIEWER_ID, "All checks passed");

            // When
            notificationService.notifyStatusChange(request, REVIEWER_ID);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            ApprovalNotification notification = notificationCaptor.getValue();

            assertThat(notification.metadata()).containsEntry("decisionReason", "All checks passed");
        }

        @Test
        @DisplayName("should handle null decision reason")
        void notifyStatusChange_NullReason_HandlesGracefully() {
            // Given
            ApprovalRequest request = createRequest();
            request.approve(REVIEWER_ID, null);

            // When
            notificationService.notifyStatusChange(request, REVIEWER_ID);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            ApprovalNotification notification = notificationCaptor.getValue();

            assertThat(notification.metadata()).containsEntry("decisionReason", "");
        }
    }

    @Nested
    @DisplayName("Notify Expiring Soon")
    class NotifyExpiringSoonTests {

        @Test
        @DisplayName("should broadcast to tenant topic")
        void notifyExpiringSoon_BroadcastsToTenant() {
            // Given
            ApprovalRequest request = createAssignedRequest();

            // When
            notificationService.notifyExpiringSoon(request);

            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/topic/tenant/" + TENANT_ID + "/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should send to assigned user queue")
        void notifyExpiringSoon_SendsToAssignee() {
            // Given
            ApprovalRequest request = createAssignedRequest();

            // When
            notificationService.notifyExpiringSoon(request);

            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/queue/user/" + REVIEWER_ID + "/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should broadcast to role topic")
        void notifyExpiringSoon_BroadcastsToRole() {
            // Given
            ApprovalRequest request = createAssignedRequest();
            request.setAssignedRole("CLINICAL_REVIEWER");

            // When
            notificationService.notifyExpiringSoon(request);

            // Then
            verify(messagingTemplate).convertAndSend(
                eq("/topic/tenant/" + TENANT_ID + "/role/CLINICAL_REVIEWER/approvals"),
                any(ApprovalNotification.class)
            );
        }

        @Test
        @DisplayName("should have EXPIRING_SOON notification type")
        void notifyExpiringSoon_CorrectType() {
            // Given
            ApprovalRequest request = createAssignedRequest();

            // When
            notificationService.notifyExpiringSoon(request);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            assertThat(notificationCaptor.getValue().type()).isEqualTo(NotificationType.EXPIRING_SOON);
        }

        @Test
        @DisplayName("should include expiration time in message")
        void notifyExpiringSoon_IncludesExpirationTime() {
            // Given
            ApprovalRequest request = createAssignedRequest();

            // When
            notificationService.notifyExpiringSoon(request);

            // Then
            verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), notificationCaptor.capture());
            ApprovalNotification notification = notificationCaptor.getValue();

            assertThat(notification.message()).contains("expiring");
            assertThat(notification.expiresAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Notification Creation")
    class NotificationCreationTests {

        @Test
        @DisplayName("should create notification with all required fields")
        void createNotification_HasAllFields() {
            // Given
            ApprovalRequest request = createRequest();

            // When
            ApprovalNotification notification = ApprovalNotification.created(request);

            // Then
            assertThat(notification.requestId()).isEqualTo(request.getId());
            assertThat(notification.type()).isEqualTo(NotificationType.CREATED);
            assertThat(notification.tenantId()).isEqualTo(TENANT_ID);
            assertThat(notification.entityType()).isEqualTo(request.getEntityType());
            assertThat(notification.actionRequested()).isEqualTo(request.getActionRequested());
            assertThat(notification.riskLevel()).isEqualTo(request.getRiskLevel());
            assertThat(notification.status()).isEqualTo(request.getStatus());
            assertThat(notification.timestamp()).isNotNull();
            assertThat(notification.expiresAt()).isEqualTo(request.getExpiresAt());
        }

        @Test
        @DisplayName("should handle null assignedTo")
        void createNotification_NullAssignee_HandlesGracefully() {
            // Given
            ApprovalRequest request = createRequest();
            request.setAssignedTo(null);

            // When
            ApprovalNotification notification = ApprovalNotification.created(request);

            // Then
            assertThat(notification.assignedTo()).isNull();
        }

        @Test
        @DisplayName("should handle null assignedRole")
        void createNotification_NullRole_HandlesGracefully() {
            // Given
            ApprovalRequest request = createRequest();
            request.setAssignedRole(null);

            // When
            ApprovalNotification notification = ApprovalNotification.created(request);

            // Then
            assertThat(notification.assignedRole()).isNull();
        }
    }

    // Helper methods

    private ApprovalRequest createRequest() {
        return ApprovalRequest.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .requestType(RequestType.AGENT_ACTION)
            .entityType("TestEntity")
            .entityId("entity-123")
            .actionRequested("EXECUTE")
            .riskLevel(RiskLevel.MEDIUM)
            .status(ApprovalStatus.PENDING)
            .requestedBy(USER_ID)
            .payload(new HashMap<>())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    private ApprovalRequest createAssignedRequest() {
        ApprovalRequest request = createRequest();
        request.setStatus(ApprovalStatus.ASSIGNED);
        request.setAssignedTo(REVIEWER_ID);
        request.setAssignedAt(Instant.now());
        return request;
    }
}
