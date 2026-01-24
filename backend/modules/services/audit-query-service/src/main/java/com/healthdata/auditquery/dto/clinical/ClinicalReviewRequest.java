package com.healthdata.auditquery.dto.clinical;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for clinical review actions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Clinical review request")
public class ClinicalReviewRequest {

    @Schema(description = "Clinical notes", example = "Recommendation validated against current guidelines")
    @Size(max = 2000, message = "Clinical notes must not exceed 2000 characters")
    private String clinicalNotes;

    @Schema(description = "Clinical rationale", example = "Patient contraindication present")
    @Size(max = 2000, message = "Clinical rationale must not exceed 2000 characters")
    private String clinicalRationale;

    @Schema(description = "Modifications", example = "Adjusted dosage based on renal function")
    @Size(max = 2000, message = "Modifications must not exceed 2000 characters")
    private String modifications;

    @Schema(description = "Alternative action", example = "Prescribed alternative medication")
    @Size(max = 1000, message = "Alternative action must not exceed 1000 characters")
    private String alternativeAction;

    @Schema(description = "Clinical reasoning", example = "Evidence-based alternative with better safety profile")
    @Size(max = 2000, message = "Clinical reasoning must not exceed 2000 characters")
    private String clinicalReasoning;

    @Schema(description = "Override reason", example = "Patient-specific contraindication")
    @Size(max = 1000, message = "Override reason must not exceed 1000 characters")
    private String overrideReason;
}
