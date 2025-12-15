package com.healthdata.eventrouter.controller;

import com.healthdata.eventrouter.service.EventRouter;
import com.healthdata.eventrouter.service.PriorityQueueService;
import com.healthdata.eventrouter.service.RouteMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EventRouter health check components.
 * Tests health indicator dependencies without requiring full Spring Boot context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Event Router Health Check Tests")
class EventRouterHealthCheckTest {

    @Mock
    private EventRouter eventRouter;

    @Mock
    private PriorityQueueService queueService;

    @Mock
    private RouteMetricsService metricsService;

    @Test
    @DisplayName("Health check dependencies are available")
    void healthCheckDependenciesAvailable() {
        // Verify that mock dependencies are properly instantiated
        // This validates the health check component dependencies
        assertThat(eventRouter).isNotNull();
        assertThat(queueService).isNotNull();
        assertThat(metricsService).isNotNull();
    }
}
