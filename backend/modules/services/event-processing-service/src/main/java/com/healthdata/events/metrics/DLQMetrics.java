package com.healthdata.events.metrics;

import org.springframework.stereotype.Component;

import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;
import com.healthdata.events.repository.DeadLetterQueueRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * DLQ Metrics Collector
 *
 * Collects and exposes metrics for Dead Letter Queue monitoring:
 * - dlq.failures.total: Counter of failures by topic and event_type
 * - dlq.retries.total: Counter of retry attempts
 * - dlq.exhausted.total: Gauge of events that exhausted all retries
 * - dlq.failed.total: Gauge of currently failed events
 * - dlq.retrying.total: Gauge of events being retried
 *
 * These metrics are exposed via Prometheus for operational monitoring.
 */
@Slf4j
@Component
public class DLQMetrics {

    private final MeterRegistry meterRegistry;
    private final DeadLetterQueueRepository dlqRepository;

    public DLQMetrics(MeterRegistry meterRegistry, DeadLetterQueueRepository dlqRepository) {
        this.meterRegistry = meterRegistry;
        this.dlqRepository = dlqRepository;

        // Register gauges during construction
        registerGauges();

        log.info("DLQ metrics initialized and registered with Prometheus");
    }

    /**
     * Record a failure event to the DLQ
     *
     * @param topic Kafka topic where event failed
     * @param eventType Type of event that failed
     */
    public void recordFailure(String topic, String eventType) {
        Counter.builder("dlq.failures.total")
                .description("Total number of events that failed and were sent to DLQ")
                .tag("topic", topic)
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();

        log.debug("Recorded DLQ failure: topic={}, eventType={}", topic, eventType);
    }

    /**
     * Record a retry attempt
     */
    public void recordRetry() {
        Counter.builder("dlq.retries.total")
                .description("Total number of DLQ retry attempts")
                .register(meterRegistry)
                .increment();

        log.debug("Recorded DLQ retry attempt");
    }

    /**
     * Register gauges for current DLQ state
     */
    private void registerGauges() {
        // Exhausted events gauge
        Gauge.builder("dlq.exhausted.total", () -> {
            return dlqRepository.countByStatus(DLQStatus.EXHAUSTED);
        })
        .description("Number of events that exhausted all retry attempts")
        .register(meterRegistry);

        // Failed events gauge
        Gauge.builder("dlq.failed.total", () -> {
            return dlqRepository.countByStatus(DLQStatus.FAILED);
        })
        .description("Number of currently failed events in DLQ")
        .register(meterRegistry);

        // Retrying events gauge
        Gauge.builder("dlq.retrying.total", () -> {
            return dlqRepository.countByStatus(DLQStatus.RETRYING);
        })
        .description("Number of events currently being retried")
        .register(meterRegistry);

        log.info("DLQ gauges registered: exhausted, failed, retrying");
    }
}
