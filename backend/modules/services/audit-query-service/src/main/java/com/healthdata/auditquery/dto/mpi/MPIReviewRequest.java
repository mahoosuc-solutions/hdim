package com.healthdata.auditquery.dto.mpi;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for MPI merge review actions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MPI merge review request")
public class MPIReviewRequest {

    @Schema(description = "Validation notes", example = "Verified demographic match across all fields")
    @Size(max = 2000, message = "Validation notes must not exceed 2000 characters")
    private String validationNotes;

    @Schema(description = "Rollback reason", example = "Incorrect patient match detected")
    @Size(max = 2000, message = "Rollback reason must not exceed 2000 characters")
    private String rollbackReason;

    @Schema(description = "Data quality assessment", example = "HIGH")
    @Size(max = 20, message = "Assessment must not exceed 20 characters")
    private String dataQualityAssessment;  // HIGH, MEDIUM, LOW

    @Schema(description = "Data quality notes", example = "All critical fields populated and consistent")
    @Size(max = 2000, message = "Data quality notes must not exceed 2000 characters")
    private String dataQualityNotes;

    @Schema(description = "Resolution notes", example = "Corrected SSN mismatch via manual verification")
    @Size(max = 2000, message = "Resolution notes must not exceed 2000 characters")
    private String resolutionNotes;
}
