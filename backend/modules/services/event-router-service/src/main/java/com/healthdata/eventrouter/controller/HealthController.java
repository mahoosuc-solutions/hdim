package com.healthdata.eventrouter.controller;

import com.healthdata.eventrouter.service.PriorityQueueService;
import com.healthdata.eventrouter.service.RouteMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("eventRouterHealthIndicator")
@RequiredArgsConstructor
public class HealthController implements HealthIndicator {

    private final PriorityQueueService queueService;
    private final RouteMetricsService metricsService;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        details.put("queueSize", queueService.size());
        details.put("queueHealthy", queueService.isHealthy());

        var metrics = metricsService.getSnapshot();
        details.put("totalRoutedEvents", metrics.getTotalRoutedEvents());
        details.put("totalFilteredEvents", metrics.getTotalFilteredEvents());
        details.put("totalUnroutedEvents", metrics.getTotalUnroutedEvents());
        details.put("eventsPerSecond", metrics.getEventsPerSecond());
        details.put("errorRate", metrics.getErrorRate());

        if (queueService.isHealthy()) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }
}
