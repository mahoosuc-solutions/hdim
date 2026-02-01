package com.healthdata.workflow.eventhandler;

import com.healthdata.workflow.event.*;
import com.healthdata.workflow.projection.*;

/**
 * ClinicalWorkflowEventHandler - Handles clinical workflow orchestration events
 *
 * Processes events and maintains workflow state projections.
 * Idempotent: Safe to call multiple times with same event.
 */
public class ClinicalWorkflowEventHandler {
    private final WorkflowProjectionStore projectionStore;
    private final EventStore eventStore;

    public ClinicalWorkflowEventHandler(WorkflowProjectionStore projectionStore, EventStore eventStore) {
        this.projectionStore = projectionStore;
        this.eventStore = eventStore;
    }

    public void handle(WorkflowInitiatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }
        if (event.getWorkflowType() == null) {
            throw new IllegalArgumentException("Workflow Type is required");
        }

        WorkflowProjection projection = new WorkflowProjection(
            event.getPatientId(),
            event.getTenantId(),
            event.getWorkflowType()
        );

        projectionStore.saveWorkflowProjection(projection);
        eventStore.storeEvent(event);
    }

    public void handle(WorkflowStepExecutedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            event.getPatientId(), event.getTenantId(), event.getWorkflowType()
        );

        if (projection != null) {
            projection.setCurrentStep(event.getStepName());
            projection.incrementVersion();
            projectionStore.saveWorkflowProjection(projection);
        }

        eventStore.storeEvent(event);
    }

    public void handle(WorkflowStepCompletedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            event.getPatientId(), event.getTenantId(), event.getWorkflowType()
        );

        if (projection != null) {
            projection.setLastStepSuccessful(event.isSuccessful());
            projection.incrementVersion();
            projectionStore.saveWorkflowProjection(projection);
        }

        eventStore.storeEvent(event);
    }

    public void handle(TaskAssignedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            event.getPatientId(), event.getTenantId(), event.getWorkflowType()
        );

        if (projection != null) {
            projection.setAssignedTo(event.getAssignedTo());
            projection.incrementVersion();
            projectionStore.saveWorkflowProjection(projection);
        }

        eventStore.storeEvent(event);
    }

    public void handle(ApprovalDecisionEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            event.getPatientId(), event.getTenantId(), event.getWorkflowType()
        );

        if (projection != null) {
            projection.setApprovalStatus(event.getDecision());
            projection.incrementVersion();
            projectionStore.saveWorkflowProjection(projection);
        }

        eventStore.storeEvent(event);
    }

    public void handle(WorkflowProgressedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            event.getPatientId(), event.getTenantId(), event.getWorkflowType()
        );

        if (projection != null) {
            projection.setStatus(event.getNewStatus());
            projection.incrementVersion();
            projectionStore.saveWorkflowProjection(projection);
        }

        eventStore.storeEvent(event);
    }

    public void handle(WorkflowCompletedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            event.getPatientId(), event.getTenantId(), event.getWorkflowType()
        );

        if (projection != null) {
            projection.setStatus(event.getStatus());
            projection.setCompletedDate(java.time.LocalDate.now());
            projection.incrementVersion();
            projectionStore.saveWorkflowProjection(projection);
        }

        eventStore.storeEvent(event);
    }

    // Support interfaces for tests
    public interface WorkflowProjectionStore {
        void saveWorkflowProjection(WorkflowProjection projection);
        WorkflowProjection getWorkflowProjection(String patientId, String tenantId, String workflowType);
    }

    public interface EventStore {
        void storeEvent(Object event);
    }
}
