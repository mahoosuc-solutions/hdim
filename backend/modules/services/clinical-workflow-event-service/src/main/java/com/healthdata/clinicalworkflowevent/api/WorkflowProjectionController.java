package com.healthdata.clinicalworkflowevent.api;

import com.healthdata.clinicalworkflowevent.projection.WorkflowProjection;
import com.healthdata.clinicalworkflowevent.repository.WorkflowProjectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Workflow Projection Query API
 *
 * Provides fast read-only queries on denormalized workflow data.
 * Part of CQRS pattern - this service contains the read model.
 */
@RestController
@RequestMapping("/api/v1/workflow-projections")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Projections", description = "CQRS Read Model - Workflow Projections")
public class WorkflowProjectionController {

    private final WorkflowProjectionRepository workflowRepository;

    /**
     * Get workflow projection by ID
     */
    @GetMapping("/{workflowId}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get workflow projection", description = "Retrieve workflow details")
    public ResponseEntity<WorkflowProjection> getWorkflowProjection(
            @PathVariable UUID workflowId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching workflow projection for workflow {} in tenant {}", workflowId, tenantId);

        return workflowRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all workflows for a patient
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get patient workflows", description = "Retrieve all workflows for patient")
    public ResponseEntity<List<WorkflowProjection>> getPatientWorkflows(
            @PathVariable UUID patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching workflows for patient {} in tenant {}", patientId, tenantId);

        List<WorkflowProjection> workflows = workflowRepository
            .findByTenantIdAndPatientIdOrderByCreatedAtDesc(tenantId, patientId);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get pending workflows for a patient
     */
    @GetMapping("/patient/{patientId}/pending")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get pending workflows", description = "Retrieve pending workflows for patient")
    public ResponseEntity<List<WorkflowProjection>> getPendingForPatient(
            @PathVariable UUID patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching pending workflows for patient {}", patientId);

        List<WorkflowProjection> workflows = workflowRepository
            .findPendingForPatient(tenantId, patientId);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get workflows assigned to a user
     */
    @GetMapping("/assigned-to/{assignedTo}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get assigned workflows", description = "Retrieve workflows assigned to user")
    public ResponseEntity<List<WorkflowProjection>> getAssignedTo(
            @PathVariable String assignedTo,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching workflows assigned to {} in tenant {}", assignedTo, tenantId);

        List<WorkflowProjection> workflows = workflowRepository
            .findAssignedTo(tenantId, assignedTo);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get pending workflows assigned to user (paginated)
     */
    @GetMapping("/assigned-to/{assignedTo}/pending")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get assigned pending workflows", description = "Retrieve pending workflows for user")
    public ResponseEntity<Page<WorkflowProjection>> getPendingAssignedTo(
            @PathVariable String assignedTo,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        log.debug("Fetching pending workflows assigned to {}", assignedTo);

        Page<WorkflowProjection> workflows = workflowRepository
            .findPendingAssignedTo(tenantId, assignedTo, pageable);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get overdue workflows
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get overdue workflows", description = "Retrieve all overdue workflows")
    public ResponseEntity<List<WorkflowProjection>> getOverdue(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching overdue workflows for tenant {}", tenantId);

        List<WorkflowProjection> workflows = workflowRepository.findOverdue(tenantId);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get workflows requiring review
     */
    @GetMapping("/requiring-review")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get workflows requiring review", description = "Retrieve workflows needing review")
    public ResponseEntity<List<WorkflowProjection>> getRequiringReview(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching workflows requiring review for tenant {}", tenantId);

        List<WorkflowProjection> workflows = workflowRepository.findRequiringReview(tenantId);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get workflows with blocking issues
     */
    @GetMapping("/blocking-issues")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get workflows with blocking issues", description = "Retrieve workflows with blocking issues")
    public ResponseEntity<List<WorkflowProjection>> getWithBlockingIssues(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching workflows with blocking issues for tenant {}", tenantId);

        List<WorkflowProjection> workflows = workflowRepository.findWithBlockingIssues(tenantId);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get workflows by type
     */
    @GetMapping("/by-type/{workflowType}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get workflows by type", description = "Retrieve workflows of specific type")
    public ResponseEntity<List<WorkflowProjection>> getByWorkflowType(
            @PathVariable String workflowType,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching workflows of type {} for tenant {}", workflowType, tenantId);

        List<WorkflowProjection> workflows = workflowRepository
            .findByWorkflowType(tenantId, workflowType);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get workflows by status (paginated)
     */
    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get workflows by status", description = "Retrieve workflows with specific status")
    public ResponseEntity<Page<WorkflowProjection>> getByStatus(
            @PathVariable String status,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        log.debug("Fetching workflows with status {} for tenant {}", status, tenantId);

        Page<WorkflowProjection> workflows = workflowRepository
            .findByStatus(tenantId, status, pageable);
        return ResponseEntity.ok(workflows);
    }

    /**
     * Get workflow statistics for tenant
     */
    @GetMapping("/stats")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get workflow statistics", description = "Retrieve workflow statistics for tenant")
    public ResponseEntity<WorkflowStatistics> getStatistics(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Calculating workflow statistics for tenant {}", tenantId);

        long pendingCount = workflowRepository.countPending(tenantId);
        long overdueCount = workflowRepository.countOverdue(tenantId);

        WorkflowStatistics stats = WorkflowStatistics.builder()
            .totalPending(pendingCount)
            .totalOverdue(overdueCount)
            .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Service health status")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Clinical workflow event service is healthy");
    }
}
