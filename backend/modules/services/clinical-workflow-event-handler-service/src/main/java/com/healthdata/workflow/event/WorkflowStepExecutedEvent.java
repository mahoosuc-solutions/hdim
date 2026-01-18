package com.healthdata.workflow.event;

import java.time.Instant;

public class WorkflowStepExecutedEvent {
    private final String tenantId;
    private final String patientId;
    private final String workflowType;
    private final String stepName;
    private final String description;
    private final Instant timestamp;

    public WorkflowStepExecutedEvent(String tenantId, String patientId, String workflowType, String stepName, String description) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.workflowType = workflowType;
        this.stepName = stepName;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getWorkflowType() { return workflowType; }
    public String getStepName() { return stepName; }
    public String getDescription() { return description; }
    public Instant getTimestamp() { return timestamp; }
}
