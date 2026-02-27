package com.healthdata.analytics.rest;

import com.healthdata.analytics.dto.DashboardDto;
import com.healthdata.analytics.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("integration")
@DisplayName("DashboardController Integration Tests")
class DashboardControllerIntegrationTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .build();
    }

    @Test
    @DisplayName("getDashboards_returns200")
    void getDashboards_returns200() throws Exception {
        DashboardDto dto = DashboardDto.builder()
                .id(UUID.randomUUID())
                .name("Pilot Dashboard")
                .isDefault(true)
                .isShared(false)
                .build();

        when(dashboardService.getDashboards(anyString())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/analytics/dashboards")
                        .header("X-Tenant-ID", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Pilot Dashboard"));
    }

    @Test
    @DisplayName("getDashboardById_notFound_returns404")
    void getDashboardById_notFound_returns404() throws Exception {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        when(dashboardService.getDashboard(eq(unknownId), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/analytics/dashboards/{id}", unknownId)
                        .header("X-Tenant-ID", "tenant-001"))
                .andExpect(status().isNotFound());
    }
}
