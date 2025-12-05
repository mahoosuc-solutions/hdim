package com.healthdata.quality.config;

import com.healthdata.quality.websocket.AuditLoggingInterceptor;
import com.healthdata.quality.websocket.HealthScoreWebSocketHandler;
import com.healthdata.quality.websocket.JwtWebSocketHandshakeInterceptor;
import com.healthdata.quality.websocket.RateLimitingInterceptor;
import com.healthdata.quality.websocket.TenantAccessInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * HIPAA-Compliant WebSocket Configuration for Real-Time Health Score Updates
 *
 * HIPAA Security Rule Compliance:
 * - §164.312(d) - Person or Entity Authentication (JWT)
 * - §164.312(a)(1) - Access Control (Tenant isolation)
 * - §164.312(b) - Audit Controls (Connection logging)
 * - §164.312(e)(1) - Transmission Security (WSS/TLS)
 *
 * Security Architecture:
 * 1. JWT Authentication - Validates user identity via JWT token
 * 2. Tenant Authorization - Ensures users only access their tenant data
 * 3. Audit Logging - Records all connection attempts for compliance
 *
 * Interceptor Chain (order matters):
 * 1. RateLimitingInterceptor - Prevents brute-force and DoS attacks (FIRST - fail fast)
 * 2. JwtWebSocketHandshakeInterceptor - Authenticates user, extracts JWT claims
 * 3. TenantAccessInterceptor - Validates tenant access authorization
 * 4. AuditLoggingInterceptor - Logs connection attempt and result
 *
 * Endpoints:
 * - /ws/health-scores - Real-time health score updates and alerts
 *
 * Connection Format:
 * wss://host/quality-measure/ws/health-scores?tenantId=TENANT001
 * Header: Authorization: Bearer <jwt-token>
 *
 * Security Requirements:
 * - MUST use WSS (TLS encrypted) in production
 * - MUST provide valid JWT token
 * - MUST match tenantId in JWT with query parameter
 * - All connections audited for HIPAA compliance
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final HealthScoreWebSocketHandler healthScoreWebSocketHandler;
    private final RateLimitingInterceptor rateLimitingInterceptor;
    private final JwtWebSocketHandshakeInterceptor jwtInterceptor;
    private final TenantAccessInterceptor tenantAccessInterceptor;
    private final AuditLoggingInterceptor auditLoggingInterceptor;

    @Value("${websocket.enabled:true}")
    private boolean websocketEnabled;

    @Value("#{'${websocket.allowed-origins:http://localhost:4200,http://localhost:4201,http://localhost:4202,http://localhost:3000,http://localhost:8082}'.split(',')}")
    private List<String> allowedOrigins;

    public WebSocketConfig(
            HealthScoreWebSocketHandler healthScoreWebSocketHandler,
            RateLimitingInterceptor rateLimitingInterceptor,
            JwtWebSocketHandshakeInterceptor jwtInterceptor,
            TenantAccessInterceptor tenantAccessInterceptor,
            AuditLoggingInterceptor auditLoggingInterceptor) {
        this.healthScoreWebSocketHandler = healthScoreWebSocketHandler;
        this.rateLimitingInterceptor = rateLimitingInterceptor;
        this.jwtInterceptor = jwtInterceptor;
        this.tenantAccessInterceptor = tenantAccessInterceptor;
        this.auditLoggingInterceptor = auditLoggingInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (websocketEnabled) {
            registry.addHandler(healthScoreWebSocketHandler, "/ws/health-scores")
                    .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                    .addInterceptors(
                            // Security interceptor chain (order is critical - fail fast approach):
                            // 1. Rate Limiting - prevents brute-force/DoS (FIRST - reject excessive attempts early)
                            rateLimitingInterceptor,
                            // 2. JWT Authentication - validates user identity
                            jwtInterceptor,
                            // 3. Tenant Authorization - validates tenant access
                            tenantAccessInterceptor,
                            // 4. Audit Logging - records connection attempt (LAST - logs all attempts)
                            auditLoggingInterceptor
                    );
        }
    }
}
