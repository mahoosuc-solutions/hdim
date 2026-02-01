package com.healthdata.auditquery.dto.qa;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for QA review actions (approve, reject, flag, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "QA review request")
public class QAReviewRequest {

    @Schema(description = "Review notes", example = "AI decision validated against clinical guidelines")
    @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
    private String reviewNotes;

    @Schema(description = "Reviewer comments")
    @Size(max = 5000, message = "Comments must not exceed 5000 characters")
    private String comments;

    @Schema(description = "Rejection reason", example = "Confidence score below threshold")
    @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
    private String rejectionReason;

    @Schema(description = "Flag reason", example = "Requires clinical expert review")
    @Size(max = 1000, message = "Flag reason must not exceed 1000 characters")
    private String flagReason;

    @Schema(description = "False positive context")
    @Size(max = 2000, message = "Context must not exceed 2000 characters")
    private String falsePositiveContext;

    @Schema(description = "False negative context")
    @Size(max = 2000, message = "Context must not exceed 2000 characters")
    private String falseNegativeContext;
}
