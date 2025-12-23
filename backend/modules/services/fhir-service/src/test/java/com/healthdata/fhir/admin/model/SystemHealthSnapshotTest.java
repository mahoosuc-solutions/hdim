package com.healthdata.fhir.admin.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class SystemHealthSnapshotTest {

    @Test
    void shouldExposeSnapshotFields() {
        SystemHealthSnapshot.DependencyHealth dependency = new SystemHealthSnapshot.DependencyHealth(
                "db",
                "Database",
                SystemHealthSnapshot.DependencyIndicator.OPERATIONAL,
                99.9,
                12.5,
                Instant.parse("2025-01-01T00:00:00Z"));
        SystemHealthSnapshot.QueueMetric queue = new SystemHealthSnapshot.QueueMetric(
                "audit-events",
                42,
                120.5,
                3.1);

        SystemHealthSnapshot snapshot = new SystemHealthSnapshot(
                Instant.parse("2025-01-02T00:00:00Z"),
                List.of(dependency),
                List.of(queue));

        assertThat(snapshot.generatedAt()).isEqualTo(Instant.parse("2025-01-02T00:00:00Z"));
        assertThat(snapshot.dependencies()).containsExactly(dependency);
        assertThat(snapshot.queues()).containsExactly(queue);
        assertThat(dependency.indicator()).isEqualTo(SystemHealthSnapshot.DependencyIndicator.OPERATIONAL);
        assertThat(SystemHealthSnapshot.DependencyIndicator.valueOf("OUTAGE"))
                .isEqualTo(SystemHealthSnapshot.DependencyIndicator.OUTAGE);
    }
}
