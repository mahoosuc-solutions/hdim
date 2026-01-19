package com.healthdata.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.workflow.api.v1.dto.InitiateWorkflowRequest;
import com.healthdata.workflow.api.v1.dto.WorkflowEventResponse;
import com.healthdata.workflow.event.WorkflowInitiatedEvent;
import com.healthdata.workflow.event.WorkflowStepCompletedEvent;
import com.healthdata.workflow.event.ApprovalDecisionEvent;
import com.healthdata.workflow.eventhandler.ClinicalWorkflowEventHandler;
import com.healthdata.workflow.persistence.WorkflowProjectionRepository;
import com.healthdata.workflow.projection.WorkflowProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Workflow Event Application Service
 *
 * Orchestrates clinical workflow execution:
 * - Initiates workflows for patients
 * - Tracks step completion and outcomes
 * - Manages approval workflows and decisions
 * - Calculates workflow duration and state transitions
 *
 * ★ Insight ─────────────────────────────────────
 * Workflow orchestration enables complex clinical processes:
 * - Steps enable breaking workflows into manageable tasks
 * - Approvals gate critical decisions (APPROVED/DENIED/PENDING)
 * - Task assignment routes work to appropriate staff
 * - State machine ensures valid transitions (INITIATED→IN_PROGRESS→COMPLETED)
 * ─────────────────────────────────────────────────
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowEventApplicationService {

    private final ClinicalWorkflowEventHandler workflowEventHandler;
    private final WorkflowProjectionRepository workflowRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String WORKFLOW_EVENTS_TOPIC = "workflow.events";

    /**
     * Initiate workflow for patient
     */
    public WorkflowEventResponse initiateWorkflow(InitiateWorkflowRequest request, String tenantId) {
        log.info("Initiating workflow: {}, patient: {}, tenant: {}",
            request.getWorkflowType(), request.getPatientId(), tenantId);

        String workflowId = "WORKFLOW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create domain event
        WorkflowInitiatedEvent event = new WorkflowInitiatedEvent(
            tenantId,
            request.getPatientId(),
            request.getWorkflowType(),
            request.getDescription()
        );

        // Delegate to Phase 4 event handler
        workflowEventHandler.handle(event);

        // Publish to Kafka
        kafkaTemplate.send(WORKFLOW_EVENTS_TOPIC, request.getPatientId(), event);

        log.info("Workflow initiated: {}", workflowId);

        return WorkflowEventResponse.builder()
            .workflowId(workflowId)
            .workflowType(request.getWorkflowType())
            .status("INITIATED")
            .initiatedDate(LocalDate.now())
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Complete workflow step
     */
    public WorkflowEventResponse completeStep(String workflowId, String stepRequest, String tenantId) {
        log.info("Completing step for workflow: {}, tenant: {}", workflowId, tenantId);

        Optional<WorkflowProjection> workflow = workflowRepository.findById(workflowId);
        if (workflow.isEmpty()) {
            throw new RuntimeException("Workflow not found: " + workflowId);
        }

        WorkflowProjection proj = workflow.get();

        try {
            StepRequest request = objectMapper.readValue(stepRequest, StepRequest.class);

            // Create domain event
            WorkflowStepCompletedEvent event = new WorkflowStepCompletedEvent(
                tenantId,
                proj.getPatientId(),
                proj.getWorkflowType(),
                request.stepName,
                true,  // successful
                request.outcome
            );

            // Delegate to Phase 4 event handler
            workflowEventHandler.handle(event);

            // Publish to Kafka
            kafkaTemplate.send(WORKFLOW_EVENTS_TOPIC, proj.getPatientId(), event);

            log.info("Step completed: workflow={}, step={}", workflowId, request.stepName);

            return WorkflowEventResponse.builder()
                .workflowId(workflowId)
                .currentStep(request.stepName)
                .status("IN_PROGRESS")
                .lastStepSuccessful(true)
                .timestamp(Instant.now())
                .build();

        } catch (Exception e) {
            log.error("Error processing step completion", e);
            throw new RuntimeException("Failed to complete step", e);
        }
    }

    /**
     * Make approval decision
     */
    public WorkflowEventResponse makeApprovalDecision(String workflowId, String decisionRequest, String tenantId) {
        log.info("Making approval decision for workflow: {}, tenant: {}", workflowId, tenantId);

        Optional<WorkflowProjection> workflow = workflowRepository.findById(workflowId);
        if (workflow.isEmpty()) {
            throw new RuntimeException("Workflow not found: " + workflowId);
        }

        WorkflowProjection proj = workflow.get();

        try {
            ApprovalRequest request = objectMapper.readValue(decisionRequest, ApprovalRequest.class);

            // Create domain event
            ApprovalDecisionEvent event = new ApprovalDecisionEvent(
                tenantId,
                proj.getPatientId(),
                proj.getWorkflowType(),
                request.decision,
                request.rationale,
                request.approvedBy
            );

            // Delegate to Phase 4 event handler
            workflowEventHandler.handle(event);

            // Publish to Kafka
            kafkaTemplate.send(WORKFLOW_EVENTS_TOPIC, proj.getPatientId(), event);

            log.info("Approval decision made: workflow={}, decision={}", workflowId, request.decision);

            return WorkflowEventResponse.builder()
                .workflowId(workflowId)
                .approvalStatus(request.decision)
                .status(request.decision.equals("APPROVED") ? "IN_PROGRESS" : "CANCELLED")
                .timestamp(Instant.now())
                .build();

        } catch (Exception e) {
            log.error("Error processing approval decision", e);
            throw new RuntimeException("Failed to make approval decision", e);
        }
    }

    /**
     * Get workflow status
     */
    @Transactional(readOnly = true)
    public WorkflowEventResponse getWorkflow(String workflowId, String tenantId) {
        log.info("Getting workflow: {}, tenant: {}", workflowId, tenantId);

        Optional<WorkflowProjection> workflow = workflowRepository.findById(workflowId);
        if (workflow.isEmpty()) {
            throw new RuntimeException("Workflow not found: " + workflowId);
        }

        WorkflowProjection proj = workflow.get();

        return WorkflowEventResponse.builder()
            .workflowId(workflowId)
            .workflowType(proj.getWorkflowType())
            .status(proj.getStatus())
            .currentStep(proj.getCurrentStep())
            .assignedTo(proj.getAssignedTo())
            .approvalStatus(proj.getApprovalStatus())
            .initiatedDate(proj.getInitiatedDate())
            .completedDate(proj.getCompletedDate())
            .timestamp(Instant.now())
            .build();
    }

    // ===== Helper DTOs for request parsing =====

    static class StepRequest {
        public String stepName;
        public String outcome;
    }

    static class ApprovalRequest {
        public String decision;  // APPROVED, DENIED, PENDING_REVIEW
        public String rationale;
        public String approvedBy;
    }
}
