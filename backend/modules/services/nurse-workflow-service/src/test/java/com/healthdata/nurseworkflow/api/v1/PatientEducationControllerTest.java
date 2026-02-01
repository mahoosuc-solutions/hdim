package com.healthdata.nurseworkflow.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.nurseworkflow.application.PatientEducationService;
import com.healthdata.nurseworkflow.domain.model.PatientEducationLogEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PatientEducationController
 *
 * Tests REST endpoint functionality including:
 * - Logging education delivery
 * - Retrieving patient education history
 * - Filtering by material type and delivery method
 * - Finding sessions with poor understanding
 * - Managing education metrics
 */
@WebMvcTest(PatientEducationController.class)
@DisplayName("PatientEducationController")
class PatientEducationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientEducationService patientEducationService;

    private String tenantId;
    private UUID patientId;
    private UUID educatorId;
    private PatientEducationLogEntity testEducationLog;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT001";
        patientId = UUID.randomUUID();
        educatorId = UUID.randomUUID();

        testEducationLog = PatientEducationLogEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .educatorId(educatorId)
            .materialType(PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT)
            .deliveryMethod(PatientEducationLogEntity.DeliveryMethod.IN_PERSON)
            .patientUnderstanding(PatientEducationLogEntity.PatientUnderstanding.GOOD)
            .deliveredAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/patient-education - should create education log")
    void testLogEducationDelivery_Success() throws Exception {
        // Given
        when(patientEducationService.logEducationDelivery(any(PatientEducationLogEntity.class)))
            .thenReturn(testEducationLog);

        String requestBody = objectMapper.writeValueAsString(testEducationLog);

        // When/Then
        mockMvc.perform(post("/api/v1/patient-education")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(testEducationLog.getId().toString()))
            .andExpect(jsonPath("$.patientId").value(patientId.toString()));

        verify(patientEducationService, times(1)).logEducationDelivery(any(PatientEducationLogEntity.class));
    }

    @Test
    @DisplayName("GET /api/v1/patient-education/{id} - should return education log")
    void testGetEducationLog_Success() throws Exception {
        // Given
        when(patientEducationService.getEducationLogById(testEducationLog.getId()))
            .thenReturn(Optional.of(testEducationLog));

        // When/Then
        mockMvc.perform(get("/api/v1/patient-education/{id}", testEducationLog.getId())
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testEducationLog.getId().toString()))
            .andExpect(jsonPath("$.materialType").value("DIABETES_MANAGEMENT"));

        verify(patientEducationService, times(1)).getEducationLogById(testEducationLog.getId());
    }

    @Test
    @DisplayName("GET /api/v1/patient-education/{id} - should return 404 when not found")
    void testGetEducationLog_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(patientEducationService.getEducationLogById(nonExistentId))
            .thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/v1/patient-education/{id}", nonExistentId)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/patient-education/patient/{patientId} - should return patient history")
    void testGetPatientEducationHistory_Success() throws Exception {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<PatientEducationLogEntity> mockPage = new PageImpl<>(
            List.of(testEducationLog), pageRequest, 1);

        when(patientEducationService.getPatientEducationHistory(tenantId, patientId, pageRequest))
            .thenReturn(mockPage);

        // When/Then
        mockMvc.perform(get("/api/v1/patient-education/patient/{patientId}", patientId)
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].patientId").value(patientId.toString()))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/patient-education/material/{materialType} - should filter by material type")
    void testGetEducationByMaterialType_Success() throws Exception {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<PatientEducationLogEntity> mockPage = new PageImpl<>(
            List.of(testEducationLog), pageRequest, 1);

        when(patientEducationService.getEducationByMaterialType(
            tenantId, PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT, pageRequest))
            .thenReturn(mockPage);

        // When/Then
        mockMvc.perform(get("/api/v1/patient-education/material/{materialType}",
                PatientEducationLogEntity.MaterialType.DIABETES_MANAGEMENT)
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].materialType").value("DIABETES_MANAGEMENT"));
    }

    @Test
    @DisplayName("GET /api/v1/patient-education/delivery/{deliveryMethod} - should filter by delivery method")
    void testGetEducationByDeliveryMethod_Success() throws Exception {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<PatientEducationLogEntity> mockPage = new PageImpl<>(
            List.of(testEducationLog), pageRequest, 1);

        when(patientEducationService.getEducationByDeliveryMethod(
            tenantId, PatientEducationLogEntity.DeliveryMethod.IN_PERSON, pageRequest))
            .thenReturn(mockPage);

        // When/Then
        mockMvc.perform(get("/api/v1/patient-education/delivery/{deliveryMethod}",
                PatientEducationLogEntity.DeliveryMethod.IN_PERSON)
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].deliveryMethod").value("IN_PERSON"));
    }

    @Test
    @DisplayName("GET /api/v1/patient-education/poor-understanding - should return sessions needing follow-up")
    void testFindWithPoorUnderstanding_Success() throws Exception {
        // Given
        PatientEducationLogEntity poorUnderstandingLog = testEducationLog.toBuilder()
            .patientUnderstanding(PatientEducationLogEntity.PatientUnderstanding.POOR)
            .build();

        when(patientEducationService.findWithPoorUnderstanding(tenantId))
            .thenReturn(List.of(poorUnderstandingLog));

        // When/Then
        mockMvc.perform(get("/api/v1/patient-education/poor-understanding")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].patientUnderstanding").value("POOR"));
    }

    @Test
    @DisplayName("PUT /api/v1/patient-education/{id} - should update education log")
    void testUpdateEducationLog_Success() throws Exception {
        // Given
        testEducationLog.setNotes("Follow-up education scheduled");
        when(patientEducationService.updateEducationLog(any(PatientEducationLogEntity.class)))
            .thenReturn(testEducationLog);

        String requestBody = objectMapper.writeValueAsString(testEducationLog);

        // When/Then
        mockMvc.perform(put("/api/v1/patient-education/{id}", testEducationLog.getId())
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notes").value("Follow-up education scheduled"));

        verify(patientEducationService, times(1)).updateEducationLog(any(PatientEducationLogEntity.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/patient-education/{id} - should delete education log")
    void testDeleteEducationLog_Success() throws Exception {
        // Given
        doNothing().when(patientEducationService).deleteEducationLog(testEducationLog.getId());

        // When/Then
        mockMvc.perform(delete("/api/v1/patient-education/{id}", testEducationLog.getId())
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNoContent());

        verify(patientEducationService, times(1)).deleteEducationLog(testEducationLog.getId());
    }

    @Test
    @DisplayName("GET /api/v1/patient-education/metrics/{patientId} - should return education metrics")
    void testGetPatientEducationMetrics_Success() throws Exception {
        // Given
        PatientEducationService.PatientEducationMetrics metrics =
            new PatientEducationService.PatientEducationMetrics(5, 1);

        when(patientEducationService.getPatientEducationMetrics(tenantId, patientId))
            .thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/v1/patient-education/metrics/{patientId}", patientId)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEducationSessions").value(5))
            .andExpect(jsonPath("$.sessionsWithPoorUnderstanding").value(1));
    }

    @Test
    @DisplayName("POST without X-Tenant-ID header - should return 400")
    void testCreateEducationLog_MissingTenantHeader() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(testEducationLog);

        // When/Then
        mockMvc.perform(post("/api/v1/patient-education")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
}
