package com.healthdata.democli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for communicating with the demo-seeding-service.
 */
@Component
public class DemoApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public DemoApiClient(
            @Value("${demo.api.base-url:http://localhost:8098}") String baseUrl,
            ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Tenant-ID", "demo")
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Reset all demo data.
     */
    public Map<String, Object> reset() {
        return post("/api/v1/demo/reset", Map.of());
    }

    /**
     * Initialize demo environment.
     */
    public Map<String, Object> initialize() {
        return post("/api/v1/demo/initialize", Map.of());
    }

    /**
     * Get demo system status.
     */
    public Map<String, Object> getStatus() {
        return get("/api/v1/demo/status");
    }

    /**
     * List available scenarios.
     */
    public List<Map<String, Object>> listScenarios() {
        JsonNode response = getNode("/api/v1/demo/scenarios");
        return objectMapper.convertValue(response, new TypeReference<>() {});
    }

    /**
     * Load a specific scenario.
     */
    public Map<String, Object> loadScenario(String scenarioId) {
        return post("/api/v1/demo/scenarios/" + scenarioId + "/load", Map.of());
    }

    /**
     * Get current scenario.
     */
    public Map<String, Object> getCurrentScenario() {
        return get("/api/v1/demo/scenarios/current");
    }

    /**
     * Generate synthetic patients.
     */
    public Map<String, Object> generatePatients(int count, String tenantId, String riskProfile) {
        Map<String, Object> request = Map.of(
                "count", count,
                "tenantId", tenantId != null ? tenantId : "demo",
                "riskProfile", riskProfile != null ? riskProfile : "MIXED"
        );
        return post("/api/v1/demo/patients/generate", request);
    }

    /**
     * Create a snapshot.
     */
    public Map<String, Object> createSnapshot(String name, String description) {
        Map<String, Object> request = Map.of(
                "name", name,
                "description", description != null ? description : ""
        );
        return post("/api/v1/demo/snapshots", request);
    }

    /**
     * List snapshots.
     */
    public List<Map<String, Object>> listSnapshots() {
        JsonNode response = getNode("/api/v1/demo/snapshots");
        return objectMapper.convertValue(response, new TypeReference<>() {});
    }

    /**
     * Restore a snapshot.
     */
    public Map<String, Object> restoreSnapshot(String snapshotId) {
        return post("/api/v1/demo/snapshots/" + snapshotId + "/restore", Map.of());
    }

    /**
     * Delete a snapshot.
     */
    public Map<String, Object> deleteSnapshot(String snapshotId) {
        return delete("/api/v1/demo/snapshots/" + snapshotId);
    }

    private Map<String, Object> get(String path) {
        try {
            return webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    private JsonNode getNode(String path) {
        try {
            return webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            return webClient.post()
                    .uri(path)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofMinutes(5))
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    private Map<String, Object> delete(String path) {
        try {
            return webClient.delete()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(60))
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * Check if the demo-seeding-service is available.
     */
    public boolean isServiceAvailable() {
        try {
            webClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
