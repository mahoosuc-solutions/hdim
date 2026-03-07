package com.healthdata.corehiveadapter.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Adapter-specific Prometheus metrics for the CoreHive adapter.
 *
 * Tracks scoring requests, ROI calculations, PHI blocking,
 * and associated latencies with the {@code adapter=corehive} tag.
 */
@Component
public class AdapterMetrics {

    private static final String TAG_ADAPTER = "adapter";
    private static final String ADAPTER_NAME = "corehive";

    private final Counter scoringRequestsCounter;
    private final Counter scoringErrorsCounter;
    private final Counter roiRequestsCounter;
    private final Counter phiBlockedCounter;
    private final Timer scoringLatencyTimer;
    private final Timer roiLatencyTimer;

    public AdapterMetrics(MeterRegistry registry) {
        this.scoringRequestsCounter = Counter.builder("hdim.adapter.corehive.scoring.requests.total")
                .description("Total scoring requests sent to CoreHive")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.scoringErrorsCounter = Counter.builder("hdim.adapter.corehive.scoring.errors.total")
                .description("Total scoring errors from CoreHive")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.roiRequestsCounter = Counter.builder("hdim.adapter.corehive.roi.requests.total")
                .description("Total ROI calculation requests sent to CoreHive")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.phiBlockedCounter = Counter.builder("hdim.adapter.corehive.phi.blocked.total")
                .description("Total PHI-containing requests blocked before reaching CoreHive")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.scoringLatencyTimer = Timer.builder("hdim.adapter.corehive.scoring.latency")
                .description("Latency of CoreHive scoring requests")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.roiLatencyTimer = Timer.builder("hdim.adapter.corehive.roi.latency")
                .description("Latency of CoreHive ROI calculation requests")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);
    }

    public void recordScoringRequest() {
        scoringRequestsCounter.increment();
    }

    public void recordScoringError() {
        scoringErrorsCounter.increment();
    }

    public void recordRoiRequest() {
        roiRequestsCounter.increment();
    }

    public void recordPhiBlocked() {
        phiBlockedCounter.increment();
    }

    public void recordScoringLatency(Duration duration) {
        scoringLatencyTimer.record(duration);
    }

    public void recordRoiLatency(Duration duration) {
        roiLatencyTimer.record(duration);
    }
}
