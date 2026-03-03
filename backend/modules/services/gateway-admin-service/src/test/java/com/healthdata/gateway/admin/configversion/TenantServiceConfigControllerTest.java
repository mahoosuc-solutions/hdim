package com.healthdata.gateway.admin.configversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantServiceConfigController")
class TenantServiceConfigControllerTest {

    @Mock
    private TenantServiceConfigService configService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TenantServiceConfigController controller = new TenantServiceConfigController(configService, objectMapper);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(converter)
            .build();
    }

    private TenantServiceConfigVersion createVersion(UUID id) {
        TenantServiceConfigVersion v = new TenantServiceConfigVersion();
        v.setId(id);
        v.setTenantId("demo-tenant");
        v.setServiceName("test-service");
        v.setVersionNumber(1);
        v.setStatus(TenantServiceConfigVersion.Status.DRAFT);
        v.setConfigJson("{\"timeout\":5000}");
        v.setConfigHash("abc123");
        v.setCreatedBy("admin@test.com");
        return v;
    }

    private TenantServiceConfigApproval createApproval(UUID id) {
        TenantServiceConfigApproval a = new TenantServiceConfigApproval();
        a.setId(id);
        a.setTenantId("demo-tenant");
        a.setServiceName("test-service");
        a.setVersionId(UUID.randomUUID());
        a.setAction(TenantServiceConfigApproval.Action.REQUESTED);
        a.setActor("admin@test.com");
        a.setComment("Please review");
        return a;
    }

    @Test
    @DisplayName("should return 200 when createVersion succeeds")
    void shouldReturnOk_WhenCreateVersionSucceeds() throws Exception {
        UUID versionId = UUID.randomUUID();
        TenantServiceConfigVersion version = createVersion(versionId);
        when(configService.createVersion(anyString(), anyString(), any(), anyString(), anyBoolean(), anyString()))
            .thenReturn(version);

        mockMvc.perform(post("/api/v1/configs/{service}/tenants/{tenantId}/versions", "test-service", "demo-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"config\":{\"timeout\":5000},\"changeSummary\":\"Initial config\",\"activate\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(versionId.toString()))
            .andExpect(jsonPath("$.tenantId").value("demo-tenant"))
            .andExpect(jsonPath("$.serviceName").value("test-service"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("should return 200 when promoteVersion succeeds")
    void shouldReturnOk_WhenPromoteVersionSucceeds() throws Exception {
        UUID versionId = UUID.randomUUID();
        TenantServiceConfigVersion version = createVersion(versionId);
        when(configService.promoteFromDemo(anyString(), anyString(), any(), anyString(), anyBoolean(), anyString()))
            .thenReturn(version);

        UUID sourceId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/configs/{service}/tenants/{tenantId}/promote", "test-service", "demo-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceVersionId\":\"" + sourceId + "\",\"changeSummary\":\"Promote from demo\",\"activate\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(versionId.toString()));
    }

    @Test
    @DisplayName("should return 200 when activateVersion succeeds")
    void shouldReturnOk_WhenActivateVersionSucceeds() throws Exception {
        UUID versionId = UUID.randomUUID();
        TenantServiceConfigVersion version = createVersion(versionId);
        version.setStatus(TenantServiceConfigVersion.Status.ACTIVE);
        when(configService.activateVersion(anyString(), anyString(), any(), anyString(), anyString()))
            .thenReturn(version);

        mockMvc.perform(post("/api/v1/configs/{service}/tenants/{tenantId}/activate/{versionId}",
                    "test-service", "demo-tenant", versionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Ready for production\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(versionId.toString()))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("should return 404 when getCurrent returns empty")
    void shouldReturn404_WhenGetCurrentReturnsEmpty() throws Exception {
        when(configService.getCurrentVersion(anyString(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/configs/{service}/tenants/{tenantId}/current", "test-service", "demo-tenant"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should return 200 when listVersions called")
    void shouldReturnOk_WhenListVersionsCalled() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(configService.listVersions(anyString(), anyString()))
            .thenReturn(List.of(createVersion(id1), createVersion(id2)));

        mockMvc.perform(get("/api/v1/configs/{service}/tenants/{tenantId}/versions", "test-service", "demo-tenant"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(id1.toString()))
            .andExpect(jsonPath("$[1].id").value(id2.toString()));
    }

    @Test
    @DisplayName("should return 200 when requestApproval called")
    void shouldReturnOk_WhenRequestApprovalCalled() throws Exception {
        UUID approvalId = UUID.randomUUID();
        TenantServiceConfigApproval approval = createApproval(approvalId);
        when(configService.requestApproval(anyString(), anyString(), any(), anyString(), any()))
            .thenReturn(approval);

        UUID versionId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/configs/{service}/tenants/{tenantId}/versions/{versionId}/approvals/request",
                    "test-service", "demo-tenant", versionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Please review\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(approvalId.toString()))
            .andExpect(jsonPath("$.action").value("REQUESTED"))
            .andExpect(jsonPath("$.actor").value("admin@test.com"))
            .andExpect(jsonPath("$.comment").value("Please review"));
    }
}
