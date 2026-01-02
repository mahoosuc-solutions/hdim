package com.healthdata.ehr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for triggering data synchronization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRequest {

    @NotBlank(message = "Patient ID is required")
    private String ehrPatientId;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;
}
