package com.healthdata.corehiveadapter.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AdapterMetricsTest {

    private MeterRegistry registry;
    private AdapterMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AdapterMetrics(registry);
    }

    @Test
    void recordScoringRequest_incrementsCounter() {
        metrics.recordScoringRequest();
        metrics.recordScoringRequest();

        assertThat(registry.counter("hdim.adapter.corehive.scoring.requests.total", "adapter", "corehive").count())
                .isEqualTo(2.0);
    }

    @Test
    void recordScoringError_incrementsCounter() {
        metrics.recordScoringError();

        assertThat(registry.counter("hdim.adapter.corehive.scoring.errors.total", "adapter", "corehive").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordRoiRequest_incrementsCounter() {
        metrics.recordRoiRequest();
        metrics.recordRoiRequest();
        metrics.recordRoiRequest();

        assertThat(registry.counter("hdim.adapter.corehive.roi.requests.total", "adapter", "corehive").count())
                .isEqualTo(3.0);
    }

    @Test
    void recordPhiBlocked_incrementsCounter() {
        metrics.recordPhiBlocked();

        assertThat(registry.counter("hdim.adapter.corehive.phi.blocked.total", "adapter", "corehive").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordScoringLatency_recordsTimer() {
        metrics.recordScoringLatency(Duration.ofMillis(250));

        assertThat(registry.timer("hdim.adapter.corehive.scoring.latency", "adapter", "corehive").count())
                .isEqualTo(1);
        assertThat(registry.timer("hdim.adapter.corehive.scoring.latency", "adapter", "corehive").totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(250.0);
    }

    @Test
    void recordRoiLatency_recordsTimer() {
        metrics.recordRoiLatency(Duration.ofMillis(500));
        metrics.recordRoiLatency(Duration.ofMillis(300));

        assertThat(registry.timer("hdim.adapter.corehive.roi.latency", "adapter", "corehive").count())
                .isEqualTo(2);
    }
}
