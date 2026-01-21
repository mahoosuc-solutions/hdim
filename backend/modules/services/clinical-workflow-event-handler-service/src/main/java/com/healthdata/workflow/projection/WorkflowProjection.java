package com.healthdata.workflow.projection;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * WorkflowProjection - Denormalized read model for workflow state tracking
 *
 * Built from workflow events via event sourcing.
 * Optimized for workflow queries (status, duration, assignments).
 */
@Entity
@Table(name = "workflow_projections", indexes = {
    @Index(name = "idx_workflow_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_workflow_patient_tenant", columnList = "patient_id, tenant_id")
})
public class WorkflowProjection {
    @Id
    @Column(name = "id")
    private String id; // Composite key: patientId + "_" + tenantId + "_" + workflowType

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "workflow_type", nullable = false)
    private String workflowType;

    @Column(name = "status")
    private String status;  // INITIATED, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "last_step_successful")
    private boolean lastStepSuccessful;

    @Column(name = "approval_status")
    private String approvalStatus;  // APPROVED, DENIED, PENDING_REVIEW

    @Column(name = "initiated_date")
    private LocalDate initiatedDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "version")
    private long version;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    public WorkflowProjection(String patientId, String tenantId, String workflowType) {
        this.patientId = patientId;
        this.tenantId = tenantId;
        this.workflowType = workflowType;
        this.status = "INITIATED";
        this.lastStepSuccessful = false;
        this.approvalStatus = "PENDING_REVIEW";
        this.version = 1L;
        this.lastUpdated = Instant.now();
        this.initiatedDate = LocalDate.now();
    }

    // Getters
    public String getPatientId() { return patientId; }
    public String getTenantId() { return tenantId; }
    public String getWorkflowType() { return workflowType; }
    public String getStatus() { return status; }
    public String getCurrentStep() { return currentStep; }
    public String getAssignedTo() { return assignedTo; }
    public boolean isLastStepSuccessful() { return lastStepSuccessful; }
    public String getApprovalStatus() { return approvalStatus; }
    public LocalDate getInitiatedDate() { return initiatedDate; }
    public LocalDate getCompletedDate() { return completedDate; }
    public long getVersion() { return version; }
    public Instant getLastUpdated() { return lastUpdated; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public void setLastStepSuccessful(boolean successful) { this.lastStepSuccessful = successful; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public void setInitiatedDate(LocalDate initiatedDate) { this.initiatedDate = initiatedDate; }
    public void setCompletedDate(LocalDate completedDate) { this.completedDate = completedDate; }
    public void setVersion(long version) { this.version = version; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public void incrementVersion() {
        this.version++;
        this.lastUpdated = Instant.now();
    }
}
