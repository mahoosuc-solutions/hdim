package com.healthdata.audit.dto.mpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of an MPI merge rollback operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIRollbackResult {
    private String mergeId;
    private String rollbackStatus;
    private String rolledBackBy;
    private LocalDateTime rolledBackAt;
    private Boolean success;
    private String message;
    private String restoredSourcePatientId;
    private String updatedTargetPatientId;
}
