package com.healthdata.agent.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Feign client for communicating with the Approval Service.
 */
@FeignClient(
    name = "approval-service",
    url = "${hdim.services.approval-service.url:http://localhost:8097}"
)
public interface ApprovalServiceClient {

    @PostMapping("/api/v1/approvals")
    @CircuitBreaker(name = "approval-service", fallbackMethod = "createApprovalRequestFallback")
    ApprovalResponse createApprovalRequest(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestHeader("X-User-Id") String userId,
        @RequestBody CreateApprovalRequest request
    );

    @GetMapping("/api/v1/approvals/{id}")
    @CircuitBreaker(name = "approval-service", fallbackMethod = "getApprovalRequestFallback")
    ApprovalResponse getApprovalRequest(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @PathVariable("id") UUID id
    );

    // Fallback methods
    default ApprovalResponse createApprovalRequestFallback(
            String tenantId, String userId, CreateApprovalRequest request, Throwable t) {
        return ApprovalResponse.builder()
            .id(null)
            .status(ApprovalStatus.PENDING)
            .fallback(true)
            .fallbackReason("Approval service unavailable: " + t.getMessage())
            .build();
    }

    default ApprovalResponse getApprovalRequestFallback(
            String tenantId, UUID id, Throwable t) {
        return ApprovalResponse.builder()
            .id(id)
            .status(ApprovalStatus.PENDING)
            .fallback(true)
            .fallbackReason("Approval service unavailable: " + t.getMessage())
            .build();
    }

    // DTOs

    record CreateApprovalRequest(
        RequestType requestType,
        String entityType,
        String entityId,
        String actionRequested,
        Map<String, Object> payload,
        BigDecimal confidenceScore,
        RiskLevel riskLevel,
        String sourceService,
        String correlationId,
        String assignedRole,
        Instant expiresAt
    ) {}

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ApprovalResponse {
        private UUID id;
        private String tenantId;
        private RequestType requestType;
        private String entityType;
        private String entityId;
        private String actionRequested;
        private Map<String, Object> payload;
        private RiskLevel riskLevel;
        private ApprovalStatus status;
        private String decisionBy;
        private Instant decisionAt;
        private String decisionReason;
        private Instant expiresAt;

        // Fallback fields
        private boolean fallback;
        private String fallbackReason;
    }

    enum RequestType {
        AGENT_ACTION,
        GUARDRAIL_REVIEW,
        DATA_MUTATION,
        EXPORT,
        WORKFLOW_DEPLOY,
        DLQ_REPROCESS,
        CONSENT_CHANGE,
        EMERGENCY_ACCESS
    }

    enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    enum ApprovalStatus {
        PENDING,
        ASSIGNED,
        APPROVED,
        REJECTED,
        EXPIRED,
        ESCALATED
    }
}
