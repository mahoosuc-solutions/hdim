package com.healthdata.quality.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthdata.authentication.service.JwtTokenService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT WebSocket Handshake Interceptor Tests")
class JwtWebSocketHandshakeInterceptorTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private WebSocketHandler webSocketHandler;

    @Test
    @DisplayName("Should reject handshake when token missing")
    void shouldRejectWhenTokenMissing() throws Exception {
        JwtWebSocketHandshakeInterceptor interceptor = new JwtWebSocketHandshakeInterceptor(jwtTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, webSocketHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should reject handshake when token invalid")
    void shouldRejectWhenTokenInvalid() throws Exception {
        JwtWebSocketHandshakeInterceptor interceptor = new JwtWebSocketHandshakeInterceptor(jwtTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.addHeader("Authorization", "Bearer bad-token");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);

        when(jwtTokenService.validateToken("bad-token")).thenReturn(false);

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, webSocketHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Should accept handshake and populate attributes when token valid")
    void shouldAcceptWhenTokenValid() throws Exception {
        JwtWebSocketHandshakeInterceptor interceptor = new JwtWebSocketHandshakeInterceptor(jwtTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setQueryString("tenantId=tenant-1&token=good-token");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());
        Map<String, Object> attributes = new HashMap<>();

        when(jwtTokenService.validateToken("good-token")).thenReturn(true);
        when(jwtTokenService.extractUsername("good-token")).thenReturn("user1");
        when(jwtTokenService.extractTenantIds("good-token")).thenReturn(Set.of("tenant-1"));
        when(jwtTokenService.extractUserId("good-token")).thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        when(jwtTokenService.extractRoles("good-token")).thenReturn(Set.of("PROVIDER", "ADMIN"));

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, webSocketHandler, attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry("authenticated", true);
        assertThat(attributes).containsEntry("username", "user1");
        assertThat(attributes).containsEntry("tenantId", "tenant-1");
        String roles = (String) attributes.get("roles");
        assertThat(roles).isNotNull();
        assertThat(Set.of(roles.split(","))).containsExactlyInAnyOrder("PROVIDER", "ADMIN");
    }

    @Test
    @DisplayName("Should accept handshake when token provided via cookie")
    void shouldAcceptWhenTokenProvidedViaCookie() throws Exception {
        JwtWebSocketHandshakeInterceptor interceptor = new JwtWebSocketHandshakeInterceptor(jwtTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setCookies(new Cookie("access_token", "cookie-token"));
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());
        Map<String, Object> attributes = new HashMap<>();

        when(jwtTokenService.validateToken("cookie-token")).thenReturn(true);
        when(jwtTokenService.extractUsername("cookie-token")).thenReturn("cookie-user");
        when(jwtTokenService.extractTenantIds("cookie-token")).thenReturn(Set.of());
        when(jwtTokenService.extractUserId("cookie-token")).thenReturn(UUID.randomUUID());
        when(jwtTokenService.extractRoles("cookie-token")).thenReturn(Set.of());

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, webSocketHandler, attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry("authenticated", true);
        assertThat(attributes.get("tenantId")).isNull();
    }

    @Test
    @DisplayName("Should return 500 when token validation throws")
    void shouldReturnInternalServerErrorWhenValidationThrows() throws Exception {
        JwtWebSocketHandshakeInterceptor interceptor = new JwtWebSocketHandshakeInterceptor(jwtTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.addHeader("Authorization", "Bearer boom");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(rawResponse);

        when(jwtTokenService.validateToken("boom")).thenThrow(new RuntimeException("boom"));

        boolean allowed = interceptor.beforeHandshake(serverRequest, serverResponse, webSocketHandler, new HashMap<>());

        assertThat(allowed).isFalse();
        assertThat(rawResponse.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    @DisplayName("Should handle afterHandshake with exception")
    void shouldHandleAfterHandshakeWithException() {
        JwtWebSocketHandshakeInterceptor interceptor = new JwtWebSocketHandshakeInterceptor(jwtTokenService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        ServletServerHttpRequest serverRequest = new ServletServerHttpRequest(request);
        ServletServerHttpResponse serverResponse = new ServletServerHttpResponse(new MockHttpServletResponse());

        interceptor.afterHandshake(serverRequest, serverResponse, webSocketHandler, new RuntimeException("handshake failed"));
    }
}
