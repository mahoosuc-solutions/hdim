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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for IngestionController.
 *
 * <p>Uses @WebMvcTest to spin up only the web layer with a mocked DataIngestionService,
 * avoiding the need for a live database or external services.
 *
 * <p>Tests:
 * <ul>
 *   <li>POST /api/v1/ingestion/start with valid payload → 200 STARTED</li>
 *   <li>GET  /api/v1/ingestion/progress               → 200 with progress data</li>
 *   <li>POST /api/v1/ingestion/start with invalid payload → 400</li>
 * </ul>
 */
@WebMvcTest(IngestionController.class)
@Tag("integration")
@DisplayName("IngestionController Integration Tests")
class IngestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataIngestionService dataIngestionService;

    private static final String BASE_URL = "/api/v1/ingestion";
    private static final String TENANT_ID = "test-tenant-001";

    // ---------------------------------------------------------------------------
    // Test 1: POST /start with valid request → 200
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("startIngestion_withValidRequest_returns200")
    void startIngestion_withValidRequest_returns200() throws Exception {
        // Given
        IngestionRequest request = IngestionRequest.builder()
                .tenantId(TENANT_ID)
                .patientCount(50)
                .includeCareGaps(true)
                .includeQualityMeasures(true)
                .scenario("hedis")
                .build();

        IngestionResponse stubResponse = IngestionResponse.builder()
                .sessionId("session-abc-123")
                .status("STARTED")
                .message("Data ingestion started: 50 patients for tenant test-tenant-001")
                .build();

        when(dataIngestionService.startIngestion(any(IngestionRequest.class))).thenReturn(stubResponse);

        // When / Then
        mockMvc.perform(post(BASE_URL + "/start")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value("session-abc-123"))
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---------------------------------------------------------------------------
    // Test 2: GET /progress → 200
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getProgress_returns200")
    void getProgress_returns200() throws Exception {
        // Given
        IngestionProgressResponse stubProgress = IngestionProgressResponse.builder()
                .sessionId("session-abc-123")
                .status("RUNNING")
                .progressPercent(42)
                .patientsGenerated(21L)
                .patientsPersisted(15L)
                .careGapsCreated(10L)
                .measuresSeeded(5L)
                .currentStage("PERSISTING")
                .build();

        when(dataIngestionService.getProgress(isNull())).thenReturn(stubProgress);

        // When / Then
        mockMvc.perform(get(BASE_URL + "/progress")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value("session-abc-123"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.progressPercent").value(42))
                .andExpect(jsonPath("$.currentStage").value("PERSISTING"));
    }

    // ---------------------------------------------------------------------------
    // Test 3: POST /start with invalid request → 400
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("startIngestion_withInvalidRequest_returns400")
    void startIngestion_withInvalidRequest_returns400() throws Exception {
        // Given: patientCount is below the minimum (10) and tenantId is null — both @NotNull/@Min violations
        IngestionRequest invalidRequest = IngestionRequest.builder()
                .tenantId(null)   // @NotNull violation
                .patientCount(5)  // @Min(10) violation
                .scenario("hedis")
                .build();

        // When / Then — Bean validation (@Valid) should reject the request with 400
        mockMvc.perform(post(BASE_URL + "/start")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
