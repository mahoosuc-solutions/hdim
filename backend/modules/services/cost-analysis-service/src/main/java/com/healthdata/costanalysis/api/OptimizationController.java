package com.healthdata.costanalysis.api;

import com.healthdata.costanalysis.application.OptimizationRecommendationEngine;
import com.healthdata.costanalysis.application.TrackCost;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class OptimizationController {

    private final OptimizationRecommendationEngine recommendationEngine;

    @GetMapping("/pending")
    @TrackCost(serviceId = "cost-analysis-service", featureKey = "recommendations-pending")
    public ResponseEntity<List<OptimizationRecommendation>> pending(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        List<OptimizationRecommendation> recommendations = recommendationEngine.getPendingRecommendations(tenantId);
        int from = Math.min(offset, recommendations.size());
        int to = Math.min(from + Math.max(limit, 0), recommendations.size());
        return ResponseEntity.ok(recommendations.subList(from, to));
    }

    @GetMapping("/high-priority")
    public ResponseEntity<List<OptimizationRecommendation>> highPriority(@RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(recommendationEngine.getHighPriorityRecommendations(tenantId));
    }

    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<OptimizationRecommendation>> byService(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable String serviceName
    ) {
        return ResponseEntity.ok(recommendationEngine.getRecommendationsByService(tenantId, serviceName));
    }

    @PostMapping("/{recommendationId}/accept")
    public ResponseEntity<OptimizationRecommendation> accept(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable UUID recommendationId
    ) {
        return ResponseEntity.ok(recommendationEngine.acceptRecommendation(recommendationId));
    }

    @PostMapping("/{recommendationId}/complete")
    public ResponseEntity<OptimizationRecommendation> complete(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable UUID recommendationId,
        @RequestParam BigDecimal actualSavings
    ) {
        return ResponseEntity.ok(recommendationEngine.completeRecommendation(recommendationId, actualSavings));
    }

    @PostMapping("/{recommendationId}/reject")
    public ResponseEntity<OptimizationRecommendation> reject(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable UUID recommendationId,
        @RequestParam String reason
    ) {
        return ResponseEntity.ok(recommendationEngine.rejectRecommendation(recommendationId, reason));
    }

    @GetMapping("/savings/potential")
    public ResponseEntity<Map<String, Object>> savingsPotential(@RequestHeader("X-Tenant-ID") String tenantId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("potentialSavings", recommendationEngine.calculateTotalPendingSavings(tenantId));
        payload.put("implementedCount", recommendationEngine.countImplementedRecommendations(tenantId));
        return ResponseEntity.ok(payload);
    }
}
