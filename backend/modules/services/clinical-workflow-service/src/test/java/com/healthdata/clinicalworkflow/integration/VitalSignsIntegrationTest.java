package com.healthdata.clinicalworkflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import com.healthdata.clinicalworkflow.domain.repository.VitalSignsRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Vital Signs Service (Tier 3 - Validation)
 *
 * Tests vital signs recording and monitoring end-to-end including:
 * - Vital signs recording with unit conversions
 * - Alert detection for critical vitals
 * - Alert acknowledgement workflow
 * - Vitals history with pagination
 * - Multi-tenant isolation
 * - Cache TTL compliance
 * - BMI calculation
 * - Audit trail verification
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("Vital Signs Integration Tests")
class VitalSignsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinical_workflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VitalSignsRecordRepository vitalsRepository;

    private static final String TENANT_ID_A = "TENANT_A";
    private static final String TENANT_ID_B = "TENANT_B";
    private static final String USER_ID = "nurse@example.com";
    private static final String PATIENT_ID = "PATIENT001";
    private static final String ENCOUNTER_ID = "ENC001";

    @BeforeEach
    void setUp() {
        vitalsRepository.deleteAll();
    }

    // ================================
    // Vital Signs Recording Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Record normal vital signs with BMI calculation")
    void testRecordNormalVitalSigns() throws Exception {
        VitalSignsRequest request = VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .measuredAt(LocalDateTime.now())
                .systolicBP(120)
                .diastolicBP(80)
                .heartRate(72)
                .respiratoryRate(16)
                .temperature(98.6)
                .temperatureUnit("F")
                .oxygenSaturation(98)
                .weight(175.5)
                .weightUnit("lbs")
                .height(68.0)
                .heightUnit("in")
                .painLevel(0)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.encounterId").value(ENCOUNTER_ID))
                .andExpect(jsonPath("$.systolicBP").value(120))
                .andExpect(jsonPath("$.diastolicBP").value(80))
                .andExpect(jsonPath("$.heartRate").value(72))
                .andExpect(jsonPath("$.bmi").exists())
                .andExpect(jsonPath("$.hasAlerts").value(false))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        VitalSignsResponse response = objectMapper.readValue(responseJson, VitalSignsResponse.class);
        UUID vitalsId = response.getId();

        // Verify entity in database
        Optional<VitalSignsRecordEntity> entityOpt = vitalsRepository.findById(vitalsId);
        assertThat(entityOpt).isPresent();
        VitalSignsRecordEntity entity = entityOpt.get();
        assertThat(entity.getTenantId()).isEqualTo(TENANT_ID_A);
        assertThat(entity.getRecordedBy()).isEqualTo(USER_ID);
        assertThat(entity.getBmi()).isNotNull();
        assertThat(entity.getBmi()).isGreaterThan(0);
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Record vitals with unit conversions: verify lbs→kg conversion in database")
    void testVitalSignsUnitConversions() throws Exception {
        double weightInLbs = 150.0;
        double expectedWeightInKg = weightInLbs * 0.453592; // 68.04 kg

        VitalSignsRequest request = VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .measuredAt(LocalDateTime.now())
                .systolicBP(120)
                .diastolicBP(80)
                .heartRate(70)
                .respiratoryRate(16)
                .temperature(98.6)
                .temperatureUnit("F")
                .oxygenSaturation(98)
                .weight(weightInLbs)
                .weightUnit("lbs")
                .height(70.0)
                .heightUnit("in")
                .painLevel(0)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        VitalSignsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), VitalSignsResponse.class);

        // Verify weight was converted to kg in database
        VitalSignsRecordEntity entity = vitalsRepository.findById(response.getId()).orElseThrow();
        assertThat(entity.getWeight()).isCloseTo(expectedWeightInKg, org.assertj.core.data.Offset.offset(0.1));
    }

    // ================================
    // Alert Detection Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Record critical vitals and verify alert is generated")
    void testAlertDetectionForCriticalVitals() throws Exception {
        // Record vitals with critically high blood pressure
        VitalSignsRequest request = VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .measuredAt(LocalDateTime.now())
                .systolicBP(180)  // Critical high
                .diastolicBP(110) // Critical high
                .heartRate(72)
                .respiratoryRate(16)
                .temperature(98.6)
                .temperatureUnit("F")
                .oxygenSaturation(98)
                .weight(175.5)
                .weightUnit("lbs")
                .height(68.0)
                .heightUnit("in")
                .painLevel(0)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasAlerts").value(true))
                .andReturn();

        VitalSignsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), VitalSignsResponse.class);

        // Verify alert in database
        VitalSignsRecordEntity entity = vitalsRepository.findById(response.getId()).orElseThrow();
        assertThat(entity.isHasAlerts()).isTrue();
        assertThat(entity.getAlertSeverity()).isNotNull();
        assertThat(entity.getAlertMessage()).isNotNull();
        assertThat(entity.isAlertAcknowledged()).isFalse();
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Verify multiple alert types (BP, temp, O2 sat)")
    void testMultipleAlertTypes() throws Exception {
        // Test high BP alert
        VitalSignsRequest highBpRequest = VitalSignsRequest.builder()
                .patientId("PATIENT_BP")
                .encounterId("ENC_BP")
                .measuredAt(LocalDateTime.now())
                .systolicBP(190)
                .diastolicBP(120)
                .heartRate(75)
                .respiratoryRate(16)
                .temperature(98.6)
                .oxygenSaturation(98)
                .build();

        mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highBpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasAlerts").value(true));

        // Test low O2 saturation alert
        VitalSignsRequest lowO2Request = VitalSignsRequest.builder()
                .patientId("PATIENT_O2")
                .encounterId("ENC_O2")
                .measuredAt(LocalDateTime.now())
                .systolicBP(120)
                .diastolicBP(80)
                .heartRate(75)
                .respiratoryRate(16)
                .temperature(98.6)
                .oxygenSaturation(88) // Critical low
                .build();

        mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lowO2Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasAlerts").value(true));

        // Test high temperature alert
        VitalSignsRequest highTempRequest = VitalSignsRequest.builder()
                .patientId("PATIENT_TEMP")
                .encounterId("ENC_TEMP")
                .measuredAt(LocalDateTime.now())
                .systolicBP(120)
                .diastolicBP(80)
                .heartRate(75)
                .respiratoryRate(16)
                .temperature(103.5) // High fever
                .temperatureUnit("F")
                .oxygenSaturation(98)
                .build();

        mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(highTempRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasAlerts").value(true));

        // Verify all 3 alerts exist
        List<VitalSignsRecordEntity> alerts = vitalsRepository.findByTenantIdAndHasAlertsTrue(TENANT_ID_A);
        assertThat(alerts).hasSize(3);
    }

    // ================================
    // Alert Acknowledgement Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Alert acknowledgement workflow: record → acknowledge → verify fields")
    void testAlertAcknowledgement() throws Exception {
        // Step 1: Record critical vitals
        VitalSignsRequest request = VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .measuredAt(LocalDateTime.now())
                .systolicBP(200)
                .diastolicBP(120)
                .heartRate(72)
                .respiratoryRate(16)
                .temperature(98.6)
                .oxygenSaturation(98)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasAlerts").value(true))
                .andReturn();

        VitalSignsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), VitalSignsResponse.class);
        UUID vitalsId = response.getId();

        // Verify alert not yet acknowledged
        VitalSignsRecordEntity entity = vitalsRepository.findById(vitalsId).orElseThrow();
        assertThat(entity.isAlertAcknowledged()).isFalse();
        assertThat(entity.getAlertAcknowledgedBy()).isNull();
        assertThat(entity.getAlertAcknowledgedAt()).isNull();

        // Step 2: Acknowledge alert
        String acknowledgerUserId = "doctor@example.com";
        mockMvc.perform(post("/api/v1/vitals/{id}/acknowledge", vitalsId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", acknowledgerUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(true))
                .andExpect(jsonPath("$.acknowledgedBy").value(acknowledgerUserId))
                .andExpect(jsonPath("$.acknowledgedAt").exists());

        // Step 3: Verify acknowledgement fields in database
        entity = vitalsRepository.findById(vitalsId).orElseThrow();
        assertThat(entity.isAlertAcknowledged()).isTrue();
        assertThat(entity.getAlertAcknowledgedBy()).isEqualTo(acknowledgerUserId);
        assertThat(entity.getAlertAcknowledgedAt()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Get unacknowledged alerts only")
    void testGetUnacknowledgedAlerts() throws Exception {
        // Create 2 critical vitals
        for (int i = 1; i <= 2; i++) {
            VitalSignsRequest request = VitalSignsRequest.builder()
                    .patientId("PATIENT00" + i)
                    .encounterId("ENC00" + i)
                    .measuredAt(LocalDateTime.now())
                    .systolicBP(190)
                    .diastolicBP(115)
                    .heartRate(72)
                    .respiratoryRate(16)
                    .temperature(98.6)
                    .oxygenSaturation(98)
                    .build();

            mockMvc.perform(post("/api/v1/vitals")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Acknowledge first alert
        List<VitalSignsRecordEntity> alerts = vitalsRepository.findByTenantIdAndHasAlertsTrue(TENANT_ID_A);
        UUID firstAlertId = alerts.get(0).getId();

        mockMvc.perform(post("/api/v1/vitals/{id}/acknowledge", firstAlertId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID))
                .andExpect(status().isOk());

        // Get unacknowledged alerts only (default)
        mockMvc.perform(get("/api/v1/vitals/alerts")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .param("includeAcknowledged", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Get all alerts including acknowledged
        mockMvc.perform(get("/api/v1/vitals/alerts")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .param("includeAcknowledged", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ================================
    // Vitals History and Pagination Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Get vitals history with pagination")
    void testVitalsHistoryWithPagination() throws Exception {
        // Create 10 vital sign records
        for (int i = 1; i <= 10; i++) {
            VitalSignsRequest request = VitalSignsRequest.builder()
                    .patientId(PATIENT_ID)
                    .encounterId(ENCOUNTER_ID + "_" + i)
                    .measuredAt(LocalDateTime.now().minusHours(i))
                    .systolicBP(120 + i)
                    .diastolicBP(80 + i)
                    .heartRate(70 + i)
                    .respiratoryRate(16)
                    .temperature(98.6)
                    .oxygenSaturation(98)
                    .build();

            mockMvc.perform(post("/api/v1/vitals")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get page 0 (first 5 records)
        mockMvc.perform(get("/api/v1/vitals/patient/{patientId}/history", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vitals", hasSize(5)))
                .andExpect(jsonPath("$.totalCount").value(10))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(5));

        // Get page 1 (next 5 records)
        mockMvc.perform(get("/api/v1/vitals/patient/{patientId}/history", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vitals", hasSize(5)))
                .andExpect(jsonPath("$.totalCount").value(10))
                .andExpect(jsonPath("$.currentPage").value(1));
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Get latest vitals for patient")
    void testGetLatestVitals() throws Exception {
        // Create 3 vital sign records
        for (int i = 3; i >= 1; i--) {
            VitalSignsRequest request = VitalSignsRequest.builder()
                    .patientId(PATIENT_ID)
                    .encounterId(ENCOUNTER_ID + "_" + i)
                    .measuredAt(LocalDateTime.now().minusHours(i))
                    .systolicBP(120)
                    .diastolicBP(80)
                    .heartRate(70 + i)
                    .respiratoryRate(16)
                    .temperature(98.6)
                    .oxygenSaturation(98)
                    .build();

            mockMvc.perform(post("/api/v1/vitals")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get latest vitals (should be most recent one, HR = 71)
        mockMvc.perform(get("/api/v1/vitals/patient/{patientId}/latest", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.heartRate").value(71));
    }

    // ================================
    // Multi-Tenant Isolation Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Multi-tenant isolation: tenant A cannot access tenant B's vitals")
    void testMultiTenantIsolation() throws Exception {
        // Create vitals in tenant A
        VitalSignsRequest request = VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .measuredAt(LocalDateTime.now())
                .systolicBP(120)
                .diastolicBP(80)
                .heartRate(72)
                .respiratoryRate(16)
                .temperature(98.6)
                .oxygenSaturation(98)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        VitalSignsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), VitalSignsResponse.class);
        UUID vitalsId = response.getId();

        // Try to access from tenant B - should fail
        mockMvc.perform(get("/api/v1/vitals/{id}", vitalsId)
                        .header("X-Tenant-ID", TENANT_ID_B))
                .andExpect(status().isNotFound());

        // Verify tenant A can still access
        mockMvc.perform(get("/api/v1/vitals/{id}", vitalsId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vitalsId.toString()));
    }

    // ================================
    // Cache TTL Compliance Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Verify Cache-Control headers for HIPAA compliance (PHI data)")
    void testCacheControlHeaders() throws Exception {
        // Create vitals
        VitalSignsRequest request = VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .measuredAt(LocalDateTime.now())
                .systolicBP(120)
                .diastolicBP(80)
                .heartRate(72)
                .respiratoryRate(16)
                .temperature(98.6)
                .oxygenSaturation(98)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        VitalSignsResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), VitalSignsResponse.class);

        // Verify Cache-Control headers on GET
        mockMvc.perform(get("/api/v1/vitals/{id}", response.getId())
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"));
    }

    // ================================
    // Error Scenario Tests
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Error: Invalid request - missing required fields")
    void testInvalidRequest() throws Exception {
        // Missing required fields
        VitalSignsRequest invalidRequest = VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .build();

        mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Error: Non-existent vitals record (404)")
    void testNonExistentVitalsRecord() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/vitals/{id}", nonExistentId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isNotFound());
    }

    // ================================
    // Additional Test Scenarios
    // ================================

    @Test
    @WithMockUser(roles = "NURSE")
    @DisplayName("Get critical alerts only")
    void testGetCriticalAlertsOnly() throws Exception {
        // Create normal vitals (no alert)
        VitalSignsRequest normalRequest = VitalSignsRequest.builder()
                .patientId("PATIENT_NORMAL")
                .encounterId("ENC_NORMAL")
                .measuredAt(LocalDateTime.now())
                .systolicBP(120)
                .diastolicBP(80)
                .heartRate(72)
                .respiratoryRate(16)
                .temperature(98.6)
                .oxygenSaturation(98)
                .build();

        mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(normalRequest)))
                .andExpect(status().isCreated());

        // Create critical vitals
        VitalSignsRequest criticalRequest = VitalSignsRequest.builder()
                .patientId("PATIENT_CRITICAL")
                .encounterId("ENC_CRITICAL")
                .measuredAt(LocalDateTime.now())
                .systolicBP(220)
                .diastolicBP(130)
                .heartRate(72)
                .respiratoryRate(16)
                .temperature(98.6)
                .oxygenSaturation(85) // Critical low
                .build();

        mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criticalRequest)))
                .andExpect(status().isCreated());

        // Get critical alerts only
        mockMvc.perform(get("/api/v1/vitals/alerts/critical")
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(roles = "MEDICAL_ASSISTANT")
    @DisplayName("Verify role-based access control")
    void testRoleBasedAccessControl() throws Exception {
        // MEDICAL_ASSISTANT role can record vitals
        VitalSignsRequest request = VitalSignsRequest.builder()
                .patientId(PATIENT_ID)
                .encounterId(ENCOUNTER_ID)
                .measuredAt(LocalDateTime.now())
                .systolicBP(120)
                .diastolicBP(80)
                .heartRate(72)
                .respiratoryRate(16)
                .temperature(98.6)
                .oxygenSaturation(98)
                .build();

        mockMvc.perform(post("/api/v1/vitals")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
