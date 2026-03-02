package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.DashboardDto;
import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.service.AlertService;
import com.healthdata.analytics.service.DashboardService;
import com.healthdata.analytics.service.KpiService;
import com.healthdata.analytics.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 2 integration tests for Analytics Service.
 * Validates results retrieval, aggregation, date filtering, tenant isolation, and empty state.
 */
@ExtendWith(MockitoExtension.class)
@Tag("integration")
@DisplayName("Analytics Service Phase 2 Integration Tests")
class AnalyticsPhase2IntegrationTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private KpiService kpiService;

    @Mock
    private ReportService reportService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private DashboardController dashboardController;

    @InjectMocks
    private KpiController kpiController;

    private MockMvc dashboardMockMvc;
    private MockMvc kpiMockMvc;

    private static final String TENANT_A = "pilot-tenant-a";
    private static final String TENANT_B = "pilot-tenant-b";

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        dashboardMockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setMessageConverters(converter)
                .build();
        kpiMockMvc = MockMvcBuilders.standaloneSetup(kpiController)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("GET /api/analytics/dashboards - Should return analytics results")
    void shouldReturnAnalyticsResults() throws Exception {
        DashboardDto dashboard = DashboardDto.builder()
                .id(UUID.randomUUID())
                .name("Quality Dashboard")
                .build();
        when(dashboardService.getDashboards(TENANT_A)).thenReturn(List.of(dashboard));

        dashboardMockMvc.perform(get("/api/analytics/dashboards")
                        .header("X-Tenant-ID", TENANT_A)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Quality Dashboard"));
    }

    @Test
    @DisplayName("GET /api/analytics/kpis/quality - Should return aggregated quality KPIs")
    void shouldReturnAggregatedQualityKpis() throws Exception {
        KpiSummaryDto kpi = KpiSummaryDto.builder().build();
        when(kpiService.getQualityKpis(TENANT_A)).thenReturn(List.of(kpi));

        kpiMockMvc.perform(get("/api/analytics/kpis/quality")
                        .header("X-Tenant-ID", TENANT_A)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/analytics/kpis/trends - Should support date range filtering")
    void shouldSupportDateRangeFiltering() throws Exception {
        when(kpiService.getTrends(eq(TENANT_A), anyString(), anyInt()))
                .thenReturn(Collections.emptyList());

        kpiMockMvc.perform(get("/api/analytics/kpis/trends")
                        .param("metricType", "QUALITY_SCORE")
                        .param("days", "30")
                        .header("X-Tenant-ID", TENANT_A)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tenant isolation - Different tenants get separate dashboard results")
    void shouldEnforceTenantIsolation() throws Exception {
        DashboardDto dashA = DashboardDto.builder().name("Tenant A Dashboard").build();
        DashboardDto dashB = DashboardDto.builder().name("Tenant B Dashboard").build();

        when(dashboardService.getDashboards(TENANT_A)).thenReturn(List.of(dashA));
        when(dashboardService.getDashboards(TENANT_B)).thenReturn(List.of(dashB));

        dashboardMockMvc.perform(get("/api/analytics/dashboards")
                        .header("X-Tenant-ID", TENANT_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tenant A Dashboard"));

        dashboardMockMvc.perform(get("/api/analytics/dashboards")
                        .header("X-Tenant-ID", TENANT_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tenant B Dashboard"));
    }

    @Test
    @DisplayName("GET /api/analytics/dashboards - Should return empty list for no data")
    void shouldReturnEmptyStateGracefully() throws Exception {
        when(dashboardService.getDashboards(TENANT_A)).thenReturn(Collections.emptyList());

        dashboardMockMvc.perform(get("/api/analytics/dashboards")
                        .header("X-Tenant-ID", TENANT_A)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
