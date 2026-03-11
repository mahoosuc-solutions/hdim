package com.healthdata.gateway.admin;

import com.healthdata.authentication.controller.UserManagementController;
import com.healthdata.authentication.domain.User;
import com.healthdata.authentication.domain.UserRole;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserManagementController")
class UserManagementControllerTest {

    @Mock private UserManagementService userManagementService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);

        UserManagementController controller = new UserManagementController(userManagementService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(converter)
            .build();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("test_user");
        testUser.setEmail("test@hdim.local");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActive(true);
        testUser.setRoles(Set.of(UserRole.VIEWER));
        testUser.setTenantIds(Set.of("demo"));
    }

    @Test
    @DisplayName("GET /api/v1/users should return all users")
    void shouldReturnAllUsers() throws Exception {
        when(userManagementService.getAllUsers()).thenReturn(List.of(testUser));

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].username").value("test_user"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return user by id")
    void shouldReturnUserById() throws Exception {
        when(userManagementService.getUser(testUser.getId())).thenReturn(testUser);

        mockMvc.perform(get("/api/v1/users/{id}", testUser.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("test_user"));
    }

    @Test
    @DisplayName("POST /api/v1/users/{id}/deactivate should deactivate user")
    void shouldDeactivateUser() throws Exception {
        testUser.setActive(false);
        when(userManagementService.deactivateUser(eq(testUser.getId()), any())).thenReturn(testUser);

        mockMvc.perform(post("/api/v1/users/{id}/deactivate", testUser.getId())
                .header("X-Auth-User-ID", UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/users/{id}/reset-password should return temp password")
    void shouldResetPassword() throws Exception {
        when(userManagementService.resetPassword(testUser.getId())).thenReturn("TempPass123!");

        mockMvc.perform(post("/api/v1/users/{id}/reset-password", testUser.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.temporaryPassword").value("TempPass123!"));
    }
}
