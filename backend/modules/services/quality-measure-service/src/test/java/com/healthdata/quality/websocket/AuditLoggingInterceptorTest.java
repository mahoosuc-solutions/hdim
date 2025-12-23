package com.healthdata.quality.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Logging Interceptor Tests")
class AuditLoggingInterceptorTest {

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private WebSocketHandler webSocketHandler;

    @Test
    @DisplayName("Should publish security event when violation present")
    void shouldPublishSecurityEventWhenViolationPresent() throws Exception {
        AuditLoggingInterceptor interceptor = new AuditLoggingInterceptor(
            new ObjectMapper(),
            auditEventPublisher
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticated", true);
        attributes.put("username", "user");
        attributes.put("tenantId", "tenant-1");
        attributes.put("securityViolation", "TENANT_MISMATCH");
        attributes.put("attemptedTenantId", "tenant-2");

        interceptor.beforeHandshake(serverRequest, serverResponse, webSocketHandler, attributes);

        verify(auditEventPublisher).publishSecurityEvent(any());
        verify(auditEventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Should publish success event after handshake without exception")
    void shouldPublishSuccessAfterHandshake() {
        AuditLoggingInterceptor interceptor = new AuditLoggingInterceptor(
            new ObjectMapper(),
            auditEventPublisher
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        interceptor.afterHandshake(serverRequest, serverResponse, webSocketHandler, null);

        verify(auditEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should publish audit event when no security violation present")
    void shouldPublishAuditEventWhenNoViolation() throws Exception {
        AuditLoggingInterceptor interceptor = new AuditLoggingInterceptor(
            new ObjectMapper(),
            auditEventPublisher
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticated", true);
        attributes.put("username", "user");
        attributes.put("tenantId", "tenant-1");

        interceptor.beforeHandshake(serverRequest, serverResponse, webSocketHandler, attributes);

        verify(auditEventPublisher).publish(any());
        verify(auditEventPublisher, never()).publishSecurityEvent(any());
    }

    @Test
    @DisplayName("Should log disconnect event without throwing")
    void shouldLogDisconnectEvent() {
        AuditLoggingInterceptor interceptor = new AuditLoggingInterceptor(
            new ObjectMapper(),
            auditEventPublisher
        );

        interceptor.logDisconnectEvent("session-1", "user", "tenant-1", 1000L);
    }

    @Test
    @DisplayName("Should publish failure event when handshake exception occurs")
    void shouldPublishFailureEventWhenHandshakeExceptionOccurs() {
        AuditLoggingInterceptor interceptor = new AuditLoggingInterceptor(
            new ObjectMapper(),
            auditEventPublisher
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        interceptor.afterHandshake(serverRequest, serverResponse, webSocketHandler, new RuntimeException("handshake failed"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditEventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).containsEntry("eventType", "WEBSOCKET_CONNECT_FAILURE");
    }

    @Test
    @DisplayName("Should include tenant access validation in audit event")
    void shouldIncludeTenantAccessValidationInAuditEvent() throws Exception {
        AuditLoggingInterceptor interceptor = new AuditLoggingInterceptor(
            new ObjectMapper(),
            auditEventPublisher
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantAccessValidated", true);

        interceptor.beforeHandshake(serverRequest, serverResponse, webSocketHandler, attributes);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(auditEventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).containsEntry("tenantAccessValidated", true);
    }

    @Test
    @DisplayName("Should swallow audit logging exceptions")
    void shouldSwallowAuditLoggingExceptions() throws Exception {
        ObjectMapper objectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("json fail"));
        AuditLoggingInterceptor interceptor = new AuditLoggingInterceptor(
            objectMapper,
            auditEventPublisher
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        interceptor.afterHandshake(serverRequest, serverResponse, webSocketHandler, null);

        verifyNoInteractions(auditEventPublisher);
    }
}
