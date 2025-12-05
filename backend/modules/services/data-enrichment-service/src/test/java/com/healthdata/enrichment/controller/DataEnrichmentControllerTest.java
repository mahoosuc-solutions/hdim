package com.healthdata.enrichment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.enrichment.analyzer.DataCompletenessAnalyzer;
import com.healthdata.enrichment.dto.*;
import com.healthdata.enrichment.model.*;
import com.healthdata.enrichment.service.*;
import com.healthdata.enrichment.validator.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD Tests for DataEnrichmentController.
 */
@WebMvcTest(DataEnrichmentController.class)
@DisplayName("DataEnrichmentController TDD Tests")
class DataEnrichmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClinicalNoteExtractor clinicalNoteExtractor;

    @MockBean
    private MedicalEntityRecognizer medicalEntityRecognizer;

    @MockBean
    private ICD10Validator icd10Validator;

    @MockBean
    private SnomedValidator snomedValidator;

    @MockBean
    private CptValidator cptValidator;

    @MockBean
    private LoincValidator loincValidator;

    @MockBean
    private CodeSuggester codeSuggester;

    @MockBean
    private DataCompletenessAnalyzer completenessAnalyzer;

    @MockBean
    private DataQualityService qualityService;

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should extract entities from clinical note")
    @WithMockUser
    void testExtractFromClinicalNote() throws Exception {
        // Given
        String clinicalNote = "Patient has Type 2 Diabetes Mellitus";
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote(clinicalNote);

        ExtractionResult mockResult = new ExtractionResult();
        mockResult.setOverallConfidence(0.85);

        when(clinicalNoteExtractor.extract(anyString())).thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overallConfidence").value(0.85));

        verify(clinicalNoteExtractor, times(1)).extract(clinicalNote);
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/validate/icd10 should validate ICD-10 code")
    @WithMockUser
    void testValidateIcd10Code() throws Exception {
        // Given
        CodeValidationRequest request = new CodeValidationRequest();
        request.setCode("E11.9");

        CodeValidationResult mockResult = new CodeValidationResult();
        mockResult.setValid(true);
        mockResult.setCode("E11.9");
        mockResult.setDescription("Type 2 diabetes mellitus without complications");

        when(icd10Validator.validate(anyString())).thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/validate/icd10")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.code").value("E11.9"))
            .andExpect(jsonPath("$.description").exists());

        verify(icd10Validator, times(1)).validate("E11.9");
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/validate/snomed should validate SNOMED code")
    @WithMockUser
    void testValidateSnomedCode() throws Exception {
        // Given
        CodeValidationRequest request = new CodeValidationRequest();
        request.setCode("73211009");

        CodeValidationResult mockResult = new CodeValidationResult();
        mockResult.setValid(true);
        mockResult.setCode("73211009");

        when(snomedValidator.validate(anyString())).thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/validate/snomed")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));

        verify(snomedValidator, times(1)).validate("73211009");
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/suggest-codes should suggest codes from text")
    @WithMockUser
    void testSuggestCodes() throws Exception {
        // Given
        CodeSuggestionRequest request = new CodeSuggestionRequest();
        request.setText("Type 2 Diabetes Mellitus");
        request.setCodeSystem("ICD10");

        CodeSuggestion suggestion = new CodeSuggestion();
        suggestion.setCode("E11.9");
        suggestion.setDescription("Type 2 diabetes mellitus without complications");
        suggestion.setConfidence(0.95);

        when(codeSuggester.suggestIcd10(anyString())).thenReturn(List.of(suggestion));

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/suggest-codes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].code").value("E11.9"))
            .andExpect(jsonPath("$[0].confidence").value(0.95));

        verify(codeSuggester, times(1)).suggestIcd10("Type 2 Diabetes Mellitus");
    }

    @Test
    @DisplayName("GET /api/v1/enrichment/completeness/{patientId} should return completeness analysis")
    @WithMockUser
    void testGetCompletenessAnalysis() throws Exception {
        // Given
        String patientId = "patient-123";

        MissingDataReport mockReport = new MissingDataReport();
        mockReport.setPatientId(patientId);
        mockReport.setCompletenessScore(75.0);

        when(completenessAnalyzer.analyze(anyString())).thenReturn(mockReport);

        // When/Then
        mockMvc.perform(get("/api/v1/enrichment/completeness/{patientId}", patientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value(patientId))
            .andExpect(jsonPath("$.completenessScore").value(75.0));

        verify(completenessAnalyzer, times(1)).analyze(patientId);
    }

    @Test
    @DisplayName("GET /api/v1/enrichment/quality/report should return data quality report")
    @WithMockUser
    void testGetQualityReport() throws Exception {
        // Given
        String patientId = "patient-123";

        DataQualityReport mockReport = new DataQualityReport();
        mockReport.setOverallScore(82.5);

        when(qualityService.generateQualityReport(anyString())).thenReturn(mockReport);

        // When/Then
        mockMvc.perform(get("/api/v1/enrichment/quality/report")
                .param("patientId", patientId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overallScore").value(82.5));

        verify(qualityService, times(1)).generateQualityReport(patientId);
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should require authentication")
    void testExtractRequiresAuth() throws Exception {
        // Given
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("Test note");

        // When/Then - No @WithMockUser, so should get 401
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should validate request")
    @WithMockUser
    void testExtractValidation() throws Exception {
        // Given - Empty clinical note
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("");

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/validate/icd10 should handle invalid code gracefully")
    @WithMockUser
    void testValidateInvalidCode() throws Exception {
        // Given
        CodeValidationRequest request = new CodeValidationRequest();
        request.setCode("INVALID");

        CodeValidationResult mockResult = new CodeValidationResult();
        mockResult.setValid(false);
        mockResult.setErrors(List.of("Invalid ICD-10 code format"));

        when(icd10Validator.validate(anyString())).thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/validate/icd10")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/suggest-codes should support multiple code systems")
    @WithMockUser
    void testSuggestCodesMultipleSystems() throws Exception {
        // Given
        CodeSuggestionRequest request = new CodeSuggestionRequest();
        request.setText("Diabetes");
        request.setCodeSystem("SNOMED");

        CodeSuggestion suggestion = new CodeSuggestion();
        suggestion.setCode("73211009");

        when(codeSuggester.suggestSnomed(anyString())).thenReturn(List.of(suggestion));

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/suggest-codes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("73211009"));
    }

    @Test
    @DisplayName("GET /api/v1/enrichment/completeness/{patientId} should handle non-existent patient")
    @WithMockUser
    void testCompletenessNonExistentPatient() throws Exception {
        // Given
        String patientId = "non-existent";

        when(completenessAnalyzer.analyze(anyString())).thenReturn(null);

        // When/Then
        mockMvc.perform(get("/api/v1/enrichment/completeness/{patientId}", patientId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should support async processing")
    @WithMockUser
    void testAsyncExtraction() throws Exception {
        // Given
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("Long clinical note...");
        request.setAsync(true);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.taskId").exists());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should support tenant isolation")
    @WithMockUser
    void testTenantIsolation() throws Exception {
        // Given
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("Test note");
        request.setTenantId("tenant-123");

        ExtractionResult mockResult = new ExtractionResult();
        mockResult.setTenantId("tenant-123");

        when(clinicalNoteExtractor.extractWithTenant(anyString(), anyString()))
            .thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantId").value("tenant-123"));
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/validate/cpt should validate CPT codes")
    @WithMockUser
    void testValidateCptCode() throws Exception {
        // Given
        CodeValidationRequest request = new CodeValidationRequest();
        request.setCode("99213");

        CodeValidationResult mockResult = new CodeValidationResult();
        mockResult.setValid(true);
        mockResult.setCode("99213");

        when(cptValidator.validate(anyString())).thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/validate/cpt")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/validate/loinc should validate LOINC codes")
    @WithMockUser
    void testValidateLoincCode() throws Exception {
        // Given
        CodeValidationRequest request = new CodeValidationRequest();
        request.setCode("4548-4");

        CodeValidationResult mockResult = new CodeValidationResult();
        mockResult.setValid(true);
        mockResult.setCode("4548-4");

        when(loincValidator.validate(anyString())).thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/validate/loinc")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }
}
