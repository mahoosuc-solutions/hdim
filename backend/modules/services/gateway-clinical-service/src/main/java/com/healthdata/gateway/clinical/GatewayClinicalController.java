package com.healthdata.gateway.clinical;

import com.healthdata.gateway.service.GatewayForwarder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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

    @Value("${backend.services.care-gap.url}")
    private String careGapUrl;

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

    @RequestMapping(value = "/api/care-gaps/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCareGap(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, careGapUrl, "/api/care-gaps");
    }

    @RequestMapping(value = "/care-gap/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCareGapDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, careGapUrl, "/care-gap");
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
}
