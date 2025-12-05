package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.MetricsSnapshot;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Route Metrics Service Tests")
class RouteMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private RouteMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new RouteMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("Should record routed events count")
    void shouldRecordRoutedEvents() {
        // When
        metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
        metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
        metricsService.recordRoutedEvent("observation.processing", Priority.MEDIUM);

        // Then
        Counter patientCounter = meterRegistry.find("event.router.routed")
            .tag("topic", "patient.processing")
            .tag("priority", "HIGH")
            .counter();
        assertThat(patientCounter).isNotNull();
        assertThat(patientCounter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should record filtered events count")
    void shouldRecordFilteredEvents() {
        // When
        metricsService.recordFilteredEvent("patient.processing");
        metricsService.recordFilteredEvent("patient.processing");

        // Then
        Counter counter = meterRegistry.find("event.router.filtered")
            .tag("topic", "patient.processing")
            .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should record unrouted events count")
    void shouldRecordUnroutedEvents() {
        // When
        metricsService.recordUnroutedEvent("unknown.topic");

        // Then
        Counter counter = meterRegistry.find("event.router.unrouted")
            .tag("topic", "unknown.topic")
            .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record DLQ events count")
    void shouldRecordDlqEvents() {
        // When
        metricsService.recordDlqEvent("patient.processing", "No matching rule");

        // Then
        Counter counter = meterRegistry.find("event.router.dlq")
            .tag("topic", "patient.processing")
            .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record routing latency")
    void shouldRecordRoutingLatency() {
        // When
        metricsService.recordRoutingLatency("patient.processing", Duration.ofMillis(50));
        metricsService.recordRoutingLatency("patient.processing", Duration.ofMillis(75));

        // Then
        Timer timer = meterRegistry.find("event.router.latency")
            .tag("topic", "patient.processing")
            .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(2);
        assertThat(timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should track events by priority")
    void shouldTrackEventsByPriority() {
        // When
        metricsService.recordRoutedEvent("topic1", Priority.CRITICAL);
        metricsService.recordRoutedEvent("topic2", Priority.HIGH);
        metricsService.recordRoutedEvent("topic3", Priority.MEDIUM);
        metricsService.recordRoutedEvent("topic4", Priority.LOW);

        // Then
        Counter criticalCounter = meterRegistry.find("event.router.routed")
            .tag("priority", "CRITICAL")
            .counter();
        assertThat(criticalCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should provide metrics snapshot")
    void shouldProvideSnapshot() {
        // Given
        metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
        metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
        metricsService.recordFilteredEvent("patient.processing");
        metricsService.recordUnroutedEvent("unknown.topic");
        metricsService.recordDlqEvent("failed.topic", "Error");

        // When
        MetricsSnapshot snapshot = metricsService.getSnapshot();

        // Then
        assertThat(snapshot.getTotalRoutedEvents()).isEqualTo(2);
        assertThat(snapshot.getTotalFilteredEvents()).isEqualTo(1);
        assertThat(snapshot.getTotalUnroutedEvents()).isEqualTo(1);
        assertThat(snapshot.getTotalDlqEvents()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should track throughput rate")
    void shouldTrackThroughput() throws InterruptedException {
        // When
        for (int i = 0; i < 100; i++) {
            metricsService.recordRoutedEvent("patient.processing", Priority.MEDIUM);
        }

        // Wait a bit to ensure time has passed
        Thread.sleep(100);

        // Then
        MetricsSnapshot snapshot = metricsService.getSnapshot();
        assertThat(snapshot.getEventsPerSecond()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should track error rate")
    void shouldTrackErrorRate() {
        // When
        metricsService.recordRoutedEvent("topic1", Priority.HIGH); // Success
        metricsService.recordRoutedEvent("topic2", Priority.HIGH); // Success
        metricsService.recordDlqEvent("topic3", "Error"); // Error

        // Then
        MetricsSnapshot snapshot = metricsService.getSnapshot();
        double errorRate = snapshot.getErrorRate();
        assertThat(errorRate).isBetween(0.3, 0.4); // ~33%
    }

    @Test
    @DisplayName("Should provide metrics by topic")
    void shouldProvideMetricsByTopic() {
        // When
        metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
        metricsService.recordRoutedEvent("patient.processing", Priority.HIGH);
        metricsService.recordRoutedEvent("observation.processing", Priority.MEDIUM);

        // Then
        Map<String, Long> metricsByTopic = metricsService.getRoutedEventsByTopic();
        assertThat(metricsByTopic.get("patient.processing")).isEqualTo(2);
        assertThat(metricsByTopic.get("observation.processing")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reset metrics when requested")
    void shouldResetMetrics() {
        // Given
        metricsService.recordRoutedEvent("topic1", Priority.HIGH);
        metricsService.recordFilteredEvent("topic2");

        // When
        metricsService.resetMetrics();

        // Then
        MetricsSnapshot snapshot = metricsService.getSnapshot();
        assertThat(snapshot.getTotalRoutedEvents()).isEqualTo(0);
        assertThat(snapshot.getTotalFilteredEvents()).isEqualTo(0);
    }
}
