package com.healthdata.audit.dto.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch Review Request
 * 
 * Request body for batch approve/reject operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchReviewRequest {
    
    @NotEmpty(message = "Decision IDs list cannot be empty")
    private List<String> decisionIds;
    
    @NotBlank(message = "Reviewer identifier is required")
    private String reviewedBy;
    
    @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
    private String reviewNotes;
}
