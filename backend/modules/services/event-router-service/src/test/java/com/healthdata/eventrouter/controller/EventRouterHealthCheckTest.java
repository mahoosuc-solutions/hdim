package com.healthdata.eventrouter.controller;

import com.healthdata.eventrouter.dto.MetricsSnapshot;
import com.healthdata.eventrouter.service.EventRouter;
import com.healthdata.eventrouter.service.PriorityQueueService;
import com.healthdata.eventrouter.service.RouteMetricsService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Health check tests are disabled as they require full Spring Boot context.
 * These are tested via integration tests with actual Actuator endpoints.
 */
@Disabled("Actuator endpoints require full Spring Boot context - tested via integration tests")
@DisplayName("Event Router Health Check Tests")
class EventRouterHealthCheckTest {

    @MockBean
    private EventRouter eventRouter;

    @MockBean
    private PriorityQueueService queueService;

    @MockBean
    private RouteMetricsService metricsService;

    @Test
    @DisplayName("Placeholder test - actual health checks tested in integration tests")
    void placeholder() {
        // Health check integration tests are run separately with full Spring Boot context
    }
}
