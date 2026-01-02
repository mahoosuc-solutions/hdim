package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.MetricsSnapshot;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class RouteMetricsService {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicLong> topicCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalRouted = new AtomicLong(0);
    private final AtomicLong totalFiltered = new AtomicLong(0);
    private final AtomicLong totalUnrouted = new AtomicLong(0);
    private final AtomicLong totalDlq = new AtomicLong(0);
    private final Instant startTime = Instant.now();

    public void recordRoutedEvent(String targetTopic, Priority priority) {
        totalRouted.incrementAndGet();
        topicCounts.computeIfAbsent(targetTopic, k -> new AtomicLong(0)).incrementAndGet();

        Counter.builder("event.router.routed")
            .tag("topic", targetTopic)
            .tag("priority", priority.name())
            .register(meterRegistry)
            .increment();
    }

    public void recordFilteredEvent(String topic) {
        totalFiltered.incrementAndGet();

        Counter.builder("event.router.filtered")
            .tag("topic", topic)
            .register(meterRegistry)
            .increment();
    }

    public void recordUnroutedEvent(String topic) {
        totalUnrouted.incrementAndGet();

        Counter.builder("event.router.unrouted")
            .tag("topic", topic)
            .register(meterRegistry)
            .increment();
    }

    public void recordDlqEvent(String topic, String reason) {
        totalDlq.incrementAndGet();

        Counter.builder("event.router.dlq")
            .tag("topic", topic)
            .register(meterRegistry)
            .increment();
    }

    public void recordRoutingLatency(String topic, Duration latency) {
        Timer.builder("event.router.latency")
            .tag("topic", topic)
            .register(meterRegistry)
            .record(latency);
    }

    public MetricsSnapshot getSnapshot() {
        long totalEvents = totalRouted.get() + totalFiltered.get() + totalUnrouted.get() + totalDlq.get();
        double secondsElapsed = Duration.between(startTime, Instant.now()).getSeconds();
        double eventsPerSecond = secondsElapsed > 0 ? totalEvents / secondsElapsed : 0;

        double errorRate = totalEvents > 0 ?
            (double) (totalUnrouted.get() + totalDlq.get()) / totalEvents : 0;

        Map<String, Long> eventsByTopic = new HashMap<>();
        topicCounts.forEach((topic, count) -> eventsByTopic.put(topic, count.get()));

        return new MetricsSnapshot(
            totalRouted.get(),
            totalFiltered.get(),
            totalUnrouted.get(),
            totalDlq.get(),
            eventsPerSecond,
            errorRate,
            eventsByTopic,
            Instant.now()
        );
    }

    public Map<String, Long> getRoutedEventsByTopic() {
        Map<String, Long> result = new HashMap<>();
        topicCounts.forEach((topic, count) -> result.put(topic, count.get()));
        return result;
    }

    public void resetMetrics() {
        totalRouted.set(0);
        totalFiltered.set(0);
        totalUnrouted.set(0);
        totalDlq.set(0);
        topicCounts.clear();
        log.info("Metrics reset");
    }
}
