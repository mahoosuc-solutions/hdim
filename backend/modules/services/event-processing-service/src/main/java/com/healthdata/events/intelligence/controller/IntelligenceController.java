package com.healthdata.events.intelligence.controller;

import com.healthdata.events.intelligence.config.IntelligenceFeatureFlags;
import com.healthdata.events.intelligence.dto.IngestRequest;
import com.healthdata.events.intelligence.dto.RecommendationLineageResponse;
import com.healthdata.events.intelligence.dto.RecommendationResponse;
import com.healthdata.events.intelligence.dto.ReviewRecommendationRequest;
import com.healthdata.events.intelligence.dto.SignalResponse;
import com.healthdata.events.intelligence.dto.TenantTrustDashboardResponse;
import com.healthdata.events.intelligence.dto.TrustProfileResponse;
import com.healthdata.events.intelligence.dto.UpdateValidationFindingStatusRequest;
import com.healthdata.events.intelligence.dto.ValidationFindingResponse;
import com.healthdata.events.intelligence.projection.TenantTrustProjectionService;
import com.healthdata.events.intelligence.security.IntelligenceActorResolver;
import com.healthdata.events.intelligence.service.IntelligenceRecommendationService;
import com.healthdata.events.intelligence.service.IntelligenceValidationService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final IntelligenceRecommendationService recommendationService;
    private final IntelligenceValidationService validationService;
    private final TenantTrustProjectionService tenantTrustProjectionService;
    private final IntelligenceFeatureFlags intelligenceFeatureFlags;
    private final IntelligenceActorResolver actorResolver;

    @PostMapping("/ingest")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DATA_ENGINEER','INTEGRATIONS_MANAGER')")
    public ResponseEntity<List<RecommendationResponse>> ingest(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody IngestRequest request) {
        intelligenceFeatureFlags.requireIngestEnabled(tenantId);
        Timer.Sample sample = Timer.start(Metrics.globalRegistry);

        var envelope = request.toEnvelope(tenantId);

        List<RecommendationResponse> responses = recommendationService.ingestAndGenerate(envelope);
        Counter.builder("intelligence.ingest.requests.total")
                .tag("tenant", tenantId)
                .tag("outcome", "accepted")
                .register(Metrics.globalRegistry)
                .increment();
        sample.stop(Timer.builder("intelligence.ingest.duration")
                .tag("tenant", tenantId)
                .tag("outcome", "accepted")
                .register(Metrics.globalRegistry));
        return ResponseEntity.accepted().body(responses);
    }

    @GetMapping("/patients/{patientId}/signals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLINICAL_ADMIN','QUALITY_OFFICER','PROVIDER','CARE_COORDINATOR')")
    public ResponseEntity<List<SignalResponse>> getSignals(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("patientId") String patientId) {
        return ResponseEntity.ok(recommendationService.getSignals(tenantId, patientId));
    }

    @GetMapping("/patients/{patientId}/validation-findings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLINICAL_ADMIN','QUALITY_OFFICER','PROVIDER','CARE_COORDINATOR')")
    public ResponseEntity<List<ValidationFindingResponse>> getValidationFindings(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("patientId") String patientId) {
        return ResponseEntity.ok(validationService.getFindings(tenantId, patientId));
    }

    @PostMapping("/validation-findings/{findingId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLINICAL_ADMIN','QUALITY_OFFICER')")
    public ResponseEntity<ValidationFindingResponse> updateValidationFindingStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("findingId") UUID findingId,
            @Valid @RequestBody UpdateValidationFindingStatusRequest request,
            HttpServletRequest httpServletRequest) {
        intelligenceFeatureFlags.requireValidationStatusUpdateEnabled(tenantId);
        String actor = actorResolver.resolveRequiredActor(httpServletRequest);
        UpdateValidationFindingStatusRequest actorBoundRequest =
                new UpdateValidationFindingStatusRequest(request.status(), actor, request.notes());
        return ResponseEntity.ok(validationService.updateFindingStatus(tenantId, findingId, actorBoundRequest));
    }

    @GetMapping("/patients/{patientId}/trust-profile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLINICAL_ADMIN','QUALITY_OFFICER','PROVIDER','CARE_COORDINATOR')")
    public ResponseEntity<TrustProfileResponse> getTrustProfile(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("patientId") String patientId) {
        return ResponseEntity.ok(validationService.getTrustProfile(tenantId, patientId));
    }

    @GetMapping("/patients/{patientId}/recommendations")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLINICAL_ADMIN','QUALITY_OFFICER','PROVIDER','CARE_COORDINATOR')")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("patientId") String patientId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(tenantId, patientId));
    }

    @PostMapping("/recommendations/{recommendationId}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLINICAL_ADMIN','QUALITY_OFFICER')")
    public ResponseEntity<RecommendationResponse> reviewRecommendation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("recommendationId") UUID recommendationId,
            @Valid @RequestBody ReviewRecommendationRequest request,
            HttpServletRequest httpServletRequest) {
        intelligenceFeatureFlags.requireRecommendationReviewEnabled(tenantId);
        String actor = actorResolver.resolveRequiredActor(httpServletRequest);
        ReviewRecommendationRequest actorBoundRequest =
                new ReviewRecommendationRequest(request.status(), actor, request.notes());
        return ResponseEntity.ok(recommendationService.reviewRecommendation(tenantId, recommendationId, actorBoundRequest));
    }

    @GetMapping("/recommendations/{recommendationId}/lineage")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLINICAL_ADMIN','QUALITY_OFFICER')")
    public ResponseEntity<RecommendationLineageResponse> getLineage(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable("recommendationId") UUID recommendationId) {
        return ResponseEntity.ok(recommendationService.getLineage(tenantId, recommendationId));
    }

    @GetMapping("/tenants/{tenantId}/trust-dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLINICAL_ADMIN','QUALITY_OFFICER')")
    public ResponseEntity<TenantTrustDashboardResponse> getTenantTrustDashboard(
            @RequestHeader("X-Tenant-ID") String requestTenantId,
            @PathVariable("tenantId") String tenantId) {
        intelligenceFeatureFlags.requireTrustDashboardEnabled(tenantId);
        if (!requestTenantId.equals(tenantId)) {
            throw new ForbiddenIntelligenceOperationException("Tenant ID mismatch: access denied");
        }
        return ResponseEntity.ok(tenantTrustProjectionService.getTenantDashboard(tenantId));
    }
}
