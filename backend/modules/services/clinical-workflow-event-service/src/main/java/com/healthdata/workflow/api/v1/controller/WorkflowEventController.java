package com.healthdata.workflow.api.v1.controller;

import com.healthdata.workflow.api.v1.dto.InitiateWorkflowRequest;
import com.healthdata.workflow.api.v1.dto.WorkflowEventResponse;
import com.healthdata.workflow.service.WorkflowEventApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Clinical Workflow Event Service REST Controller
 *
 * Handles workflow orchestration events:
 * - POST /api/v1/workflows/initiate - Initiate new workflow
 * - POST /api/v1/workflows/steps/complete - Complete workflow step
 * - POST /api/v1/workflows/approvals/decide - Make approval decision
 * - GET /api/v1/workflows/{workflowId} - Get workflow status
 *
 * All endpoints return 202 Accepted (async event processing)
 * Multi-tenant isolation via X-Tenant-ID header
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Validated
public class WorkflowEventController {

    private final WorkflowEventApplicationService workflowEventService;

    /**
     * Initiate workflow
     *
     * @param request Initiation request with patientId, workflowType, description
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with WorkflowEventResponse
     */
    @PostMapping(path = "/initiate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowEventResponse> initiateWorkflow(
            @Valid @RequestBody InitiateWorkflowRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Initiating workflow: {}, patient: {}, tenant: {}",
            request.getWorkflowType(), request.getPatientId(), tenantId);

        WorkflowEventResponse response = workflowEventService.initiateWorkflow(request, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Complete workflow step
     *
     * @param workflowId Workflow identifier
     * @param stepRequest JSON with stepName and outcome
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with updated workflow status
     */
    @PostMapping(path = "/steps/complete/{workflowId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowEventResponse> completeStep(
            @PathVariable String workflowId,
            @RequestBody String stepRequest,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Completing step for workflow: {}, tenant: {}", workflowId, tenantId);

        WorkflowEventResponse response = workflowEventService.completeStep(workflowId, stepRequest, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Make approval decision
     *
     * @param workflowId Workflow identifier
     * @param decisionRequest JSON with decision (APPROVED/DENIED) and rationale
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 202 Accepted with updated approval status
     */
    @PostMapping(path = "/approvals/decide/{workflowId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowEventResponse> makeApprovalDecision(
            @PathVariable String workflowId,
            @RequestBody String decisionRequest,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Making approval decision for workflow: {}, tenant: {}", workflowId, tenantId);

        WorkflowEventResponse response = workflowEventService.makeApprovalDecision(workflowId, decisionRequest, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get workflow status
     *
     * @param workflowId Workflow identifier
     * @param tenantId Tenant identifier from X-Tenant-ID header
     * @return 200 OK with workflow details
     */
    @GetMapping(path = "/{workflowId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowEventResponse> getWorkflow(
            @PathVariable String workflowId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Getting workflow: {}, tenant: {}", workflowId, tenantId);

        WorkflowEventResponse response = workflowEventService.getWorkflow(workflowId, tenantId);

        return ResponseEntity.ok(response);
    }
}
