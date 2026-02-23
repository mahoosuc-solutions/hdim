package com.healthdata.aiassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.aiassistant.config.ClaudeConfig;
import com.healthdata.aiassistant.dto.ChatRequest;
import com.healthdata.aiassistant.dto.ChatResponse;
import com.healthdata.aiassistant.service.ClaudeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AiAssistantController.
 *
 * Tests TDD principles:
 * - Controller endpoint responses
 * - Error handling when AI disabled
 * - Query type validation
 * - Status and health check endpoints
 */
@WebMvcTest(AiAssistantController.class)
@ContextConfiguration(classes = {AiAssistantController.class, ClaudeConfig.class})
@DisplayName("AiAssistantController Tests")
class AiAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClaudeService claudeService;

    @Autowired
    private ClaudeConfig claudeConfig;

    @BeforeEach
    void setUp() {
        // Reset config to defaults before each test
        claudeConfig.setEnabled(false);
        claudeConfig.setModel("claude-3-5-sonnet-20241022");
        claudeConfig.setCachingEnabled(true);
        claudeConfig.setRateLimitPerMinute(60);
    }

    @Test
    @DisplayName("GET /api/v1/ai/status should return configuration information")
    @WithMockUser
    void testGetStatus_ReturnsConfigInfo() throws Exception {
        // Given: AI is enabled
        claudeConfig.setEnabled(true);

        // When/Then: Status endpoint should return config details
        mockMvc.perform(get("/api/v1/ai/status"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.model").value("claude-3-5-sonnet-20241022"))
            .andExpect(jsonPath("$.allowedQueryTypes").isArray())
            .andExpect(jsonPath("$.allowedQueryTypes", hasSize(6)))
            .andExpect(jsonPath("$.allowedQueryTypes", hasItem("care_gaps")))
            .andExpect(jsonPath("$.allowedQueryTypes", hasItem("quality_measures")))
            .andExpect(jsonPath("$.cachingEnabled").value(true))
            .andExpect(jsonPath("$.rateLimitPerMinute").value(60));
    }

    @Test
    @DisplayName("GET /api/v1/ai/status should work when AI disabled")
    @WithMockUser
    void testGetStatus_WhenDisabled() throws Exception {
        // Given: AI is disabled
        claudeConfig.setEnabled(false);

        // When/Then: Status should still return (showing disabled state)
        mockMvc.perform(get("/api/v1/ai/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false))
            .andExpect(jsonPath("$.model").exists());
    }

    @Test
    @DisplayName("GET /api/v1/ai/health should return UP when AI enabled")
    @WithMockUser
    void testHealthCheck_WhenEnabled() throws Exception {
        // Given: AI is enabled and service is available
        claudeConfig.setEnabled(true);

        // When/Then: Health should be UP
        mockMvc.perform(get("/api/v1/ai/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.aiEnabled").value(true))
            .andExpect(jsonPath("$.serviceAvailable").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/ai/health should return DOWN when AI disabled")
    @WithMockUser
    void testHealthCheck_WhenDisabled() throws Exception {
        // Given: AI is disabled
        claudeConfig.setEnabled(false);

        // When/Then: Health should be DOWN
        mockMvc.perform(get("/api/v1/ai/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.aiEnabled").value(false))
            .andExpect(jsonPath("$.serviceAvailable").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat should return error when AI disabled")
    @WithMockUser(username = "testuser")
    void testChat_WhenAiDisabled() throws Exception {
        // Given: AI is disabled
        claudeConfig.setEnabled(false);

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("What are the care gaps for patient?")
            .tenantId("test-tenant")
            .build();

        // When/Then: Should return error response (not exception)
        mockMvc.perform(post("/api/v1/ai/chat")
                .with(csrf())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").value(true))
            .andExpect(jsonPath("$.errorMessage").value("AI service disabled"))
            .andExpect(jsonPath("$.response").value(containsString("not enabled")));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat should reject invalid query types")
    @WithMockUser(username = "testuser")
    void testChat_WithInvalidQueryType() throws Exception {
        // Given: AI is enabled but query type is not allowed
        claudeConfig.setEnabled(true);

        ChatRequest request = ChatRequest.builder()
            .queryType("invalid_type")
            .query("Some query")
            .tenantId("test-tenant")
            .build();

        // When/Then: Should return 400 Bad Request
        mockMvc.perform(post("/api/v1/ai/chat")
                .with(csrf())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat should succeed with valid request")
    @WithMockUser(username = "testuser")
    void testChat_WithValidRequest() throws Exception {
        // Given: AI is enabled and query type is valid
        claudeConfig.setEnabled(true);

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("What are the care gaps for patient?")
            .tenantId("test-tenant")
            .build();

        ChatResponse mockResponse = ChatResponse.builder()
            .id("resp-123")
            .queryType("care_gaps")
            .response("Patient has 2 care gaps...")
            .model("claude-3-5-sonnet-20241022")
            .inputTokens(100)
            .outputTokens(200)
            .cached(false)
            .error(false)
            .build();

        when(claudeService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        // When/Then: Should return successful response
        mockMvc.perform(post("/api/v1/ai/chat")
                .with(csrf())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("resp-123"))
            .andExpect(jsonPath("$.queryType").value("care_gaps"))
            .andExpect(jsonPath("$.response").value(containsString("care gaps")))
            .andExpect(jsonPath("$.model").value("claude-3-5-sonnet-20241022"))
            .andExpect(jsonPath("$.inputTokens").value(100))
            .andExpect(jsonPath("$.outputTokens").value(200))
            .andExpect(jsonPath("$.error").value(false));

        verify(claudeService, times(1)).chat(any(ChatRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat should validate required fields")
    @WithMockUser(username = "testuser")
    void testChat_WithMissingRequiredFields() throws Exception {
        // Given: Request is missing required fields
        claudeConfig.setEnabled(true);

        ChatRequest request = ChatRequest.builder()
            // Missing queryType
            .query("Some query")
            // Missing tenantId
            .build();

        // When/Then: Should return 400 Bad Request due to validation failure
        mockMvc.perform(post("/api/v1/ai/chat")
                .with(csrf())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/ai/patient-summary should return error when AI disabled")
    @WithMockUser(username = "testuser")
    void testGeneratePatientSummary_WhenDisabled() throws Exception {
        // Given: AI is disabled
        claudeConfig.setEnabled(false);

        // When/Then: Should return error response
        mockMvc.perform(post("/api/v1/ai/patient-summary/patient-123")
                .with(csrf())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":\"patient-123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").value(true))
            .andExpect(jsonPath("$.errorMessage").value("AI service disabled"));
    }

    @Test
    @DisplayName("POST /api/v1/ai/patient-summary should succeed when enabled")
    @WithMockUser(username = "testuser")
    void testGeneratePatientSummary_WhenEnabled() throws Exception {
        // Given: AI is enabled
        claudeConfig.setEnabled(true);

        ChatResponse mockResponse = ChatResponse.builder()
            .response("Patient summary: 65yo male with diabetes...")
            .error(false)
            .build();

        when(claudeService.generatePatientSummary(eq("patient-123"), anyString(), eq("test-tenant")))
            .thenReturn(mockResponse);

        // When/Then: Should return summary
        mockMvc.perform(post("/api/v1/ai/patient-summary/patient-123")
                .with(csrf())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":\"patient-123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value(containsString("Patient summary")))
            .andExpect(jsonPath("$.error").value(false));

        verify(claudeService, times(1))
            .generatePatientSummary(eq("patient-123"), anyString(), eq("test-tenant"));
    }

    @Test
    @DisplayName("POST /api/v1/ai/care-gaps/analyze should return error when AI disabled")
    @WithMockUser(username = "testuser")
    void testAnalyzeCareGaps_WhenDisabled() throws Exception {
        // Given: AI is disabled
        claudeConfig.setEnabled(false);

        // When/Then: Should return error response
        mockMvc.perform(post("/api/v1/ai/care-gaps/analyze")
                .with(csrf())
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gaps\":[]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").value(true))
            .andExpect(jsonPath("$.errorMessage").value("AI service disabled"));
    }

    @Test
    @DisplayName("GET /api/v1/ai/query should return error when AI disabled")
    @WithMockUser(username = "testuser")
    void testAnswerQuery_WhenDisabled() throws Exception {
        // Given: AI is disabled
        claudeConfig.setEnabled(false);

        // When/Then: Should return error response
        mockMvc.perform(get("/api/v1/ai/query")
                .param("query", "What is diabetes?")
                .header("X-Tenant-ID", "test-tenant"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").value(true))
            .andExpect(jsonPath("$.errorMessage").value("AI service disabled"));
    }

    @Test
    @DisplayName("GET /api/v1/ai/query should succeed when enabled")
    @WithMockUser(username = "testuser")
    void testAnswerQuery_WhenEnabled() throws Exception {
        // Given: AI is enabled
        claudeConfig.setEnabled(true);

        ChatResponse mockResponse = ChatResponse.builder()
            .response("Diabetes is a chronic condition...")
            .error(false)
            .build();

        when(claudeService.answerClinicalQuery(eq("What is diabetes?"), anyString(), eq("test-tenant")))
            .thenReturn(mockResponse);

        // When/Then: Should return answer
        mockMvc.perform(get("/api/v1/ai/query")
                .param("query", "What is diabetes?")
                .header("X-Tenant-ID", "test-tenant"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value(containsString("Diabetes")))
            .andExpect(jsonPath("$.error").value(false));

        verify(claudeService, times(1))
            .answerClinicalQuery(eq("What is diabetes?"), anyString(), eq("test-tenant"));
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat should accept all allowed query types")
    @WithMockUser(username = "testuser")
    void testChat_WithAllAllowedQueryTypes() throws Exception {
        // Given: AI is enabled
        claudeConfig.setEnabled(true);

        ChatResponse mockResponse = ChatResponse.builder()
            .response("Response")
            .error(false)
            .build();

        when(claudeService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        String[] allowedTypes = {
            "care_gaps",
            "quality_measures",
            "patient_summary",
            "measure_compliance",
            "population_health",
            "care_recommendations"
        };

        // When/Then: Each allowed type should succeed
        for (String queryType : allowedTypes) {
            ChatRequest request = ChatRequest.builder()
                .queryType(queryType)
                .query("Test query for " + queryType)
                .tenantId("test-tenant")
                .build();

            mockMvc.perform(post("/api/v1/ai/chat")
                    .with(csrf())
                    .header("X-Tenant-ID", "test-tenant")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(false));
        }

        verify(claudeService, times(allowedTypes.length)).chat(any(ChatRequest.class));
    }
}
