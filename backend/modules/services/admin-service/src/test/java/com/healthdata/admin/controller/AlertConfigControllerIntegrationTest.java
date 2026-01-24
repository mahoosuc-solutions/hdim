package com.healthdata.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.admin.domain.AlertConfig;
import com.healthdata.admin.dto.AlertConfigRequest;
import com.healthdata.admin.dto.AlertConfigUpdateRequest;
import com.healthdata.admin.repository.AlertConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AlertConfigController.
 *
 * Tests full REST API flow with in-memory database.
 * Validates multi-tenant isolation, role-based access control, and CRUD operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AlertConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertConfigRepository alertConfigRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT_ID = "other-tenant";
    private static final String TENANT_HEADER = "X-Tenant-ID";

    private AlertConfig testAlertConfig;

    @BeforeEach
    void setUp() {
        alertConfigRepository.deleteAll();

        testAlertConfig = AlertConfig.builder()
                .tenantId(TENANT_ID)
                .serviceName("patient-service")
                .displayName("Patient Service CPU Alert")
                .alertType(AlertConfig.AlertType.CPU_USAGE)
                .threshold(80.0)
                .durationMinutes(5)
                .severity(AlertConfig.AlertSeverity.WARNING)
                .enabled(true)
                .notificationChannels(Arrays.asList(
                        AlertConfig.NotificationChannel.EMAIL,
                        AlertConfig.NotificationChannel.SLACK
                ))
                .createdBy("test-user")
                .build();

        testAlertConfig = alertConfigRepository.save(testAlertConfig);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAlertConfigs_shouldReturnAllConfigsForTenant() throws Exception {
        mockMvc.perform(get("/api/v1/admin/alerts/configs")
                        .header(TENANT_HEADER, TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testAlertConfig.getId().toString()))
                .andExpect(jsonPath("$[0].serviceName").value("patient-service"))
                .andExpect(jsonPath("$[0].alertType").value("CPU_USAGE"))
                .andExpect(jsonPath("$[0].threshold").value(80.0))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllAlertConfigs_shouldEnforceMultiTenantIsolation() throws Exception {
        // Create alert for different tenant
        AlertConfig otherTenantAlert = AlertConfig.builder()
                .tenantId(OTHER_TENANT_ID)
                .serviceName("care-gap-service")
                .displayName("Care Gap Service Memory Alert")
                .alertType(AlertConfig.AlertType.MEMORY_USAGE)
                .threshold(85.0)
                .durationMinutes(10)
                .severity(AlertConfig.AlertSeverity.CRITICAL)
                .enabled(true)
                .notificationChannels(List.of(AlertConfig.NotificationChannel.EMAIL))
                .createdBy("other-user")
                .build();
        alertConfigRepository.save(otherTenantAlert);

        // Request should only return alerts for TENANT_ID
        mockMvc.perform(get("/api/v1/admin/alerts/configs")
                        .header(TENANT_HEADER, TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testAlertConfig.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAlertConfig_shouldReturnSpecificConfig() throws Exception {
        mockMvc.perform(get("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAlertConfig.getId().toString()))
                .andExpect(jsonPath("$.serviceName").value("patient-service"))
                .andExpect(jsonPath("$.displayName").value("Patient Service CPU Alert"))
                .andExpect(jsonPath("$.alertType").value("CPU_USAGE"))
                .andExpect(jsonPath("$.threshold").value(80.0))
                .andExpect(jsonPath("$.durationMinutes").value(5))
                .andExpect(jsonPath("$.severity").value("WARNING"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.notificationChannels", hasSize(2)))
                .andExpect(jsonPath("$.notificationChannels", containsInAnyOrder("EMAIL", "SLACK")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAlertConfig_shouldReturn404WhenNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/admin/alerts/configs/{id}", nonExistentId)
                        .header(TENANT_HEADER, TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAlertConfig_shouldEnforceMultiTenantIsolation() throws Exception {
        // Try to access alert from different tenant
        mockMvc.perform(get("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, OTHER_TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAlertConfig_shouldCreateNewConfig() throws Exception {
        AlertConfigRequest request = AlertConfigRequest.builder()
                .serviceName("care-gap-service")
                .displayName("Care Gap Service Error Rate Alert")
                .alertType(AlertConfig.AlertType.ERROR_RATE)
                .threshold(5.0)
                .durationMinutes(3)
                .severity(AlertConfig.AlertSeverity.CRITICAL)
                .enabled(true)
                .notificationChannels(Arrays.asList(
                        AlertConfig.NotificationChannel.EMAIL,
                        AlertConfig.NotificationChannel.WEBHOOK
                ))
                .build();

        mockMvc.perform(post("/api/v1/admin/alerts/configs")
                        .header(TENANT_HEADER, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.serviceName").value("care-gap-service"))
                .andExpect(jsonPath("$.displayName").value("Care Gap Service Error Rate Alert"))
                .andExpect(jsonPath("$.alertType").value("ERROR_RATE"))
                .andExpect(jsonPath("$.threshold").value(5.0))
                .andExpect(jsonPath("$.durationMinutes").value(3))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.notificationChannels", hasSize(2)))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createAlertConfig_shouldValidateRequiredFields() throws Exception {
        AlertConfigRequest invalidRequest = AlertConfigRequest.builder()
                .serviceName("") // Invalid: empty
                .displayName("Test")
                .alertType(AlertConfig.AlertType.CPU_USAGE)
                .threshold(null) // Invalid: null
                .durationMinutes(0) // Invalid: less than 1
                .severity(AlertConfig.AlertSeverity.WARNING)
                .enabled(true)
                .notificationChannels(List.of()) // Invalid: empty
                .build();

        mockMvc.perform(post("/api/v1/admin/alerts/configs")
                        .header(TENANT_HEADER, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAlertConfig_shouldUpdateExistingConfig() throws Exception {
        AlertConfigUpdateRequest updateRequest = AlertConfigUpdateRequest.builder()
                .threshold(90.0)
                .severity(AlertConfig.AlertSeverity.CRITICAL)
                .enabled(false)
                .build();

        mockMvc.perform(put("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAlertConfig.getId().toString()))
                .andExpect(jsonPath("$.threshold").value(90.0))
                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.serviceName").value("patient-service")); // Unchanged
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAlertConfig_shouldOnlyUpdateProvidedFields() throws Exception {
        AlertConfigUpdateRequest partialUpdate = AlertConfigUpdateRequest.builder()
                .threshold(95.0)
                .build();

        mockMvc.perform(put("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threshold").value(95.0))
                .andExpect(jsonPath("$.severity").value("WARNING")) // Unchanged
                .andExpect(jsonPath("$.enabled").value(true)); // Unchanged
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAlertConfig_shouldEnforceMultiTenantIsolation() throws Exception {
        AlertConfigUpdateRequest updateRequest = AlertConfigUpdateRequest.builder()
                .threshold(95.0)
                .build();

        mockMvc.perform(put("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, OTHER_TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAlertConfig_shouldDeleteExistingConfig() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, TENANT_ID))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteAlertConfig_shouldEnforceMultiTenantIsolation() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, OTHER_TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void createAlertConfig_shouldDenyAccessForNonAdminRoles() throws Exception {
        AlertConfigRequest request = AlertConfigRequest.builder()
                .serviceName("test-service")
                .displayName("Test Alert")
                .alertType(AlertConfig.AlertType.CPU_USAGE)
                .threshold(80.0)
                .durationMinutes(5)
                .severity(AlertConfig.AlertSeverity.WARNING)
                .enabled(true)
                .notificationChannels(List.of(AlertConfig.NotificationChannel.EMAIL))
                .build();

        mockMvc.perform(post("/api/v1/admin/alerts/configs")
                        .header(TENANT_HEADER, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createAlertConfig_shouldAllowSuperAdminAccess() throws Exception {
        AlertConfigRequest request = AlertConfigRequest.builder()
                .serviceName("test-service")
                .displayName("Test Alert")
                .alertType(AlertConfig.AlertType.CPU_USAGE)
                .threshold(80.0)
                .durationMinutes(5)
                .severity(AlertConfig.AlertSeverity.WARNING)
                .enabled(true)
                .notificationChannels(List.of(AlertConfig.NotificationChannel.EMAIL))
                .build();

        mockMvc.perform(post("/api/v1/admin/alerts/configs")
                        .header(TENANT_HEADER, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateAlertConfig_shouldCascadeUpdateToNotificationChannels() throws Exception {
        AlertConfigUpdateRequest updateRequest = AlertConfigUpdateRequest.builder()
                .notificationChannels(Arrays.asList(
                        AlertConfig.NotificationChannel.EMAIL,
                        AlertConfig.NotificationChannel.SMS,
                        AlertConfig.NotificationChannel.WEBHOOK
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/alerts/configs/{id}", testAlertConfig.getId())
                        .header(TENANT_HEADER, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationChannels", hasSize(3)))
                .andExpect(jsonPath("$.notificationChannels", containsInAnyOrder("EMAIL", "SMS", "WEBHOOK")));
    }
}
