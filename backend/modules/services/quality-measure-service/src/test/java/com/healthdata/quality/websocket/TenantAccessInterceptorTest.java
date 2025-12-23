package com.healthdata.quality.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.mockito.Mockito;

@DisplayName("Tenant Access Interceptor Tests")
class TenantAccessInterceptorTest {

    @Test
    @DisplayName("Should reject when JWT tenant missing")
    void shouldRejectWhenJwtTenantMissing() throws Exception {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setQueryString("tenantId=tenant-1");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should reject when JWT tenant is empty")
    void shouldRejectWhenJwtTenantEmpty() throws Exception {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setQueryString("tenantId=tenant-1");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "");

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Should reject when requested tenant missing")
    void shouldRejectWhenRequestedTenantMissing() throws Exception {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant-1");
        attributes.put("username", "user1");

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should allow when tenant matches with additional query params")
    void shouldAllowWhenTenantMatchesWithAdditionalQueryParams() throws Exception {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setQueryString("foo=1&tenantId=tenant-1&bar=2");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant-1");
        attributes.put("username", "user1");

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry("tenantAccessValidated", true);
    }

    @Test
    @DisplayName("Should reject when tenant mismatch and record violation")
    void shouldRejectWhenTenantMismatch() throws Exception {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setQueryString("tenantId=tenant-2");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant-1");
        attributes.put("username", "user1");

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(attributes).containsEntry("securityViolation", "TENANT_ACCESS_VIOLATION");
        assertThat(attributes).containsEntry("attemptedTenantId", "tenant-2");
    }

    @Test
    @DisplayName("Should reject with server error when attributes are null")
    void shouldRejectWithServerErrorWhenAttributesNull() throws Exception {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setQueryString("tenantId=tenant-1");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), null);

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("Should allow when tenant matches")
    void shouldAllowWhenTenantMatches() throws Exception {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setQueryString("tenantId=tenant-1");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant-1");
        attributes.put("username", "user1");

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry("tenantAccessValidated", true);
    }

    @Test
    @DisplayName("Should handle query extraction exceptions")
    void shouldHandleQueryExtractionExceptions() throws Exception {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();
        ServerHttpRequest serverRequest = Mockito.mock(ServerHttpRequest.class);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant-1");
        attributes.put("username", "user1");

        Mockito.when(serverRequest.getURI()).thenThrow(new RuntimeException("bad uri"));
        Mockito.when(serverRequest.getRemoteAddress()).thenReturn(null);

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Should log when handshake completes with exception")
    void shouldLogWhenHandshakeCompletesWithException() {
        TenantAccessInterceptor interceptor = new TenantAccessInterceptor();

        interceptor.afterHandshake(
            Mockito.mock(ServerHttpRequest.class),
            Mockito.mock(ServletServerHttpResponse.class),
            Mockito.mock(WebSocketHandler.class),
            new RuntimeException("handshake error")
        );
    }
}
