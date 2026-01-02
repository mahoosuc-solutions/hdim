package com.healthdata.approval.websocket;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * WebSocket message handler for approval-related interactions.
 * Handles subscriptions and client-initiated messages.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ApprovalWebSocketHandler {

    private final ApprovalService approvalService;

    /**
     * Handle subscription to tenant's approval notifications.
     * Returns current pending count when client subscribes.
     */
    @SubscribeMapping("/tenant/{tenantId}/approvals")
    public SubscriptionResponse subscribeToTenant(@DestinationVariable String tenantId) {
        log.debug("Client subscribed to tenant approvals: {}", tenantId);

        var stats = approvalService.getStats(tenantId, null);
        return new SubscriptionResponse(
            "subscribed",
            tenantId,
            stats.pending(),
            "Successfully subscribed to tenant approval notifications"
        );
    }

    /**
     * Handle subscription to role-based approval notifications.
     */
    @SubscribeMapping("/tenant/{tenantId}/role/{role}/approvals")
    public SubscriptionResponse subscribeToRole(
            @DestinationVariable String tenantId,
            @DestinationVariable String role) {
        log.debug("Client subscribed to role approvals: tenant={}, role={}", tenantId, role);

        return new SubscriptionResponse(
            "subscribed",
            tenantId,
            0,  // Role-specific count would require additional query
            String.format("Successfully subscribed to %s approval notifications", role)
        );
    }

    /**
     * Handle request for approval details via WebSocket.
     */
    @MessageMapping("/approval/details")
    public ApprovalDetailsResponse getApprovalDetails(@Payload ApprovalDetailsRequest request) {
        log.debug("Client requested approval details: {}", request.requestId());

        try {
            ApprovalRequest approval = approvalService.getRequest(
                UUID.fromString(request.requestId()),
                request.tenantId()
            );

            return ApprovalDetailsResponse.success(approval);
        } catch (Exception e) {
            log.warn("Failed to get approval details: {}", e.getMessage());
            return ApprovalDetailsResponse.error(request.requestId(), e.getMessage());
        }
    }

    /**
     * Handle quick action (approve/reject) via WebSocket for faster UX.
     */
    @MessageMapping("/approval/quickAction")
    public QuickActionResponse handleQuickAction(@Payload QuickActionRequest request) {
        log.info("Quick action received: action={}, requestId={}, actor={}",
            request.action(), request.requestId(), request.actorId());

        try {
            UUID requestId = UUID.fromString(request.requestId());
            ApprovalRequest result;

            switch (request.action().toUpperCase()) {
                case "APPROVE" -> result = approvalService.approve(
                    requestId, request.tenantId(), request.actorId(), request.reason());
                case "REJECT" -> result = approvalService.reject(
                    requestId, request.tenantId(), request.actorId(), request.reason());
                case "ESCALATE" -> result = approvalService.escalate(
                    requestId, request.tenantId(), request.actorId(),
                    request.escalateTo(), request.reason());
                default -> throw new IllegalArgumentException("Unknown action: " + request.action());
            }

            return QuickActionResponse.success(result.getId().toString(), result.getStatus().name());
        } catch (Exception e) {
            log.warn("Quick action failed: {}", e.getMessage());
            return QuickActionResponse.error(request.requestId(), e.getMessage());
        }
    }

    // Request/Response DTOs

    public record SubscriptionResponse(
        String status,
        String tenantId,
        long pendingCount,
        String message
    ) {}

    public record ApprovalDetailsRequest(
        String requestId,
        String tenantId
    ) {}

    public record ApprovalDetailsResponse(
        String status,
        String requestId,
        String entityType,
        String actionRequested,
        String riskLevel,
        String approvalStatus,
        String assignedTo,
        String assignedRole,
        String message
    ) {
        public static ApprovalDetailsResponse success(ApprovalRequest request) {
            return new ApprovalDetailsResponse(
                "success",
                request.getId().toString(),
                request.getEntityType(),
                request.getActionRequested(),
                request.getRiskLevel().name(),
                request.getStatus().name(),
                request.getAssignedTo(),
                request.getAssignedRole(),
                null
            );
        }

        public static ApprovalDetailsResponse error(String requestId, String message) {
            return new ApprovalDetailsResponse(
                "error",
                requestId,
                null, null, null, null, null, null,
                message
            );
        }
    }

    public record QuickActionRequest(
        String requestId,
        String tenantId,
        String action,
        String actorId,
        String reason,
        String escalateTo
    ) {}

    public record QuickActionResponse(
        String status,
        String requestId,
        String newStatus,
        String message
    ) {
        public static QuickActionResponse success(String requestId, String newStatus) {
            return new QuickActionResponse("success", requestId, newStatus, null);
        }

        public static QuickActionResponse error(String requestId, String message) {
            return new QuickActionResponse("error", requestId, null, message);
        }
    }
}
