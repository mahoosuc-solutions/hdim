package com.healthdata.agentvalidation.controller;

import com.healthdata.agentvalidation.service.CorrelationAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Correlation Analysis", description = "Service dependency and root cause analysis endpoints")
public class CorrelationAnalysisController {

    private final CorrelationAnalysisService correlationAnalysisService;

    @GetMapping("/root-causes/{anomalyId}")
    @Operation(summary = "Get ranked root-cause candidates for an anomaly")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<CorrelationAnalysisService.RootCauseAnalysisResponse> getRootCauses(
        @PathVariable String anomalyId,
        @RequestParam("service") String service,
        @RequestParam(value = "hours", defaultValue = "24") int hours,
        @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(correlationAnalysisService.analyzeRootCauses(anomalyId, service, hours, limit));
    }

    @GetMapping("/service-dependencies")
    @Operation(summary = "Get service dependency graph from Jaeger traces")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<CorrelationAnalysisService.ServiceDependencyResponse> getServiceDependencies(
        @RequestParam("service") String service,
        @RequestParam(value = "hours", defaultValue = "24") int hours,
        @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(correlationAnalysisService.getServiceDependencies(service, hours, limit));
    }

    @GetMapping("/failure-paths/{serviceId}")
    @Operation(summary = "Get failure propagation paths from a given service")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<CorrelationAnalysisService.FailurePathResponse> getFailurePaths(
        @PathVariable String serviceId,
        @RequestParam("service") String service,
        @RequestParam(value = "hours", defaultValue = "24") int hours,
        @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(correlationAnalysisService.getFailurePaths(serviceId, service, hours, limit));
    }
}
