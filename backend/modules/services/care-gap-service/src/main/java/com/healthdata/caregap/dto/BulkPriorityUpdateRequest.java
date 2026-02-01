package com.healthdata.caregap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk Priority Update Request DTO
 *
 * Request payload for updating priority of multiple care gaps.
 * Used for bulk priority change workflows.
 *
 * Issue #241: Care Gap Bulk Actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPriorityUpdateRequest {

    /**
     * List of care gap IDs to update (UUID strings)
     */
    @NotEmpty(message = "Gap IDs cannot be empty")
    private List<String> gapIds;

    /**
     * New priority level
     * Valid values: HIGH, MEDIUM, LOW, CRITICAL
     */
    @NotBlank(message = "Priority is required")
    @Pattern(regexp = "^(HIGH|MEDIUM|LOW|CRITICAL)$",
             message = "Priority must be HIGH, MEDIUM, LOW, or CRITICAL")
    private String priority;
}
