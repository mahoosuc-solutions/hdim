package com.healthdata.nurseworkflow.api.v1;

import com.healthdata.nurseworkflow.application.OutreachLogService;
import com.healthdata.nurseworkflow.domain.model.OutreachLogEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OutreachLogController
 *
 * Tests REST endpoints for patient outreach logging.
 * Uses MockMvc for testing HTTP endpoints without full context.
 */
@WebMvcTest(OutreachLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("OutreachLogController")
class OutreachLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OutreachLogService outreachLogService;

    private String tenantId;
    private UUID patientId;
    private UUID nurseId;
    private OutreachLogEntity testOutreachLog;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT001";
        patientId = UUID.randomUUID();
        nurseId = UUID.randomUUID();

        testOutreachLog = OutreachLogEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .nurseId(nurseId)
            .outcomeType(OutreachLogEntity.OutcomeType.SUCCESSFUL_CONTACT)
            .contactMethod(OutreachLogEntity.ContactMethod.PHONE)
            .reason("post-discharge")
            .notes("Patient doing well, no concerns")
            .attemptedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("POST /nurse-workflow/api/v1/outreach-logs - should create outreach log")
    void testCreateOutreachLog_Success() throws Exception {
        // Given
        when(outreachLogService.createOutreachLog(any(OutreachLogEntity.class)))
            .thenReturn(testOutreachLog);

        String requestBody = """
            {
                "patientId": "%s",
                "nurseId": "%s",
                "outcomeType": "SUCCESSFUL_CONTACT",
                "contactMethod": "PHONE",
                "reason": "post-discharge",
                "notes": "Patient doing well, no concerns"
            }
            """.formatted(patientId, nurseId);

        // When & Then
        mockMvc.perform(post("/api/v1/outreach-logs")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(testOutreachLog.getId().toString()))
            .andExpect(jsonPath("$.patientId").value(patientId.toString()))
            .andExpect(jsonPath("$.outcomeType").value("SUCCESSFUL_CONTACT"));

        verify(outreachLogService, times(1)).createOutreachLog(any(OutreachLogEntity.class));
    }

    @Test
    @DisplayName("GET /nurse-workflow/api/v1/outreach-logs/{id} - should retrieve outreach log")
    void testGetOutreachLog_Success() throws Exception {
        // Given
        when(outreachLogService.getOutreachLogById(testOutreachLog.getId()))
            .thenReturn(Optional.of(testOutreachLog));

        // When & Then
        mockMvc.perform(get("/api/v1/outreach-logs/{id}", testOutreachLog.getId())
                .header("X-Tenant-ID", tenantId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testOutreachLog.getId().toString()))
            .andExpect(jsonPath("$.outcomeType").value("SUCCESSFUL_CONTACT"));

        verify(outreachLogService, times(1)).getOutreachLogById(testOutreachLog.getId());
    }

    @Test
    @DisplayName("GET /nurse-workflow/api/v1/outreach-logs/{id} - should return 404 when not found")
    void testGetOutreachLog_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(outreachLogService.getOutreachLogById(nonExistentId))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/outreach-logs/{id}", nonExistentId)
                .header("X-Tenant-ID", tenantId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /nurse-workflow/api/v1/outreach-logs/patient/{patientId} - should retrieve patient history")
    void testGetPatientOutreachHistory_Success() throws Exception {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<OutreachLogEntity> mockPage = new PageImpl<>(
            List.of(testOutreachLog), pageRequest, 1);

        when(outreachLogService.getPatientOutreachHistory(tenantId, patientId, pageRequest))
            .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/api/v1/outreach-logs/patient/{patientId}", patientId)
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].patientId").value(patientId.toString()));
    }

    @Test
    @DisplayName("PUT /nurse-workflow/api/v1/outreach-logs/{id} - should update outreach log")
    void testUpdateOutreachLog_Success() throws Exception {
        // Given
        testOutreachLog.setNotes("Updated notes");
        when(outreachLogService.updateOutreachLog(any(OutreachLogEntity.class)))
            .thenReturn(testOutreachLog);

        String requestBody = """
            {
                "id": "%s",
                "notes": "Updated notes"
            }
            """.formatted(testOutreachLog.getId());

        // When & Then
        mockMvc.perform(put("/api/v1/outreach-logs/{id}", testOutreachLog.getId())
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notes").value("Updated notes"));

        verify(outreachLogService, times(1)).updateOutreachLog(any(OutreachLogEntity.class));
    }

    @Test
    @DisplayName("DELETE /nurse-workflow/api/v1/outreach-logs/{id} - should delete outreach log")
    void testDeleteOutreachLog_Success() throws Exception {
        // Given
        doNothing().when(outreachLogService).deleteOutreachLog(testOutreachLog.getId());

        // When & Then
        mockMvc.perform(delete("/api/v1/outreach-logs/{id}", testOutreachLog.getId())
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNoContent());

        verify(outreachLogService, times(1)).deleteOutreachLog(testOutreachLog.getId());
    }

    @Test
    @DisplayName("GET /nurse-workflow/api/v1/outreach-logs/metrics/{patientId} - should return metrics")
    void testGetOutreachMetrics_Success() throws Exception {
        // Given
        OutreachLogService.OutreachMetrics metrics = OutreachLogService.OutreachMetrics.builder()
            .totalOutreachAttempts(5)
            .successfulContacts(3)
            .successRate(60)
            .build();

        when(outreachLogService.getPatientOutreachMetrics(tenantId, patientId))
            .thenReturn(metrics);

        // When & Then
        mockMvc.perform(get("/api/v1/outreach-logs/metrics/{patientId}", patientId)
                .header("X-Tenant-ID", tenantId)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalOutreachAttempts").value(5))
            .andExpect(jsonPath("$.successfulContacts").value(3))
            .andExpect(jsonPath("$.successRate").value(60));
    }

    @Test
    @DisplayName("GET /api/v1/outreach-logs/{id} - should require tenant header")
    void testMissingTenantHeader() throws Exception {
        // When & Then - use existing endpoint that requires X-Tenant-ID header
        mockMvc.perform(get("/api/v1/outreach-logs/{id}", testOutreachLog.getId())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
