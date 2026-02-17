package com.healthdata.costanalysis.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for Cost Analysis Controller.
 * Tests REST API endpoints for cost analysis functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CostAnalysisControllerTest {

    private static final String TENANT_ID = "TENANT_001";
    private static final String API_BASE = "/api/v1/analysis";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Setup test data if needed
    }

    @Test
    void shouldGetCostsByTypeAndPeriod() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/costs")
                .param("analysisType", "drill-down")
                .param("analysisPeriod", "2025-01")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().exists("Cache-Control"))
            .andExpect(header().string("Cache-Control",
                org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    void shouldRequireAuthenticationForCostAnalysis() throws Exception {
        // When & Then - Missing X-Tenant-ID should result in error
        mockMvc.perform(get(API_BASE + "/costs")
                .param("analysisType", "drill-down")
                .param("analysisPeriod", "2025-01"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGetDrilldownAnalysis() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/drilldown")
                .param("serviceName", "patient-service")
                .param("dimension", "cost-category")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID));
    }

    @Test
    void shouldGetRecentAnalyses() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/recent")
                .param("limit", "10")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldGetCacheMetrics() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/cache-metrics")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hitRatio").exists());
    }

    @Test
    void shouldInvalidateCacheManually() throws Exception {
        // When & Then
        mockMvc.perform(post(API_BASE + "/cache/invalidate")
                .param("analysisType", "drill-down")
                .param("analysisPeriod", "2025-01")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    void shouldCleanupExpiredCaches() throws Exception {
        // When & Then
        mockMvc.perform(post(API_BASE + "/cache/cleanup")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    void shouldEnforceHIPAACacheHeaders() throws Exception {
        // When & Then - All responses should include HIPAA cache headers
        mockMvc.perform(get(API_BASE + "/costs")
                .param("analysisType", "drill-down")
                .param("analysisPeriod", "2025-01")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control",
                org.hamcrest.Matchers.allOf(
                    org.hamcrest.Matchers.containsString("no-store"),
                    org.hamcrest.Matchers.containsString("no-cache"),
                    org.hamcrest.Matchers.containsString("must-revalidate")
                )))
            .andExpect(header().exists("Pragma"));
    }

    @Test
    void shouldReturnProperContentType() throws Exception {
        // When & Then
        mockMvc.perform(get(API_BASE + "/costs")
                .param("analysisType", "drill-down")
                .param("analysisPeriod", "2025-01")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"));
    }
}
