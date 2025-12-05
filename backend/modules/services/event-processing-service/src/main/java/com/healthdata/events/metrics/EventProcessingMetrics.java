package com.healthdata.events.metrics;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event Processing Metrics Collector
 *
 * Collects and exposes metrics for event processing:
 * - event.processing.duration: Timer for processing duration by event_type
 * - event.processing.success: Counter of successful event processing
 * - event.processing.failure: Counter of failed event processing
 *
 * These metrics enable monitoring of event processing performance and reliability.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventProcessingMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Record the duration of event processing
     *
     * @param eventType Type of event processed
     * @param duration Duration of processing
     */
    public void recordProcessingDuration(String eventType, Duration duration) {
        Timer.builder("event.processing.duration")
                .description("Duration of event processing")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .record(duration);

        log.debug("Recorded processing duration: eventType={}, duration={}ms",
                eventType, duration.toMillis());
    }

    /**
     * Record a successful event processing
     *
     * @param eventType Type of event processed
     */
    public void recordSuccess(String eventType) {
        Counter.builder("event.processing.success")
                .description("Total number of successfully processed events")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();

        log.debug("Recorded successful event processing: eventType={}", eventType);
    }

    /**
     * Record a failed event processing
     *
     * @param eventType Type of event that failed
     */
    public void recordFailure(String eventType) {
        Counter.builder("event.processing.failure")
                .description("Total number of failed event processing attempts")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .increment();

        log.debug("Recorded failed event processing: eventType={}", eventType);
    }

    /**
     * Start a timer for measuring event processing duration
     *
     * @return Timer sample to be stopped later
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop a timer and record the duration
     *
     * @param sample Timer sample started earlier
     * @param eventType Type of event processed
     */
    public void stopTimer(Timer.Sample sample, String eventType) {
        sample.stop(Timer.builder("event.processing.duration")
                .description("Duration of event processing")
                .tag("event_type", eventType)
                .register(meterRegistry));

        log.debug("Stopped timer for event processing: eventType={}", eventType);
    }
}
