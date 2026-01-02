package com.healthdata.quality.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for addressing a care gap
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressCareGapRequest {

    /**
     * Provider addressing the gap
     */
    @NotBlank(message = "Addressed by is required")
    private String addressedBy;

    /**
     * Notes about how the gap was addressed
     */
    @NotBlank(message = "Notes are required")
    private String notes;

    /**
     * New status (in-progress, addressed, closed, dismissed)
     */
    @NotBlank(message = "Status is required")
    private String status;
}
