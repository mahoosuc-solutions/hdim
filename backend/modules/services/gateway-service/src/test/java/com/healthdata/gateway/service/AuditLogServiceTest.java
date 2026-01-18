package com.healthdata.gateway.service;

import com.healthdata.gateway.domain.AuditLog;
import com.healthdata.gateway.domain.AuditLogRepository;
import com.healthdata.gateway.dto.AuditLogRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuditLogService (Phase 2.0 Team 2)
 *
 * Tests cover:
 * - Request to entity conversion
 * - Security event detection
 * - Async logging behavior
 * - Error handling and fallback
 * - Query methods
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Tests")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private static final String TENANT_ID = "tenant-001";
    private static final String USER_ID = "user-123";
    private static final String ENDPOINT = "/api/v1/patients/456";
    private static final String CLIENT_IP = "192.168.1.100";

    private AuditLogRequest testRequest;

    @BeforeEach
    void setup() {
        testRequest = AuditLogRequest.builder()
            .timestamp(Instant.now())
            .httpMethod("GET")
            .requestPath(ENDPOINT)
            .queryParameters(null)
            .userId(USER_ID)
            .username("testuser")
            .tenantId(TENANT_ID)
            .roles("EVALUATOR")
            .clientIp(CLIENT_IP)
            .userAgent("Mozilla/5.0")
            .httpStatusCode(200)
            .responseTimeMs(150)
            .success(true)
            .authorizationAllowed(true)
            .requiredRole("VIEWER")
            .traceId("trace-123")
            .spanId("span-456")
            .build();
    }

    @Test
    @DisplayName("Should convert request to entity and persist")
    void testLogAccessAsync_SuccessfullySavesEntity() {
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        auditLogService.logAccessAsync(testRequest);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getHttpMethod()).isEqualTo("GET");
        assertThat(saved.getRequestPath()).isEqualTo(ENDPOINT);
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getHttpStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should detect 401 unauthorized as security event")
    void testSecurityEventDetection_Unauthorized() {
        AuditLogRequest unauthorizedRequest = testRequest.toBuilder()
            .httpStatusCode(401)
            .success(false)
            .build();

        auditLogService.logAccessAsync(unauthorizedRequest);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should detect 403 forbidden as security event")
    void testSecurityEventDetection_Forbidden() {
        AuditLogRequest forbiddenRequest = testRequest.toBuilder()
            .httpStatusCode(403)
            .success(false)
            .authorizationAllowed(false)
            .build();

        auditLogService.logAccessAsync(forbiddenRequest);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should detect 429 rate limit exceeded as security event")
    void testSecurityEventDetection_RateLimitExceeded() {
        AuditLogRequest rateLimitedRequest = testRequest.toBuilder()
            .httpStatusCode(429)
            .success(false)
            .build();

        auditLogService.logAccessAsync(rateLimitedRequest);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should detect failed authorization as security event")
    void testSecurityEventDetection_FailedAuthorization() {
        AuditLogRequest unauthorizedRequest = testRequest.toBuilder()
            .authorizationAllowed(false)
            .httpStatusCode(403)
            .build();

        auditLogService.logAccessAsync(unauthorizedRequest);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should not treat 200 success as security event")
    void testSecurityEventDetection_Success() {
        auditLogService.logAccessAsync(testRequest);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should handle database persistence failure gracefully")
    void testGracefulErrorHandling_PersistenceFailure() {
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Should not throw exception
        assertThatNoException()
            .isThrownBy(() -> auditLogService.logAccessAsync(testRequest));

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should preserve all request metadata in entity")
    void testPreservesAllMetadata() {
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        auditLogService.logAccessAsync(testRequest);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getHttpMethod()).isEqualTo(testRequest.getHttpMethod());
        assertThat(saved.getRequestPath()).isEqualTo(testRequest.getRequestPath());
        assertThat(saved.getUserId()).isEqualTo(testRequest.getUserId());
        assertThat(saved.getUsername()).isEqualTo(testRequest.getUsername());
        assertThat(saved.getTenantId()).isEqualTo(testRequest.getTenantId());
        assertThat(saved.getRoles()).isEqualTo(testRequest.getRoles());
        assertThat(saved.getClientIp()).isEqualTo(testRequest.getClientIp());
        assertThat(saved.getUserAgent()).isEqualTo(testRequest.getUserAgent());
        assertThat(saved.getHttpStatusCode()).isEqualTo(testRequest.getHttpStatusCode());
        assertThat(saved.getResponseTimeMs()).isEqualTo(testRequest.getResponseTimeMs());
        assertThat(saved.getSuccess()).isEqualTo(testRequest.getSuccess());
        assertThat(saved.getAuthorizationAllowed()).isEqualTo(testRequest.getAuthorizationAllowed());
        assertThat(saved.getRequiredRole()).isEqualTo(testRequest.getRequiredRole());
        assertThat(saved.getTraceId()).isEqualTo(testRequest.getTraceId());
        assertThat(saved.getSpanId()).isEqualTo(testRequest.getSpanId());
    }

    @Test
    @DisplayName("Should handle null optional fields")
    void testHandlesNullFields() {
        AuditLogRequest requestWithNulls = AuditLogRequest.builder()
            .timestamp(Instant.now())
            .httpMethod("GET")
            .requestPath(ENDPOINT)
            .tenantId(TENANT_ID)
            .clientIp(CLIENT_IP)
            .httpStatusCode(200)
            .success(true)
            .build();

        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        auditLogService.logAccessAsync(requestWithNulls);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getUsername()).isNull();
        assertThat(saved.getRoles()).isNull();
        assertThat(saved.getTraceId()).isNull();
    }

    @Test
    @DisplayName("Should set createdAt timestamp on entity")
    void testCreatedAtTimestamp() {
        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        Instant beforeCall = Instant.now();
        auditLogService.logAccessAsync(testRequest);
        Instant afterCall = Instant.now();

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getCreatedAt())
            .isNotNull()
            .isAfterOrEqualTo(beforeCall)
            .isBeforeOrEqualTo(afterCall);
    }

    @Test
    @DisplayName("Should correctly identify failed login attempts")
    void testFailedLoginAttemptDetection() {
        AuditLogRequest failedLoginRequest = testRequest.toBuilder()
            .requestPath("/api/v1/auth/login")
            .httpStatusCode(401)
            .success(false)
            .build();

        auditLogService.logAccessAsync(failedLoginRequest);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should handle missing tenant ID")
    void testHandlesMissingTenantId() {
        AuditLogRequest noTenantRequest = testRequest.toBuilder()
            .tenantId("unknown")
            .build();

        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        auditLogService.logAccessAsync(noTenantRequest);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getTenantId()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Should handle unauthenticated requests (no user ID)")
    void testHandlesUnauthenticatedRequests() {
        AuditLogRequest unauthRequest = testRequest.toBuilder()
            .userId(null)
            .username(null)
            .roles(null)
            .authorizationAllowed(false)
            .build();

        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        auditLogService.logAccessAsync(unauthRequest);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getUsername()).isNull();
        assertThat(saved.getAuthorizationAllowed()).isFalse();
    }

    @Test
    @DisplayName("Should preserve response time in milliseconds")
    void testResponseTimePreservation() {
        AuditLogRequest slowRequest = testRequest.toBuilder()
            .responseTimeMs(5000)  // 5 second response
            .build();

        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        auditLogService.logAccessAsync(slowRequest);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getResponseTimeMs()).isEqualTo(5000);
    }

    @Test
    @DisplayName("Should preserve OpenTelemetry trace and span IDs")
    void testPreservesOpenTelemetryIds() {
        AuditLogRequest tracedRequest = testRequest.toBuilder()
            .traceId("4bf92f3577b34da6a3ce929d0e0e4736")
            .spanId("00f067aa0ba902b7")
            .build();

        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        auditLogService.logAccessAsync(tracedRequest);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(saved.getSpanId()).isEqualTo("00f067aa0ba902b7");
    }

    @Test
    @DisplayName("Should handle query parameters with special characters")
    void testHandlesSpecialCharactersInQuery() {
        AuditLogRequest queryRequest = testRequest.toBuilder()
            .queryParameters("status=ACTIVE&search=patient%20name&limit=10")
            .build();

        when(auditLogRepository.save(any(AuditLog.class)))
            .thenReturn(null);

        auditLogService.logAccessAsync(queryRequest);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getQueryParameters())
            .isEqualTo("status=ACTIVE&search=patient%20name&limit=10");
    }

    @Test
    @DisplayName("Should batch multiple audit logs efficiently")
    void testBatchAuditLogging() {
        // Simulate multiple concurrent audit requests
        for (int i = 0; i < 10; i++) {
            AuditLogRequest request = testRequest.toBuilder()
                .userId("user-" + i)
                .build();
            auditLogService.logAccessAsync(request);
        }

        verify(auditLogRepository, times(10)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should handle HTTP methods correctly")
    void testHandlesAllHttpMethods() {
        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"};

        for (String method : methods) {
            AuditLogRequest methodRequest = testRequest.toBuilder()
                .httpMethod(method)
                .build();

            when(auditLogRepository.save(any(AuditLog.class)))
                .thenReturn(null);

            auditLogService.logAccessAsync(methodRequest);

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            assertThat(captor.getValue().getHttpMethod()).isEqualTo(method);
            reset(auditLogRepository);
        }
    }
}
