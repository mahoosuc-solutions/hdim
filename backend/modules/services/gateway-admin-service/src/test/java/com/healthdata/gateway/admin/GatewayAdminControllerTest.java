package com.healthdata.gateway.admin;

import com.healthdata.gateway.service.GatewayForwarder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for GatewayAdminController.
 * Tests route forwarding to downstream services using standalone MockMvc.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayAdminController")
class GatewayAdminControllerTest {

    @Mock
    private GatewayForwarder forwarder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GatewayAdminController controller = new GatewayAdminController(forwarder);
        ReflectionTestUtils.setField(controller, "agentBuilderUrl", "http://agent-builder:8090");
        ReflectionTestUtils.setField(controller, "agentRuntimeUrl", "http://agent-runtime:8091");
        ReflectionTestUtils.setField(controller, "salesAutomationUrl", "http://sales:8092");
        ReflectionTestUtils.setField(controller, "auditUrl", "http://audit:8093");
        ReflectionTestUtils.setField(controller, "auditBaseUrl", "http://audit-base:8094");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("should forward to agent-builder when path matches /api/v1/agent-builder/**")
    void shouldForwardToAgentBuilder_WhenPathMatched() throws Exception {
        doReturn(ResponseEntity.ok("ok"))
            .when(forwarder).forwardRequest(any(), any(), eq("http://agent-builder:8090"), eq("/api/v1/agent-builder"));

        mockMvc.perform(get("/api/v1/agent-builder/some/path"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should forward to audit service when path matches /api/v1/audit/**")
    void shouldForwardToAudit_WhenAuditPathMatched() throws Exception {
        doReturn(ResponseEntity.ok("ok"))
            .when(forwarder).forwardRequest(any(), any(), eq("http://audit:8093"), eq("/api/v1/audit"));

        mockMvc.perform(get("/api/v1/audit/logs"))
            .andExpect(status().isOk());
    }
}
