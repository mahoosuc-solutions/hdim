package com.healthdata.clinicalworkflowevent.listener;

import com.healthdata.clinicalworkflowevent.projection.WorkflowProjection;
import com.healthdata.clinicalworkflowevent.repository.WorkflowProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Mock-based unit tests for WorkflowEventListener
 *
 * No database or Kafka required - validates event processing logic only
 *
 * ★ Insight ─────────────────────────────────────
 * Workflow event listeners implement the CQRS Query side pattern:
 *
 * 1. **Event-Driven Updates** - Kafka events trigger projection updates
 * 2. **Lifecycle Tracking** - PENDING → IN_PROGRESS → COMPLETED/CANCELLED
 * 3. **Progress Calculation** - stepsCompleted / totalSteps * 100
 * 4. **Status Flags** - requiresReview, hasBlockingIssue, isOverdue
 *
 * These projections optimize read queries by pre-computing derived fields.
 * Tests verify correct state transitions and field calculations.
 * ─────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class WorkflowEventListenerTest {

    @Mock
    private WorkflowProjectionRepository workflowRepository;

    @InjectMocks
    private WorkflowEventListener eventListener;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID WORKFLOW_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String USER_ID = "user-123";

    // ==================== onWorkflowStarted() Tests ====================

    @Test
    void onWorkflowStarted_ShouldCreateNewWorkflow() {
        // Given
        String workflowType = "APPOINTMENT_SCHEDULING";
        String priority = "HIGH";
        String description = "Schedule diabetes follow-up";
        Integer totalSteps = 5;

        // When
        eventListener.onWorkflowStarted(TENANT_ID, WORKFLOW_ID, PATIENT_ID, workflowType, priority, description, totalSteps);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getWorkflowId()).isEqualTo(WORKFLOW_ID);
        assertThat(saved.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(saved.getWorkflowType()).isEqualTo(workflowType);
        assertThat(saved.getPriority()).isEqualTo(priority);
        assertThat(saved.getDescription()).isEqualTo(description);
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getProgressPercentage()).isEqualTo(0);
        assertThat(saved.getStepsCompleted()).isEqualTo(0);
        assertThat(saved.getTotalSteps()).isEqualTo(5);
        assertThat(saved.getDaysPending()).isEqualTo(0);
        assertThat(saved.getIsOverdue()).isFalse();
        assertThat(saved.getRequiresReview()).isFalse();
        assertThat(saved.getHasBlockingIssue()).isFalse();
        assertThat(saved.getEventVersion()).isEqualTo(1L);
    }

    @Test
    void onWorkflowStarted_ShouldDefaultToOneStep_WhenTotalStepsNull() {
        // Given
        Integer totalSteps = null;

        // When
        eventListener.onWorkflowStarted(TENANT_ID, WORKFLOW_ID, PATIENT_ID, "REFERRAL", "MEDIUM", "Referral to specialist", totalSteps);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getTotalSteps()).isEqualTo(1);  // defaults to 1
    }

    // ==================== onWorkflowAssigned() Tests ====================

    @Test
    void onWorkflowAssigned_ShouldUpdateAssignment_WhenWorkflowExists() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .status("PENDING")
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When
        eventListener.onWorkflowAssigned(TENANT_ID, WORKFLOW_ID, USER_ID);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getAssignedTo()).isEqualTo(USER_ID);
        assertThat(saved.getAssignedAt()).isNotNull();
        assertThat(saved.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void onWorkflowAssigned_ShouldTransitionToInProgress_WhenStatusPENDING() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .status("PENDING")
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When
        eventListener.onWorkflowAssigned(TENANT_ID, WORKFLOW_ID, USER_ID);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(saved.getStartedAt()).isNotNull();
    }

    @Test
    void onWorkflowAssigned_ShouldNotChangeStatus_WhenAlreadyInProgress() {
        // Given - workflow already IN_PROGRESS
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .status("IN_PROGRESS")
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When
        eventListener.onWorkflowAssigned(TENANT_ID, WORKFLOW_ID, USER_ID);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("IN_PROGRESS");  // unchanged
    }

    @Test
    void onWorkflowAssigned_ShouldLogWarning_WhenWorkflowNotFound() {
        // Given
        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.empty());

        // When
        eventListener.onWorkflowAssigned(TENANT_ID, WORKFLOW_ID, USER_ID);

        // Then
        verify(workflowRepository, never()).save(any());
    }

    // ==================== onWorkflowReassigned() Tests ====================

    @Test
    void onWorkflowReassigned_ShouldUpdateAssignment_WhenWorkflowExists() {
        // Given
        String oldAssignee = "user-456";
        String newAssignee = "user-789";

        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .assignedTo(oldAssignee)
            .status("IN_PROGRESS")
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When
        eventListener.onWorkflowReassigned(TENANT_ID, WORKFLOW_ID, oldAssignee, newAssignee);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getPreviousAssignee()).isEqualTo(oldAssignee);
        assertThat(saved.getAssignedTo()).isEqualTo(newAssignee);
        assertThat(saved.getAssignedAt()).isNotNull();
        assertThat(saved.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void onWorkflowReassigned_ShouldLogWarning_WhenWorkflowNotFound() {
        // Given
        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.empty());

        // When
        eventListener.onWorkflowReassigned(TENANT_ID, WORKFLOW_ID, "old", "new");

        // Then
        verify(workflowRepository, never()).save(any());
    }

    // ==================== onProgressUpdated() Tests ====================

    @Test
    void onProgressUpdated_ShouldUpdateProgressPercentage() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .stepsCompleted(0)
            .totalSteps(10)
            .progressPercentage(0)
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When - complete 5 of 10 steps
        eventListener.onProgressUpdated(TENANT_ID, WORKFLOW_ID, 5, 10);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getStepsCompleted()).isEqualTo(5);
        assertThat(saved.getTotalSteps()).isEqualTo(10);
        assertThat(saved.getProgressPercentage()).isEqualTo(50);  // 5/10 * 100
        assertThat(saved.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void onProgressUpdated_ShouldCalculate100Percent_WhenAllStepsCompleted() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .stepsCompleted(8)
            .totalSteps(10)
            .progressPercentage(80)
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When - complete all 10 steps
        eventListener.onProgressUpdated(TENANT_ID, WORKFLOW_ID, 10, 10);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getProgressPercentage()).isEqualTo(100);
    }

    @Test
    void onProgressUpdated_ShouldNotDivideByZero_WhenTotalStepsZero() {
        // Given - edge case: totalSteps = 0
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .stepsCompleted(0)
            .totalSteps(1)
            .progressPercentage(0)
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When - totalSteps = 0 should not crash
        eventListener.onProgressUpdated(TENANT_ID, WORKFLOW_ID, 0, 0);

        // Then - no division by zero exception, progressPercentage unchanged
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getProgressPercentage()).isEqualTo(0);  // unchanged
    }

    @Test
    void onProgressUpdated_ShouldLogWarning_WhenWorkflowNotFound() {
        // Given
        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.empty());

        // When
        eventListener.onProgressUpdated(TENANT_ID, WORKFLOW_ID, 5, 10);

        // Then
        verify(workflowRepository, never()).save(any());
    }

    // ==================== onWorkflowCompleted() Tests ====================

    @Test
    void onWorkflowCompleted_ShouldMarkCompleted() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .status("IN_PROGRESS")
            .progressPercentage(80)
            .isOverdue(true)  // was overdue
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When
        eventListener.onWorkflowCompleted(TENANT_ID, WORKFLOW_ID);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getCompletedAt()).isNotNull();
        assertThat(saved.getProgressPercentage()).isEqualTo(100);  // forced to 100
        assertThat(saved.getIsOverdue()).isFalse();  // cleared on completion
        assertThat(saved.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void onWorkflowCompleted_ShouldLogWarning_WhenWorkflowNotFound() {
        // Given
        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.empty());

        // When
        eventListener.onWorkflowCompleted(TENANT_ID, WORKFLOW_ID);

        // Then
        verify(workflowRepository, never()).save(any());
    }

    // ==================== onWorkflowCancelled() Tests ====================

    @Test
    void onWorkflowCancelled_ShouldMarkCancelled() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .status("IN_PROGRESS")
            .progressPercentage(30)
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When
        eventListener.onWorkflowCancelled(TENANT_ID, WORKFLOW_ID);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("CANCELLED");
        assertThat(saved.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void onWorkflowCancelled_ShouldLogWarning_WhenWorkflowNotFound() {
        // Given
        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.empty());

        // When
        eventListener.onWorkflowCancelled(TENANT_ID, WORKFLOW_ID);

        // Then
        verify(workflowRepository, never()).save(any());
    }

    // ==================== onReviewRequired() Tests ====================

    @Test
    void onReviewRequired_ShouldSetRequiresReviewFlag() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .status("IN_PROGRESS")
            .requiresReview(false)
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When
        eventListener.onReviewRequired(TENANT_ID, WORKFLOW_ID);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getRequiresReview()).isTrue();
        assertThat(saved.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void onReviewRequired_ShouldLogWarning_WhenWorkflowNotFound() {
        // Given
        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.empty());

        // When
        eventListener.onReviewRequired(TENANT_ID, WORKFLOW_ID);

        // Then
        verify(workflowRepository, never()).save(any());
    }

    // ==================== onBlockingIssue() Tests ====================

    @Test
    void onBlockingIssue_ShouldSetBlockingIssueFlag() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .status("IN_PROGRESS")
            .hasBlockingIssue(false)
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When
        eventListener.onBlockingIssue(TENANT_ID, WORKFLOW_ID);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getHasBlockingIssue()).isTrue();
        assertThat(saved.getLastUpdatedAt()).isNotNull();
    }

    @Test
    void onBlockingIssue_ShouldLogWarning_WhenWorkflowNotFound() {
        // Given
        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.empty());

        // When
        eventListener.onBlockingIssue(TENANT_ID, WORKFLOW_ID);

        // Then
        verify(workflowRepository, never()).save(any());
    }

    // ==================== Edge Cases & State Transitions ====================

    @Test
    void onWorkflowStarted_ShouldHandleLongDescriptions() {
        // Given
        String longDescription = "A".repeat(500);

        // When
        eventListener.onWorkflowStarted(TENANT_ID, WORKFLOW_ID, PATIENT_ID, "TEST", "LOW", longDescription, 1);

        // Then
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getDescription()).isEqualTo(longDescription);
    }

    @Test
    void onProgressUpdated_ShouldHandlePartialProgress() {
        // Given
        WorkflowProjection existing = WorkflowProjection.builder()
            .tenantId(TENANT_ID)
            .workflowId(WORKFLOW_ID)
            .stepsCompleted(0)
            .totalSteps(3)
            .progressPercentage(0)
            .build();

        when(workflowRepository.findByTenantIdAndWorkflowId(TENANT_ID, WORKFLOW_ID))
            .thenReturn(Optional.of(existing));

        // When - complete 1 of 3 steps
        eventListener.onProgressUpdated(TENANT_ID, WORKFLOW_ID, 1, 3);

        // Then - 1/3 = 33% (integer division)
        ArgumentCaptor<WorkflowProjection> captor = ArgumentCaptor.forClass(WorkflowProjection.class);
        verify(workflowRepository).save(captor.capture());

        WorkflowProjection saved = captor.getValue();
        assertThat(saved.getProgressPercentage()).isEqualTo(33);  // 1/3 * 100 = 33
    }
}
