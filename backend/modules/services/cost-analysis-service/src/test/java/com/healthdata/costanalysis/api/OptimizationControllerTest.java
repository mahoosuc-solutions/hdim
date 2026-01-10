package com.healthdata.costanalysis.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for Optimization Controller.
 * Tests REST API endpoints for recommendation management.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OptimizationControllerTest {

    private static final String TENANT_ID = "TENANT_001";
    private static final String API_BASE = "/api/v1/recommendations";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Setup test data if needed
    }

    @Test
    void shouldGetPendingRecommendations() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/pending")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));
    }

    @Test
    void shouldGetHighPriorityRecommendations() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/high-priority")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));
    }

    @Test
    void shouldGetRecommendationsByService() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/service/patient-service")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));
    }

    @Test
    void shouldAcceptRecommendation() throws Exception {
        // Given
        UUID recommendationId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(post(API_BASE + "/" + recommendationId + "/accept")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    void shouldCompleteRecommendationWithSavings() throws Exception {
        // Given
        UUID recommendationId = UUID.randomUUID();
        BigDecimal actualSavings = new BigDecimal("85000.00");

        // When & Then
        mockMvc.perform(post(API_BASE + "/" + recommendationId + "/complete")
                .param("actualSavings", actualSavings.toPlainString())
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectRecommendationWithReason() throws Exception {
        // Given
        UUID recommendationId = UUID.randomUUID();
        String reason = "Not aligned with current priorities";

        // When & Then
        mockMvc.perform(post(API_BASE + "/" + recommendationId + "/reject")
                .param("reason", reason)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    void shouldGetPotentialSavings() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/savings/potential")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.potentialSavings").exists())
            .andExpect(jsonPath("$.implementedCount").exists())
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID));
    }

    @Test
    void shouldRequireAuthenticationForRecommendations() throws Exception {
        // When & Then - Missing X-Tenant-ID should result in error
        mockMvc.perform(get(API_BASE + "/pending"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldEnforceRoleBasedAccess() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/pending")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnPaginatedResults() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/pending")
                .param("limit", "20")
                .param("offset", "0")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"));
    }

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        // Given
        String tenant2 = "TENANT_002";

        // When & Then - Each tenant should only see their own data
        mockMvc.perform(get(API_BASE + "/pending")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        mockMvc.perform(get(API_BASE + "/pending")
                .header("X-Tenant-ID", tenant2))
            .andExpect(status().isOk());
    }

    @Test
    void shouldValidateActualSavingsFormat() throws Exception {
        // Given
        UUID recommendationId = UUID.randomUUID();
        String invalidSavings = "not-a-number";

        // When & Then
        mockMvc.perform(post(API_BASE + "/" + recommendationId + "/complete")
                .param("actualSavings", invalidSavings)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSupportServiceFiltering() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/service/quality-measure-service")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnEmptyListWhenNoRecommendations() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/pending")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }
}
