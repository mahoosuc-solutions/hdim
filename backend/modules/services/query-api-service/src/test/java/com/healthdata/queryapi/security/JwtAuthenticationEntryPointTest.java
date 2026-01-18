package com.healthdata.queryapi.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for JwtAuthenticationEntryPoint (Team 2)
 * Verifies 401 Unauthorized error handling with structured JSON responses
 *
 * Test Coverage:
 * - JSON error response format
 * - HTTP status code (401 Unauthorized)
 * - Cache prevention headers
 * - WWW-Authenticate header
 * - Error message extraction and formatting
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Authentication Entry Point 401 Error Handling")
class JwtAuthenticationEntryPointTest {

    @InjectMocks
    private JwtAuthenticationEntryPoint entryPoint;

    @Mock
    private MockHttpServletRequest mockRequest;

    // ============ Response Status and Content Type Tests ============

    @Test
    @DisplayName("Should return 401 Unauthorized status")
    void shouldReturn401UnauthorizedStatus() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/patients/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new AuthenticationCredentialsNotFoundException("Missing authentication");

        entryPoint.commence(request, response, authException);

        assertEquals(401, response.getStatus(),
            "Should return 401 Unauthorized status");
    }

    @Test
    @DisplayName("Should return JSON content type")
    void shouldReturnJsonContentType() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/conditions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new BadCredentialsException("Invalid token");

        entryPoint.commence(request, response, authException);

        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType(),
            "Should return JSON content type");
    }

    // ============ Cache Prevention Headers Tests ============

    @Test
    @DisplayName("Should include no-cache and no-store directives")
    void shouldIncludeCachePreventionHeaders() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/care-plans");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new AuthenticationCredentialsNotFoundException("No token provided");

        entryPoint.commence(request, response, authException);

        String cacheControl = response.getHeader("Cache-Control");
        assertNotNull(cacheControl, "Cache-Control header should be present");
        assertTrue(cacheControl.contains("no-store"), "Should contain no-store directive");
        assertTrue(cacheControl.contains("no-cache"), "Should contain no-cache directive");
        assertTrue(cacheControl.contains("must-revalidate"), "Should contain must-revalidate directive");
    }

    @Test
    @DisplayName("Should set Pragma header to no-cache")
    void shouldSetPragmaNoCache() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/patients");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new BadCredentialsException("Malformed token");

        entryPoint.commence(request, response, authException);

        assertEquals("no-cache", response.getHeader("Pragma"),
            "Should set Pragma header to no-cache");
    }

    @Test
    @DisplayName("Should set Expires header to 0")
    void shouldSetExpiresHeaderToZero() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/observations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new AuthenticationCredentialsNotFoundException("Token expired");

        entryPoint.commence(request, response, authException);

        assertEquals("0", response.getHeader("Expires"),
            "Should set Expires header to 0");
    }

    // ============ WWW-Authenticate Header Tests ============

    @Test
    @DisplayName("Should include WWW-Authenticate header with Bearer scheme")
    void shouldIncludeWWWAuthenticateHeader() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/patients");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new BadCredentialsException("Invalid signature");

        entryPoint.commence(request, response, authException);

        String wwwAuth = response.getHeader("WWW-Authenticate");
        assertNotNull(wwwAuth, "WWW-Authenticate header should be present");
        assertTrue(wwwAuth.contains("Bearer"), "Should use Bearer authentication scheme");
        assertTrue(wwwAuth.contains("hdim-api"), "Should reference hdim-api realm");
    }

    // ============ JSON Response Body Tests ============

    @Test
    @DisplayName("Should include 'error' field in response")
    void shouldIncludeErrorField() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/conditions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new BadCredentialsException("Invalid token");

        entryPoint.commence(request, response, authException);

        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("\"error\""), "Should contain 'error' field");
        assertTrue(responseBody.contains("Unauthorized"), "Should contain Unauthorized error name");
    }

    @Test
    @DisplayName("Should include error message in response")
    void shouldIncludeErrorMessage() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/patients/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String errorMessage = "Invalid JWT signature";
        AuthenticationException authException = new BadCredentialsException(errorMessage);

        entryPoint.commence(request, response, authException);

        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains(errorMessage), "Should include error message from exception");
    }

    @Test
    @DisplayName("Should include request path in response")
    void shouldIncludeRequestPath() throws IOException, ServletException {
        String requestPath = "/api/v1/care-plans/patient/123";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestPath);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = new AuthenticationCredentialsNotFoundException("No credentials");

        entryPoint.commence(request, response, authException);

        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains(requestPath), "Should include request path in response");
    }

    @Test
    @DisplayName("Should include timestamp in response")
    void shouldIncludeTimestampInResponse() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/observations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        long beforeTimestamp = System.currentTimeMillis();
        AuthenticationException authException = new BadCredentialsException("Invalid token");

        entryPoint.commence(request, response, authException);

        long afterTimestamp = System.currentTimeMillis();
        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("timestamp"), "Should include timestamp field");
        assertTrue(responseBody.contains("\"message\""), "Should include message field");
    }
}
