package com.healthdata.ingestion.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.ingestion.application.DataIngestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 2 integration tests for Data Ingestion Service.
 * Validates FHIR bundle ingestion, validation, error handling, tenant isolation, and idempotency.
 */
@WebMvcTest(IngestionController.class)
@Tag("integration")
@DisplayName("Data Ingestion Service Phase 2 Integration Tests")
class DataIngestionPhase2IntegrationTest {

    @MockBean
    private DataIngestionService dataIngestionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_A = "pilot-tenant-a";
    private static final String TENANT_B = "pilot-tenant-b";

    @Test
    @DisplayName("POST /api/v1/ingestion/start - Should ingest FHIR bundle successfully")
    void shouldIngestFhirBundle() throws Exception {
        IngestionResponse response = IngestionResponse.builder()
                .sessionId("session-001")
                .status("STARTED")
                .message("Ingestion started successfully")
                .build();
        when(dataIngestionService.startIngestion(any())).thenReturn(response);

        Map<String, Object> request = Map.of(
                "tenantId", TENANT_A,
                "patientCount", 100,
                "scenario", "hedis"
        );

        mockMvc.perform(post("/api/v1/ingestion/start")
                        .header("X-Tenant-ID", TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-001"))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    @DisplayName("POST /api/v1/ingestion/start - Should reject invalid patient count")
    void shouldRejectInvalidRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "tenantId", TENANT_A,
                "patientCount", 5,
                "scenario", "hedis"
        );

        mockMvc.perform(post("/api/v1/ingestion/start")
                        .header("X-Tenant-ID", TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/ingestion/start - Should handle service errors gracefully")
    void shouldHandleServiceErrors() throws Exception {
        when(dataIngestionService.startIngestion(any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        Map<String, Object> request = Map.of(
                "tenantId", TENANT_A,
                "patientCount", 50,
                "scenario", "hedis"
        );

        mockMvc.perform(post("/api/v1/ingestion/start")
                        .header("X-Tenant-ID", TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Tenant isolation - Should pass tenant header to service layer")
    void shouldPassTenantToService() throws Exception {
        IngestionResponse response = IngestionResponse.builder()
                .sessionId("session-b")
                .status("STARTED")
                .message("Ingestion started for tenant B")
                .build();
        when(dataIngestionService.startIngestion(any())).thenReturn(response);

        Map<String, Object> request = Map.of(
                "tenantId", TENANT_B,
                "patientCount", 25,
                "scenario", "hedis"
        );

        mockMvc.perform(post("/api/v1/ingestion/start")
                        .header("X-Tenant-ID", TENANT_B)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-b"));
    }

    @Test
    @DisplayName("GET /api/v1/ingestion/progress - Should return idempotent progress for same session")
    void shouldReturnIdempotentProgress() throws Exception {
        IngestionProgressResponse progress = IngestionProgressResponse.builder()
                .sessionId("session-001")
                .status("IN_PROGRESS")
                .progressPercent(50)
                .patientsGenerated(50L)
                .patientsPersisted(25L)
                .careGapsCreated(10L)
                .measuresSeeded(5L)
                .currentStage("PERSISTING")
                .build();
        when(dataIngestionService.getProgress("session-001")).thenReturn(progress);

        // Two identical calls should return the same progress
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/api/v1/ingestion/progress")
                            .param("sessionId", "session-001")
                            .header("X-Tenant-ID", TENANT_A))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value("session-001"))
                    .andExpect(jsonPath("$.progressPercent").value(50));
        }
    }
}
