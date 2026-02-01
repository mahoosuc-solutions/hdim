package com.healthdata.audit.dto.mpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Filter criteria for MPI merge history queries
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIMergeFilter {
    private String mergeStatus;        // PENDING, VALIDATED, ROLLED_BACK, FAILED
    private String validationStatus;   // NOT_VALIDATED, VALIDATED, VALIDATION_FAILED
    private LocalDate startDate;
    private LocalDate endDate;
    private String analystId;
    private String mergeType;          // AUTOMATIC, MANUAL, ASSISTED
    private Double minConfidenceScore;
    private Double maxConfidenceScore;
    private Boolean hasDataQualityIssues;
}
