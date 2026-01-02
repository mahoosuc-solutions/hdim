package com.healthdata.events.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * TDD Test Suite for Event Processing Metrics
 *
 * Tests verify that event processing metrics are properly collected:
 * - event.processing.duration (timer)
 * - event.processing.success (counter)
 * - event.processing.failure (counter)
 */
@DisplayName("Event Processing Metrics Tests")
class EventProcessingMetricsTest {

    private MeterRegistry meterRegistry;
    private EventProcessingMetrics eventMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        eventMetrics = new EventProcessingMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Should record processing duration with timer")
    void shouldRecordProcessingDuration() {
        // Given
        String eventType = "CARE_GAP_DETECTED";
        Duration duration = Duration.ofMillis(150);

        // When
        eventMetrics.recordProcessingDuration(eventType, duration);

        // Then
        Timer timer = meterRegistry.find("event.processing.duration")
                .tag("event_type", eventType)
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(150.0);
    }

    @Test
    @DisplayName("Should record multiple processing durations")
    void shouldRecordMultipleProcessingDurations() {
        // Given
        String eventType = "CARE_GAP_DETECTED";

        // When
        eventMetrics.recordProcessingDuration(eventType, Duration.ofMillis(100));
        eventMetrics.recordProcessingDuration(eventType, Duration.ofMillis(200));
        eventMetrics.recordProcessingDuration(eventType, Duration.ofMillis(150));

        // Then
        Timer timer = meterRegistry.find("event.processing.duration")
                .tag("event_type", eventType)
                .timer();

        assertThat(timer.count()).isEqualTo(3);
        assertThat(timer.mean(TimeUnit.MILLISECONDS)).isGreaterThan(0);
        assertThat(timer.max(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(200.0);
    }

    @Test
    @DisplayName("Should track different event types separately in timer")
    void shouldTrackDifferentEventTypesSeparatelyInTimer() {
        // Given
        String eventType1 = "CARE_GAP_DETECTED";
        String eventType2 = "RISK_SCORE_UPDATED";

        // When
        eventMetrics.recordProcessingDuration(eventType1, Duration.ofMillis(100));
        eventMetrics.recordProcessingDuration(eventType1, Duration.ofMillis(150));
        eventMetrics.recordProcessingDuration(eventType2, Duration.ofMillis(50));

        // Then
        Timer timer1 = meterRegistry.find("event.processing.duration")
                .tag("event_type", eventType1)
                .timer();

        Timer timer2 = meterRegistry.find("event.processing.duration")
                .tag("event_type", eventType2)
                .timer();

        assertThat(timer1.count()).isEqualTo(2);
        assertThat(timer2.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should increment success counter")
    void shouldIncrementSuccessCounter() {
        // Given
        String eventType = "CARE_GAP_DETECTED";

        // When
        eventMetrics.recordSuccess(eventType);
        eventMetrics.recordSuccess(eventType);

        // Then
        Counter counter = meterRegistry.find("event.processing.success")
                .tag("event_type", eventType)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should increment failure counter")
    void shouldIncrementFailureCounter() {
        // Given
        String eventType = "CARE_GAP_DETECTED";

        // When
        eventMetrics.recordFailure(eventType);

        // Then
        Counter counter = meterRegistry.find("event.processing.failure")
                .tag("event_type", eventType)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should track success and failure separately")
    void shouldTrackSuccessAndFailureSeparately() {
        // Given
        String eventType = "CARE_GAP_DETECTED";

        // When
        eventMetrics.recordSuccess(eventType);
        eventMetrics.recordSuccess(eventType);
        eventMetrics.recordSuccess(eventType);
        eventMetrics.recordFailure(eventType);

        // Then
        Counter successCounter = meterRegistry.find("event.processing.success")
                .tag("event_type", eventType)
                .counter();

        Counter failureCounter = meterRegistry.find("event.processing.failure")
                .tag("event_type", eventType)
                .counter();

        assertThat(successCounter.count()).isEqualTo(3.0);
        assertThat(failureCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should track success for different event types")
    void shouldTrackSuccessForDifferentEventTypes() {
        // Given
        String eventType1 = "CARE_GAP_DETECTED";
        String eventType2 = "RISK_SCORE_UPDATED";

        // When
        eventMetrics.recordSuccess(eventType1);
        eventMetrics.recordSuccess(eventType2);
        eventMetrics.recordSuccess(eventType2);

        // Then
        Counter counter1 = meterRegistry.find("event.processing.success")
                .tag("event_type", eventType1)
                .counter();

        Counter counter2 = meterRegistry.find("event.processing.success")
                .tag("event_type", eventType2)
                .counter();

        assertThat(counter1.count()).isEqualTo(1.0);
        assertThat(counter2.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should track failure for different event types")
    void shouldTrackFailureForDifferentEventTypes() {
        // Given
        String eventType1 = "CARE_GAP_DETECTED";
        String eventType2 = "RISK_SCORE_UPDATED";

        // When
        eventMetrics.recordFailure(eventType1);
        eventMetrics.recordFailure(eventType1);
        eventMetrics.recordFailure(eventType2);

        // Then
        Counter counter1 = meterRegistry.find("event.processing.failure")
                .tag("event_type", eventType1)
                .counter();

        Counter counter2 = meterRegistry.find("event.processing.failure")
                .tag("event_type", eventType2)
                .counter();

        assertThat(counter1.count()).isEqualTo(2.0);
        assertThat(counter2.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should provide timer sample for duration recording")
    void shouldProvideTimerSampleForDurationRecording() {
        // Given
        String eventType = "CARE_GAP_DETECTED";

        // When
        Timer.Sample sample = eventMetrics.startTimer();

        // Simulate some processing time
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        eventMetrics.stopTimer(sample, eventType);

        // Then
        Timer timer = meterRegistry.find("event.processing.duration")
                .tag("event_type", eventType)
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should expose all event processing metrics")
    void shouldExposeAllEventProcessingMetrics() {
        // Given
        String eventType = "CARE_GAP_DETECTED";

        // When
        Timer.Sample sample = eventMetrics.startTimer();
        eventMetrics.stopTimer(sample, eventType);
        eventMetrics.recordSuccess(eventType);
        eventMetrics.recordFailure(eventType);

        // Then
        assertThat(meterRegistry.find("event.processing.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("event.processing.success").counter()).isNotNull();
        assertThat(meterRegistry.find("event.processing.failure").counter()).isNotNull();
    }

    @Test
    @DisplayName("Should handle high-frequency event processing")
    void shouldHandleHighFrequencyEventProcessing() {
        // Given
        String eventType = "CARE_GAP_DETECTED";
        int eventCount = 1000;

        // When
        for (int i = 0; i < eventCount; i++) {
            eventMetrics.recordProcessingDuration(eventType, Duration.ofMillis(50));
            eventMetrics.recordSuccess(eventType);
        }

        // Then
        Timer timer = meterRegistry.find("event.processing.duration")
                .tag("event_type", eventType)
                .timer();

        Counter successCounter = meterRegistry.find("event.processing.success")
                .tag("event_type", eventType)
                .counter();

        assertThat(timer.count()).isEqualTo(eventCount);
        assertThat(successCounter.count()).isEqualTo((double) eventCount);
    }
}
