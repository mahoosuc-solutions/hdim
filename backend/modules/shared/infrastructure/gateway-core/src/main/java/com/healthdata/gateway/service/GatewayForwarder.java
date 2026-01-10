package com.healthdata.gateway.service;

import com.healthdata.gateway.config.GatewayAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GatewayForwarder {

    private final RestTemplate restTemplate;
    private final GatewayAuthProperties authProperties;

    /**
     * HTTP hop-by-hop headers that should not be forwarded by proxies.
     * RFC 2616 Section 13.5.1 defines these as connection-specific headers.
     */
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
        "transfer-encoding", "connection", "keep-alive",
        "proxy-authenticate", "proxy-authorization",
        "te", "trailers", "upgrade"
    );

    public ResponseEntity<?> forwardRequest(
        HttpServletRequest request,
        String body,
        String serviceUrl,
        String pathPrefix
    ) {
        try {
            String requestId = request.getHeader("X-Request-ID");
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            log.info(
                "Gateway request {} {} {} -> {}",
                requestId,
                method,
                request.getRequestURI(),
                serviceUrl
            );

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
                String normalizedHeader = headerName.toLowerCase(Locale.ROOT);
                if (normalizedHeader.startsWith("x-auth-") ||
                    headerName.equalsIgnoreCase("Authorization") ||
                    headerName.equalsIgnoreCase("X-Tenant-ID") ||
                    headerName.equalsIgnoreCase("X-User-ID") ||
                    headerName.equalsIgnoreCase("Content-Type") ||
                    headerName.equalsIgnoreCase("Accept")) {
                    headers.put(headerName, Collections.list(request.getHeaders(headerName)));
                }
            }

            // In demo mode, X-Auth-Tenant-Ids is set by GatewayAuthenticationFilter
            // Override X-Tenant-ID to use the first allowed tenant from authenticated context
            String authTenantIds = request.getHeader("X-Auth-Tenant-Ids");
            String authUser = request.getHeader("X-Auth-User");
            String authRoles = request.getHeader("X-Auth-Roles");
            String currentTenant = headers.getFirst("X-Tenant-ID");
            if ((currentTenant == null || currentTenant.isBlank()) &&
                authTenantIds != null && !authTenantIds.trim().isEmpty()) {
                String primaryTenant = authTenantIds.split(",")[0].trim();
                headers.set("X-Tenant-ID", primaryTenant);
                log.debug("Request {} set X-Tenant-ID from gateway auth context: {}", requestId, primaryTenant);
            }
            if ((headers.getFirst("X-Tenant-ID") == null || headers.getFirst("X-Tenant-ID").isBlank()) &&
                Boolean.FALSE.equals(authProperties.getEnforced())) {
                var demoTenants = authProperties.getDemoUser().getTenantIds();
                if (demoTenants != null && !demoTenants.isEmpty()) {
                    headers.set("X-Tenant-ID", demoTenants.get(0));
                    log.debug("Request {} set X-Tenant-ID from demo user config: {}", requestId, demoTenants.get(0));
                }
            }
            if (Boolean.FALSE.equals(authProperties.getEnforced())) {
                log.info(
                    "Gateway demo mode forwarding tenant for request {}: {}",
                    requestId,
                    headers.getFirst("X-Tenant-ID")
                );
            }

            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            // Forward request
            log.info(
                "Gateway forward request {} -> {} (tenant={}, user={}, roles={})",
                requestId,
                targetUri,
                headers.getFirst("X-Tenant-ID"),
                authUser,
                authRoles
            );
            log.debug(
                "Gateway request {} headers forwarded: {}",
                requestId,
                headers.keySet()
            );

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

            log.info(
                "Gateway response {} status {} for {}",
                requestId,
                response.getStatusCode(),
                targetUri
            );
            return ResponseEntity
                .status(response.getStatusCode())
                .headers(filteredHeaders)
                .body(response.getBody());

        } catch (Exception e) {
            log.error("Gateway error forwarding request to {}: {}", serviceUrl, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Gateway error: " + e.getMessage());
        }
    }
}
