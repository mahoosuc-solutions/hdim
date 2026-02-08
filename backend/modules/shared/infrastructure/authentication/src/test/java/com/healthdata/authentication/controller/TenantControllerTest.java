package com.healthdata.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.authentication.domain.TenantStatus;
import com.healthdata.authentication.domain.UserRole;
import com.healthdata.authentication.dto.TenantRegistrationRequest;
import com.healthdata.authentication.dto.TenantRegistrationResponse;
import com.healthdata.authentication.exception.HdimGlobalExceptionHandler;
import com.healthdata.authentication.exception.TenantAlreadyExistsException;
import com.healthdata.authentication.exception.UserAlreadyExistsException;
import com.healthdata.authentication.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TenantController.
 * Tests REST endpoint behavior, request validation, and error handling.
 * Uses standalone MockMvc setup to test controller in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantController Tests")
class TenantControllerTest {

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TenantController tenantController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private TenantRegistrationRequest validRequest;
    private TenantRegistrationResponse mockResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();  // For Java 8 time support

        // Set up standalone MockMvc with explicit JSON converter and exception handler
        mockMvc = MockMvcBuilders.standaloneSetup(tenantController)
            .setControllerAdvice(new HdimGlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();

        // Valid registration request
        validRequest = TenantRegistrationRequest.builder()
            .tenantId("test-clinic")
            .tenantName("Test Clinic")
            .adminUser(TenantRegistrationRequest.AdminUserRequest.builder()
                .username("admin@test-clinic")
                .email("admin@test-clinic.com")
                .password("Test2026!")
                .firstName("John")
                .lastName("Doe")
                .build())
            .build();

        // Mock response
        mockResponse = TenantRegistrationResponse.builder()
            .tenantId("test-clinic")
            .tenantName("Test Clinic")
            .status(TenantStatus.ACTIVE)
            .createdAt(Instant.now())
            .adminUser(TenantRegistrationResponse.AdminUserInfo.builder()
                .userId(UUID.randomUUID())
                .username("admin@test-clinic")
                .email("admin@test-clinic.com")
                .roles(Set.of(UserRole.ADMIN))
                .tenantIds(Set.of("test-clinic"))
                .build())
            .build();
    }

    @Nested
    @DisplayName("POST /api/v1/tenants/register - Tenant Registration Tests")
    class TenantRegistrationEndpointTests {

        @Test
        
        @DisplayName("Should register new tenant with valid request")
        void shouldRegisterNewTenant() throws Exception {
            // Given
            when(tenantService.registerTenant(any(TenantRegistrationRequest.class)))
                .thenReturn(mockResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tenantId").value("test-clinic"))
                .andExpect(jsonPath("$.tenantName").value("Test Clinic"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.adminUser.username").value("admin@test-clinic"))
                .andExpect(jsonPath("$.adminUser.email").value("admin@test-clinic.com"))
                .andExpect(jsonPath("$.adminUser.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.adminUser.tenantIds[0]").value("test-clinic"));

            verify(tenantService).registerTenant(any(TenantRegistrationRequest.class));
        }

        @Test
        
        @DisplayName("Should return 409 Conflict when tenant already exists")
        void shouldReturn409WhenTenantExists() throws Exception {
            // Given
            when(tenantService.registerTenant(any(TenantRegistrationRequest.class)))
                .thenThrow(new TenantAlreadyExistsException("test-clinic"));

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

            verify(tenantService).registerTenant(any(TenantRegistrationRequest.class));
        }

        @Test
        
        @DisplayName("Should return 409 Conflict when username already exists")
        void shouldReturn409WhenUsernameExists() throws Exception {
            // Given
            when(tenantService.registerTenant(any(TenantRegistrationRequest.class)))
                .thenThrow(UserAlreadyExistsException.forUsername("admin@test-clinic"));

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

            verify(tenantService).registerTenant(any(TenantRegistrationRequest.class));
        }

        @Test
        
        @DisplayName("Should return 409 Conflict when email already exists")
        void shouldReturn409WhenEmailExists() throws Exception {
            // Given
            when(tenantService.registerTenant(any(TenantRegistrationRequest.class)))
                .thenThrow(UserAlreadyExistsException.forEmail("admin@test-clinic.com"));

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

            verify(tenantService).registerTenant(any(TenantRegistrationRequest.class));
        }

        @Test
        
        @DisplayName("Should return 500 Internal Server Error on unexpected exception")
        void shouldReturn500OnUnexpectedException() throws Exception {
            // Given
            when(tenantService.registerTenant(any(TenantRegistrationRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));

            verify(tenantService).registerTenant(any(TenantRegistrationRequest.class));
        }

        @Test
        
        @DisplayName("Should return 400 Bad Request when tenant ID is missing")
        void shouldReturn400WhenTenantIdMissing() throws Exception {
            // Given
            TenantRegistrationRequest invalidRequest = TenantRegistrationRequest.builder()
                .tenantId(null)  // Missing required field
                .tenantName("Test Clinic")
                .adminUser(validRequest.getAdminUser())
                .build();

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

            verify(tenantService, never()).registerTenant(any(TenantRegistrationRequest.class));
        }

        @Test
        
        @DisplayName("Should return 400 Bad Request when tenant name is missing")
        void shouldReturn400WhenTenantNameMissing() throws Exception {
            // Given
            TenantRegistrationRequest invalidRequest = TenantRegistrationRequest.builder()
                .tenantId("test-clinic")
                .tenantName(null)  // Missing required field
                .adminUser(validRequest.getAdminUser())
                .build();

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

            verify(tenantService, never()).registerTenant(any(TenantRegistrationRequest.class));
        }

        @Test
        
        @DisplayName("Should return 400 Bad Request when admin user is missing")
        void shouldReturn400WhenAdminUserMissing() throws Exception {
            // Given
            TenantRegistrationRequest invalidRequest = TenantRegistrationRequest.builder()
                .tenantId("test-clinic")
                .tenantName("Test Clinic")
                .adminUser(null)  // Missing required field
                .build();

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

            verify(tenantService, never()).registerTenant(any(TenantRegistrationRequest.class));
        }

        @Test
        
        @DisplayName("Should return 400 Bad Request when email is invalid format")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // Given
            TenantRegistrationRequest invalidRequest = TenantRegistrationRequest.builder()
                .tenantId("test-clinic")
                .tenantName("Test Clinic")
                .adminUser(TenantRegistrationRequest.AdminUserRequest.builder()
                    .username("admin@test-clinic")
                    .email("invalid-email")  // Invalid email format
                    .password("Test2026!")
                    .firstName("John")
                    .lastName("Doe")
                    .build())
                .build();

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/register")
                    
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

            verify(tenantService, never()).registerTenant(any(TenantRegistrationRequest.class));
        }

    }

    @Nested
    @DisplayName("POST /api/v1/tenants/{tenantId}/activate - Tenant Activation Tests")
    class TenantActivationEndpointTests {

        @Test
        
        @DisplayName("Should activate tenant when user has SUPER_ADMIN role")
        void shouldActivateTenantWithSuperAdmin() throws Exception {
            // Given
            doNothing().when(tenantService).activateTenant("test-clinic");

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/test-clinic/activate")
                    )
                .andExpect(status().isNoContent());

            verify(tenantService).activateTenant("test-clinic");
        }

        @Test

        @DisplayName("Should return 400 Bad Request when tenant not found")
        void shouldReturn400WhenTenantNotFound() throws Exception {
            // Given
            doThrow(new IllegalArgumentException("Tenant not found: nonexistent"))
                .when(tenantService).activateTenant("nonexistent");

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/nonexistent/activate")
                    )
                .andExpect(status().isBadRequest());

            verify(tenantService).activateTenant("nonexistent");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tenants/{tenantId}/suspend - Tenant Suspension Tests")
    class TenantSuspensionEndpointTests {

        @Test
        
        @DisplayName("Should suspend tenant when user has SUPER_ADMIN role")
        void shouldSuspendTenantWithSuperAdmin() throws Exception {
            // Given
            doNothing().when(tenantService).suspendTenant("test-clinic");

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/test-clinic/suspend")
                    )
                .andExpect(status().isNoContent());

            verify(tenantService).suspendTenant("test-clinic");
        }

        @Test

        @DisplayName("Should return 400 Bad Request when tenant not found")
        void shouldReturn400WhenTenantNotFound() throws Exception {
            // Given
            doThrow(new IllegalArgumentException("Tenant not found: nonexistent"))
                .when(tenantService).suspendTenant("nonexistent");

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/nonexistent/suspend")
                    )
                .andExpect(status().isBadRequest());

            verify(tenantService).suspendTenant("nonexistent");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tenants/{tenantId}/deactivate - Tenant Deactivation Tests")
    class TenantDeactivationEndpointTests {

        @Test
        
        @DisplayName("Should deactivate tenant when user has SUPER_ADMIN role")
        void shouldDeactivateTenantWithSuperAdmin() throws Exception {
            // Given
            doNothing().when(tenantService).deactivateTenant("test-clinic");

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/test-clinic/deactivate")
                    )
                .andExpect(status().isNoContent());

            verify(tenantService).deactivateTenant("test-clinic");
        }

        @Test

        @DisplayName("Should return 400 Bad Request when tenant not found")
        void shouldReturn400WhenTenantNotFound() throws Exception {
            // Given
            doThrow(new IllegalArgumentException("Tenant not found: nonexistent"))
                .when(tenantService).deactivateTenant("nonexistent");

            // When/Then
            mockMvc.perform(post("/api/v1/tenants/nonexistent/deactivate")
                    )
                .andExpect(status().isBadRequest());

            verify(tenantService).deactivateTenant("nonexistent");
        }
    }
}
