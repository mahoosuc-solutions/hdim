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

    @Value("${backend.services.agent-runtime.url}")
    private String agentRuntimeUrl;

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
     * Route to FHIR Service
     */
    @RequestMapping(value = "/api/fhir/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToFhir(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwardRequest(request, body, fhirUrl, "/api/fhir");
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

            return ResponseEntity
                .status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());

        } catch (Exception e) {
            log.error("Error forwarding request to {}: {}", serviceUrl, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Gateway error: " + e.getMessage());
        }
    }
}
