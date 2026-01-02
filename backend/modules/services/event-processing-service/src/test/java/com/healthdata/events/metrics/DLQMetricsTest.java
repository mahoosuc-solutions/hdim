package com.healthdata.events.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthdata.events.entity.DeadLetterQueueEntity;
import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;
import com.healthdata.events.repository.DeadLetterQueueRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * TDD Test Suite for DLQ Metrics Collection
 *
 * Tests verify that all DLQ metrics are properly collected and exposed:
 * - dlq.failures.total (counter by topic, event_type)
 * - dlq.retries.total (counter)
 * - dlq.exhausted.total (gauge)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DLQ Metrics Collection Tests")
class DLQMetricsTest {

    private MeterRegistry meterRegistry;

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    private DLQMetrics dlqMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        dlqMetrics = new DLQMetrics(meterRegistry, dlqRepository);
    }

    @Test
    @DisplayName("Should increment failure counter with topic and event_type tags")
    void shouldIncrementFailureCounterWithTags() {
        // Given
        String topic = "patient.health.events";
        String eventType = "CARE_GAP_DETECTED";

        // When
        dlqMetrics.recordFailure(topic, eventType);

        // Then
        Counter counter = meterRegistry.find("dlq.failures.total")
                .tag("topic", topic)
                .tag("event_type", eventType)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment failure counter multiple times")
    void shouldIncrementFailureCounterMultipleTimes() {
        // Given
        String topic = "patient.health.events";
        String eventType = "CARE_GAP_DETECTED";

        // When
        dlqMetrics.recordFailure(topic, eventType);
        dlqMetrics.recordFailure(topic, eventType);
        dlqMetrics.recordFailure(topic, eventType);

        // Then
        Counter counter = meterRegistry.find("dlq.failures.total")
                .tag("topic", topic)
                .tag("event_type", eventType)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Should track different topics separately")
    void shouldTrackDifferentTopicsSeparately() {
        // Given
        String topic1 = "patient.health.events";
        String topic2 = "quality.measure.events";
        String eventType = "CARE_GAP_DETECTED";

        // When
        dlqMetrics.recordFailure(topic1, eventType);
        dlqMetrics.recordFailure(topic1, eventType);
        dlqMetrics.recordFailure(topic2, eventType);

        // Then
        Counter counter1 = meterRegistry.find("dlq.failures.total")
                .tag("topic", topic1)
                .tag("event_type", eventType)
                .counter();

        Counter counter2 = meterRegistry.find("dlq.failures.total")
                .tag("topic", topic2)
                .tag("event_type", eventType)
                .counter();

        assertThat(counter1.count()).isEqualTo(2.0);
        assertThat(counter2.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should track different event types separately")
    void shouldTrackDifferentEventTypesSeparately() {
        // Given
        String topic = "patient.health.events";
        String eventType1 = "CARE_GAP_DETECTED";
        String eventType2 = "RISK_SCORE_UPDATED";

        // When
        dlqMetrics.recordFailure(topic, eventType1);
        dlqMetrics.recordFailure(topic, eventType2);
        dlqMetrics.recordFailure(topic, eventType2);

        // Then
        Counter counter1 = meterRegistry.find("dlq.failures.total")
                .tag("topic", topic)
                .tag("event_type", eventType1)
                .counter();

        Counter counter2 = meterRegistry.find("dlq.failures.total")
                .tag("topic", topic)
                .tag("event_type", eventType2)
                .counter();

        assertThat(counter1.count()).isEqualTo(1.0);
        assertThat(counter2.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should increment retry counter")
    void shouldIncrementRetryCounter() {
        // When
        dlqMetrics.recordRetry();
        dlqMetrics.recordRetry();

        // Then
        Counter counter = meterRegistry.find("dlq.retries.total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Should register exhausted gauge")
    void shouldRegisterExhaustedGauge() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(5L);

        // When - gauge is auto-registered during construction
        // Force gauge evaluation
        Gauge gauge = meterRegistry.find("dlq.exhausted.total").gauge();

        // Then
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(5.0);
        verify(dlqRepository).countByStatus(DLQStatus.EXHAUSTED);
    }

    @Test
    @DisplayName("Should update exhausted gauge dynamically")
    void shouldUpdateExhaustedGaugeDynamically() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED))
                .thenReturn(3L)
                .thenReturn(7L);

        // When
        Gauge gauge = meterRegistry.find("dlq.exhausted.total").gauge();
        double firstValue = gauge.value();

        // Force re-evaluation
        double secondValue = gauge.value();

        // Then
        assertThat(firstValue).isEqualTo(3.0);
        assertThat(secondValue).isEqualTo(7.0);
    }

    @Test
    @DisplayName("Should register failed gauge")
    void shouldRegisterFailedGauge() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(12L);

        // When
        Gauge gauge = meterRegistry.find("dlq.failed.total").gauge();

        // Then
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(12.0);
        verify(dlqRepository).countByStatus(DLQStatus.FAILED);
    }

    @Test
    @DisplayName("Should register retrying gauge")
    void shouldRegisterRetryingGauge() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(4L);

        // When
        Gauge gauge = meterRegistry.find("dlq.retrying.total").gauge();

        // Then
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(4.0);
        verify(dlqRepository).countByStatus(DLQStatus.RETRYING);
    }

    @Test
    @DisplayName("Should handle zero counts gracefully")
    void shouldHandleZeroCountsGracefully() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(0L);

        // When
        Gauge gauge = meterRegistry.find("dlq.exhausted.total").gauge();

        // Then
        assertThat(gauge.value()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should expose all metrics to registry")
    void shouldExposeAllMetricsToRegistry() {
        // Given - gauges are registered during construction
        // When
        dlqMetrics.recordFailure("test.topic", "TEST_EVENT");
        dlqMetrics.recordRetry();

        // Force gauge evaluation to trigger repository calls
        meterRegistry.find("dlq.exhausted.total").gauge().value();
        meterRegistry.find("dlq.failed.total").gauge().value();
        meterRegistry.find("dlq.retrying.total").gauge().value();

        // Then
        assertThat(meterRegistry.find("dlq.failures.total").counter()).isNotNull();
        assertThat(meterRegistry.find("dlq.retries.total").counter()).isNotNull();
        assertThat(meterRegistry.find("dlq.exhausted.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("dlq.failed.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("dlq.retrying.total").gauge()).isNotNull();
    }
}
