package com.healthdata.audit.dto.mpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to validate an MPI merge
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIValidationRequest {
    @NotBlank(message = "Validation outcome is required")
    private String validationOutcome;  // VALID, INVALID, NEEDS_REVIEW
    
    @Size(max = 2000, message = "Validation notes cannot exceed 2000 characters")
    private String validationNotes;
    
    private Boolean hasMergeErrors;
    private Boolean hasDataQualityIssues;
    private String dataQualityAssessment;  // EXCELLENT, GOOD, FAIR, POOR
}
