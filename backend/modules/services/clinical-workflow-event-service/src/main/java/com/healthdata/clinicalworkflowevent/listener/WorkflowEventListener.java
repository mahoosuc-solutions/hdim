package com.healthdata.clinicalworkflowevent.listener;

import com.healthdata.clinicalworkflowevent.projection.WorkflowProjection;
import com.healthdata.clinicalworkflowevent.repository.WorkflowProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Workflow Event Listener (CQRS Event Handler)
 *
 * Consumes domain events from Kafka and updates the workflow projection.
 *
 * Events consumed:
 * - workflow.started: Workflow initiated
 * - workflow.assigned: Assigned to user
 * - workflow.reassigned: Reassigned to different user
 * - workflow.progress.updated: Progress/steps changed
 * - workflow.completed: Workflow finished
 * - workflow.cancelled: Workflow cancelled
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEventListener {

    private final WorkflowProjectionRepository workflowRepository;

    /**
     * Handle workflow.started event
     * Creates a new workflow projection
     */
    @KafkaListener(
        topics = "workflow.started",
        groupId = "clinical-workflow-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowStarted(String tenantId, UUID workflowId, UUID patientId, String workflowType,
                                  String priority, String description, Integer totalSteps) {
        log.debug("Processing workflow.started for workflow {} (patient: {}, type: {})",
            workflowId, patientId, workflowType);

        WorkflowProjection projection = WorkflowProjection.builder()
            .tenantId(tenantId)
            .workflowId(workflowId)
            .patientId(patientId)
            .workflowType(workflowType)
            .priority(priority)
            .description(description)
            .status("PENDING")
            .progressPercentage(0)
            .stepsCompleted(0)
            .totalSteps(totalSteps != null ? totalSteps : 1)
            .daysPending(0)
            .isOverdue(false)
            .requiresReview(false)
            .hasBlockingIssue(false)
            .createdAt(Instant.now())
            .lastUpdatedAt(Instant.now())
            .eventVersion(1L)
            .build();

        workflowRepository.save(projection);
        log.info("Created workflow projection for workflow {}", workflowId);
    }

    /**
     * Handle workflow.assigned event
     * Updates workflow assignment
     */
    @KafkaListener(
        topics = "workflow.assigned",
        groupId = "clinical-workflow-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowAssigned(String tenantId, UUID workflowId, String assignedTo) {
        log.debug("Processing workflow.assigned for workflow {}: assigned to {}", workflowId, assignedTo);

        workflowRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
            .ifPresentOrElse(
                projection -> {
                    projection.setAssignedTo(assignedTo);
                    projection.setAssignedAt(Instant.now());
                    if (projection.getStatus().equals("PENDING")) {
                        projection.setStatus("IN_PROGRESS");
                        projection.setStartedAt(Instant.now());
                    }
                    projection.setLastUpdatedAt(Instant.now());
                    workflowRepository.save(projection);
                    log.info("Updated assignment for workflow {}", workflowId);
                },
                () -> log.warn("Workflow projection not found for workflow {} in tenant {}", workflowId, tenantId)
            );
    }

    /**
     * Handle workflow.reassigned event
     * Updates workflow assignment, tracking previous assignee
     */
    @KafkaListener(
        topics = "workflow.reassigned",
        groupId = "clinical-workflow-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowReassigned(String tenantId, UUID workflowId, String oldAssignee, String newAssignee) {
        log.debug("Processing workflow.reassigned for workflow {}: {} -> {}", workflowId, oldAssignee, newAssignee);

        workflowRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
            .ifPresentOrElse(
                projection -> {
                    projection.setPreviousAssignee(oldAssignee);
                    projection.setAssignedTo(newAssignee);
                    projection.setAssignedAt(Instant.now());
                    projection.setLastUpdatedAt(Instant.now());
                    workflowRepository.save(projection);
                    log.info("Reassigned workflow {} from {} to {}", workflowId, oldAssignee, newAssignee);
                },
                () -> log.warn("Workflow projection not found for workflow {} in tenant {}", workflowId, tenantId)
            );
    }

    /**
     * Handle workflow.progress.updated event
     * Updates progress percentage and steps completed
     */
    @KafkaListener(
        topics = "workflow.progress.updated",
        groupId = "clinical-workflow-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onProgressUpdated(String tenantId, UUID workflowId, Integer stepsCompleted, Integer totalSteps) {
        log.debug("Processing workflow.progress.updated for workflow {}: {}/{} steps", workflowId, stepsCompleted, totalSteps);

        workflowRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
            .ifPresentOrElse(
                projection -> {
                    projection.setStepsCompleted(stepsCompleted);
                    projection.setTotalSteps(totalSteps);
                    if (totalSteps > 0) {
                        projection.setProgressPercentage((stepsCompleted * 100) / totalSteps);
                    }
                    projection.setLastUpdatedAt(Instant.now());
                    workflowRepository.save(projection);
                    log.debug("Updated progress for workflow {}", workflowId);
                },
                () -> log.warn("Workflow projection not found for workflow {}", workflowId)
            );
    }

    /**
     * Handle workflow.completed event
     * Marks workflow as completed
     */
    @KafkaListener(
        topics = "workflow.completed",
        groupId = "clinical-workflow-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowCompleted(String tenantId, UUID workflowId) {
        log.debug("Processing workflow.completed for workflow {}", workflowId);

        workflowRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
            .ifPresentOrElse(
                projection -> {
                    projection.setStatus("COMPLETED");
                    projection.setCompletedAt(Instant.now());
                    projection.setProgressPercentage(100);
                    projection.setIsOverdue(false);  // Can't be overdue if completed
                    projection.setLastUpdatedAt(Instant.now());
                    workflowRepository.save(projection);
                    log.info("Completed workflow projection for workflow {}", workflowId);
                },
                () -> log.warn("Workflow projection not found for workflow {}", workflowId)
            );
    }

    /**
     * Handle workflow.cancelled event
     * Cancels the workflow
     */
    @KafkaListener(
        topics = "workflow.cancelled",
        groupId = "clinical-workflow-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onWorkflowCancelled(String tenantId, UUID workflowId) {
        log.debug("Processing workflow.cancelled for workflow {}", workflowId);

        workflowRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
            .ifPresentOrElse(
                projection -> {
                    projection.setStatus("CANCELLED");
                    projection.setLastUpdatedAt(Instant.now());
                    workflowRepository.save(projection);
                    log.info("Cancelled workflow projection for workflow {}", workflowId);
                },
                () -> log.warn("Workflow projection not found for workflow {}", workflowId)
            );
    }

    /**
     * Handle workflow.review.required event
     * Marks workflow as requiring review
     */
    @KafkaListener(
        topics = "workflow.review.required",
        groupId = "clinical-workflow-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onReviewRequired(String tenantId, UUID workflowId) {
        log.debug("Processing workflow.review.required for workflow {}", workflowId);

        workflowRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
            .ifPresentOrElse(
                projection -> {
                    projection.setRequiresReview(true);
                    projection.setLastUpdatedAt(Instant.now());
                    workflowRepository.save(projection);
                    log.debug("Marked workflow {} as requiring review", workflowId);
                },
                () -> log.warn("Workflow projection not found for workflow {}", workflowId)
            );
    }

    /**
     * Handle workflow.blocking.issue event
     * Marks workflow as having blocking issues
     */
    @KafkaListener(
        topics = "workflow.blocking.issue",
        groupId = "clinical-workflow-event-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void onBlockingIssue(String tenantId, UUID workflowId) {
        log.debug("Processing workflow.blocking.issue for workflow {}", workflowId);

        workflowRepository.findByTenantIdAndWorkflowId(tenantId, workflowId)
            .ifPresentOrElse(
                projection -> {
                    projection.setHasBlockingIssue(true);
                    projection.setLastUpdatedAt(Instant.now());
                    workflowRepository.save(projection);
                    log.debug("Marked workflow {} as having blocking issues", workflowId);
                },
                () -> log.warn("Workflow projection not found for workflow {}", workflowId)
            );
    }
}
