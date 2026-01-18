package com.healthdata.workflow.event;

import java.time.Instant;

public class ApprovalDecisionEvent {
    private final String tenantId;
    private final String patientId;
    private final String workflowType;
    private final String decision;  // APPROVED, DENIED, PENDING_REVIEW
    private final String rationale;
    private final String approvedBy;
    private final Instant timestamp;

    public ApprovalDecisionEvent(String tenantId, String patientId, String workflowType, String decision, String rationale, String approvedBy) {
        this.tenantId = tenantId;
        this.patientId = patientId;
        this.workflowType = workflowType;
        this.decision = decision;
        this.rationale = rationale;
        this.approvedBy = approvedBy;
        this.timestamp = Instant.now();
    }

    public String getTenantId() { return tenantId; }
    public String getPatientId() { return patientId; }
    public String getWorkflowType() { return workflowType; }
    public String getDecision() { return decision; }
    public String getRationale() { return rationale; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getTimestamp() { return timestamp; }
}
