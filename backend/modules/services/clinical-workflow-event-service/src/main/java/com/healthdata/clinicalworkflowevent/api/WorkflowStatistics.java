package com.healthdata.clinicalworkflowevent.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow Statistics DTO
 * Aggregated workflow statistics for a tenant
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStatistics {
    private long totalPending;
    private long totalOverdue;
}
