package com.healthdata.workflow.event;

import java.time.Instant;

public class WorkflowProgressedEvent {
    private final String tenantId;
    private final String patientId;
    private final String workflowType;
    private final String newStatus;  // IN_PROGRESS, BLOCKED, AWAITING_INPUT
    private final String description;
    private final Instant timestamp;

    public WorkflowProgressedEvent(String tenantId, String patientId, String workflowType, String newStatus, String description) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.workflowType = workflowType;
        this.newStatus = newStatus;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getWorkflowType() { return workflowType; }
    public String getNewStatus() { return newStatus; }
    public String getDescription() { return description; }
    public Instant getTimestamp() { return timestamp; }
}
