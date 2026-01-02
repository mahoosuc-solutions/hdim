package com.healthdata.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "care-gap-service", url = "${feign.client.config.care-gap-service.url:http://localhost:8086}")
public interface CareGapClient {

    @GetMapping("/api/care-gap/summary")
    Map<String, Object> getCareGapSummary(@RequestHeader("X-Tenant-ID") String tenantId);

    @GetMapping("/api/care-gap/by-measure")
    Map<String, Object> getCareGapsByMeasure(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "measureId", required = false) String measureId);

    @GetMapping("/api/care-gap/closure-rates")
    Map<String, Object> getClosureRates(@RequestHeader("X-Tenant-ID") String tenantId);
}
