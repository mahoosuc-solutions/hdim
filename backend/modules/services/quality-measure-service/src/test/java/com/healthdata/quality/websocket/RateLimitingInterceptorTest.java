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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.mockito.Mockito;

@DisplayName("Rate Limiting Interceptor Tests")
class RateLimitingInterceptorTest {

    @Test
    @DisplayName("Should allow handshake when rate limiting disabled")
    void shouldAllowWhenRateLimitingDisabled() throws Exception {
        RateLimitingInterceptor interceptor = new RateLimitingInterceptor();
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), new HashMap<>());

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Should reject handshake when rate limit exceeded")
    void shouldRejectWhenRateLimitExceeded() throws Exception {
        RateLimitingInterceptor interceptor = new RateLimitingInterceptor();
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(interceptor, "connectionsPerMinute", 1);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setRemoteAddr("10.0.0.1");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse1 = new MockHttpServletResponse();
        MockHttpServletResponse rawResponse2 = new MockHttpServletResponse();
        ServletServerHttpResponse response1 = new ServletServerHttpResponse(rawResponse1);
        ServletServerHttpResponse response2 = new ServletServerHttpResponse(rawResponse2);

        Map<String, Object> attrs1 = new HashMap<>();
        Map<String, Object> attrs2 = new HashMap<>();

        boolean allowed1 = interceptor.beforeHandshake(serverRequest, response1, Mockito.mock(WebSocketHandler.class), attrs1);
        boolean allowed2 = interceptor.beforeHandshake(serverRequest, response2, Mockito.mock(WebSocketHandler.class), attrs2);

        assertThat(allowed1).isTrue();
        assertThat(allowed2).isFalse();
        assertThat(rawResponse2.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(attrs2).containsEntry("rateLimitViolation", true);
        assertThat(attrs2).containsEntry("attemptsCount", 2);
        assertThat(attrs2.get("clientIp")).isNotNull();
    }

    @Test
    @DisplayName("Should use X-Forwarded-For header for rate limiting")
    void shouldUseForwardedForHeader() throws Exception {
        RateLimitingInterceptor interceptor = new RateLimitingInterceptor();
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(interceptor, "connectionsPerMinute", 1);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        boolean allowed = interceptor.beforeHandshake(
            serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), new HashMap<>());

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Should allow when client IP cannot be determined")
    void shouldAllowWhenClientIpMissing() throws Exception {
        RateLimitingInterceptor interceptor = new RateLimitingInterceptor();
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", true);

        ServerHttpRequest serverRequest = Mockito.mock(ServerHttpRequest.class);
        Mockito.when(serverRequest.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        Mockito.when(serverRequest.getRemoteAddress()).thenReturn(null);

        boolean allowed = interceptor.beforeHandshake(
            serverRequest,
            new ServletServerHttpResponse(new MockHttpServletResponse()),
            Mockito.mock(WebSocketHandler.class),
            new HashMap<>()
        );

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("Should cleanup expired connection attempts")
    void shouldCleanupExpiredEntries() throws Exception {
        RateLimitingInterceptor interceptor = new RateLimitingInterceptor();
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setRemoteAddr("10.0.0.2");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        interceptor.beforeHandshake(serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), new HashMap<>());

        @SuppressWarnings("unchecked")
        Map<String, Object> attemptsMap = (Map<String, Object>) ReflectionTestUtils.getField(
            interceptor, "ipConnectionAttempts");
        Object attempts = attemptsMap.values().stream().findFirst().orElse(null);
        assertThat(attempts).isNotNull();
        ReflectionTestUtils.setField(attempts, "windowStart",
            System.currentTimeMillis() - 61000);

        interceptor.cleanupExpiredEntries();

        assertThat(attemptsMap).isEmpty();
    }

    @Test
    @DisplayName("Should reset attempts after window expires")
    void shouldResetAttemptsAfterWindowExpires() throws Exception {
        RateLimitingInterceptor interceptor = new RateLimitingInterceptor();
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(interceptor, "connectionsPerMinute", 1);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setRemoteAddr("10.0.0.3");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        boolean firstAllowed = interceptor.beforeHandshake(
            serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), new HashMap<>());
        assertThat(firstAllowed).isTrue();

        @SuppressWarnings("unchecked")
        Map<String, Object> attemptsMap = (Map<String, Object>) ReflectionTestUtils.getField(
            interceptor, "ipConnectionAttempts");
        Object attempts = attemptsMap.values().stream().findFirst().orElse(null);
        assertThat(attempts).isNotNull();
        ReflectionTestUtils.setField(attempts, "windowStart",
            System.currentTimeMillis() - 61000);

        boolean secondAllowed = interceptor.beforeHandshake(
            serverRequest, serverResponse, Mockito.mock(WebSocketHandler.class), new HashMap<>());

        assertThat(secondAllowed).isTrue();
    }

    @Test
    @DisplayName("Should expose rate limit configuration")
    void shouldExposeRateLimitConfiguration() {
        RateLimitingInterceptor interceptor = new RateLimitingInterceptor();
        ReflectionTestUtils.setField(interceptor, "connectionsPerMinute", 7);
        ReflectionTestUtils.setField(interceptor, "rateLimitEnabled", true);

        assertThat(interceptor.getConnectionsPerMinute()).isEqualTo(7);
        assertThat(interceptor.isRateLimitEnabled()).isTrue();
    }
}
