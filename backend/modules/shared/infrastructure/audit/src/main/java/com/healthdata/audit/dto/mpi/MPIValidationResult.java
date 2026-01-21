package com.healthdata.audit.dto.mpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of an MPI merge validation operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIValidationResult {
    private String mergeId;
    private String validationOutcome;
    private String validatedBy;
    private LocalDateTime validatedAt;
    private Boolean success;
    private String message;
}
