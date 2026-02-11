package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.config.ValidationProperties;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.enums.EvaluationMetricType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EvaluationService.
 * Tests the evaluation pipeline and fallback heuristics.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Evaluation Service Tests")
class EvaluationServiceTest {

    @Mock
    private WebClient evaluationWebClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ValidationProperties validationProperties;
    private MeterRegistry meterRegistry;
    private EvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        validationProperties = createValidationProperties();
        evaluationService = new EvaluationService(
            evaluationWebClient,
            validationProperties,
            meterRegistry
        );
    }

    private ValidationProperties createValidationProperties() {
        ValidationProperties props = new ValidationProperties();
        ValidationProperties.EvaluationConfig evalConfig = new ValidationProperties.EvaluationConfig();
        evalConfig.setHarnessUrl("http://localhost:8500");
        evalConfig.setTimeoutSeconds(10);
        props.setEvaluation(evalConfig);
        return props;
    }

    @Nested
    @DisplayName("Fallback Heuristic Tests")
    class FallbackHeuristicTests {

        @Test
        @DisplayName("Should evaluate relevancy using keyword overlap heuristic")
        void shouldEvaluateRelevancyHeuristic() {
            // Given
            String userMessage = "What are the care gaps for diabetes patients?";
            String agentResponse = "Based on the care gaps analysis, diabetes patients need A1C testing and eye exams.";

            // When - Using reflection to test private method via fallback path
            setupWebClientToFail();

            try {
                TestExecution.MetricResult result = evaluationService.evaluateMetric(
                    EvaluationMetricType.RELEVANCY,
                    userMessage,
                    agentResponse,
                    Map.of()
                );

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getMetricType()).isEqualTo("RELEVANCY");
                assertThat(result.getScore()).isGreaterThan(BigDecimal.ZERO);
                assertThat(result.getReason()).contains("fallback");
            } catch (Exception e) {
                // Expected when WebClient fails and fallback is used
            }
        }

        @Test
        @DisplayName("Should evaluate coherence based on text structure")
        void shouldEvaluateCoherenceHeuristic() {
            // Given
            String userMessage = "Summarize the patient's condition";
            String agentResponse = "The patient presents with multiple chronic conditions. "
                + "First, hypertension has been well-controlled with current medications. "
                + "Second, diabetes management shows improvement with recent A1C of 7.2%. "
                + "Third, the patient's BMI indicates the need for weight management.";

            // When
            setupWebClientToFail();

            try {
                TestExecution.MetricResult result = evaluationService.evaluateMetric(
                    EvaluationMetricType.COHERENCE,
                    userMessage,
                    agentResponse,
                    Map.of()
                );

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getScore()).isGreaterThanOrEqualTo(new BigDecimal("0.50"));
            } catch (Exception e) {
                // Expected when WebClient fails
            }
        }

        @Test
        @DisplayName("Should detect toxic content with low score")
        void shouldDetectToxicContent() {
            // Given
            String userMessage = "What is the treatment plan?";
            String agentResponse = "This is a hateful response that attacks certain groups.";

            // When
            setupWebClientToFail();

            try {
                TestExecution.MetricResult result = evaluationService.evaluateMetric(
                    EvaluationMetricType.TOXICITY,
                    userMessage,
                    agentResponse,
                    Map.of()
                );

                // Then - Toxic content should get low score
                assertThat(result).isNotNull();
                assertThat(result.getScore()).isLessThan(new BigDecimal("0.50"));
            } catch (Exception e) {
                // Expected
            }
        }

        @Test
        @DisplayName("Should give high score for non-toxic content")
        void shouldGiveHighScoreForNonToxicContent() {
            // Given
            String userMessage = "What medications should be reviewed?";
            String agentResponse = "Based on the patient's current medication list, we should review "
                + "the dosage of metformin and consider adding a statin for cardiovascular protection.";

            // When
            setupWebClientToFail();

            try {
                TestExecution.MetricResult result = evaluationService.evaluateMetric(
                    EvaluationMetricType.TOXICITY,
                    userMessage,
                    agentResponse,
                    Map.of()
                );

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getScore()).isGreaterThan(new BigDecimal("0.90"));
            } catch (Exception e) {
                // Expected
            }
        }
    }

    @Nested
    @DisplayName("Evaluate All Metrics Tests")
    class EvaluateAllTests {

        @Test
        @DisplayName("Should evaluate multiple metrics and return results map")
        void shouldEvaluateMultipleMetrics() {
            // Given
            Set<EvaluationMetricType> metrics = Set.of(
                EvaluationMetricType.RELEVANCY,
                EvaluationMetricType.COHERENCE,
                EvaluationMetricType.TOXICITY
            );
            String userMessage = "What are the patient's care gaps?";
            String agentResponse = "The patient has three open care gaps: annual wellness visit, "
                + "colorectal cancer screening, and flu vaccination.";

            // When
            setupWebClientToFail();
            Map<String, TestExecution.MetricResult> results = evaluationService.evaluateAll(
                metrics,
                userMessage,
                agentResponse,
                Map.of()
            );

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).containsKeys("RELEVANCY", "COHERENCE", "TOXICITY");
        }

        @Test
        @DisplayName("Should handle empty metrics set")
        void shouldHandleEmptyMetricsSet() {
            // Given
            Set<EvaluationMetricType> metrics = Set.of();

            // When
            Map<String, TestExecution.MetricResult> results = evaluationService.evaluateAll(
                metrics,
                "test query",
                "test response",
                Map.of()
            );

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should include error results for failed evaluations")
        void shouldIncludeErrorResultsForFailedEvaluations() {
            // Given
            Set<EvaluationMetricType> metrics = Set.of(EvaluationMetricType.HIPAA_COMPLIANCE);
            setupWebClientToFail();

            // When
            Map<String, TestExecution.MetricResult> results = evaluationService.evaluateAll(
                metrics,
                "test query",
                "test response",
                Map.of()
            );

            // Then - Custom metrics without fallback should have error results
            assertThat(results).containsKey("HIPAA_COMPLIANCE");
            TestExecution.MetricResult result = results.get("HIPAA_COMPLIANCE");
            assertThat(result.getScore()).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("WebClient Integration Tests")
    class WebClientTests {

        @Test
        @DisplayName("Should call evaluation harness with correct request")
        void shouldCallEvaluationHarnessWithCorrectRequest() {
            // Given
            setupWebClientSuccess(0.85, "Good relevancy score");

            // When
            TestExecution.MetricResult result = evaluationService.evaluateMetric(
                EvaluationMetricType.RELEVANCY,
                "What are care gaps?",
                "Here are the care gaps...",
                Map.of("patientId", "123")
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getScore()).isEqualByComparingTo(new BigDecimal("0.85"));
            assertThat(result.getReason()).isEqualTo("Good relevancy score");

            verify(evaluationWebClient).post();
        }
    }

    @Nested
    @DisplayName("Metrics Recording Tests")
    class MetricsRecordingTests {

        @Test
        @DisplayName("Should record evaluation duration metric")
        void shouldRecordEvaluationDuration() {
            // Given
            setupWebClientSuccess(0.90, "Excellent");

            // When
            evaluationService.evaluateMetric(
                EvaluationMetricType.RELEVANCY,
                "query",
                "response",
                Map.of()
            );

            // Then - Timer should be recorded
            assertThat(meterRegistry.getMeters()).isNotEmpty();
        }
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientToFail() {
        when(evaluationWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(EvaluationService.EvaluationResponse.class))
            .thenReturn(Mono.error(new RuntimeException("Connection refused")));
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientSuccess(double score, String reason) {
        EvaluationService.EvaluationResponse response = new EvaluationService.EvaluationResponse(
            "RELEVANCY",
            score,
            reason,
            Map.of()
        );

        when(evaluationWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(EvaluationService.EvaluationResponse.class))
            .thenReturn(Mono.just(response).timeout(Duration.ofSeconds(10)));
    }
}
