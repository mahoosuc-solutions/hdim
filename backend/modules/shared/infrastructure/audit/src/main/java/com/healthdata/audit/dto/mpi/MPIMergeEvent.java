package com.healthdata.audit.dto.mpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MPI merge event in the review queue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIMergeEvent {
    private String mergeId;
    private String tenantId;
    private String sourcePatientId;
    private String targetPatientId;
    private String mergeType;              // AUTOMATIC, MANUAL, ASSISTED
    private Double confidenceScore;
    private String mergeStatus;            // PENDING, VALIDATED, ROLLED_BACK, FAILED
    private String validationStatus;       // NOT_VALIDATED, VALIDATED, VALIDATION_FAILED
    private LocalDateTime mergeTimestamp;
    private String performedBy;
    private Integer dataQualityIssueCount;
    private List<String> matchedAttributes;
    private String priority;               // CRITICAL, HIGH, MEDIUM, LOW
}
