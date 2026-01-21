package com.healthdata.workflow.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowEventResponse {

    private String workflowId;  // For response mapping
    private String workflowType;
    private String status;  // INITIATED, IN_PROGRESS, COMPLETED, CANCELLED
    private String currentStep;
    private String assignedTo;
    private boolean lastStepSuccessful;
    private String approvalStatus;  // APPROVED, DENIED, PENDING_REVIEW
    private LocalDate initiatedDate;
    private LocalDate completedDate;
    private long version;
    private Instant timestamp;
}
