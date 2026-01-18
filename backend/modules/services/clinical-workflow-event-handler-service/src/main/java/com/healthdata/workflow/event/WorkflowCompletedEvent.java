package com.healthdata.workflow.event;

import java.time.Instant;

public class WorkflowCompletedEvent {
    private final String tenantId;
    private final String patientId;
    private final String workflowType;
    private final String summary;
    private final String status;  // COMPLETED, CANCELLED, ABANDONED
    private final Instant timestamp;

    public WorkflowCompletedEvent(String tenantId, String patientId, String workflowType, String summary, String status) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.workflowType = workflowType;
        this.summary = summary;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getWorkflowType() { return workflowType; }
    public String getSummary() { return summary; }
    public String getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
}
