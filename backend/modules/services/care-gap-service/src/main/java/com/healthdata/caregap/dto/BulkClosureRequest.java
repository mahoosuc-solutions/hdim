package com.healthdata.caregap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk Closure Request DTO
 *
 * Request payload for closing multiple care gaps in a single operation.
 * Used for bulk gap closure workflows in the Clinical Portal.
 *
 * Issue #241: Care Gap Bulk Actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkClosureRequest {

    /**
     * List of care gap IDs to close (UUID strings)
     */
    @NotEmpty(message = "Gap IDs cannot be empty")
    private List<String> gapIds;

    /**
     * Reason for closure
     * Valid values: completed, not-applicable, patient-declined, other
     */
    @NotBlank(message = "Closure reason is required")
    private String closureReason;

    /**
     * Optional notes/comments about the closure
     */
    private String notes;

    /**
     * User/system performing the closure
     */
    @NotBlank(message = "Closed by is required")
    private String closedBy;

    /**
     * Optional action taken to close the gap
     * Examples: "Vaccination administered", "Screening completed", etc.
     */
    private String closureAction;
}
