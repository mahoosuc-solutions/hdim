package com.healthdata.costanalysis.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.costanalysis.application.CostAnalysisService;
import com.healthdata.costanalysis.application.TrackCost;
import com.healthdata.costanalysis.domain.model.CostAnalysisCache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class CostAnalysisController {

    private static final MediaType JSON_UTF8 = MediaType.valueOf("application/json;charset=UTF-8");

    private final CostAnalysisService costAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/costs")
    @TrackCost(serviceId = "cost-analysis-service", featureKey = "analysis-costs")
    public ResponseEntity<?> getCosts(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam String analysisType,
        @RequestParam String analysisPeriod
    ) {
        CostAnalysisCache cache = costAnalysisService.analyzeCosts(tenantId, analysisType, analysisPeriod, null);
        Object payload = parseResult(cache.getResultData());
        return ResponseEntity.ok()
            .headers(noStoreHeaders())
            .contentType(JSON_UTF8)
            .body(payload);
    }

    @GetMapping("/drilldown")
    @TrackCost(serviceId = "cost-analysis-service", featureKey = "analysis-drilldown")
    public ResponseEntity<Map<String, Object>> getDrilldown(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam String serviceName,
        @RequestParam String dimension
    ) {
        CostAnalysisCache cache = costAnalysisService.performDrilldownAnalysis(tenantId, serviceName, dimension, "2025-01");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", tenantId);
        response.put("serviceName", serviceName);
        response.put("dimension", dimension);
        response.put("analysis", parseResult(cache.getResultData()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<CostAnalysisCache>> getRecent(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(costAnalysisService.getRecentAnalysis(tenantId, limit));
    }

    @GetMapping("/cache-metrics")
    public ResponseEntity<Map<String, Object>> cacheMetrics(@RequestHeader("X-Tenant-ID") String tenantId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("hitRatio", costAnalysisService.getCacheHitRatio(tenantId));
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/cache/invalidate")
    public ResponseEntity<Map<String, Object>> invalidate(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam String analysisType,
        @RequestParam String analysisPeriod
    ) {
        costAnalysisService.invalidateAnalysisCache(tenantId, analysisType, analysisPeriod);
        return ResponseEntity.ok(Map.of("status", "invalidated"));
    }

    @PostMapping("/cache/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup(@RequestHeader("X-Tenant-ID") String tenantId) {
        costAnalysisService.clearExpiredCache();
        return ResponseEntity.ok(Map.of("status", "cleanup-complete", "tenantId", tenantId));
    }

    private Object parseResult(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ignored) {
            return Map.of("raw", json);
        }
    }

    private HttpHeaders noStoreHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        return headers;
    }
}
