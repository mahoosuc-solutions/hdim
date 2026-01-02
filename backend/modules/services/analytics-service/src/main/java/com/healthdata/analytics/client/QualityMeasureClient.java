package com.healthdata.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "quality-measure-service", url = "${feign.client.config.quality-measure-service.url:http://localhost:8087}")
public interface QualityMeasureClient {

    @GetMapping("/api/quality-measure/measures/summary")
    Map<String, Object> getMeasureSummary(@RequestHeader("X-Tenant-ID") String tenantId);

    @GetMapping("/api/quality-measure/measures/performance")
    Map<String, Object> getMeasurePerformance(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "measureYear", required = false) Integer measureYear);

    @GetMapping("/api/quality-measure/star-ratings")
    Map<String, Object> getStarRatings(@RequestHeader("X-Tenant-ID") String tenantId);
}
