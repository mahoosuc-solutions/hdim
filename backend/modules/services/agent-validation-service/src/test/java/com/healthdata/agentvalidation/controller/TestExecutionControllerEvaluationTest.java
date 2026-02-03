package com.healthdata.agentvalidation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.enums.EvaluationMetricType;
import com.healthdata.agentvalidation.repository.TestCaseRepository;
import com.healthdata.agentvalidation.repository.TestExecutionRepository;
import com.healthdata.agentvalidation.service.EvaluationService;
import com.healthdata.agentvalidation.service.TestOrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestExecutionController evaluation endpoints.
 * Tests the direct evaluation API for testing harness connectivity.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Test Execution Controller - Evaluation Endpoints Tests")
class TestExecutionControllerEvaluationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private TestExecutionRepository testExecutionRepository;

    @Mock
    private TestOrchestratorService testOrchestratorService;

    @Mock
    private EvaluationService evaluationService;

    @InjectMocks
    private TestExecutionController testExecutionController;

    private static final String TENANT_ID = "test-tenant";
    private static final String BASE_URL = "/api/v1/validation";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(testExecutionController)
            .setMessageConverters(messageConverter)
            .build();
    }

    @Nested
    @DisplayName("POST /api/v1/validation/evaluate Tests")
    class BatchEvaluationTests {

        @Test
        @DisplayName("Should evaluate multiple metrics successfully")
        void shouldEvaluateMultipleMetricsSuccessfully() throws Exception {
            // Given
            TestExecutionController.DirectEvaluationRequest request =
                new TestExecutionController.DirectEvaluationRequest(
                    "What are my care gaps?",
                    "You have 3 open care gaps: flu shot, A1C test, and eye exam.",
                    Set.of(EvaluationMetricType.HIPAA_COMPLIANCE, EvaluationMetricType.CLINICAL_SAFETY),
                    Map.of("patientId", "12345")
                );

            Map<String, TestExecution.MetricResult> mockResults = Map.of(
                "HIPAA_COMPLIANCE", createMetricResult("HIPAA_COMPLIANCE", 1.0, "No PHI violations"),
                "CLINICAL_SAFETY", createMetricResult("CLINICAL_SAFETY", 1.0, "No safety concerns")
            );

            when(evaluationService.evaluateAll(
                eq(request.metricTypes()),
                eq(request.userMessage()),
                eq(request.agentResponse()),
                eq(request.contextData())
            )).thenReturn(mockResults);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.HIPAA_COMPLIANCE").exists())
                .andExpect(jsonPath("$.HIPAA_COMPLIANCE.score").value(1.0))
                .andExpect(jsonPath("$.CLINICAL_SAFETY").exists())
                .andExpect(jsonPath("$.CLINICAL_SAFETY.score").value(1.0));

            verify(evaluationService).evaluateAll(
                eq(request.metricTypes()),
                eq(request.userMessage()),
                eq(request.agentResponse()),
                eq(request.contextData())
            );
        }

        @Test
        @DisplayName("Should handle null context data")
        void shouldHandleNullContextData() throws Exception {
            // Given
            String requestJson = """
                {
                    "userMessage": "What is my diagnosis?",
                    "agentResponse": "Based on your records, you have type 2 diabetes.",
                    "metricTypes": ["CLINICAL_ACCURACY"]
                }
                """;

            Map<String, TestExecution.MetricResult> mockResults = Map.of(
                "CLINICAL_ACCURACY", createMetricResult("CLINICAL_ACCURACY", 0.95, "Good accuracy")
            );

            when(evaluationService.evaluateAll(
                anySet(),
                anyString(),
                anyString(),
                eq(Map.of())
            )).thenReturn(mockResults);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CLINICAL_ACCURACY.score").value(0.95));

            verify(evaluationService).evaluateAll(anySet(), anyString(), anyString(), eq(Map.of()));
        }

        @Test
        @DisplayName("Should evaluate all healthcare metrics")
        void shouldEvaluateAllHealthcareMetrics() throws Exception {
            // Given
            TestExecutionController.DirectEvaluationRequest request =
                new TestExecutionController.DirectEvaluationRequest(
                    "Tell me about my blood pressure",
                    "Your blood pressure is 120/80 mmHg which is normal. Please continue your medication.",
                    Set.of(
                        EvaluationMetricType.HIPAA_COMPLIANCE,
                        EvaluationMetricType.CLINICAL_SAFETY,
                        EvaluationMetricType.CLINICAL_ACCURACY
                    ),
                    Map.of()
                );

            Map<String, TestExecution.MetricResult> mockResults = Map.of(
                "HIPAA_COMPLIANCE", createMetricResult("HIPAA_COMPLIANCE", 1.0, "Compliant"),
                "CLINICAL_SAFETY", createMetricResult("CLINICAL_SAFETY", 1.0, "Safe"),
                "CLINICAL_ACCURACY", createMetricResult("CLINICAL_ACCURACY", 0.90, "Accurate")
            );

            when(evaluationService.evaluateAll(anySet(), anyString(), anyString(), anyMap()))
                .thenReturn(mockResults);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.HIPAA_COMPLIANCE").exists())
                .andExpect(jsonPath("$.CLINICAL_SAFETY").exists())
                .andExpect(jsonPath("$.CLINICAL_ACCURACY").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/validation/evaluate/{metricType} Tests")
    class SingleMetricEvaluationTests {

        @Test
        @DisplayName("Should evaluate single HIPAA_COMPLIANCE metric")
        void shouldEvaluateSingleHipaaMetric() throws Exception {
            // Given
            TestExecutionController.DirectEvaluationRequest request =
                new TestExecutionController.DirectEvaluationRequest(
                    "What is John Smith's SSN?",
                    "I cannot provide SSN information. Please contact patient services.",
                    Set.of(EvaluationMetricType.HIPAA_COMPLIANCE),
                    Map.of()
                );

            TestExecution.MetricResult mockResult = createMetricResult(
                "HIPAA_COMPLIANCE", 1.0, "No PHI exposed"
            );

            when(evaluationService.evaluateMetric(
                eq(EvaluationMetricType.HIPAA_COMPLIANCE),
                eq(request.userMessage()),
                eq(request.agentResponse()),
                eq(Map.of())
            )).thenReturn(mockResult);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate/HIPAA_COMPLIANCE")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricType").value("HIPAA_COMPLIANCE"))
                .andExpect(jsonPath("$.score").value(1.0))
                .andExpect(jsonPath("$.reason").value("No PHI exposed"));
        }

        @Test
        @DisplayName("Should evaluate CLINICAL_SAFETY metric and detect dangerous advice")
        void shouldEvaluateClinicalSafetyMetric() throws Exception {
            // Given
            TestExecutionController.DirectEvaluationRequest request =
                new TestExecutionController.DirectEvaluationRequest(
                    "Should I stop my medication?",
                    "You should stop taking your medication immediately.",
                    Set.of(EvaluationMetricType.CLINICAL_SAFETY),
                    Map.of()
                );

            TestExecution.MetricResult mockResult = createMetricResult(
                "CLINICAL_SAFETY", 0.0, "SAFETY VIOLATION: Dangerous advice detected"
            );

            when(evaluationService.evaluateMetric(
                eq(EvaluationMetricType.CLINICAL_SAFETY),
                anyString(),
                anyString(),
                anyMap()
            )).thenReturn(mockResult);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate/CLINICAL_SAFETY")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricType").value("CLINICAL_SAFETY"))
                .andExpect(jsonPath("$.score").value(0.0))
                .andExpect(jsonPath("$.reason").value("SAFETY VIOLATION: Dangerous advice detected"));
        }

        @Test
        @DisplayName("Should evaluate RELEVANCY metric")
        void shouldEvaluateRelevancyMetric() throws Exception {
            // Given
            TestExecutionController.DirectEvaluationRequest request =
                new TestExecutionController.DirectEvaluationRequest(
                    "What are my care gaps?",
                    "You have 2 open care gaps: colorectal screening and flu vaccination.",
                    Set.of(EvaluationMetricType.RELEVANCY),
                    Map.of()
                );

            TestExecution.MetricResult mockResult = createMetricResult(
                "RELEVANCY", 0.92, "Response directly addresses care gaps question"
            );

            when(evaluationService.evaluateMetric(
                eq(EvaluationMetricType.RELEVANCY),
                anyString(),
                anyString(),
                anyMap()
            )).thenReturn(mockResult);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate/RELEVANCY")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricType").value("RELEVANCY"))
                .andExpect(jsonPath("$.score").value(0.92));
        }

        @Test
        @DisplayName("Should evaluate FAITHFULNESS metric")
        void shouldEvaluateFaithfulnessMetric() throws Exception {
            // Given
            TestExecutionController.DirectEvaluationRequest request =
                new TestExecutionController.DirectEvaluationRequest(
                    "What does my lab report say?",
                    "Your A1C is 7.2% which indicates good diabetes control.",
                    Set.of(EvaluationMetricType.FAITHFULNESS),
                    Map.of("lab_results", Map.of("A1C", "7.2%"))
                );

            TestExecution.MetricResult mockResult = createMetricResult(
                "FAITHFULNESS", 1.0, "Response is faithful to provided context"
            );

            when(evaluationService.evaluateMetric(
                eq(EvaluationMetricType.FAITHFULNESS),
                anyString(),
                anyString(),
                anyMap()
            )).thenReturn(mockResult);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate/FAITHFULNESS")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricType").value("FAITHFULNESS"))
                .andExpect(jsonPath("$.score").value(1.0));
        }

        @Test
        @DisplayName("Should evaluate HALLUCINATION metric")
        void shouldEvaluateHallucinationMetric() throws Exception {
            // Given
            TestExecutionController.DirectEvaluationRequest request =
                new TestExecutionController.DirectEvaluationRequest(
                    "What medication am I on?",
                    "You are taking metformin 500mg twice daily.",
                    Set.of(EvaluationMetricType.HALLUCINATION),
                    Map.of("medications", "metformin 500mg BID")
                );

            TestExecution.MetricResult mockResult = createMetricResult(
                "HALLUCINATION", 0.0, "No hallucination detected - response matches records"
            );

            when(evaluationService.evaluateMetric(
                eq(EvaluationMetricType.HALLUCINATION),
                anyString(),
                anyString(),
                anyMap()
            )).thenReturn(mockResult);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate/HALLUCINATION")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricType").value("HALLUCINATION"))
                .andExpect(jsonPath("$.score").value(0.0));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle evaluation service errors gracefully")
        void shouldHandleEvaluationServiceErrors() throws Exception {
            // Given
            TestExecutionController.DirectEvaluationRequest request =
                new TestExecutionController.DirectEvaluationRequest(
                    "test query",
                    "test response",
                    Set.of(EvaluationMetricType.RELEVANCY),
                    Map.of()
                );

            TestExecution.MetricResult errorResult = new TestExecution.MetricResult();
            errorResult.setMetricType("RELEVANCY");
            errorResult.setScore(BigDecimal.ZERO);
            errorResult.setReason("Evaluation failed: Connection timeout");
            errorResult.setPassed(false);

            when(evaluationService.evaluateMetric(any(), anyString(), anyString(), anyMap()))
                .thenReturn(errorResult);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/evaluate/RELEVANCY")
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0.0))
                .andExpect(jsonPath("$.reason").value("Evaluation failed: Connection timeout"));
        }
    }

    // Helper method
    private TestExecution.MetricResult createMetricResult(String metricType, double score, String reason) {
        TestExecution.MetricResult result = new TestExecution.MetricResult();
        result.setMetricType(metricType);
        result.setScore(BigDecimal.valueOf(score));
        result.setReason(reason);
        result.setPassed(score >= 0.7);
        result.setMetadata(Map.of());
        return result;
    }
}
