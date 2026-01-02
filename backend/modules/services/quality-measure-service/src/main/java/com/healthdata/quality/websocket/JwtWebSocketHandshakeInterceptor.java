package com.healthdata.quality.websocket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.healthdata.authentication.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * JWT Authentication Interceptor for WebSocket Handshake
 *
 * HIPAA Security Rule Compliance:
 * - §164.312(d) - Person or Entity Authentication
 * - §164.312(a)(1) - Access Control
 *
 * Security Features:
 * - Validates JWT token before WebSocket connection
 * - Extracts user identity and tenant context
 * - Stores authentication in WebSocket session attributes
 * - Rejects unauthenticated connections
 *
 * Token Extraction Order:
 * 1. Authorization header (Bearer token)
 * 2. Query parameter 'token'
 * 3. Cookie 'access_token'
 *
 * Connection URL formats:
 * - With header: ws://...?tenantId=TENANT001 (+ Authorization: Bearer <token>)
 * - With param:  ws://...?tenantId=TENANT001&token=<jwt-token>
 */
@ConditionalOnProperty(name = "websocket.enabled", havingValue = "true", matchIfMissing = true)
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenService jwtTokenService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        try {
            // Extract JWT token from request
            String token = extractToken(request);

            if (token == null || token.isEmpty()) {
                log.warn("WebSocket handshake rejected: No JWT token provided from {}",
                        request.getRemoteAddress());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Validate JWT token
            if (!jwtTokenService.validateToken(token)) {
                log.warn("WebSocket handshake rejected: Invalid JWT token from {}",
                        request.getRemoteAddress());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Extract claims from token
            String username = jwtTokenService.extractUsername(token);
            java.util.Set<String> tenantIds = jwtTokenService.extractTenantIds(token);
            String userId = jwtTokenService.extractUserId(token).toString();
            java.util.Set<String> roleSet = jwtTokenService.extractRoles(token);

            // Get first tenant ID (for single-tenant WebSocket connections)
            String tenantId = tenantIds.isEmpty() ? null : tenantIds.iterator().next();

            // Convert roles set to comma-separated string
            String roles = String.join(",", roleSet);

            // Store authentication context in WebSocket session attributes
            attributes.put("authenticated", true);
            attributes.put("username", username);
            attributes.put("userId", userId);
            attributes.put("tenantId", tenantId);
            attributes.put("roles", roles);
            attributes.put("authTime", System.currentTimeMillis());

            log.info("WebSocket handshake authorized for user: {} tenant: {} from: {}",
                    username, tenantId, request.getRemoteAddress());

            return true;

        } catch (Exception e) {
            log.error("WebSocket handshake failed with exception: {} from {}",
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
            log.error("WebSocket handshake completed with exception: {}", exception.getMessage());
        }
    }

    /**
     * Extract JWT token from request
     *
     * Checks in order:
     * 1. Authorization header (Bearer token)
     * 2. Query parameter 'token'
     * 3. Cookie 'access_token'
     */
    private String extractToken(ServerHttpRequest request) {
        // 1. Try Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Try query parameter
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }

        // 3. Try cookie (if this is a servlet request)
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            var cookies = servletRequest.getServletRequest().getCookies();
            if (cookies != null) {
                for (var cookie : cookies) {
                    if ("access_token".equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
        }

        return null;
    }
}
