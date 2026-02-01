package com.healthdata.ingestion.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for interacting with Care Gap Service.
 *
 * Handles:
 * - Creating care gaps for a tenant via POST /care-gap/
 * - Counting care gaps for validation via GET /care-gap/count
 * - Deleting all care gaps for tenant reset via DELETE /care-gap/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CareGapIngestionClient {

    private final RestTemplate restTemplate;

    @Value("${care-gap.service.url:http://care-gap-service:8086/care-gap}")
    private String careGapServiceUrl;

    /**
     * Create care gaps for all eligible patients in a tenant.
     *
     * Triggers care gap detection logic in the Care Gap Service,
     * which evaluates patients against quality measures and identifies gaps.
     *
     * @param tenantId Tenant to create care gaps for
     * @return Number of care gaps created
     */
    public int createCareGaps(String tenantId) {
        String url = careGapServiceUrl + "/api/v1/care-gaps/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = Map.of(
                "tenantId", tenantId,
                "evaluateAll", true  // Evaluate all patients
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            var response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("careGapsCreated")) {
                int count = (int) response.get("careGapsCreated");
                log.info("Created {} care gaps for tenant: {}", count, tenantId);
                return count;
            }

            log.warn("Care gap response did not contain expected count field");
            return 0;

        } catch (Exception e) {
            log.error("Failed to create care gaps: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Count total care gaps for a tenant (validation).
     *
     * @param tenantId Tenant to count care gaps for
     * @return Care gap count
     */
    public int countCareGaps(String tenantId) {
        String url = careGapServiceUrl + "/api/v1/care-gaps/count";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);

        try {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            var response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("count")) {
                return (int) response.getBody().get("count");
            }

            return 0;
        } catch (Exception e) {
            log.error("Failed to count care gaps: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Delete all care gaps for a tenant (reset operation).
     *
     * WARNING: Destructive operation - use only in demo environments.
     *
     * @param tenantId Tenant to delete care gaps for
     */
    public void deleteAllCareGaps(String tenantId) {
        String url = careGapServiceUrl + "/api/v1/care-gaps";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);

        try {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);

            log.info("Deleted all care gaps for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to delete care gaps: {}", e.getMessage(), e);
            throw new RuntimeException("Care gap deletion failed", e);
        }
    }
}
