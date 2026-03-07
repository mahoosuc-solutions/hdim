package com.healthdata.healthixadapter.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

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
    void recordFhirNotification_incrementsCounter() {
        metrics.recordFhirNotification();
        metrics.recordFhirNotification();

        assertThat(registry.counter("hdim.adapter.healthix.fhir.notifications.total", "adapter", "healthix").count())
                .isEqualTo(2.0);
    }

    @Test
    void recordCcdaDocument_incrementsCounter() {
        metrics.recordCcdaDocument();

        assertThat(registry.counter("hdim.adapter.healthix.ccda.documents.total", "adapter", "healthix").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordMpiQuery_incrementsCounter() {
        metrics.recordMpiQuery();
        metrics.recordMpiQuery();
        metrics.recordMpiQuery();

        assertThat(registry.counter("hdim.adapter.healthix.mpi.queries.total", "adapter", "healthix").count())
                .isEqualTo(3.0);
    }

    @Test
    void recordHl7Message_incrementsCounter() {
        metrics.recordHl7Message();

        assertThat(registry.counter("hdim.adapter.healthix.hl7.messages.total", "adapter", "healthix").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordHl7Error_incrementsCounter() {
        metrics.recordHl7Error();
        metrics.recordHl7Error();

        assertThat(registry.counter("hdim.adapter.healthix.hl7.errors.total", "adapter", "healthix").count())
                .isEqualTo(2.0);
    }

    @Test
    void recordFhirLatency_recordsTimer() {
        metrics.recordFhirLatency(Duration.ofMillis(150));

        assertThat(registry.timer("hdim.adapter.healthix.fhir.latency", "adapter", "healthix").count())
                .isEqualTo(1);
        assertThat(registry.timer("hdim.adapter.healthix.fhir.latency", "adapter", "healthix").totalTime(TimeUnit.MILLISECONDS))
                .isEqualTo(150.0);
    }

    @Test
    void recordMpiLatency_recordsTimer() {
        metrics.recordMpiLatency(Duration.ofMillis(200));
        metrics.recordMpiLatency(Duration.ofMillis(400));

        assertThat(registry.timer("hdim.adapter.healthix.mpi.latency", "adapter", "healthix").count())
                .isEqualTo(2);
    }

    @Test
    void recordCcdaLatency_recordsTimer() {
        metrics.recordCcdaLatency(Duration.ofMillis(1000));

        assertThat(registry.timer("hdim.adapter.healthix.ccda.latency", "adapter", "healthix").count())
                .isEqualTo(1);
        assertThat(registry.timer("hdim.adapter.healthix.ccda.latency", "adapter", "healthix").totalTime(TimeUnit.MILLISECONDS))
                .isEqualTo(1000.0);
    }
}
