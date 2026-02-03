package com.healthdata.approval.service;

import com.healthdata.approval.audit.ApprovalAuditIntegration;
import com.healthdata.approval.domain.entity.ApprovalHistory;
import com.healthdata.approval.domain.entity.ApprovalHistory.HistoryAction;
import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import com.healthdata.approval.event.ApprovalEventPublisher;
import com.healthdata.approval.repository.ApprovalHistoryRepository;
import com.healthdata.approval.repository.ApprovalRequestRepository;
import com.healthdata.approval.websocket.ApprovalNotificationService;
import com.healthdata.approval.webhook.WebhookCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Core approval service for Human-in-the-Loop workflows.
 * Handles creation, assignment, approval/rejection, and escalation of approval requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalHistoryRepository historyRepository;
    private final ApprovalNotificationService notificationService;
    private final WebhookCallbackService webhookCallbackService;
    private final ApprovalEventPublisher eventPublisher;
    private final ApprovalAuditIntegration approvalAuditIntegration;

    @Value("${hdim.approval.default-timeout-hours:24}")
    private int defaultTimeoutHours;

    @Value("${hdim.approval.auto-escalation-hours:4}")
    private int autoEscalationHours;

    /**
     * Create a new approval request.
     */
    @Transactional
    public ApprovalRequest createApprovalRequest(CreateApprovalRequestDTO dto) {
        log.info("Creating approval request: type={}, entity={}, risk={}",
            dto.requestType(), dto.entityType(), dto.riskLevel());

        ApprovalRequest request = ApprovalRequest.builder()
            .tenantId(dto.tenantId())
            .requestType(dto.requestType())
            .entityType(dto.entityType())
            .entityId(dto.entityId())
            .actionRequested(dto.actionRequested())
            .payload(dto.payload() != null ? dto.payload() : new HashMap<>())
            .confidenceScore(dto.confidenceScore())
            .riskLevel(dto.riskLevel())
            .requestedBy(dto.requestedBy())
            .sourceService(dto.sourceService())
            .correlationId(dto.correlationId())
            .assignedRole(dto.assignedRole())
            .expiresAt(dto.expiresAt() != null ? dto.expiresAt() :
                Instant.now().plus(Duration.ofHours(defaultTimeoutHours)))
            .status(ApprovalStatus.PENDING)
            .build();

        request = requestRepository.save(request);

        // Record creation in history
        recordHistory(request, HistoryAction.CREATED, dto.requestedBy(),
            Map.of("requestType", dto.requestType().name(), "riskLevel", dto.riskLevel().name()));

        // Broadcast notification
        notificationService.notifyNewRequest(request);

        // Publish Kafka event for other services
        eventPublisher.publishCreated(request);

        // Publish audit event
        approvalAuditIntegration.publishApprovalRequestEvent(
            dto.tenantId(),
            request.getId(),
            dto.requestType().name(),
            dto.entityType(),
            dto.entityId(),
            dto.riskLevel().name(),
            dto.confidenceScore() != null ? dto.confidenceScore().doubleValue() : 0.0,
            dto.requestedBy()
        );

        log.info("Created approval request: id={}", request.getId());
        return request;
    }

    /**
     * Assign a request to a specific reviewer.
     */
    @Transactional
    public ApprovalRequest assignRequest(UUID requestId, String tenantId, String assignedTo, String assignedBy) {
        ApprovalRequest request = getRequest(requestId, tenantId);

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Can only assign PENDING requests. Current status: " + request.getStatus());
        }

        String previousAssignee = request.getAssignedTo();
        request.assign(assignedTo);
        request = requestRepository.save(request);

        recordHistory(request,
            previousAssignee == null ? HistoryAction.ASSIGNED : HistoryAction.REASSIGNED,
            assignedBy,
            Map.of("assignedTo", assignedTo,
                   "previousAssignee", previousAssignee != null ? previousAssignee : "none"));

        // Broadcast notification
        notificationService.notifyAssigned(request);

        // Publish Kafka event
        eventPublisher.publishAssigned(request, assignedBy);

        log.info("Assigned request {} to {}", requestId, assignedTo);
        return request;
    }

    /**
     * Approve a request.
     */
    @Transactional
    public ApprovalRequest approve(UUID requestId, String tenantId, String approvedBy, String reason) {
        ApprovalRequest request = getRequest(requestId, tenantId);

        if (!canDecide(request)) {
            throw new IllegalStateException("Cannot approve request in status: " + request.getStatus());
        }

        request.approve(approvedBy, reason);
        request = requestRepository.save(request);

        recordHistory(request, HistoryAction.APPROVED, approvedBy,
            Map.of("reason", reason != null ? reason : "No reason provided"));

        // Broadcast notification
        notificationService.notifyStatusChange(request, approvedBy);

        // Send webhook callback to n8n or other integrations
        webhookCallbackService.sendDecisionCallback(request);

        // Publish Kafka event for Agent Runtime and other services
        eventPublisher.publishApproved(request, approvedBy);

        // Publish audit event
        approvalAuditIntegration.publishApprovalDecisionEvent(
            tenantId,
            requestId,
            request.getEntityType(),
            request.getEntityId(),
            true, // approved
            approvedBy,
            reason,
            Duration.between(request.getCreatedAt(), Instant.now()).toMillis(),
            approvedBy
        );

        log.info("Approved request {} by {}", requestId, approvedBy);
        return request;
    }

    /**
     * Reject a request.
     */
    @Transactional
    public ApprovalRequest reject(UUID requestId, String tenantId, String rejectedBy, String reason) {
        ApprovalRequest request = getRequest(requestId, tenantId);

        if (!canDecide(request)) {
            throw new IllegalStateException("Cannot reject request in status: " + request.getStatus());
        }

        request.reject(rejectedBy, reason);
        request = requestRepository.save(request);

        recordHistory(request, HistoryAction.REJECTED, rejectedBy,
            Map.of("reason", reason != null ? reason : "No reason provided"));

        // Broadcast notification
        notificationService.notifyStatusChange(request, rejectedBy);

        // Send webhook callback to n8n or other integrations
        webhookCallbackService.sendDecisionCallback(request);

        // Publish Kafka event for Agent Runtime and other services
        eventPublisher.publishRejected(request, rejectedBy);

        // Publish audit event
        approvalAuditIntegration.publishApprovalDecisionEvent(
            tenantId,
            requestId,
            request.getEntityType(),
            request.getEntityId(),
            false, // rejected
            rejectedBy,
            reason,
            Duration.between(request.getCreatedAt(), Instant.now()).toMillis(),
            rejectedBy
        );

        log.info("Rejected request {} by {}", requestId, rejectedBy);
        return request;
    }

    /**
     * Escalate a request to a higher authority.
     */
    @Transactional
    public ApprovalRequest escalate(UUID requestId, String tenantId, String escalatedBy,
                                    String escalatedTo, String reason) {
        ApprovalRequest request = getRequest(requestId, tenantId);

        if (!canDecide(request)) {
            throw new IllegalStateException("Cannot escalate request in status: " + request.getStatus());
        }

        request.escalate(escalatedTo, reason);
        request = requestRepository.save(request);

        recordHistory(request, HistoryAction.ESCALATED, escalatedBy,
            Map.of("escalatedTo", escalatedTo,
                   "reason", reason != null ? reason : "No reason provided",
                   "escalationCount", request.getEscalationCount()));

        // Broadcast notification
        notificationService.notifyStatusChange(request, escalatedBy);

        // Publish Kafka event
        eventPublisher.publishEscalated(request, escalatedBy);

        // Publish audit event
        approvalAuditIntegration.publishApprovalEscalationEvent(
            tenantId,
            requestId,
            request.getEntityType(),
            request.getEntityId(),
            reason,
            escalatedTo,
            escalatedBy
        );

        log.info("Escalated request {} to {} by {}", requestId, escalatedTo, escalatedBy);
        return request;
    }

    /**
     * Get a request by ID and tenant.
     */
    @Transactional(readOnly = true)
    public ApprovalRequest getRequest(UUID requestId, String tenantId) {
        return requestRepository.findByTenantIdAndId(tenantId, requestId)
            .orElseThrow(() -> new NoSuchElementException("Approval request not found: " + requestId));
    }

    /**
     * Get pending requests for a user (by role or assignment).
     */
    @Transactional(readOnly = true)
    public Page<ApprovalRequest> getPendingForUser(String tenantId, String role, Pageable pageable) {
        return requestRepository.findPendingByTenantAndRole(tenantId, role, pageable);
    }

    /**
     * Get requests assigned to a specific user.
     */
    @Transactional(readOnly = true)
    public Page<ApprovalRequest> getAssignedToUser(String assignedTo, ApprovalStatus status, Pageable pageable) {
        if (status != null) {
            return requestRepository.findByAssignedToAndStatus(assignedTo, status, pageable);
        }
        return requestRepository.findByAssignedTo(assignedTo, pageable);
    }

    /**
     * Get all requests for a tenant.
     */
    @Transactional(readOnly = true)
    public Page<ApprovalRequest> getAllForTenant(String tenantId, ApprovalStatus status, Pageable pageable) {
        if (status != null) {
            return requestRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        }
        return requestRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get history for a request.
     */
    @Transactional(readOnly = true)
    public List<ApprovalHistory> getHistory(UUID requestId) {
        return historyRepository.findByApprovalRequestIdOrderByCreatedAtAsc(requestId);
    }

    /**
     * Get approval statistics for a tenant.
     */
    @Transactional(readOnly = true)
    public ApprovalStats getStats(String tenantId, Instant since) {
        long pending = requestRepository.countByTenantIdAndStatus(tenantId, ApprovalStatus.PENDING);
        long assigned = requestRepository.countByTenantIdAndStatus(tenantId, ApprovalStatus.ASSIGNED);
        long approved = requestRepository.countByTenantIdAndStatus(tenantId, ApprovalStatus.APPROVED);
        long rejected = requestRepository.countByTenantIdAndStatus(tenantId, ApprovalStatus.REJECTED);
        long expired = requestRepository.countByTenantIdAndStatus(tenantId, ApprovalStatus.EXPIRED);
        long escalated = requestRepository.countByTenantIdAndStatus(tenantId, ApprovalStatus.ESCALATED);

        Double avgDecisionTime = requestRepository.averageDecisionTimeSeconds(tenantId, since);

        List<Object[]> byRiskLevel = requestRepository.countPendingByRiskLevel(tenantId);
        Map<RiskLevel, Long> pendingByRisk = new HashMap<>();
        for (Object[] row : byRiskLevel) {
            pendingByRisk.put((RiskLevel) row[0], (Long) row[1]);
        }

        return new ApprovalStats(
            pending, assigned, approved, rejected, expired, escalated,
            avgDecisionTime != null ? avgDecisionTime : 0.0,
            pendingByRisk
        );
    }

    /**
     * Expire old pending requests (scheduled job).
     */
    @Scheduled(fixedRateString = "${hdim.approval.expiration-check-interval-ms:60000}")
    @Transactional
    public void expireOldRequests() {
        Instant now = Instant.now();
        int expired = requestRepository.expireRequests(now);
        if (expired > 0) {
            log.info("Expired {} approval requests", expired);
        }
    }

    /**
     * Find requests expiring soon (for notifications).
     */
    @Transactional(readOnly = true)
    public List<ApprovalRequest> findExpiringSoon(String tenantId, Duration withinDuration) {
        return requestRepository.findExpiringSoon(tenantId, Instant.now().plus(withinDuration));
    }

    // Helper methods

    private boolean canDecide(ApprovalRequest request) {
        return request.getStatus() == ApprovalStatus.PENDING ||
               request.getStatus() == ApprovalStatus.ASSIGNED;
    }

    private void recordHistory(ApprovalRequest request, HistoryAction action,
                               String actor, Map<String, Object> details) {
        ApprovalHistory history = ApprovalHistory.of(request, action, actor, details);
        historyRepository.save(history);
    }

    // DTOs

    public record CreateApprovalRequestDTO(
        String tenantId,
        RequestType requestType,
        String entityType,
        String entityId,
        String actionRequested,
        Map<String, Object> payload,
        java.math.BigDecimal confidenceScore,
        RiskLevel riskLevel,
        String requestedBy,
        String sourceService,
        String correlationId,
        String assignedRole,
        Instant expiresAt
    ) {}

    public record ApprovalStats(
        long pending,
        long assigned,
        long approved,
        long rejected,
        long expired,
        long escalated,
        double avgDecisionTimeSeconds,
        Map<RiskLevel, Long> pendingByRiskLevel
    ) {}
}
