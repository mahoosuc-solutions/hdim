package com.healthdata.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

/**
 * HTTP request/response metrics for HDIM services.
 *
 * Provides standardized HTTP metrics:
 * - Request duration by endpoint and method
 * - Response status codes
 * - Error rates
 * - Request sizes
 *
 * Note: Spring Boot Actuator already provides http.server.requests metrics.
 * This class provides additional custom HTTP metrics for specific use cases.
 */
public class HttpMetrics {

    private final MeterRegistry registry;
    private final String serviceName;

    public HttpMetrics(MeterRegistry registry, String serviceName) {
        this.registry = registry;
        this.serviceName = serviceName;
    }

    /**
     * Record an HTTP request.
     */
    public void recordRequest(String method, String uri, int statusCode, Duration duration) {
        // Request duration
        Timer.builder("hdim.http.request.duration")
                .description("HTTP request duration")
                .tag("service", serviceName)
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .tag("status", String.valueOf(statusCode))
                .tag("status_class", getStatusClass(statusCode))
                .register(registry)
                .record(duration);

        // Request count by status
        Counter.builder("hdim.http.request.total")
                .description("Total HTTP requests")
                .tag("service", serviceName)
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .tag("status", String.valueOf(statusCode))
                .tag("status_class", getStatusClass(statusCode))
                .register(registry)
                .increment();
    }

    /**
     * Record an HTTP error.
     */
    public void recordError(String method, String uri, String errorType) {
        Counter.builder("hdim.http.error.total")
                .description("Total HTTP errors")
                .tag("service", serviceName)
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .tag("error_type", errorType)
                .register(registry)
                .increment();
    }

    /**
     * Record outbound HTTP call to external service.
     */
    public void recordOutboundCall(String targetService, String method, String uri, int statusCode, Duration duration) {
        Timer.builder("hdim.http.outbound.duration")
                .description("Outbound HTTP call duration")
                .tag("service", serviceName)
                .tag("target_service", targetService)
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .tag("status", String.valueOf(statusCode))
                .register(registry)
                .record(duration);
    }

    /**
     * Record circuit breaker state.
     */
    public void recordCircuitBreakerState(String circuitName, String state) {
        Counter.builder("hdim.circuit_breaker.state_change.total")
                .description("Circuit breaker state changes")
                .tag("service", serviceName)
                .tag("circuit_name", circuitName)
                .tag("state", state)
                .register(registry)
                .increment();
    }

    /**
     * Record retry attempt.
     */
    public void recordRetry(String operation, int attemptNumber, boolean success) {
        Counter.builder("hdim.retry.attempt.total")
                .description("Retry attempts")
                .tag("service", serviceName)
                .tag("operation", operation)
                .tag("attempt", String.valueOf(attemptNumber))
                .tag("success", String.valueOf(success))
                .register(registry)
                .increment();
    }

    /**
     * Start a timer for HTTP request.
     */
    public Timer.Sample startRequestTimer() {
        return Timer.start(registry);
    }

    /**
     * Stop timer and record HTTP request.
     */
    public void stopRequestTimer(Timer.Sample sample, String method, String uri, int statusCode) {
        sample.stop(Timer.builder("hdim.http.request.duration")
                .description("HTTP request duration")
                .tag("service", serviceName)
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .tag("status", String.valueOf(statusCode))
                .tag("status_class", getStatusClass(statusCode))
                .register(registry));
    }

    /**
     * Normalize URI to avoid high cardinality (replace IDs with placeholders).
     */
    private String normalizeUri(String uri) {
        if (uri == null) {
            return "unknown";
        }
        // Replace UUIDs
        String normalized = uri.replaceAll(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                "{id}");
        // Replace numeric IDs
        normalized = normalized.replaceAll("/\\d+", "/{id}");
        // Limit length
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        return normalized;
    }

    /**
     * Get status class (2xx, 3xx, 4xx, 5xx).
     */
    private String getStatusClass(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "2xx";
        } else if (statusCode >= 300 && statusCode < 400) {
            return "3xx";
        } else if (statusCode >= 400 && statusCode < 500) {
            return "4xx";
        } else if (statusCode >= 500) {
            return "5xx";
        }
        return "unknown";
    }
}
