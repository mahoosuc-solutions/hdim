package com.healthdata.clinicalworkflow.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response containing overall queue status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Overall queue status and statistics")
public class QueueStatusResponse {

    @Schema(description = "All active queue entries")
    private List<QueuePositionResponse> queueEntries;

    @Schema(description = "Total patients in all queues", example = "12")
    private Integer totalPatients;

    @Schema(description = "Patients waiting for check-in", example = "3")
    private Integer checkInQueueCount;

    @Schema(description = "Patients waiting for vitals", example = "2")
    private Integer vitalsQueueCount;

    @Schema(description = "Patients waiting for provider", example = "5")
    private Integer providerQueueCount;

    @Schema(description = "Patients waiting for checkout", example = "2")
    private Integer checkoutQueueCount;

    @Schema(description = "Average wait time in minutes", example = "12")
    private Integer averageWaitMinutes;

    @Schema(description = "Longest wait time in minutes", example = "25")
    private Integer longestWaitMinutes;

    @Schema(description = "Queue counts by priority (STAT, URGENT, ROUTINE)")
    private Map<String, Integer> countsByPriority;

    @Schema(description = "Queue counts by visit type")
    private Map<String, Integer> countsByVisitType;
}
