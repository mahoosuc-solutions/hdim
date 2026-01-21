package com.healthdata.audit.dto.mpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to rollback an incorrect MPI merge
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIRollbackRequest {
    @NotBlank(message = "Rollback reason is required")
    @Size(max = 2000, message = "Rollback reason cannot exceed 2000 characters")
    private String rollbackReason;
    
    private Boolean recreateSourcePatient;
    private Boolean preserveTargetPatient;
    private String rollbackStrategy;  // FULL_ROLLBACK, PARTIAL_ROLLBACK, SPLIT_RECORDS
}
