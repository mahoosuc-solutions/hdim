package com.healthdata.approval.websocket;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.ApprovalStatus;
import com.healthdata.approval.domain.entity.ApprovalRequest.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for broadcasting approval notifications via WebSocket.
 * Sends real-time updates to connected clients when approval status changes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify when a new approval request is created.
     */
    public void notifyNewRequest(ApprovalRequest request) {
        ApprovalNotification notification = ApprovalNotification.created(request);

        // Broadcast to tenant topic
        String tenantTopic = String.format("/topic/tenant/%s/approvals", request.getTenantId());
        messagingTemplate.convertAndSend(tenantTopic, notification);

        // Broadcast to role topic if assigned role is set
        if (request.getAssignedRole() != null) {
            String roleTopic = String.format("/topic/tenant/%s/role/%s/approvals",
                request.getTenantId(), request.getAssignedRole());
            messagingTemplate.convertAndSend(roleTopic, notification);
        }

        log.debug("Broadcast new approval request notification: id={}, tenant={}",
            request.getId(), request.getTenantId());
    }

    /**
     * Notify when an approval request is assigned to a user.
     */
    public void notifyAssigned(ApprovalRequest request) {
        ApprovalNotification notification = ApprovalNotification.assigned(request);

        // Broadcast to tenant topic
        String tenantTopic = String.format("/topic/tenant/%s/approvals", request.getTenantId());
        messagingTemplate.convertAndSend(tenantTopic, notification);

        // Send to assigned user's personal queue
        if (request.getAssignedTo() != null) {
            String userQueue = String.format("/queue/user/%s/approvals", request.getAssignedTo());
            messagingTemplate.convertAndSend(userQueue, notification);
        }

        log.debug("Broadcast assignment notification: id={}, assignedTo={}",
            request.getId(), request.getAssignedTo());
    }

    /**
     * Notify when an approval request status changes (approved, rejected, escalated, expired).
     */
    public void notifyStatusChange(ApprovalRequest request, String actor) {
        ApprovalNotification notification = ApprovalNotification.statusChanged(request, actor);

        // Broadcast to tenant topic
        String tenantTopic = String.format("/topic/tenant/%s/approvals", request.getTenantId());
        messagingTemplate.convertAndSend(tenantTopic, notification);

        // Notify the original requester
        if (request.getRequestedBy() != null) {
            String requesterQueue = String.format("/queue/user/%s/approvals", request.getRequestedBy());
            messagingTemplate.convertAndSend(requesterQueue, notification);
        }

        // Notify the assigned user if different from actor
        if (request.getAssignedTo() != null && !request.getAssignedTo().equals(actor)) {
            String assigneeQueue = String.format("/queue/user/%s/approvals", request.getAssignedTo());
            messagingTemplate.convertAndSend(assigneeQueue, notification);
        }

        log.debug("Broadcast status change notification: id={}, status={}, actor={}",
            request.getId(), request.getStatus(), actor);
    }

    /**
     * Notify about an approval that is expiring soon.
     */
    public void notifyExpiringSoon(ApprovalRequest request) {
        ApprovalNotification notification = ApprovalNotification.expiringSoon(request);

        // Broadcast to tenant topic
        String tenantTopic = String.format("/topic/tenant/%s/approvals", request.getTenantId());
        messagingTemplate.convertAndSend(tenantTopic, notification);

        // Notify the assigned user
        if (request.getAssignedTo() != null) {
            String assigneeQueue = String.format("/queue/user/%s/approvals", request.getAssignedTo());
            messagingTemplate.convertAndSend(assigneeQueue, notification);
        }

        // Broadcast to role topic
        if (request.getAssignedRole() != null) {
            String roleTopic = String.format("/topic/tenant/%s/role/%s/approvals",
                request.getTenantId(), request.getAssignedRole());
            messagingTemplate.convertAndSend(roleTopic, notification);
        }

        log.debug("Broadcast expiring soon notification: id={}, expiresAt={}",
            request.getId(), request.getExpiresAt());
    }

    /**
     * Notification DTO sent to clients.
     */
    public record ApprovalNotification(
        UUID requestId,
        NotificationType type,
        String tenantId,
        String entityType,
        String actionRequested,
        RiskLevel riskLevel,
        ApprovalStatus status,
        String assignedTo,
        String assignedRole,
        String actor,
        String message,
        Instant timestamp,
        Instant expiresAt,
        Map<String, Object> metadata
    ) {
        public static ApprovalNotification created(ApprovalRequest request) {
            return new ApprovalNotification(
                request.getId(),
                NotificationType.CREATED,
                request.getTenantId(),
                request.getEntityType(),
                request.getActionRequested(),
                request.getRiskLevel(),
                request.getStatus(),
                request.getAssignedTo(),
                request.getAssignedRole(),
                request.getRequestedBy(),
                String.format("New %s approval request for %s",
                    request.getRiskLevel(), request.getEntityType()),
                Instant.now(),
                request.getExpiresAt(),
                Map.of()
            );
        }

        public static ApprovalNotification assigned(ApprovalRequest request) {
            return new ApprovalNotification(
                request.getId(),
                NotificationType.ASSIGNED,
                request.getTenantId(),
                request.getEntityType(),
                request.getActionRequested(),
                request.getRiskLevel(),
                request.getStatus(),
                request.getAssignedTo(),
                request.getAssignedRole(),
                null,
                String.format("Approval request assigned to %s", request.getAssignedTo()),
                Instant.now(),
                request.getExpiresAt(),
                Map.of()
            );
        }

        public static ApprovalNotification statusChanged(ApprovalRequest request, String actor) {
            String message = switch (request.getStatus()) {
                case APPROVED -> String.format("Approval request approved by %s", actor);
                case REJECTED -> String.format("Approval request rejected by %s", actor);
                case ESCALATED -> String.format("Approval request escalated to %s", request.getEscalatedTo());
                case EXPIRED -> "Approval request has expired";
                default -> "Approval request status changed to " + request.getStatus();
            };

            return new ApprovalNotification(
                request.getId(),
                NotificationType.STATUS_CHANGED,
                request.getTenantId(),
                request.getEntityType(),
                request.getActionRequested(),
                request.getRiskLevel(),
                request.getStatus(),
                request.getAssignedTo(),
                request.getAssignedRole(),
                actor,
                message,
                Instant.now(),
                request.getExpiresAt(),
                Map.of("decisionReason", request.getDecisionReason() != null ? request.getDecisionReason() : "")
            );
        }

        public static ApprovalNotification expiringSoon(ApprovalRequest request) {
            return new ApprovalNotification(
                request.getId(),
                NotificationType.EXPIRING_SOON,
                request.getTenantId(),
                request.getEntityType(),
                request.getActionRequested(),
                request.getRiskLevel(),
                request.getStatus(),
                request.getAssignedTo(),
                request.getAssignedRole(),
                null,
                String.format("Approval request expiring at %s", request.getExpiresAt()),
                Instant.now(),
                request.getExpiresAt(),
                Map.of()
            );
        }
    }

    public enum NotificationType {
        CREATED,
        ASSIGNED,
        STATUS_CHANGED,
        EXPIRING_SOON
    }
}
