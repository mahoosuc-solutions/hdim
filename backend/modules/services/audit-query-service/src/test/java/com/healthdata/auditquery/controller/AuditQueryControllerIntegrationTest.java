package com.healthdata.auditquery.controller;

import com.healthdata.audit.entity.shared.AuditEventEntity;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.repository.shared.AuditEventRepository;
import com.healthdata.auditquery.dto.AuditSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Audit Query API.
 *
 * <p>Tests multi-tenant isolation, search functionality, and statistics generation.
 */
@Tag("integration")
@SpringBootTest(
    properties = {
        "spring.kafka.enabled=false",
        "healthdata.persistence.rls-enabled=false"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditQueryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private static final String TENANT_ID = "test-tenant-001";
    private static final String USER_ID = "test-user-123";

    @BeforeEach
    void setUp() {
        // Clean up before each test
        auditEventRepository.deleteAll();

        // Insert test data
        createTestAuditEvent("patient-service", "Patient", "pat-001", AuditAction.READ, AuditOutcome.SUCCESS);
        createTestAuditEvent("patient-service", "Patient", "pat-002", AuditAction.READ, AuditOutcome.SUCCESS);
        createTestAuditEvent("fhir-service", "Observation", "obs-001", AuditAction.CREATE, AuditOutcome.SUCCESS);
        createTestAuditEvent("patient-service", "Patient", "pat-001", AuditAction.UPDATE, AuditOutcome.MINOR_FAILURE);
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void testSearchAuditLogs_WithFilters() throws Exception {
        String requestBody = """
            {
                "resourceType": "Patient",
                "outcome": "SUCCESS",
                "page": 0,
                "size": 10
            }
            """;

        mockMvc.perform(post("/api/v1/audit/logs/search")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].resourceType").value("Patient"))
            .andExpect(jsonPath("$.content[0].outcome").value("SUCCESS"));
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void testGetAuditEvent_ById() throws Exception {
        // Create a specific event
        AuditEventEntity event = createTestAuditEvent(
            "test-service", "TestResource", "res-123",
            AuditAction.READ, AuditOutcome.SUCCESS
        );

        mockMvc.perform(get("/api/v1/audit/logs/" + event.getId())
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(event.getId().toString()))
            .andExpect(jsonPath("$.resourceType").value("TestResource"))
            .andExpect(jsonPath("$.resourceId").value("res-123"));
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void testGetStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/audit/logs/statistics")
                .header("X-Tenant-ID", TENANT_ID)
                .param("startTime", Instant.now().minus(1, ChronoUnit.DAYS).toString())
                .param("endTime", Instant.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEvents").value(greaterThanOrEqualTo(4)))
            .andExpect(jsonPath("$.eventsByAction").exists())
            .andExpect(jsonPath("$.eventsByOutcome").exists())
            .andExpect(jsonPath("$.eventsByResourceType.Patient").value(greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.failedEvents").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testExportAuditLogs_CSV() throws Exception {
        String requestBody = """
            {
                "page": 0,
                "size": 100
            }
            """;

        mockMvc.perform(post("/api/v1/audit/logs/export")
                .header("X-Tenant-ID", TENANT_ID)
                .param("format", "CSV")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/csv")))
            .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")  // Wrong role
    void testSearchAuditLogs_InsufficientPermissions() throws Exception {
        String requestBody = """
            {
                "page": 0,
                "size": 10
            }
            """;

        mockMvc.perform(post("/api/v1/audit/logs/search")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void testMultiTenantIsolation() throws Exception {
        // Create event for different tenant
        AuditEventEntity otherTenantEvent = new AuditEventEntity();
        otherTenantEvent.setId(UUID.randomUUID());
        otherTenantEvent.setTimestamp(Instant.now());
        otherTenantEvent.setTenantId("other-tenant-999");
        otherTenantEvent.setUserId(USER_ID);
        otherTenantEvent.setAction(AuditAction.READ);
        otherTenantEvent.setResourceType("Patient");
        otherTenantEvent.setResourceId("pat-999");
        otherTenantEvent.setOutcome(AuditOutcome.SUCCESS);
        otherTenantEvent.setServiceName("test-service");
        auditEventRepository.save(otherTenantEvent);

        // Search should only return events for TENANT_ID
        String requestBody = """
            {
                "page": 0,
                "size": 100
            }
            """;

        mockMvc.perform(post("/api/v1/audit/logs/search")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].tenantId", everyItem(is(TENANT_ID))))
            .andExpect(jsonPath("$.content[*].tenantId", not(hasItem("other-tenant-999"))));
    }

    /**
     * Helper method to create test audit events.
     */
    private AuditEventEntity createTestAuditEvent(
        String serviceName,
        String resourceType,
        String resourceId,
        AuditAction action,
        AuditOutcome outcome
    ) {
        AuditEventEntity event = new AuditEventEntity();
        event.setId(UUID.randomUUID());
        event.setTimestamp(Instant.now());
        event.setTenantId(TENANT_ID);
        event.setUserId(USER_ID);
        event.setUsername("test.user@example.com");
        event.setRole("EVALUATOR");
        event.setIpAddress("192.168.1.100");
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setOutcome(outcome);
        event.setServiceName(serviceName);
        event.setMethodName("testMethod");
        event.setRequestPath("/api/v1/test");
        event.setDurationMs(100L);

        return auditEventRepository.save(event);
    }
}
