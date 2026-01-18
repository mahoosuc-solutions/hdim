package com.healthdata.workflow.event;

import java.time.Instant;

public class WorkflowStepCompletedEvent {
    private final String tenantId;
    private final String patientId;
    private final String workflowType;
    private final String stepName;
    private final boolean successful;
    private final String outcome;
    private final Instant timestamp;

    public WorkflowStepCompletedEvent(String tenantId, String patientId, String workflowType, String stepName, boolean successful, String outcome) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.workflowType = workflowType;
        this.stepName = stepName;
        this.successful = successful;
        this.outcome = outcome;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getWorkflowType() { return workflowType; }
    public String getStepName() { return stepName; }
    public boolean isSuccessful() { return successful; }
    public String getOutcome() { return outcome; }
    public Instant getTimestamp() { return timestamp; }
}
