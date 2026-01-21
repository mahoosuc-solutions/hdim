package com.healthdata.gateway.filter;

import com.healthdata.gateway.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Test Suite for AuditLoggingFilter (Phase 2.0 Team 2)
 *
 * Tests cover:
 * - Request/response metadata capture
 * - Security context extraction
 * - Proxy header handling
 * - Excluded path behavior
 * - Async audit logging
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLoggingFilter Tests")
class AuditLoggingFilterTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AuditLoggingFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String TENANT_ID = "test-tenant";
    private static final String ENDPOINT = "/api/v1/patients/123";

    @BeforeEach
    void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // Clear security context
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should capture successful request")
    void testCapturesSuccessfulRequest() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.setStatus(200);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getHttpMethod().equals("GET") &&
            auditRequest.getRequestPath().equals(ENDPOINT) &&
            auditRequest.getTenantId().equals(TENANT_ID) &&
            auditRequest.getHttpStatusCode() == 200 &&
            auditRequest.getSuccess()
        ));
    }

    @Test
    @DisplayName("Should capture failed request")
    void testCapturesFailedRequest() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.setStatus(500);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getHttpStatusCode() == 500 &&
            !auditRequest.getSuccess()
        ));
    }

    @Test
    @DisplayName("Should capture all HTTP methods")
    void testCapturesAllHttpMethods() throws ServletException, IOException {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH"};

        for (String method : methods) {
            request = new MockHttpServletRequest(method, ENDPOINT);
            request.addHeader("X-Tenant-ID", TENANT_ID);
            response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            verify(auditLogService).logAccessAsync(argThat(auditRequest ->
                auditRequest.getHttpMethod().equals(method)
            ));
        }
    }

    @Test
    @DisplayName("Should include query parameters")
    void testCapturesQueryParameters() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", ENDPOINT);
        request.setQueryString("status=ACTIVE&limit=10");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getQueryParameters() != null &&
            auditRequest.getQueryParameters().contains("status") &&
            auditRequest.getQueryParameters().contains("ACTIVE")
        ));
    }

    @Test
    @DisplayName("Should sanitize sensitive query parameters")
    void testSanitizesSensitiveParameters() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", ENDPOINT);
        request.setQueryString("ssn=123-45-6789&mrn=12345&dob=1990-01-01");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getQueryParameters().contains("[REDACTED]")
        ));
    }

    @Test
    @DisplayName("Should extract tenant ID from header")
    void testExtractsTenantId() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", "tenant-special-001");

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getTenantId().equals("tenant-special-001")
        ));
    }

    @Test
    @DisplayName("Should handle missing tenant ID")
    void testHandlesMissingTenantId() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        // No X-Tenant-ID header

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getTenantId().equals("unknown")
        ));
    }

    @Test
    @DisplayName("Should capture client IP address")
    void testCapturesClientIp() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.setRemoteAddr("192.168.1.100");

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getClientIp().equals("192.168.1.100")
        ));
    }

    @Test
    @DisplayName("Should respect X-Forwarded-For header for proxy IP")
    void testRespectXForwardedForHeader() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", ENDPOINT);
        request.addHeader("X-Forwarded-For", "203.0.113.45, 198.51.100.178");
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.setRemoteAddr("127.0.0.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getClientIp().equals("203.0.113.45")
        ));
    }

    @Test
    @DisplayName("Should respect X-Real-IP header")
    void testRespectXRealIpHeader() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", ENDPOINT);
        request.addHeader("X-Real-IP", "198.51.100.123");
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.setRemoteAddr("127.0.0.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getClientIp().equals("198.51.100.123")
        ));
    }

    @Test
    @DisplayName("Should capture user-agent header")
    void testCapturesUserAgent() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getUserAgent() != null &&
            auditRequest.getUserAgent().contains("Mozilla")
        ));
    }

    @Test
    @DisplayName("Should measure response time")
    void testMeasuresResponseTime() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getResponseTimeMs() >= 0
        ));
    }

    @Test
    @DisplayName("Should skip audit logging for health endpoint")
    void testSkipsHealthEndpoint() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService, never()).logAccessAsync(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip audit logging for actuator endpoints")
    void testSkipsActuatorEndpoint() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", "/actuator/metrics");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService, never()).logAccessAsync(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should skip audit logging for swagger endpoints")
    void testSkipsSwaggerEndpoint() throws ServletException, IOException {
        request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService, never()).logAccessAsync(any());
    }

    @Test
    @DisplayName("Should continue filter chain after logging")
    void testContinuesFilterChain() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle authentication exception gracefully")
    void testHandlesAuthenticationException() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);

        doThrow(new ServletException("Auth error"))
            .when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
            .isInstanceOf(ServletException.class);
    }

    @Test
    @DisplayName("Should handle audit service exception gracefully")
    void testHandlesAuditServiceException() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);

        doThrow(new RuntimeException("Audit error"))
            .when(auditLogService).logAccessAsync(any());

        // Should not throw - audit logging should fail silently
        assertThatNoException()
            .isThrownBy(() -> filter.doFilterInternal(request, response, filterChain));

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should capture response status code 200")
    void testCapturesStatus200() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        response.setStatus(200);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getHttpStatusCode() == 200 &&
            auditRequest.getSuccess()
        ));
    }

    @Test
    @DisplayName("Should capture response status code 404")
    void testCapturesStatus404() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        response.setStatus(404);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getHttpStatusCode() == 404 &&
            !auditRequest.getSuccess()
        ));
    }

    @Test
    @DisplayName("Should capture response status code 401")
    void testCapturesStatus401() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        response.setStatus(401);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getHttpStatusCode() == 401 &&
            !auditRequest.getSuccess()
        ));
    }

    @Test
    @DisplayName("Should capture response status code 429")
    void testCapturesStatus429() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        response.setStatus(429);

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getHttpStatusCode() == 429 &&
            !auditRequest.getSuccess()
        ));
    }

    @Test
    @DisplayName("Should handle OpenTelemetry trace headers")
    void testHandlesOpenTelemetryHeaders() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getTraceId() != null &&
            auditRequest.getSpanId() != null
        ));
    }

    @Test
    @DisplayName("Should handle custom trace ID headers")
    void testHandlesCustomTraceHeaders() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI(ENDPOINT);
        request.addHeader("X-Tenant-ID", TENANT_ID);
        request.addHeader("X-Trace-ID", "custom-trace-123");
        request.addHeader("X-Span-ID", "custom-span-456");

        filter.doFilterInternal(request, response, filterChain);

        verify(auditLogService).logAccessAsync(argThat(auditRequest ->
            auditRequest.getTraceId() != null &&
            auditRequest.getSpanId() != null
        ));
    }

    @Test
    @DisplayName("Should log all endpoint paths")
    void testLogsAllEndpointPaths() throws ServletException, IOException {
        String[] paths = {
            "/api/v1/patients/123",
            "/api/v1/measures/456",
            "/cql-engine/evaluate",
            "/fhir/Patient/789"
        };

        for (String path : paths) {
            request = new MockHttpServletRequest("GET", path);
            request.addHeader("X-Tenant-ID", TENANT_ID);
            response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            verify(auditLogService).logAccessAsync(argThat(auditRequest ->
                auditRequest.getRequestPath().equals(path)
            ));
        }
    }
}
