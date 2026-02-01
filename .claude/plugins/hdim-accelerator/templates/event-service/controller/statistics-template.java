package com.healthdata.{{DOMAIN}}event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {{DOMAIN_PASCAL}}Statistics - Aggregate Statistics DTO
 *
 * Pre-calculated aggregate statistics for {{DOMAIN}} projections.
 * Exposed via /api/v1/{{DOMAIN}}-projections/stats endpoint.
 *
 * Purpose: Provide dashboard metrics without expensive aggregation queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class {{DOMAIN_PASCAL}}Statistics {

    /**
     * Tenant ID (for reference)
     */
    private String tenantId;

    /**
     * Total count of projections for tenant
     */
    private long totalCount;

    // ========================================
    // Domain-Specific Statistics
    // ========================================

    // TODO: Add domain-specific aggregate fields
    // Examples:
    //
    // Count by status:
    // private long activeCount;
    // private long completedCount;
    // private long cancelledCount;
    //
    // Count by priority:
    // private long urgentCount;
    // private long highPriorityCount;
    // private long mediumPriorityCount;
    // private long lowPriorityCount;
    //
    // Overdue tracking:
    // private long overdueCount;
    // private double overduePercentage;
    //
    // Averages/totals:
    // private double averageScore;
    // private long totalPoints;
    //
    // Date ranges:
    // private long createdLast7Days;
    // private long createdLast30Days;
    //
    // Flags:
    // private long withAlertsCount;
    // private long criticalCount;
}
