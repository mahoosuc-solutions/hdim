package com.healthdata.approval.controller;

import com.healthdata.approval.domain.entity.ApprovalHistory;
import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import com.healthdata.approval.service.ApprovalService;
import com.healthdata.approval.service.ApprovalService.ApprovalStats;
import com.healthdata.approval.service.ApprovalService.CreateApprovalRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for Human-in-the-Loop approval workflows.
 */
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Approval", description = "Human-in-the-Loop approval management")
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping
    @Operation(summary = "Create a new approval request")
    public ResponseEntity<ApprovalRequest> createRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateRequestDTO body) {

        CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
            tenantId,
            body.requestType(),
            body.entityType(),
            body.entityId(),
            body.actionRequested(),
            body.payload(),
            body.confidenceScore(),
            body.riskLevel(),
            userId,
            body.sourceService(),
            body.correlationId(),
            body.assignedRole(),
            body.expiresAt()
        );

        ApprovalRequest request = approvalService.createApprovalRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an approval request by ID")
    public ResponseEntity<ApprovalRequest> getRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(approvalService.getRequest(id, tenantId));
    }

    @GetMapping
    @Operation(summary = "Get all approval requests for tenant")
    public ResponseEntity<Page<ApprovalRequest>> getAllRequests(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) ApprovalStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(approvalService.getAllForTenant(tenantId, status, pageable));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending approval requests for current user's role")
    public ResponseEntity<Page<ApprovalRequest>> getPendingRequests(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(approvalService.getPendingForUser(tenantId, role, pageable));
    }

    @GetMapping("/assigned")
    @Operation(summary = "Get requests assigned to current user")
    public ResponseEntity<Page<ApprovalRequest>> getAssignedRequests(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) ApprovalStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(approvalService.getAssignedToUser(userId, status, pageable));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign request to a reviewer")
    public ResponseEntity<ApprovalRequest> assignRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody AssignRequestDTO body) {
        return ResponseEntity.ok(
            approvalService.assignRequest(id, tenantId, body.assignedTo(), userId)
        );
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a request")
    public ResponseEntity<ApprovalRequest> approveRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody DecisionDTO body) {
        return ResponseEntity.ok(
            approvalService.approve(id, tenantId, userId, body.reason())
        );
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a request")
    public ResponseEntity<ApprovalRequest> rejectRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody DecisionDTO body) {
        return ResponseEntity.ok(
            approvalService.reject(id, tenantId, userId, body.reason())
        );
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "Escalate a request to higher authority")
    public ResponseEntity<ApprovalRequest> escalateRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody EscalateDTO body) {
        return ResponseEntity.ok(
            approvalService.escalate(id, tenantId, userId, body.escalatedTo(), body.reason())
        );
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get history for an approval request")
    public ResponseEntity<List<ApprovalHistory>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(approvalService.getHistory(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get approval statistics")
    public ResponseEntity<ApprovalStats> getStats(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "30") int days) {
        Instant since = Instant.now().minus(Duration.ofDays(days));
        return ResponseEntity.ok(approvalService.getStats(tenantId, since));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get requests expiring soon")
    public ResponseEntity<List<ApprovalRequest>> getExpiringSoon(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "4") int hours) {
        return ResponseEntity.ok(
            approvalService.findExpiringSoon(tenantId, Duration.ofHours(hours))
        );
    }

    // Request DTOs

    public record CreateRequestDTO(
        RequestType requestType,
        @NotBlank String entityType,
        String entityId,
        @NotBlank String actionRequested,
        Map<String, Object> payload,
        BigDecimal confidenceScore,
        RiskLevel riskLevel,
        String sourceService,
        String correlationId,
        String assignedRole,
        Instant expiresAt
    ) {}

    public record AssignRequestDTO(
        @NotBlank String assignedTo
    ) {}

    public record DecisionDTO(
        String reason
    ) {}

    public record EscalateDTO(
        @NotBlank String escalatedTo,
        String reason
    ) {}
}
