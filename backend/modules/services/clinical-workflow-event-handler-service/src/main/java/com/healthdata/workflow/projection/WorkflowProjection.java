package com.healthdata.workflow.projection;

import java.time.Instant;
import java.time.LocalDate;

/**
 * WorkflowProjection - Denormalized read model for workflow state tracking
 *
 * Built from workflow events via event sourcing.
 * Optimized for workflow queries (status, duration, assignments).
 */
public class WorkflowProjection {
    private final String patientId;
    private final String tenantId;
    private final String workflowType;
    private String status;  // INITIATED, IN_PROGRESS, COMPLETED, CANCELLED
    private String currentStep;
    private String assignedTo;
    private boolean lastStepSuccessful;
    private String approvalStatus;  // APPROVED, DENIED, PENDING_REVIEW
    private LocalDate initiatedDate;
    private LocalDate completedDate;
    private long version;
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
