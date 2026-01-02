package com.healthdata.fhir.admin.model;

import java.time.Instant;
import java.util.List;

public record DashboardSnapshot(
        Instant timestamp,
        List<PlatformMetric> metrics,
        List<ServiceHealth> services
) {

    public record PlatformMetric(
            String id,
            String label,
            String value,
            String unit,
            TrendSummary delta,
            String measurementWindow,
            BaselineSnapshot baseline
    ) {
    }

    public record TrendSummary(
            double percentage,
            TrendDirection direction,
            Double confidence
    ) {
    }

    public enum TrendDirection {
        UP,
        DOWN,
        STABLE
    }

    public record BaselineSnapshot(
            String value,
            String description
    ) {
    }

    public record ServiceHealth(
            String serviceId,
            String displayName,
            ServiceHealthStatus status,
            String region,
            ServiceLatency latency,
            ServiceThroughput throughput,
            double uptimePercentage,
            List<Alert> activeAlerts,
            Instant lastUpdated
    ) {
    }

    public enum ServiceHealthStatus {
        HEALTHY,
        DEGRADED,
        OUTAGE
    }

    public record ServiceLatency(
            Integer p90,
            Integer p95,
            Integer p99
    ) {
    }

    public record ServiceThroughput(
            double requestsPerMinute,
            Double errorRatePerMinute
    ) {
    }

    public record Alert(
            String id,
            String severity,
            String message,
            Instant startedAt
    ) {
    }
}
