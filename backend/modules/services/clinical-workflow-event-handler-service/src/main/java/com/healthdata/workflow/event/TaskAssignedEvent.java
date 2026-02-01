package com.healthdata.workflow.event;

import java.time.Instant;

public class TaskAssignedEvent {
    private final String tenantId;
    private final String patientId;
    private final String workflowType;
    private final String assignedTo;
    private final String priority;
    private final Instant timestamp;

    public TaskAssignedEvent(String tenantId, String patientId, String workflowType, String assignedTo, String priority) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.workflowType = workflowType;
        this.assignedTo = assignedTo;
        this.priority = priority;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getWorkflowType() { return workflowType; }
    public String getAssignedTo() { return assignedTo; }
    public String getPriority() { return priority; }
    public Instant getTimestamp() { return timestamp; }
}
