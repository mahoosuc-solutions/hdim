package com.healthdata.notification.infrastructure.websocket;

import com.healthdata.notification.api.v1.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for pushing notifications via WebSocket.
 *
 * Supports:
 * - User-specific notifications (sent to /user/{userId}/queue/notifications)
 * - Tenant broadcasts (sent to /topic/tenant/{tenantId}/notifications)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push a notification to a specific user via WebSocket.
     *
     * @param userId   The user's ID (must match their WebSocket session principal)
     * @param notification The notification to send
     */
    public void pushToUser(String userId, NotificationResponse notification) {
        try {
            String destination = "/queue/notifications";
            messagingTemplate.convertAndSendToUser(userId, destination, notification);
            log.debug("Pushed notification {} to user {} via WebSocket", notification.getId(), userId);
        } catch (Exception e) {
            log.warn("Failed to push notification via WebSocket to user {}: {}", userId, e.getMessage());
            // Don't throw - WebSocket delivery is best-effort for IN_APP
        }
    }

    /**
     * Broadcast a notification to all users in a tenant.
     *
     * @param tenantId     The tenant ID
     * @param notification The notification to broadcast
     */
    public void broadcastToTenant(String tenantId, NotificationResponse notification) {
        try {
            String destination = "/topic/tenant/" + tenantId + "/notifications";
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("Broadcast notification {} to tenant {} via WebSocket", notification.getId(), tenantId);
        } catch (Exception e) {
            log.warn("Failed to broadcast notification via WebSocket to tenant {}: {}", tenantId, e.getMessage());
        }
    }

    /**
     * Push a notification badge update to a user.
     * Used to update unread notification count.
     *
     * @param userId      The user's ID
     * @param unreadCount The current unread notification count
     */
    public void pushBadgeUpdate(String userId, int unreadCount) {
        try {
            String destination = "/queue/badge";
            messagingTemplate.convertAndSendToUser(userId, destination, new BadgeUpdate(unreadCount));
            log.debug("Pushed badge update (count={}) to user {}", unreadCount, userId);
        } catch (Exception e) {
            log.warn("Failed to push badge update via WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Badge update payload.
     */
    public record BadgeUpdate(int unreadCount) {}
}
