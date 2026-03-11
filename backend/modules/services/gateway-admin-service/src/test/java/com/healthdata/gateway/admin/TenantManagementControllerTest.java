package com.healthdata.gateway.admin;

import com.healthdata.authentication.controller.TenantManagementController;
import com.healthdata.authentication.domain.Tenant;
import com.healthdata.authentication.repository.TenantRepository;
import com.healthdata.authentication.service.UserManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantManagementController")
class TenantManagementControllerTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserManagementService userManagementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);

        TenantManagementController controller = new TenantManagementController(tenantRepository, userManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(converter)
            .build();
    }

    @Test
    @DisplayName("GET /api/v1/tenants should return all tenants")
    void shouldReturnAllTenants() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId("demo");
        tenant.setName("HDIM Demo");
        tenant.activate();
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(userManagementService.getUsersByTenant("demo")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tenants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value("demo"));
    }

    @Test
    @DisplayName("GET /api/v1/tenants/{id}/users should return tenant users")
    void shouldReturnTenantUsers() throws Exception {
        when(userManagementService.getUsersByTenant("demo")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tenants/demo/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }
}
