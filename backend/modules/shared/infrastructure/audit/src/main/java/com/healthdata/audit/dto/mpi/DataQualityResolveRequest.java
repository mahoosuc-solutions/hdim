package com.healthdata.audit.dto.mpi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to resolve a data quality issue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityResolveRequest {
    @NotBlank(message = "Resolution action is required")
    private String resolutionAction;  // ACCEPT_SUGGESTION, MANUAL_FIX, IGNORE, MERGE_REQUIRED
    
    private String correctedValue;
    
    @Size(max = 2000, message = "Resolution notes cannot exceed 2000 characters")
    private String resolutionNotes;
}
