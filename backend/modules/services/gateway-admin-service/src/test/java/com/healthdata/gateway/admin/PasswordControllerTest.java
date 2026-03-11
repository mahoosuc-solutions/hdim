package com.healthdata.gateway.admin;

import com.healthdata.authentication.controller.PasswordController;
import com.healthdata.authentication.dto.ChangePasswordRequest;
import com.healthdata.authentication.dto.ForceChangePasswordRequest;
import com.healthdata.authentication.service.UserManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordController")
class PasswordControllerTest {

    @Mock private UserManagementService userManagementService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PasswordController controller = new PasswordController(userManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/change should change password")
    void shouldChangePassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldpass123");
        request.setNewPassword("newpass456");

        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/auth/password/change")
                .header("X-Auth-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(userManagementService).changePassword(userId, "oldpass123", "newpass456");
    }

    @Test
    @DisplayName("POST /api/v1/auth/password/force-change should force change password")
    void shouldForceChangePassword() throws Exception {
        ForceChangePasswordRequest request = new ForceChangePasswordRequest();
        request.setNewPassword("newpass456");

        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/auth/password/force-change")
                .header("X-Auth-User-ID", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        verify(userManagementService).forceChangePassword(userId, "newpass456");
    }
}
