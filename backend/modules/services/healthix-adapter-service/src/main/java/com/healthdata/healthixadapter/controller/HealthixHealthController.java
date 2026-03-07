package com.healthdata.healthixadapter.controller;

import com.healthdata.healthixadapter.config.HealthixProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/external/healthix")
@RequiredArgsConstructor
@Slf4j
public class HealthixHealthController {

    private final HealthixProperties properties;
    private final Map<String, EndpointHealth> healthStatus = new ConcurrentHashMap<>();

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "service", "healthix-adapter-service",
                "enabled", properties.isEnabled(),
                "phiLevel", "FULL",
                "mtlsEnabled", properties.getMtls().isEnabled(),
                "endpoints", healthStatus
        ));
    }

    @Scheduled(fixedDelayString = "${external.healthix.health-check-interval-ms:60000}")
    public void checkEndpointHealth() {
        if (!properties.isEnabled()) return;

        checkEndpoint("fhir", properties.getFhirUrl() + "/fhir/metadata");
        checkEndpoint("mpi", properties.getMpiUrl() + "/health");
        checkEndpoint("documents", properties.getDocumentServiceUrl() + "/health");
        checkEndpoint("gateway", properties.getGatewayUrl() + "/health");
    }

    private void checkEndpoint(String name, String url) {
        try {
            RestTemplate rt = new RestTemplate();
            rt.getForEntity(url, String.class);
            healthStatus.put(name, new EndpointHealth("UP", Instant.now(), null));
        } catch (Exception e) {
            healthStatus.put(name, new EndpointHealth("DOWN", Instant.now(), e.getMessage()));
            log.warn("Healthix endpoint {} is DOWN: {}", name, e.getMessage());
        }
    }

    public record EndpointHealth(String status, Instant lastChecked, String error) {}
}
