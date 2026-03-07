package com.healthdata.hedisadapter.observability;

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
    void recordMeasureSynced_incrementsCounter() {
        metrics.recordMeasureSynced();
        metrics.recordMeasureSynced();

        assertThat(registry.counter("hdim.adapter.hedis.measures.synced.total", "adapter", "hedis").count())
                .isEqualTo(2.0);
    }

    @Test
    void recordCqlDelegation_incrementsCounter() {
        metrics.recordCqlDelegation();

        assertThat(registry.counter("hdim.adapter.hedis.cql.delegations.total", "adapter", "hedis").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordCrmWebhook_incrementsCounter() {
        metrics.recordCrmWebhook();
        metrics.recordCrmWebhook();
        metrics.recordCrmWebhook();

        assertThat(registry.counter("hdim.adapter.hedis.crm.webhooks.total", "adapter", "hedis").count())
                .isEqualTo(3.0);
    }

    @Test
    void recordWebsocketMessage_incrementsCounter() {
        metrics.recordWebsocketMessage();

        assertThat(registry.counter("hdim.adapter.hedis.websocket.messages.total", "adapter", "hedis").count())
                .isEqualTo(1.0);
    }

    @Test
    void setActiveWebsocketConnections_updatesGauge() {
        metrics.setActiveWebsocketConnections(42);

        assertThat(registry.find("hdim.adapter.hedis.websocket.connections.active").gauge().value())
                .isEqualTo(42.0);
    }

    @Test
    void setActiveWebsocketConnections_updatesGaugeMultipleTimes() {
        metrics.setActiveWebsocketConnections(10);
        metrics.setActiveWebsocketConnections(25);
        metrics.setActiveWebsocketConnections(5);

        assertThat(registry.find("hdim.adapter.hedis.websocket.connections.active").gauge().value())
                .isEqualTo(5.0);
    }

    @Test
    void recordMeasureSyncLatency_recordsTimer() {
        metrics.recordMeasureSyncLatency(Duration.ofMillis(750));

        assertThat(registry.timer("hdim.adapter.hedis.measures.sync.latency", "adapter", "hedis").count())
                .isEqualTo(1);
        assertThat(registry.timer("hdim.adapter.hedis.measures.sync.latency", "adapter", "hedis").totalTime(TimeUnit.MILLISECONDS))
                .isEqualTo(750.0);
    }

    @Test
    void recordCqlLatency_recordsTimer() {
        metrics.recordCqlLatency(Duration.ofMillis(100));
        metrics.recordCqlLatency(Duration.ofMillis(200));

        assertThat(registry.timer("hdim.adapter.hedis.cql.latency", "adapter", "hedis").count())
                .isEqualTo(2);
    }
}
