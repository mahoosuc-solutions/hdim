package com.healthdata.quality.config;

import com.healthdata.quality.websocket.AuditLoggingInterceptor;
import com.healthdata.quality.websocket.HealthScoreWebSocketHandler;
import com.healthdata.quality.websocket.JwtWebSocketHandshakeInterceptor;
import com.healthdata.quality.websocket.RateLimitingInterceptor;
import com.healthdata.quality.websocket.TenantAccessInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    @Test
    void shouldRegisterHandlersWhenEnabled() {
        HealthScoreWebSocketHandler handler = mock(HealthScoreWebSocketHandler.class);
        RateLimitingInterceptor rateLimitingInterceptor = mock(RateLimitingInterceptor.class);
        JwtWebSocketHandshakeInterceptor jwtInterceptor = mock(JwtWebSocketHandshakeInterceptor.class);
        TenantAccessInterceptor tenantAccessInterceptor = mock(TenantAccessInterceptor.class);
        AuditLoggingInterceptor auditLoggingInterceptor = mock(AuditLoggingInterceptor.class);

        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);

        when(registry.addHandler(handler, "/ws/health-scores")).thenReturn(registration);
        when(registration.setAllowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.addInterceptors(any())).thenReturn(registration);

        WebSocketConfig config = new WebSocketConfig(
            handler,
            rateLimitingInterceptor,
            jwtInterceptor,
            tenantAccessInterceptor,
            auditLoggingInterceptor
        );
        ReflectionTestUtils.setField(config, "websocketEnabled", true);
        ReflectionTestUtils.setField(config, "allowedOrigins", List.of("http://localhost:4200"));

        config.registerWebSocketHandlers(registry);

        verify(registry).addHandler(handler, "/ws/health-scores");
        verify(registration).setAllowedOrigins(any(String[].class));
        verify(registration).addInterceptors(
            rateLimitingInterceptor,
            jwtInterceptor,
            tenantAccessInterceptor,
            auditLoggingInterceptor
        );
    }

    @Test
    void shouldSkipRegistrationWhenDisabled() {
        WebSocketConfig config = new WebSocketConfig(
            mock(HealthScoreWebSocketHandler.class),
            mock(RateLimitingInterceptor.class),
            mock(JwtWebSocketHandshakeInterceptor.class),
            mock(TenantAccessInterceptor.class),
            mock(AuditLoggingInterceptor.class)
        );

        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        ReflectionTestUtils.setField(config, "websocketEnabled", false);

        config.registerWebSocketHandlers(registry);

        verifyNoInteractions(registry);
    }
}
