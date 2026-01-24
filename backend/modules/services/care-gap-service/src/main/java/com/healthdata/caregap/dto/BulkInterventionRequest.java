package com.healthdata.caregap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk Intervention Request DTO
 *
 * Request payload for assigning interventions to multiple care gaps.
 * Used for bulk intervention assignment workflows.
 *
 * Issue #241: Care Gap Bulk Actions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkInterventionRequest {

    /**
     * List of care gap IDs for intervention assignment (UUID strings)
     */
    @NotEmpty(message = "Gap IDs cannot be empty")
    private List<String> gapIds;

    /**
     * Type of intervention to assign
     * Valid values: OUTREACH, REMINDER, EDUCATION, REFERRAL, APPOINTMENT_SCHEDULED, MEDICATION_REVIEW, OTHER
     */
    @NotBlank(message = "Intervention type is required")
    private String interventionType;

    /**
     * Description of the intervention
     */
    @NotBlank(message = "Description is required")
    private String description;

    /**
     * Scheduled/target date for intervention (ISO 8601 format)
     */
    private String scheduledDate;

    /**
     * User/team assigned to perform the intervention
     */
    private String assignedTo;

    /**
     * Optional notes about the intervention
     */
    private String notes;
}
