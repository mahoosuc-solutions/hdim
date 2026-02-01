package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing estimated wait times
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estimated wait times by queue type")
public class QueueWaitTimeResponse {

    @Schema(description = "Estimated wait for check-in (minutes)", example = "5")
    private Integer checkInWaitMinutes;

    @Schema(description = "Estimated wait for vitals (minutes)", example = "8")
    private Integer vitalsWaitMinutes;

    @Schema(description = "Estimated wait for provider (minutes)", example = "15")
    private Integer providerWaitMinutes;

    @Schema(description = "Estimated wait for checkout (minutes)", example = "3")
    private Integer checkoutWaitMinutes;

    @Schema(description = "Total estimated wait time (minutes)", example = "31")
    private Integer totalEstimatedMinutes;

    @Schema(description = "Average wait time across all queues (minutes)", example = "12")
    private Integer averageWaitMinutes;
}
