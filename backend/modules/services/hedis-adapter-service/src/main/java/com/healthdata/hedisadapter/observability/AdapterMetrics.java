package com.healthdata.hedisadapter.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adapter-specific Prometheus metrics for the HEDIS adapter.
 *
 * Tracks measure syncs, CQL delegations, CRM webhooks,
 * WebSocket messages/connections, and associated latencies
 * with the {@code adapter=hedis} tag.
 */
@Component
public class AdapterMetrics {

    private static final String TAG_ADAPTER = "adapter";
    private static final String ADAPTER_NAME = "hedis";

    private final Counter measuresSyncedCounter;
    private final Counter cqlDelegationsCounter;
    private final Counter crmWebhooksCounter;
    private final Counter websocketMessagesCounter;
    private final AtomicLong activeWebsocketConnections;
    private final Timer measuresSyncLatencyTimer;
    private final Timer cqlLatencyTimer;

    public AdapterMetrics(MeterRegistry registry) {
        this.measuresSyncedCounter = Counter.builder("hdim.adapter.hedis.measures.synced.total")
                .description("Total HEDIS measures synced from external registry")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.cqlDelegationsCounter = Counter.builder("hdim.adapter.hedis.cql.delegations.total")
                .description("Total CQL evaluation delegations to cql-engine-service")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.crmWebhooksCounter = Counter.builder("hdim.adapter.hedis.crm.webhooks.total")
                .description("Total CRM webhook notifications received")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.websocketMessagesCounter = Counter.builder("hdim.adapter.hedis.websocket.messages.total")
                .description("Total WebSocket messages sent to connected clients")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.activeWebsocketConnections = new AtomicLong(0);
        Gauge.builder("hdim.adapter.hedis.websocket.connections.active", activeWebsocketConnections, AtomicLong::get)
                .description("Currently active WebSocket connections")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.measuresSyncLatencyTimer = Timer.builder("hdim.adapter.hedis.measures.sync.latency")
                .description("Latency of HEDIS measure sync operations")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.cqlLatencyTimer = Timer.builder("hdim.adapter.hedis.cql.latency")
                .description("Latency of CQL evaluation delegations")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);
    }

    public void recordMeasureSynced() {
        measuresSyncedCounter.increment();
    }

    public void recordCqlDelegation() {
        cqlDelegationsCounter.increment();
    }

    public void recordCrmWebhook() {
        crmWebhooksCounter.increment();
    }

    public void recordWebsocketMessage() {
        websocketMessagesCounter.increment();
    }

    public void setActiveWebsocketConnections(long count) {
        activeWebsocketConnections.set(count);
    }

    public void recordMeasureSyncLatency(Duration duration) {
        measuresSyncLatencyTimer.record(duration);
    }

    public void recordCqlLatency(Duration duration) {
        cqlLatencyTimer.record(duration);
    }
}
