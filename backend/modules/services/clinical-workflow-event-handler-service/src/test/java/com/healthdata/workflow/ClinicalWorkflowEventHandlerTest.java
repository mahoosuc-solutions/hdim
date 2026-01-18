package com.healthdata.workflow;

import com.healthdata.workflow.event.*;
import com.healthdata.workflow.eventhandler.ClinicalWorkflowEventHandler;
import com.healthdata.workflow.projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.within;

/**
 * RED Phase Tests for ClinicalWorkflowEventHandler
 *
 * Tests validate clinical workflow event handling:
 * - Workflow instance creation and state management
 * - Step execution and completion tracking
 * - Workflow task assignment and routing
 * - Approval workflows and decision tracking
 * - Temporal workflow lifecycle management
 * - Multi-tenant workflow isolation
 * - Error handling and workflow recovery
 */
@DisplayName("ClinicalWorkflowEventHandler Tests")
class ClinicalWorkflowEventHandlerTest {

    private ClinicalWorkflowEventHandler workflowEventHandler;
    private MockWorkflowProjectionStore projectionStore;
    private MockEventStore eventStore;

    @BeforeEach
    void setup() {
        projectionStore = new MockWorkflowProjectionStore();
        eventStore = new MockEventStore();
        workflowEventHandler = new ClinicalWorkflowEventHandler(projectionStore, eventStore);
    }

    // ===== Workflow Initialization Tests =====

    @Test
    @DisplayName("Should create workflow instance from WorkflowInitiatedEvent")
    void testWorkflowInitiation() {
        // Given: Workflow initiation event
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";
        String workflowType = "TREATMENT_PLAN_REVIEW";

        WorkflowInitiatedEvent event = new WorkflowInitiatedEvent(
            tenantId, patientId, workflowType, "Treatment Plan Review Workflow"
        );

        // When: Event is handled
        workflowEventHandler.handle(event);

        // Then: Projection should be created
        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            patientId, tenantId, workflowType
        );
        assertThat(projection).isNotNull();
        assertThat(projection.getWorkflowType()).isEqualTo("TREATMENT_PLAN_REVIEW");
        assertThat(projection.getStatus()).isEqualTo("INITIATED");
    }

    @Test
    @DisplayName("Should store workflow event in event store")
    void testWorkflowEventStorage() {
        // Given: Workflow event
        WorkflowInitiatedEvent event = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-456", "MEDICATION_REVIEW", "Medication Review Workflow"
        );

        // When: Event is handled
        workflowEventHandler.handle(event);

        // Then: Event should be stored
        assertThat(eventStore.getEventCount()).isGreaterThan(0);
        assertThat(eventStore.getLastEventType()).isEqualTo("WorkflowInitiatedEvent");
    }

    // ===== Step Execution Tests =====

    @Test
    @DisplayName("Should track step execution progression")
    void testStepExecution() {
        // Given: Initiated workflow
        WorkflowInitiatedEvent initEvent = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-789", "CARE_COORDINATION", "Care Coordination Workflow"
        );
        workflowEventHandler.handle(initEvent);

        // When: Step is executed
        WorkflowStepExecutedEvent stepEvent = new WorkflowStepExecutedEvent(
            "TENANT-001", "PATIENT-789", "CARE_COORDINATION", "ASSESSMENT", "Step assessment completed"
        );
        workflowEventHandler.handle(stepEvent);

        // Then: Step should be recorded
        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            "PATIENT-789", "TENANT-001", "CARE_COORDINATION"
        );
        assertThat(projection.getCurrentStep()).isEqualTo("ASSESSMENT");
    }

    @Test
    @DisplayName("Should track step completion with outcomes")
    void testStepCompletion() {
        // Given: Running workflow
        WorkflowInitiatedEvent initEvent = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-111", "REFERRAL_MANAGEMENT", "Referral Management"
        );
        workflowEventHandler.handle(initEvent);

        // When: Step completed
        WorkflowStepCompletedEvent completionEvent = new WorkflowStepCompletedEvent(
            "TENANT-001", "PATIENT-111", "REFERRAL_MANAGEMENT", "AUTHORIZATION", true, "Authorization approved"
        );
        workflowEventHandler.handle(completionEvent);

        // Then: Completion should be recorded
        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            "PATIENT-111", "TENANT-001", "REFERRAL_MANAGEMENT"
        );
        assertThat(projection.isLastStepSuccessful()).isTrue();
    }

    // ===== Task Assignment Tests =====

    @Test
    @DisplayName("Should track task assignment and routing")
    void testTaskAssignment() {
        // Given: Workflow with task
        WorkflowInitiatedEvent initEvent = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-222", "APPROVAL_WORKFLOW", "Approval Workflow"
        );
        workflowEventHandler.handle(initEvent);

        // When: Task assigned
        TaskAssignedEvent assignmentEvent = new TaskAssignedEvent(
            "TENANT-001", "PATIENT-222", "APPROVAL_WORKFLOW", "reviewer@hospital.com", "HIGH_PRIORITY"
        );
        workflowEventHandler.handle(assignmentEvent);

        // Then: Assignment should be recorded
        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            "PATIENT-222", "TENANT-001", "APPROVAL_WORKFLOW"
        );
        assertThat(projection.getAssignedTo()).isNotEmpty();
    }

    // ===== Approval Workflow Tests =====

    @Test
    @DisplayName("Should track approval decisions")
    void testApprovalDecision() {
        // Given: Initiated workflow
        WorkflowInitiatedEvent initEvent = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-333", "CLINICAL_APPROVAL", "Clinical Approval"
        );
        workflowEventHandler.handle(initEvent);

        // When: Approval decision made
        ApprovalDecisionEvent decisionEvent = new ApprovalDecisionEvent(
            "TENANT-001", "PATIENT-333", "CLINICAL_APPROVAL", "APPROVED", "Meets all criteria", "dr.smith@hospital.com"
        );
        workflowEventHandler.handle(decisionEvent);

        // Then: Decision should be recorded
        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            "PATIENT-333", "TENANT-001", "CLINICAL_APPROVAL"
        );
        assertThat(projection.getApprovalStatus()).isEqualTo("APPROVED");
    }

    // ===== Workflow Completion Tests =====

    @Test
    @DisplayName("Should mark workflow as completed")
    void testWorkflowCompletion() {
        // Given: Running workflow
        WorkflowInitiatedEvent initEvent = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-444", "DISCHARGE_PLANNING", "Discharge Planning"
        );
        workflowEventHandler.handle(initEvent);

        // When: Workflow completed
        WorkflowCompletedEvent completionEvent = new WorkflowCompletedEvent(
            "TENANT-001", "PATIENT-444", "DISCHARGE_PLANNING", "Successfully completed discharge planning", "COMPLETED"
        );
        workflowEventHandler.handle(completionEvent);

        // Then: Status should be COMPLETED
        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            "PATIENT-444", "TENANT-001", "DISCHARGE_PLANNING"
        );
        assertThat(projection.getStatus()).isEqualTo("COMPLETED");
    }

    // ===== Temporal Tracking Tests =====

    @Test
    @DisplayName("Should track workflow duration")
    void testWorkflowDuration() {
        // Given: Initiated workflow
        WorkflowInitiatedEvent event = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-555", "ASSESSMENT_WORKFLOW", "Assessment"
        );
        workflowEventHandler.handle(event);

        // When/Then: Duration should be trackable
        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            "PATIENT-555", "TENANT-001", "ASSESSMENT_WORKFLOW"
        );
        assertThat(projection.getInitiatedDate()).isNotNull();
    }

    @Test
    @DisplayName("Should track workflow state transitions")
    void testStateTransitions() {
        // Given: Workflow lifecycle
        WorkflowInitiatedEvent initEvent = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-666", "QUALITY_REVIEW", "Quality Review"
        );
        workflowEventHandler.handle(initEvent);

        WorkflowProgressedEvent progressEvent = new WorkflowProgressedEvent(
            "TENANT-001", "PATIENT-666", "QUALITY_REVIEW", "IN_PROGRESS", "Assessment started"
        );
        workflowEventHandler.handle(progressEvent);

        // When/Then: Progression should be recorded
        WorkflowProjection projection = projectionStore.getWorkflowProjection(
            "PATIENT-666", "TENANT-001", "QUALITY_REVIEW"
        );
        assertThat(projection.getStatus()).isEqualTo("IN_PROGRESS");
    }

    // ===== Multi-Tenant Isolation Tests =====

    @Test
    @DisplayName("Should isolate workflows by tenant")
    void testMultiTenantIsolation() {
        // Given: Same workflow in different tenants
        String patientId = "PATIENT-999";
        String workflowType = "CARE_PLAN";

        WorkflowInitiatedEvent tenant1Event = new WorkflowInitiatedEvent(
            "TENANT-001", patientId, workflowType, "Tenant 1 workflow"
        );
        WorkflowInitiatedEvent tenant2Event = new WorkflowInitiatedEvent(
            "TENANT-002", patientId, workflowType, "Tenant 2 workflow"
        );

        // When: Events handled for both tenants
        workflowEventHandler.handle(tenant1Event);
        workflowEventHandler.handle(tenant2Event);

        // Then: Projections should be isolated
        WorkflowProjection tenant1Projection = projectionStore.getWorkflowProjection(
            patientId, "TENANT-001", workflowType
        );
        WorkflowProjection tenant2Projection = projectionStore.getWorkflowProjection(
            patientId, "TENANT-002", workflowType
        );

        assertThat(tenant1Projection).isNotNull();
        assertThat(tenant2Projection).isNotNull();
    }

    // ===== Idempotency Tests =====

    @Test
    @DisplayName("Should handle duplicate workflow events idempotently")
    void testIdempotentWorkflowInitiation() {
        // Given: Workflow event
        WorkflowInitiatedEvent event = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-101", "TEST_WORKFLOW", "Test Workflow"
        );

        // When: Same event handled twice
        workflowEventHandler.handle(event);
        WorkflowProjection projection1 = projectionStore.getWorkflowProjection(
            "PATIENT-101", "TENANT-001", "TEST_WORKFLOW"
        );
        long version1 = projection1.getVersion();

        workflowEventHandler.handle(event);
        WorkflowProjection projection2 = projectionStore.getWorkflowProjection(
            "PATIENT-101", "TENANT-001", "TEST_WORKFLOW"
        );
        long version2 = projection2.getVersion();

        // Then: Version should not significantly increase
        assertThat(version2).isLessThanOrEqualTo(version1 + 1);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle null workflow event")
    void testNullWorkflowEvent() {
        // When/Then: Should throw validation error
        assertThatThrownBy(() -> workflowEventHandler.handle((WorkflowInitiatedEvent) null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate workflow type")
    void testInvalidWorkflowType() {
        // Given: Event with null workflow type
        WorkflowInitiatedEvent event = new WorkflowInitiatedEvent(
            "TENANT-001", "PATIENT-202", null, "Invalid workflow"
        );

        // When/Then: Should throw validation error
        assertThatThrownBy(() -> workflowEventHandler.handle(event))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate tenant ID")
    void testMissingTenantId() {
        // Given: Event with null tenant
        WorkflowInitiatedEvent event = new WorkflowInitiatedEvent(
            null, "PATIENT-303", "TEST_WORKFLOW", "Test Workflow"
        );

        // When/Then: Should throw validation error
        assertThatThrownBy(() -> workflowEventHandler.handle(event))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Mock Classes =====

    static class MockWorkflowProjectionStore implements ClinicalWorkflowEventHandler.WorkflowProjectionStore {
        private final java.util.Map<String, WorkflowProjection> workflowStore = new java.util.HashMap<>();

        @Override
        public void saveWorkflowProjection(WorkflowProjection projection) {
            String key = projection.getTenantId() + ":" + projection.getPatientId() + ":" + projection.getWorkflowType();
            workflowStore.put(key, projection);
        }

        @Override
        public WorkflowProjection getWorkflowProjection(String patientId, String tenantId, String workflowType) {
            String key = tenantId + ":" + patientId + ":" + workflowType;
            return workflowStore.get(key);
        }
    }

    static class MockEventStore implements ClinicalWorkflowEventHandler.EventStore {
        private int eventCount = 0;
        private String lastEventType = "";

        @Override
        public void storeEvent(Object event) {
            eventCount++;
            lastEventType = event.getClass().getSimpleName();
        }

        int getEventCount() { return eventCount; }
        String getLastEventType() { return lastEventType; }
    }
}
