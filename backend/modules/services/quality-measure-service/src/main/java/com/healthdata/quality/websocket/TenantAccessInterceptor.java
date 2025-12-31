package com.healthdata.quality.websocket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Tenant Access Control Interceptor for WebSocket Handshake
 *
 * HIPAA Security Rule Compliance:
 * - §164.312(a)(1) - Access Control (Unique User Identification)
 * - §164.308(a)(4)(i) - Isolate Healthcare Clearinghouse Functions
 * - §164.308(a)(3)(i) - Workforce Security (Authorization/Supervision)
 *
 * Security Features:
 * - Validates user's tenant access during WebSocket handshake
 * - Ensures users can only connect to WebSocket sessions for their authorized tenant
 * - Compares JWT tenantId claim with WebSocket query parameter tenantId
 * - Prevents cross-tenant data leakage
 * - Logs all authorization failures for audit trail
 * - Blocks unauthorized tenant access with HTTP 403 Forbidden
 *
 * Connection Flow:
 * 1. JwtWebSocketHandshakeInterceptor extracts tenantId from JWT → stores in attributes
 * 2. TenantAccessInterceptor validates tenantId from URL matches JWT tenantId
 * 3. If mismatch or missing → reject connection with 403
 * 4. If valid → allow connection and log access
 *
 * URL Format:
 * ws://...?tenantId=TENANT001  (+ Authorization: Bearer <token> with tenantId claim)
 */
@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true", matchIfMissing = true)
@Component
@Slf4j
public class TenantAccessInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        try {
            // Extract tenantId from JWT claims (stored by JwtWebSocketHandshakeInterceptor)
            String jwtTenantId = (String) attributes.get("tenantId");

            // Extract requested tenantId from query parameter
            String requestedTenantId = extractTenantIdFromQuery(request);

            // Validate tenant access
            if (jwtTenantId == null || jwtTenantId.isEmpty()) {
                log.warn("WebSocket tenant access denied: No tenantId in JWT claims from {}",
                        request.getRemoteAddress());
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            if (requestedTenantId == null || requestedTenantId.isEmpty()) {
                log.warn("WebSocket tenant access denied: No tenantId in query parameters from {} (user: {})",
                        request.getRemoteAddress(), attributes.get("username"));
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }

            // Ensure user can only access their authorized tenant
            if (!jwtTenantId.equals(requestedTenantId)) {
                String username = (String) attributes.get("username");
                log.warn("SECURITY ALERT: WebSocket tenant access violation - User {} (tenant: {}) attempted to access tenant: {} from {}",
                        username, jwtTenantId, requestedTenantId, request.getRemoteAddress());

                // Store security violation for audit logging
                attributes.put("securityViolation", "TENANT_ACCESS_VIOLATION");
                attributes.put("attemptedTenantId", requestedTenantId);

                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            // Access granted - log successful authorization
            log.info("WebSocket tenant access authorized: user={} tenant={} from={}",
                    attributes.get("username"), jwtTenantId, request.getRemoteAddress());

            // Store validated tenant access in attributes
            attributes.put("tenantAccessValidated", true);
            attributes.put("tenantAccessTime", System.currentTimeMillis());

            return true;

        } catch (Exception e) {
            log.error("WebSocket tenant access check failed with exception: {} from {}",
                    e.getMessage(), request.getRemoteAddress());
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        if (exception != null) {
            log.error("WebSocket tenant access handshake completed with exception: {}", exception.getMessage());
        }
    }

    /**
     * Extract tenantId from WebSocket URL query parameters
     *
     * Example: ws://localhost:8087/quality-measure/ws/health-scores?tenantId=TENANT001
     */
    private String extractTenantIdFromQuery(ServerHttpRequest request) {
        try {
            String query = request.getURI().getQuery();
            if (query != null && query.contains("tenantId=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("tenantId=")) {
                        return param.substring("tenantId=".length());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract tenantId from query params: {}", e.getMessage());
        }
        return null;
    }
}
