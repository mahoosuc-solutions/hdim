package com.healthdata.caregapevent.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Care Gap Statistics DTO
 * Aggregated statistics for a tenant's care gaps
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareGapStatistics {
    private long totalOpenCareGaps;
    private long urgentCareGaps;
    private long overdueCareGaps;
}
