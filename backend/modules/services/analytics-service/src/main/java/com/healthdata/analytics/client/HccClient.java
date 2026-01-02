package com.healthdata.analytics.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "hcc-service", url = "${feign.client.config.hcc-service.url:http://localhost:8091}")
public interface HccClient {

    @GetMapping("/api/hcc/raf-scores/summary")
    Map<String, Object> getRafScoreSummary(@RequestHeader("X-Tenant-ID") String tenantId);

    @GetMapping("/api/hcc/raf-scores/distribution")
    Map<String, Object> getRafScoreDistribution(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "year", required = false) Integer year);

    @GetMapping("/api/hcc/opportunities")
    Map<String, Object> getHccOpportunities(@RequestHeader("X-Tenant-ID") String tenantId);
}
