package com.healthdata.fhir.admin.model;

import java.time.Instant;
import java.util.List;

public record SystemHealthSnapshot(
        Instant generatedAt,
        List<DependencyHealth> dependencies,
        List<QueueMetric> queues
) {

    public record DependencyHealth(
            String dependencyId,
            String displayName,
            DependencyIndicator indicator,
            double uptimePercentage,
            double latencyMs,
            Instant lastChecked
    ) {
    }

    public enum DependencyIndicator {
        OPERATIONAL,
        ATTENTION,
        OUTAGE
    }

    public record QueueMetric(
            String topic,
            long depth,
            double eventsPerMinute,
            double lagSeconds
    ) {
    }
}
