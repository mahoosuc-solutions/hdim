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
 * HTTP client for interacting with Quality Measure Service.
 *
 * Handles:
 * - Seeding quality measures for a tenant via POST /quality-measure/api/v1/measures/seed
 * - Counting measures for validation via GET /quality-measure/api/v1/measures/count
 * - Deleting all measures for tenant reset via DELETE /quality-measure/api/v1/measures
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QualityMeasureIngestionClient {

    private final RestTemplate restTemplate;

    @Value("${quality-measure.service.url:http://quality-measure-service:8087/quality-measure}")
    private String qualityMeasureServiceUrl;

    /**
     * Seed quality measures for a tenant.
     *
     * Pre-loads HEDIS measures (BCS, COL, CBP, CDC, CCS, EED, SPC) and
     * triggers initial evaluation for all patients.
     *
     * @param tenantId Tenant to seed measures for
     * @return Number of measures seeded
     */
    public int seedMeasures(String tenantId) {
        String url = qualityMeasureServiceUrl + "/api/v1/measures/seed";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = Map.of(
                "tenantId", tenantId,
                "measureSet", "HEDIS",  // HEDIS quality measures
                "evaluatePatients", true  // Run initial evaluation
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            var response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("measuresSeeded")) {
                int count = (int) response.get("measuresSeeded");
                log.info("Seeded {} quality measures for tenant: {}", count, tenantId);
                return count;
            }

            log.warn("Quality measure response did not contain expected count field");
            return 0;

        } catch (Exception e) {
            log.error("Failed to seed quality measures: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Count total quality measures for a tenant (validation).
     *
     * @param tenantId Tenant to count measures for
     * @return Measure count
     */
    public int countMeasures(String tenantId) {
        String url = qualityMeasureServiceUrl + "/api/v1/measures/count";

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
            log.error("Failed to count quality measures: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Delete all quality measures for a tenant (reset operation).
     *
     * WARNING: Destructive operation - use only in demo environments.
     *
     * @param tenantId Tenant to delete measures for
     */
    public void deleteAllMeasures(String tenantId) {
        String url = qualityMeasureServiceUrl + "/api/v1/measures";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);

        try {
            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);

            log.info("Deleted all quality measures for tenant: {}", tenantId);
        } catch (Exception e) {
            log.error("Failed to delete quality measures: {}", e.getMessage(), e);
            throw new RuntimeException("Quality measure deletion failed", e);
        }
    }
}
