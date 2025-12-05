package com.healthdata.quality.config;

import com.healthdata.quality.websocket.AuditLoggingInterceptor;
import com.healthdata.quality.websocket.HealthScoreWebSocketHandler;
import com.healthdata.quality.websocket.JwtWebSocketHandshakeInterceptor;
import com.healthdata.quality.websocket.RateLimitingInterceptor;
import com.healthdata.quality.websocket.TenantAccessInterceptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test WebSocket Configuration for Quality Measure Service Tests
 *
 * Provides mock implementations of WebSocket-related beans to avoid complex
 * WebSocket setup during unit and integration tests.
 *
 * Key Features:
 * - Mock WebSocket handlers and interceptors
 * - Minimal overhead for tests that don't need WebSocket functionality
 * - Prevents ApplicationContext failures due to missing WebSocket dependencies
 *
 * Note: WebSockets are disabled by default in test profile via:
 * websocket.enabled=false in application-test.yml
 */
@TestConfiguration
public class TestWebSocketConfiguration {

    /**
     * Mock HealthScoreWebSocketHandler for tests.
     */
    @Bean
    @Primary
    public HealthScoreWebSocketHandler healthScoreWebSocketHandler() {
        return mock(HealthScoreWebSocketHandler.class);
    }

    /**
     * Mock RateLimitingInterceptor for tests.
     * Always allows WebSocket handshake to proceed.
     */
    @Bean
    @Primary
    public RateLimitingInterceptor rateLimitingInterceptor() {
        RateLimitingInterceptor mock = mock(RateLimitingInterceptor.class);
        try {
            when(mock.beforeHandshake(any(), any(), any(), any())).thenReturn(true);
        } catch (Exception e) {
            // Should never happen in mock
        }
        return mock;
    }

    /**
     * Mock JwtWebSocketHandshakeInterceptor for tests.
     * Always allows WebSocket handshake to proceed.
     */
    @Bean
    @Primary
    public JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor() {
        JwtWebSocketHandshakeInterceptor mock = mock(JwtWebSocketHandshakeInterceptor.class);
        try {
            when(mock.beforeHandshake(any(), any(), any(), any())).thenReturn(true);
        } catch (Exception e) {
            // Should never happen in mock
        }
        return mock;
    }

    /**
     * Mock TenantAccessInterceptor for tests.
     * Always allows WebSocket handshake to proceed.
     */
    @Bean
    @Primary
    public TenantAccessInterceptor tenantAccessInterceptor() {
        TenantAccessInterceptor mock = mock(TenantAccessInterceptor.class);
        try {
            when(mock.beforeHandshake(any(), any(), any(), any())).thenReturn(true);
        } catch (Exception e) {
            // Should never happen in mock
        }
        return mock;
    }

    /**
     * Mock AuditLoggingInterceptor for tests.
     * Always allows WebSocket handshake to proceed.
     */
    @Bean
    @Primary
    public AuditLoggingInterceptor auditLoggingInterceptor() {
        AuditLoggingInterceptor mock = mock(AuditLoggingInterceptor.class);
        try {
            when(mock.beforeHandshake(any(), any(), any(), any())).thenReturn(true);
        } catch (Exception e) {
            // Should never happen in mock
        }
        return mock;
    }
}
