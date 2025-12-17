package com.healthdata.quality.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Audit Logging Interceptor for WebSocket Handshake
 *
 * HIPAA Security Rule Compliance:
 * - §164.312(b) - Audit Controls
 * - §164.308(a)(1)(ii)(D) - Information System Activity Review
 * - §164.308(a)(5)(ii)(C) - Log-in Monitoring
 *
 * Audit Trail Requirements:
 * - Records all WebSocket connection attempts (successful and failed)
 * - Captures user identity, tenant, timestamp, IP address
 * - Logs security violations (authentication failures, tenant access violations)
 * - Provides structured audit events for SIEM integration
 * - Maintains immutable audit records
 *
 * Logged Events:
 * - WEBSOCKET_CONNECT_ATTEMPT - Initial handshake
 * - WEBSOCKET_CONNECT_SUCCESS - Successful authentication and authorization
 * - WEBSOCKET_CONNECT_FAILURE - Authentication or authorization failure
 * - WEBSOCKET_DISCONNECT - Session termination
 *
 * Audit Log Format:
 * JSON-structured logs compatible with ELK stack, Splunk, CloudWatch
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingInterceptor implements HandshakeInterceptor {

    private final ObjectMapper objectMapper;
    private final AuditEventPublisher auditEventPublisher;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        // Record connection attempt
        logAuditEventWithResponse("WEBSOCKET_CONNECT_ATTEMPT", request, response, attributes, null);

        // Store connection attempt time for duration tracking
        attributes.put("connectionAttemptTime", Instant.now().toEpochMilli());

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {

        // This is called after all interceptors have run
        // We don't have access to attributes here, so we log based on exception presence

        if (exception != null) {
            // Connection failed
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("exceptionType", exception.getClass().getSimpleName());
            auditData.put("exceptionMessage", exception.getMessage());

            logAuditEvent("WEBSOCKET_CONNECT_FAILURE", request, auditData, exception);
        } else {
            // Connection successful (all interceptors passed)
            logAuditEvent("WEBSOCKET_CONNECT_SUCCESS", request, new HashMap<>(), null);
        }
    }

    /**
     * Log structured audit event
     *
     * @param eventType Type of audit event
     * @param request HTTP request
     * @param attributes Session attributes (may be empty in afterHandshake)
     * @param exception Exception if error occurred
     */
    private void logAuditEvent(
            String eventType,
            ServerHttpRequest request,
            Map<String, Object> attributes,
            Exception exception) {
        logAuditEventWithResponse(eventType, request, null, attributes, exception);
    }

    /**
     * Log structured audit event with response
     *
     * @param eventType Type of audit event
     * @param request HTTP request
     * @param response HTTP response (may be null)
     * @param attributes Session attributes (may be empty in afterHandshake)
     * @param exception Exception if error occurred
     */
    private void logAuditEventWithResponse(
            String eventType,
            ServerHttpRequest request,
            ServerHttpResponse response,
            Map<String, Object> attributes,
            Exception exception) {

        try {
            Map<String, Object> auditEvent = new HashMap<>();

            // Event metadata
            auditEvent.put("eventType", eventType);
            auditEvent.put("timestamp", Instant.now().toString());
            auditEvent.put("eventTime", System.currentTimeMillis());

            // Connection details
            auditEvent.put("protocol", "WEBSOCKET");
            auditEvent.put("uri", request.getURI().toString());
            auditEvent.put("remoteAddress", request.getRemoteAddress() != null
                    ? request.getRemoteAddress().toString() : "unknown");
            auditEvent.put("userAgent", request.getHeaders().getFirst("User-Agent"));

            // Authentication details (if available)
            if (attributes != null && !attributes.isEmpty()) {
                Boolean authenticated = (Boolean) attributes.get("authenticated");
                if (authenticated != null && authenticated) {
                    auditEvent.put("authenticated", true);
                    auditEvent.put("username", attributes.get("username"));
                    auditEvent.put("userId", attributes.get("userId"));
                    auditEvent.put("tenantId", attributes.get("tenantId"));
                    auditEvent.put("roles", attributes.get("roles"));
                }

                // Security violation details (if any)
                String securityViolation = (String) attributes.get("securityViolation");
                if (securityViolation != null) {
                    auditEvent.put("securityViolation", securityViolation);
                    auditEvent.put("attemptedTenantId", attributes.get("attemptedTenantId"));
                    auditEvent.put("severity", "HIGH");
                }

                // Tenant access validation
                Boolean tenantAccessValidated = (Boolean) attributes.get("tenantAccessValidated");
                if (tenantAccessValidated != null) {
                    auditEvent.put("tenantAccessValidated", tenantAccessValidated);
                }
            }

            // Exception details (if any)
            if (exception != null) {
                auditEvent.put("exceptionType", exception.getClass().getName());
                auditEvent.put("exceptionMessage", exception.getMessage());
                auditEvent.put("severity", "ERROR");
            }

            // Note: HTTP response status is not readily available via ServerHttpResponse interface
            // Status codes are inferred from success/failure of interceptor chain

            // Log as structured JSON for SIEM ingestion
            String auditJson = objectMapper.writeValueAsString(auditEvent);

            // Use appropriate log level based on event type
            switch (eventType) {
                case "WEBSOCKET_CONNECT_FAILURE":
                    log.warn("AUDIT: {}", auditJson);
                    break;
                case "WEBSOCKET_CONNECT_SUCCESS":
                    log.info("AUDIT: {}", auditJson);
                    break;
                default:
                    log.debug("AUDIT: {}", auditJson);
            }

            // Publish to Kafka for dedicated audit service/database consumption
            if (auditEvent.containsKey("securityViolation")) {
                auditEventPublisher.publishSecurityEvent(auditEvent);
            } else {
                auditEventPublisher.publish(auditEvent);
            }

        } catch (Exception e) {
            // Never fail the connection due to audit logging errors
            // But log the audit system failure
            log.error("CRITICAL: Audit logging failed for event {}: {}", eventType, e.getMessage(), e);
        }
    }

    /**
     * Log WebSocket disconnection event
     * (Called from WebSocketHandler when connection closes)
     *
     * @param sessionId WebSocket session ID
     * @param username Username from session attributes
     * @param tenantId Tenant ID from session attributes
     * @param connectionDuration Duration of connection in milliseconds
     */
    public void logDisconnectEvent(String sessionId, String username, String tenantId, long connectionDuration) {
        try {
            Map<String, Object> auditEvent = new HashMap<>();

            auditEvent.put("eventType", "WEBSOCKET_DISCONNECT");
            auditEvent.put("timestamp", Instant.now().toString());
            auditEvent.put("eventTime", System.currentTimeMillis());
            auditEvent.put("protocol", "WEBSOCKET");
            auditEvent.put("sessionId", sessionId);
            auditEvent.put("username", username);
            auditEvent.put("tenantId", tenantId);
            auditEvent.put("connectionDurationMs", connectionDuration);

            String auditJson = objectMapper.writeValueAsString(auditEvent);
            log.info("AUDIT: {}", auditJson);

        } catch (Exception e) {
            log.error("CRITICAL: Audit logging failed for disconnect event: {}", e.getMessage(), e);
        }
    }
}
