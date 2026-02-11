package com.healthdata.agentvalidation.client;

import com.healthdata.agentvalidation.client.dto.ApprovalRequest;
import com.healthdata.agentvalidation.client.dto.ApprovalResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for Approval Service communication.
 * Used to submit flagged test executions for QA review.
 */
@FeignClient(
    name = "approval-service",
    url = "${hdim.services.approval.url:http://approval-service:8096}"
)
public interface ApprovalServiceClient {

    /**
     * Create a new approval request for QA review.
     */
    @PostMapping("/api/v1/approvals")
    @CircuitBreaker(name = "approval-service")
    @Retry(name = "approval-service")
    ApprovalResponse createApprovalRequest(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestHeader("X-User-ID") String userId,
        @RequestBody ApprovalRequest request
    );

    /**
     * Get status of an approval request.
     */
    @GetMapping("/api/v1/approvals/{approvalId}")
    @CircuitBreaker(name = "approval-service")
    ApprovalResponse getApprovalStatus(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable("approvalId") String approvalId
    );

    /**
     * List pending approvals for a tenant.
     */
    @GetMapping("/api/v1/approvals")
    @CircuitBreaker(name = "approval-service")
    List<ApprovalResponse> listPendingApprovals(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam(value = "category", defaultValue = "AGENT_VALIDATION") String category,
        @RequestParam(value = "status", defaultValue = "PENDING") String status
    );
}
