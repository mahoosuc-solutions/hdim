package com.healthdata.devops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.devops.model.FhirValidationResult;
import com.healthdata.devops.validation.FhirDataValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FhirValidationController.
 * Verifies REST endpoint delegation to FhirDataValidationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FHIR Validation Controller Tests")
@Tag("unit")
class FhirValidationControllerTest {

    @Mock
    private FhirDataValidationService validationService;

    @InjectMocks
    private FhirValidationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    @DisplayName("POST /validate should return 200 with validation result")
    void postValidateShouldReturn200WithValidationResult() throws Exception {
        // Given
        FhirValidationResult result = buildSampleResult("PASS");
        when(validationService.validateDemoData()).thenReturn(result);

        // When / Then
        mockMvc.perform(post("/api/v1/devops/fhir-validation/validate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationId").value("val-001"))
                .andExpect(jsonPath("$.overallStatus").value("PASS"))
                .andExpect(jsonPath("$.totalChecks").value(5))
                .andExpect(jsonPath("$.passedChecks").value(5))
                .andExpect(jsonPath("$.failedChecks").value(0));

        verify(validationService, times(1)).validateDemoData();
    }

    @Test
    @DisplayName("GET /status should return 200 with validation result")
    void getStatusShouldReturn200WithValidationResult() throws Exception {
        // Given
        FhirValidationResult result = buildSampleResult("FAIL");
        when(validationService.validateDemoData()).thenReturn(result);

        // When / Then
        mockMvc.perform(get("/api/v1/devops/fhir-validation/status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationId").value("val-001"))
                .andExpect(jsonPath("$.overallStatus").value("FAIL"));

        verify(validationService, times(1)).validateDemoData();
    }

    @Test
    @DisplayName("POST /validate should delegate to validation service exactly once")
    void postValidateShouldDelegateToService() throws Exception {
        // Given
        FhirValidationResult result = buildSampleResult("PASS");
        when(validationService.validateDemoData()).thenReturn(result);

        // When
        mockMvc.perform(post("/api/v1/devops/fhir-validation/validate")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        verify(validationService, times(1)).validateDemoData();
        verifyNoMoreInteractions(validationService);
    }

    @Test
    @DisplayName("GET /status should delegate to validation service exactly once")
    void getStatusShouldDelegateToService() throws Exception {
        // Given
        FhirValidationResult result = buildSampleResult("PASS");
        when(validationService.validateDemoData()).thenReturn(result);

        // When
        mockMvc.perform(get("/api/v1/devops/fhir-validation/status")
                .accept(MediaType.APPLICATION_JSON));

        // Then
        verify(validationService, times(1)).validateDemoData();
        verifyNoMoreInteractions(validationService);
    }

    @Test
    @DisplayName("POST /validate should include resource count checks in response")
    void postValidateShouldIncludeResourceCountChecks() throws Exception {
        // Given
        FhirValidationResult result = buildSampleResult("PASS");
        when(validationService.validateDemoData()).thenReturn(result);

        // When / Then
        mockMvc.perform(post("/api/v1/devops/fhir-validation/validate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceCountChecks").isArray())
                .andExpect(jsonPath("$.resourceCountChecks[0].resourceType").value("Patient"))
                .andExpect(jsonPath("$.resourceCountChecks[0].actualCount").value(100))
                .andExpect(jsonPath("$.resourceCountChecks[0].status").value("PASS"));
    }

    private FhirValidationResult buildSampleResult(String overallStatus) {
        return FhirValidationResult.builder()
                .validationId("val-001")
                .validationTimestamp(Instant.parse("2026-03-05T12:00:00Z"))
                .overallStatus(overallStatus)
                .totalChecks(5)
                .passedChecks(5)
                .failedChecks(0)
                .warningChecks(0)
                .resourceCountChecks(List.of(
                        FhirValidationResult.ResourceCountCheck.builder()
                                .resourceType("Patient")
                                .actualCount(100)
                                .minimumRequired(50)
                                .status("PASS")
                                .message("Patient count meets minimum")
                                .build()
                ))
                .codeSystemChecks(List.of())
                .authenticityChecks(List.of())
                .complianceChecks(List.of())
                .relationshipChecks(List.of())
                .summary(Map.of("totalResources", 500))
                .build();
    }
}
