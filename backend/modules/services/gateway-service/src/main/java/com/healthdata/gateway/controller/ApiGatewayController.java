package com.healthdata.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

/**
 * API Gateway Controller
 *
 * Routes authenticated requests to backend microservices.
 * Forwards JWT tokens and tenant headers to downstream services.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ApiGatewayController {

    private final RestTemplate restTemplate;

    @Value("${backend.services.cql-engine.url}")
    private String cqlEngineUrl;

    @Value("${backend.services.quality-measure.url}")
    private String qualityMeasureUrl;

    @Value("${backend.services.fhir.url}")
    private String fhirUrl;

    @Value("${backend.services.patient.url}")
    private String patientUrl;

    @Value("${backend.services.care-gap.url}")
    private String careGapUrl;

    @Value("${backend.services.agent-builder.url}")
    private String agentBuilderUrl;

    @Value("${backend.services.consent.url}")
    private String consentUrl;

    @Value("${backend.services.events.url}")
    private String eventsUrl;

    @Value("${backend.services.agent-runtime.url}")
    private String agentRuntimeUrl;

    @Value("${backend.services.qrda-export.url}")
    private String qrdaExportUrl;

    @Value("${backend.services.hcc.url}")
    private String hccUrl;

    @Value("${backend.services.ecr.url}")
    private String ecrUrl;

    @Value("${backend.services.prior-auth.url}")
    private String priorAuthUrl;

    /**
     * HTTP hop-by-hop headers that should not be forwarded by proxies.
     * RFC 2616 Section 13.5.1 defines these as connection-specific headers.
     */
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
        "transfer-encoding", "connection", "keep-alive",
        "proxy-authenticate", "proxy-authorization",
        "te", "trailers", "upgrade"
    );

    /**
     * Route to CQL Engine Service
     */
    @RequestMapping(value = "/api/cql/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCqlEngine(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, cqlEngineUrl, "/api/cql");
    }

    /**
     * Route to CQL Engine Service (direct /cql-engine path)
     */
    @RequestMapping(value = "/cql-engine/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCqlEngineDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, cqlEngineUrl, "/cql-engine");
    }

    /**
     * Route to Quality Measure Service
     */
    @RequestMapping(value = "/api/quality/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQualityMeasure(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, qualityMeasureUrl, "/api/quality");
    }

    /**
     * Route to Quality Measure Service (direct /quality-measure path)
     */
    @RequestMapping(value = "/quality-measure/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQualityMeasureDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, qualityMeasureUrl, "/quality-measure");
    }

    /**
     * Route to FHIR Service (via /api/fhir)
     */
    @RequestMapping(value = "/api/fhir/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToFhir(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, fhirUrl, "/api/fhir");
    }

    /**
     * Route to FHIR Service (direct /fhir path)
     */
    @RequestMapping(value = "/fhir/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToFhirDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, fhirUrl, "/fhir");
    }

    /**
     * Route to Patient Service
     */
    @RequestMapping(value = "/api/patients/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToPatient(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, patientUrl, "/api/patients");
    }

    /**
     * Route to Patient Service (direct /patient path)
     */
    @RequestMapping(value = "/patient/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToPatientDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, patientUrl, "/patient");
    }

    /**
     * Route to Care Gap Service
     */
    @RequestMapping(value = "/api/care-gaps/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCareGap(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, careGapUrl, "/api/care-gaps");
    }

    /**
     * Route to Care Gap Service (direct /care-gap path)
     */
    @RequestMapping(value = "/care-gap/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCareGapDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, careGapUrl, "/care-gap");
    }

    /**
     * Route to Consent Service (via /api/consent)
     */
    @RequestMapping(value = "/api/consent/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToConsent(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, consentUrl, "/api/consent");
    }

    /**
     * Route to Consent Service (direct /consent path)
     */
    @RequestMapping(value = "/consent/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToConsentDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, consentUrl, "/consent");
    }

    /**
     * Route to Events Service (via /api/events)
     */
    @RequestMapping(value = "/api/events/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToEvents(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, eventsUrl, "/api/events");
    }

    /**
     * Route to Events Service (direct /events path)
     */
    @RequestMapping(value = "/events/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToEventsDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, eventsUrl, "/events");
    }

    /**
     * Route to Agent Builder Service - All endpoints
     * Base path: /api/v1/agent-builder (agents, templates, test sessions)
     */
    @RequestMapping(value = "/api/v1/agent-builder/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToAgentBuilder(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, agentBuilderUrl, "/api/v1/agent-builder");
    }

    /**
     * Route to Agent Runtime Service - Tools metadata
     */
    @RequestMapping(value = "/api/v1/tools/**", method = {RequestMethod.GET})
    public ResponseEntity<?> routeToAgentRuntimeTools(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, agentRuntimeUrl, "/api/v1/tools");
    }

    /**
     * Route to Agent Runtime Service - Providers metadata
     */
    @RequestMapping(value = "/api/v1/providers/**", method = {RequestMethod.GET})
    public ResponseEntity<?> routeToAgentRuntimeProviders(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, agentRuntimeUrl, "/api/v1/providers");
    }

    /**
     * Route to Agent Runtime Service - Runtime health
     */
    @RequestMapping(value = "/api/v1/runtime/**", method = {RequestMethod.GET})
    public ResponseEntity<?> routeToAgentRuntimeHealth(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, agentRuntimeUrl, "/api/v1/runtime");
    }

    // ==================== NEW REGULATORY COMPLIANCE SERVICES ====================

    /**
     * Route to QRDA Export Service - Quality reporting exports
     */
    @RequestMapping(value = "/api/v1/qrda/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQrdaExport(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, qrdaExportUrl, "/api/v1/qrda");
    }

    /**
     * Route to HCC Service - Risk adjustment calculations
     */
    @RequestMapping(value = "/api/v1/hcc/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToHcc(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, hccUrl, "/api/v1/hcc");
    }

    /**
     * Route to eCR Service - Electronic Case Reporting
     */
    @RequestMapping(value = "/api/ecr/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToEcr(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, ecrUrl, "/api/ecr");
    }

    /**
     * Route to Prior Auth Service - Prior Authorization management
     */
    @RequestMapping(value = "/api/v1/prior-auth/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToPriorAuth(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, priorAuthUrl, "/api/v1/prior-auth");
    }

    /**
     * Route to Prior Auth Service - Provider Access API
     */
    @RequestMapping(value = "/api/v1/provider-access/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToProviderAccess(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, priorAuthUrl, "/api/v1/provider-access");
    }

    /**
     * Forward request to backend service
     */
    private ResponseEntity<?> forwardRequest(
        HttpServletRequest request,
        String body,
        String serviceUrl,
        String pathPrefix
    ) {
        try {
            // Build target URL
            String path = request.getRequestURI().substring(pathPrefix.length());
            String queryString = request.getQueryString();
            
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serviceUrl + path);
            if (queryString != null) {
                builder.query(queryString);
            }
            URI targetUri = builder.build(true).toUri();

            // Copy headers
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // Forward important headers including user identity for Pattern 3 services
                if (headerName.equalsIgnoreCase("Authorization") ||
                    headerName.equalsIgnoreCase("X-Tenant-ID") ||
                    headerName.equalsIgnoreCase("X-User-ID") ||
                    headerName.equalsIgnoreCase("Content-Type") ||
                    headerName.equalsIgnoreCase("Accept")) {
                    headers.put(headerName, Collections.list(request.getHeaders(headerName)));
                }
            }

            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            // Forward request
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            log.debug("Forwarding {} {} to {}", method, request.getRequestURI(), targetUri);

            ResponseEntity<String> response = restTemplate.exchange(
                targetUri,
                method,
                requestEntity,
                String.class
            );

            // Filter out hop-by-hop headers to prevent duplicates when nginx proxies
            HttpHeaders filteredHeaders = new HttpHeaders();
            response.getHeaders().forEach((name, values) -> {
                if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                    filteredHeaders.put(name, values);
                }
            });

            return ResponseEntity
                .status(response.getStatusCode())
                .headers(filteredHeaders)
                .body(response.getBody());

        } catch (Exception e) {
            log.error("Error forwarding request to {}: {}", serviceUrl, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Gateway error: " + e.getMessage());
        }
    }
}
