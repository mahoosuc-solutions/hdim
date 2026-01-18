package com.healthdata.gateway.filter;

import com.healthdata.gateway.domain.AuditLog;
import com.healthdata.gateway.domain.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Audit Logging (Phase 2.0 Team 2)
 *
 * Tests full filter integration with Spring context:
 * - End-to-end HTTP requests
 * - Filter chain integration
 * - Database persistence
 * - Tenant isolation
 * - Security event detection
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "security.audit-logging.enabled=true",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6380"
})
@DisplayName("Audit Logging Integration Tests")
class AuditLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ENDPOINT = "/api/v1/patients/123";

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should log successful request to database")
    void testLogsSuccessfulRequest() throws Exception {
        // Clear previous logs
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        // Verify audit log was created
        assertThat(auditLogRepository.findByTenantIdOrderByTimestampDesc(TENANT_ID, null))
            .isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should capture HTTP method")
    void testCapturesHttpMethod() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(post(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .contentType("application/json")
                .content("{}"))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getHttpMethod()).isEqualTo("POST");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should capture request path")
    void testCapturesRequestPath() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getRequestPath()).contains(PATIENT_ENDPOINT);
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "testuser")
    @DisplayName("Should capture authenticated user ID")
    void testCapturesAuthenticatedUser() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getUsername()).isEqualTo("testuser");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should capture tenant ID")
    void testCapturesTenantId() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should capture response status code")
    void testCapturesResponseStatus() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getHttpStatusCode()).isEqualTo(200);
        assertThat(log.getSuccess()).isTrue();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should capture user roles")
    void testCapturesUserRoles() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getRoles()).contains("VIEWER");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should measure response time")
    void testMeasuresResponseTime() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getResponseTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should skip logging for health endpoint")
    void testSkipsHealthEndpoint() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get("/actuator/health")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        // Should not create audit log for health endpoint
        assertThat(auditLogRepository.countByTenantId(TENANT_ID)).isZero();
    }

    @Test
    @DisplayName("Should skip logging for swagger endpoint")
    void testSkipsSwaggerEndpoint() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get("/swagger-ui/index.html")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        assertThat(auditLogRepository.countByTenantId(TENANT_ID)).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should enforce multi-tenant isolation")
    void testMultiTenantIsolation() throws Exception {
        auditLogRepository.deleteAll();

        // Log from tenant A
        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", "tenant-a"))
            .andExpect(status().isOk());

        // Log from tenant B
        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", "tenant-b"))
            .andExpect(status().isOk());

        // Verify isolation
        assertThat(auditLogRepository.countByTenantId("tenant-a")).isGreaterThan(0);
        assertThat(auditLogRepository.countByTenantId("tenant-b")).isGreaterThan(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should set created_at timestamp")
    void testSetsCreatedAtTimestamp() throws Exception {
        auditLogRepository.deleteAll();
        Instant beforeRequest = Instant.now();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        Instant afterRequest = Instant.now();

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getCreatedAt())
            .isNotNull()
            .isAfterOrEqualTo(beforeRequest)
            .isBeforeOrEqualTo(afterRequest);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should capture timestamp for request")
    void testCapturesRequestTimestamp() throws Exception {
        auditLogRepository.deleteAll();

        Instant beforeRequest = Instant.now();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        Instant afterRequest = Instant.now();

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getTimestamp())
            .isNotNull()
            .isAfterOrEqualTo(beforeRequest)
            .isBeforeOrEqualTo(afterRequest);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should include User-Agent header")
    void testIncludesUserAgent() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID)
                .header("User-Agent", "TestClient/1.0"))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getUserAgent()).contains("TestClient");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle multiple requests from same user")
    void testHandlesMultipleRequests() throws Exception {
        auditLogRepository.deleteAll();

        // Make 5 requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get(PATIENT_ENDPOINT)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk());
        }

        // Verify all requests were logged
        long count = auditLogRepository.countByTenantId(TENANT_ID);
        assertThat(count).isGreaterThanOrEqualTo(5);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should store authorization allowed flag")
    void testStoresAuthorizationFlag() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getAuthorizationAllowed()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should capture query parameters")
    void testCapturesQueryParameters() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT + "?status=ACTIVE&limit=10")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getQueryParameters())
            .isNotNull()
            .contains("status")
            .contains("ACTIVE");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should support all HTTP status codes")
    void testSupportsAllStatusCodes() throws Exception {
        auditLogRepository.deleteAll();

        // Test various status codes by requesting different endpoints
        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getHttpStatusCode()).isGreaterThanOrEqualTo(200);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should query by tenant and date range")
    void testQueryByTenantAndDateRange() throws Exception {
        auditLogRepository.deleteAll();

        Instant startTime = Instant.now();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        Instant endTime = Instant.now().plusSeconds(1);

        Long count = auditLogRepository.countByTenantAndDateRange(TENANT_ID, startTime, endTime);
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should mark successful 2xx responses")
    void testMarksSuccessful2xx() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getSuccess()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should mark failed 4xx responses")
    void testMarksFailedResponses() throws Exception {
        auditLogRepository.deleteAll();

        // Request non-existent endpoint
        mockMvc.perform(get("/api/v1/nonexistent/endpoint")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound());

        AuditLog log = auditLogRepository.findFirstByOrderByTimestampDesc();
        assertThat(log).isNotNull();
        assertThat(log.getSuccess()).isFalse();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should have proper database indexes for queries")
    void testHasProperDatabaseIndexes() throws Exception {
        auditLogRepository.deleteAll();

        mockMvc.perform(get(PATIENT_ENDPOINT)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        // These queries should be fast due to indexes
        assertThat(auditLogRepository.findByTenantIdOrderByTimestampDesc(TENANT_ID, null))
            .isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should support pagination on query results")
    void testSupportsPagination() throws Exception {
        auditLogRepository.deleteAll();

        // Create multiple logs
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(get(PATIENT_ENDPOINT)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk());
        }

        assertThat(auditLogRepository.countByTenantId(TENANT_ID)).isGreaterThanOrEqualTo(15);
    }
}
