package com.healthdata.events.intelligence.dto;

import java.time.Instant;

public record TenantTrustDashboardResponse(
        String tenantId,
        int trustScore,
        long totalOpenFindings,
        long highSeverityOpenFindings,
        long consistencyOpenFindings,
        long dataCompletenessOpenFindings,
        long temporalOpenFindings,
        Instant lastUpdatedAt
) {
}
