package com.healthdata.gateway.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Service Routing Service with Circuit Breaker Protection
 *
 * Provides resilient routing to backend microservices with:
 * - Circuit breaker pattern for fault tolerance
 * - Retry with exponential backoff
 * - Timeout protection
 * - Fallback responses
 *
 * Each backend service has its own circuit breaker to prevent
 * cascading failures across the platform.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceRoutingService {

    private final RestTemplate restTemplate;

    @Value("${backend.services.cql-engine.url}")
    private String cqlEngineUrl;

    @Value("${backend.services.quality-measure.url}")
    private String qualityMeasureUrl;

    @Value("${backend.services.fhir.url}")
    private String fhirUrl;

    @Value("${backend.services.patient.url}")
    private String patientUrl;

    @Value("${backend.services.care-gap.url}")
    private String careGapUrl;

    // ==================== CQL Engine Service ====================

    @CircuitBreaker(name = "cqlEngine", fallbackMethod = "cqlEngineFallback")
    @Retry(name = "cqlEngine")
    @TimeLimiter(name = "cqlEngine")
    public CompletableFuture<ResponseEntity<String>> routeToCqlEngine(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body
    ) {
        return CompletableFuture.supplyAsync(() ->
            executeRequest(cqlEngineUrl + path, method, headers, body)
        );
    }

    public CompletableFuture<ResponseEntity<String>> cqlEngineFallback(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body,
            Throwable t
    ) {
        log.error("CQL Engine circuit breaker triggered for {}: {}", path, t.getMessage());
        return CompletableFuture.completedFuture(
            createFallbackResponse("CQL Engine Service", t)
        );
    }

    // ==================== Quality Measure Service ====================

    @CircuitBreaker(name = "qualityMeasure", fallbackMethod = "qualityMeasureFallback")
    @Retry(name = "qualityMeasure")
    @TimeLimiter(name = "qualityMeasure")
    public CompletableFuture<ResponseEntity<String>> routeToQualityMeasure(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body
    ) {
        return CompletableFuture.supplyAsync(() ->
            executeRequest(qualityMeasureUrl + path, method, headers, body)
        );
    }

    public CompletableFuture<ResponseEntity<String>> qualityMeasureFallback(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body,
            Throwable t
    ) {
        log.error("Quality Measure circuit breaker triggered for {}: {}", path, t.getMessage());
        return CompletableFuture.completedFuture(
            createFallbackResponse("Quality Measure Service", t)
        );
    }

    // ==================== FHIR Service ====================

    @CircuitBreaker(name = "fhirService", fallbackMethod = "fhirServiceFallback")
    @Retry(name = "fhirService")
    @TimeLimiter(name = "fhirService")
    public CompletableFuture<ResponseEntity<String>> routeToFhir(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body
    ) {
        return CompletableFuture.supplyAsync(() ->
            executeRequest(fhirUrl + path, method, headers, body)
        );
    }

    public CompletableFuture<ResponseEntity<String>> fhirServiceFallback(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body,
            Throwable t
    ) {
        log.error("FHIR Service circuit breaker triggered for {}: {}", path, t.getMessage());
        return CompletableFuture.completedFuture(
            createFallbackResponse("FHIR Service", t)
        );
    }

    // ==================== Patient Service ====================

    @CircuitBreaker(name = "patientService", fallbackMethod = "patientServiceFallback")
    @Retry(name = "patientService")
    @TimeLimiter(name = "patientService")
    public CompletableFuture<ResponseEntity<String>> routeToPatient(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body
    ) {
        return CompletableFuture.supplyAsync(() ->
            executeRequest(patientUrl + path, method, headers, body)
        );
    }

    public CompletableFuture<ResponseEntity<String>> patientServiceFallback(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body,
            Throwable t
    ) {
        log.error("Patient Service circuit breaker triggered for {}: {}", path, t.getMessage());
        return CompletableFuture.completedFuture(
            createFallbackResponse("Patient Service", t)
        );
    }

    // ==================== Care Gap Service ====================

    @CircuitBreaker(name = "careGapService", fallbackMethod = "careGapServiceFallback")
    @Retry(name = "careGapService")
    @TimeLimiter(name = "careGapService")
    public CompletableFuture<ResponseEntity<String>> routeToCareGap(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body
    ) {
        return CompletableFuture.supplyAsync(() ->
            executeRequest(careGapUrl + path, method, headers, body)
        );
    }

    public CompletableFuture<ResponseEntity<String>> careGapServiceFallback(
            String path,
            HttpMethod method,
            HttpHeaders headers,
            String body,
            Throwable t
    ) {
        log.error("Care Gap Service circuit breaker triggered for {}: {}", path, t.getMessage());
        return CompletableFuture.completedFuture(
            createFallbackResponse("Care Gap Service", t)
        );
    }

    // ==================== Helper Methods ====================

    private ResponseEntity<String> executeRequest(
            String url,
            HttpMethod method,
            HttpHeaders headers,
            String body
    ) {
        try {
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
            return restTemplate.exchange(
                URI.create(url),
                method,
                requestEntity,
                String.class
            );
        } catch (Exception e) {
            log.error("Error executing request to {}: {}", url, e.getMessage());
            throw e;
        }
    }

    private ResponseEntity<String> createFallbackResponse(String serviceName, Throwable t) {
        String errorMessage = String.format(
            "{\"error\":\"Service Unavailable\",\"service\":\"%s\",\"message\":\"The service is temporarily unavailable. Please try again later.\",\"status\":503}",
            serviceName
        );

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Retry-After", "30")
            .body(errorMessage);
    }
}
