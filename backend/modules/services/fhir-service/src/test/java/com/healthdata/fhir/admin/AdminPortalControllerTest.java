package com.healthdata.fhir.admin;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.auth.context.ScopedTenant;
import com.healthdata.fhir.admin.model.ApiPreset;
import com.healthdata.fhir.admin.model.DashboardSnapshot;
import com.healthdata.fhir.admin.model.ServiceCatalog;
import com.healthdata.fhir.admin.model.SystemHealthSnapshot;

@ExtendWith(MockitoExtension.class)
@Tag("integration")
class AdminPortalControllerTest {

    @Mock
    private AdminPortalService adminPortalService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminPortalController controller = new AdminPortalController(adminPortalService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() {
        ScopedTenant.clear();
    }

    @Test
    void shouldReturnDashboard() throws Exception {
        DashboardSnapshot snapshot = new DashboardSnapshot(
                Instant.now(),
                List.of(),
                List.of()
        );
        when(adminPortalService.getDashboardSnapshot("tenant-a")).thenReturn(snapshot);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("X-Tenant-Id", "tenant-a"))
                .andExpect(status().isOk());

        verify(adminPortalService).getDashboardSnapshot(eq("tenant-a"));
    }

    @Test
    void shouldUseScopedTenantWhenHeaderMissing() throws Exception {
        ScopedTenant.setTenant("scoped-tenant");
        ServiceCatalog catalog = new ServiceCatalog(Instant.now(), List.of());
        when(adminPortalService.getServiceCatalog("scoped-tenant")).thenReturn(catalog);

        mockMvc.perform(get("/api/admin/service-catalog"))
                .andExpect(status().isOk());

        verify(adminPortalService).getServiceCatalog(eq("scoped-tenant"));
    }

    @Test
    void shouldReturnSystemHealth() throws Exception {
        SystemHealthSnapshot snapshot = new SystemHealthSnapshot(
                Instant.now(),
                List.of(),
                List.of()
        );
        when(adminPortalService.getSystemHealth("tenant-b")).thenReturn(snapshot);

        mockMvc.perform(get("/api/admin/system-health")
                        .header("X-Tenant-Id", "tenant-b"))
                .andExpect(status().isOk());

        verify(adminPortalService).getSystemHealth(eq("tenant-b"));
    }

    @Test
    void shouldReturnApiPresets() throws Exception {
        List<ApiPreset> presets = List.of(new ApiPreset(
                "read-patient",
                "Read Patient",
                "GET",
                "/fhir/Patient/123",
                java.util.Map.of(),
                java.util.Map.of(),
                null
        ));
        when(adminPortalService.getApiPresets("tenant-c")).thenReturn(presets);

        mockMvc.perform(get("/api/admin/api-presets")
                        .header("X-Tenant-Id", "tenant-c"))
                .andExpect(status().isOk());

        verify(adminPortalService).getApiPresets(eq("tenant-c"));
    }
}
