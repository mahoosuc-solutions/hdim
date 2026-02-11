package com.healthdata.agentvalidation.client;

import com.healthdata.agentvalidation.client.dto.JaegerTraceResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for Jaeger REST API communication.
 * Used to fetch trace data for correlation with test executions.
 */
@FeignClient(
    name = "jaeger-api",
    url = "${hdim.services.jaeger.url:http://jaeger:16686}"
)
public interface JaegerApiClient {

    /**
     * Fetch a trace by its ID.
     */
    @GetMapping("/api/traces/{traceId}")
    @CircuitBreaker(name = "jaeger-api")
    @Retry(name = "jaeger-api")
    JaegerTraceResponse getTrace(
        @PathVariable("traceId") String traceId
    );

    /**
     * Search for traces matching criteria.
     */
    @GetMapping("/api/traces")
    @CircuitBreaker(name = "jaeger-api")
    @Retry(name = "jaeger-api")
    JaegerTraceSearchResponse searchTraces(
        @RequestParam("service") String serviceName,
        @RequestParam(value = "operation", required = false) String operationName,
        @RequestParam(value = "start", required = false) Long startTimeUnixMicro,
        @RequestParam(value = "end", required = false) Long endTimeUnixMicro,
        @RequestParam(value = "limit", defaultValue = "100") int limit,
        @RequestParam(value = "tags", required = false) String tags
    );

    /**
     * Get available services in Jaeger.
     */
    @GetMapping("/api/services")
    @CircuitBreaker(name = "jaeger-api")
    JaegerServicesResponse getServices();

    /**
     * Get operations for a service.
     */
    @GetMapping("/api/services/{service}/operations")
    @CircuitBreaker(name = "jaeger-api")
    JaegerOperationsResponse getOperations(
        @PathVariable("service") String serviceName
    );

    /**
     * Response for trace search.
     */
    record JaegerTraceSearchResponse(
        List<JaegerTraceResponse.TraceData> data,
        int total,
        int limit,
        int offset,
        List<String> errors
    ) {}

    /**
     * Response for services list.
     */
    record JaegerServicesResponse(
        List<String> data,
        int total,
        int limit,
        int offset,
        List<String> errors
    ) {}

    /**
     * Response for operations list.
     */
    record JaegerOperationsResponse(
        List<String> data,
        int total,
        int limit,
        int offset,
        List<String> errors
    ) {}
}
