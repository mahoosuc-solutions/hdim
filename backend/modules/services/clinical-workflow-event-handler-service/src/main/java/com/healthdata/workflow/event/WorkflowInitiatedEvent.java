package com.healthdata.workflow.event;

import java.time.Instant;

public class WorkflowInitiatedEvent {
    private final String tenantId;
    private final String patientId;
    private final String workflowType;
    private final String description;
    private final Instant timestamp;

    public WorkflowInitiatedEvent(String tenantId, String patientId, String workflowType, String description) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.workflowType = workflowType;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getWorkflowType() { return workflowType; }
    public String getDescription() { return description; }
    public Instant getTimestamp() { return timestamp; }
}
