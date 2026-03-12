package com.healthdata.gateway.clinical;

import com.healthdata.gateway.clinical.executive.CmoOnboardingAggregationService;
import com.healthdata.gateway.service.GatewayForwarder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway routing for clinical and regulatory services.
 */
@RestController
@RequiredArgsConstructor
public class GatewayClinicalController {

    private final GatewayForwarder forwarder;
    private final CmoOnboardingAggregationService cmoOnboardingAggregationService;

    @Value("${backend.services.care-gap.url}")
    private String careGapUrl;

    @Value("${backend.services.quality-measure.url}")
    private String qualityMeasureUrl;

    @Value("${backend.services.consent.url}")
    private String consentUrl;

    @Value("${backend.services.events.url}")
    private String eventsUrl;

    @Value("${backend.services.qrda-export.url}")
    private String qrdaExportUrl;

    @Value("${backend.services.hcc.url}")
    private String hccUrl;

    @Value("${backend.services.ecr.url}")
    private String ecrUrl;

    @Value("${backend.services.prior-auth.url}")
    private String priorAuthUrl;

    @Value("${backend.services.demo-seeding.url}")
    private String demoSeedingUrl;

    @Value("${backend.services.analytics.url}")
    private String analyticsUrl;

    @Value("${backend.services.notification.url}")
    private String notificationUrl;

    @Value("${backend.services.nurse-workflow.url}")
    private String nurseWorkflowUrl;

    @Value("${backend.services.clinical-workflow.url}")
    private String clinicalWorkflowUrl;

    @Value("${backend.services.payer-workflows.url}")
    private String payerWorkflowsUrl;

    @Value("${backend.services.data-ingestion.url}")
    private String dataIngestionUrl;

    @RequestMapping(value = "/api/care-gaps/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCareGap(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, careGapUrl, "/api/care-gaps");
    }

    @RequestMapping(value = "/api/v1/care-gaps/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCareGapV1(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, careGapUrl, "/api/v1/care-gaps");
    }

    @RequestMapping(value = "/care-gap/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCareGapDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, careGapUrl, "/care-gap");
    }

    @RequestMapping(value = "/quality-measure/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQualityMeasure(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, qualityMeasureUrl, "/quality-measure");
    }

    @RequestMapping(value = "/api/quality-measure/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQualityMeasureApi(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, qualityMeasureUrl, "/api/quality-measure");
    }

    @RequestMapping(value = "/api/consent/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToConsent(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, consentUrl, "/api/consent");
    }

    @RequestMapping(value = "/consent/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToConsentDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, consentUrl, "/consent");
    }

    @RequestMapping(value = "/api/events/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToEvents(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, eventsUrl, "/api/events");
    }

    @RequestMapping(value = "/events/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToEventsDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, eventsUrl, "/events");
    }

    @RequestMapping(value = "/api/v1/qrda/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQrdaExport(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, qrdaExportUrl, "/api/v1/qrda");
    }

    @RequestMapping(value = "/api/v1/hcc/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToHcc(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, hccUrl, "/api/v1/hcc");
    }

    @RequestMapping(value = "/api/ecr/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToEcr(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, ecrUrl, "/api/ecr");
    }

    @RequestMapping(value = "/api/v1/prior-auth/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToPriorAuth(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, priorAuthUrl, "/api/v1/prior-auth");
    }

    @RequestMapping(value = "/api/v1/provider-access/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToProviderAccess(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, priorAuthUrl, "/api/v1/provider-access");
    }

    @RequestMapping(value = "/api/v1/revenue/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToRevenueContracts(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, payerWorkflowsUrl, "/api/v1/revenue");
    }

    @RequestMapping(value = "/api/v1/interoperability/adt/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToAdtInteroperability(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, dataIngestionUrl, "/api/v1/interoperability/adt");
    }

    @RequestMapping(value = "/demo/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToDemoSeeding(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, demoSeedingUrl, "/demo");
    }

    @RequestMapping(value = "/api/v1/analytics/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToAnalytics(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, analyticsUrl, "/api/v1/analytics");
    }

    @RequestMapping(value = "/analytics/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToAnalyticsDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, analyticsUrl, "/analytics");
    }

    @RequestMapping(value = "/notification/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToNotification(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, notificationUrl, "/notification");
    }

    @RequestMapping(value = "/nurse-workflow/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToNurseWorkflow(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, nurseWorkflowUrl, "/nurse-workflow");
    }

    @RequestMapping(value = "/clinical-workflow/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToClinicalWorkflow(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, clinicalWorkflowUrl, "/clinical-workflow");
    }

    /**
     * CMO onboarding summary contract used by the clinical portal executive onboarding page.
     *
     * This gateway-level endpoint provides a stable response contract for the UI while downstream
     * cross-service aggregation is finalized. The contract is intentionally aligned with
     * apps/clinical-portal/src/app/services/cmo-onboarding.service.ts.
     */
    @RequestMapping(value = "/api/executive/cmo-onboarding/summary", method = RequestMethod.GET)
    public ResponseEntity<?> getCmoOnboardingSummary(
        @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return ResponseEntity.ok(cmoOnboardingAggregationService.buildSummary(tenantId, authorizationHeader));
    }
}
