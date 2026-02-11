package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.config.ValidationProperties;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.enums.EvaluationMetricType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Service for evaluating agent responses using DeepEval metrics.
 * Bridges Java service to Python evaluation harness.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final WebClient evaluationWebClient;
    private final ValidationProperties validationProperties;
    private final MeterRegistry meterRegistry;

    /**
     * Evaluate a response against all required metrics.
     */
    public Map<String, TestExecution.MetricResult> evaluateAll(
            Set<EvaluationMetricType> requiredMetrics,
            String userMessage,
            String agentResponse,
            Map<String, Object> contextData) {

        Map<String, TestExecution.MetricResult> results = new HashMap<>();

        for (EvaluationMetricType metricType : requiredMetrics) {
            try {
                TestExecution.MetricResult result = evaluateMetric(
                    metricType, userMessage, agentResponse, contextData);
                results.put(metricType.name(), result);
            } catch (Exception e) {
                log.error("Error evaluating metric {}: {}", metricType, e.getMessage());
                results.put(metricType.name(), createErrorResult(metricType, e.getMessage()));
            }
        }

        return results;
    }

    /**
     * Evaluate a single metric.
     */
    public TestExecution.MetricResult evaluateMetric(
            EvaluationMetricType metricType,
            String userMessage,
            String agentResponse,
            Map<String, Object> contextData) {

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.debug("Evaluating metric {} for response length {}", metricType, agentResponse.length());

            // Build request for Python harness
            EvaluationRequest request = new EvaluationRequest(
                metricType.name(),
                userMessage,
                agentResponse,
                contextData
            );

            // Call Python evaluation harness
            EvaluationResponse response = evaluationWebClient.post()
                .uri("/evaluate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EvaluationResponse.class)
                .timeout(Duration.ofSeconds(validationProperties.getEvaluation().getTimeoutSeconds()))
                .block();

            if (response == null) {
                throw new RuntimeException("No response from evaluation harness");
            }

            // Record metric to Prometheus
            meterRegistry.gauge(
                "agent.validation.metric.score",
                response.score(),
                score -> score
            );

            return TestExecution.MetricResult.builder()
                .metricType(metricType.name())
                .score(BigDecimal.valueOf(response.score()))
                .reason(response.reason())
                .metadata(response.metadata())
                .build();

        } catch (Exception e) {
            log.error("Error calling evaluation harness for metric {}: {}", metricType, e.getMessage());

            // For native DeepEval metrics, we can try a fallback approach
            if (metricType.isDeepEvalNative()) {
                return evaluateNativeMetricFallback(metricType, userMessage, agentResponse);
            }

            throw e;
        } finally {
            sample.stop(Timer.builder("agent.validation.evaluation.duration")
                .tag("metric", metricType.name())
                .description("Evaluation metric duration")
                .register(meterRegistry));
        }
    }

    /**
     * Fallback evaluation for native DeepEval metrics when harness is unavailable.
     * Uses heuristic-based scoring.
     */
    private TestExecution.MetricResult evaluateNativeMetricFallback(
            EvaluationMetricType metricType,
            String userMessage,
            String agentResponse) {

        log.warn("Using fallback evaluation for metric {}", metricType);

        BigDecimal score;
        String reason;

        switch (metricType) {
            case RELEVANCY:
                score = evaluateRelevancyHeuristic(userMessage, agentResponse);
                reason = "Heuristic-based relevancy scoring (fallback)";
                break;
            case COHERENCE:
                score = evaluateCoherenceHeuristic(agentResponse);
                reason = "Heuristic-based coherence scoring (fallback)";
                break;
            case TOXICITY:
                score = evaluateToxicityHeuristic(agentResponse);
                reason = "Keyword-based toxicity check (fallback)";
                break;
            default:
                score = new BigDecimal("0.50");
                reason = "Unable to evaluate - harness unavailable";
        }

        return TestExecution.MetricResult.builder()
            .metricType(metricType.name())
            .score(score)
            .reason(reason)
            .metadata(Map.of("fallback", true))
            .build();
    }

    /**
     * Heuristic relevancy scoring based on keyword overlap.
     */
    private BigDecimal evaluateRelevancyHeuristic(String userMessage, String agentResponse) {
        String[] queryWords = userMessage.toLowerCase().split("\\s+");
        String responseLower = agentResponse.toLowerCase();

        int matchCount = 0;
        for (String word : queryWords) {
            if (word.length() > 3 && responseLower.contains(word)) {
                matchCount++;
            }
        }

        double ratio = queryWords.length > 0 ?
            (double) matchCount / queryWords.length : 0.0;

        // Scale to reasonable range
        return BigDecimal.valueOf(Math.min(0.95, 0.50 + (ratio * 0.45)));
    }

    /**
     * Heuristic coherence scoring based on structure.
     */
    private BigDecimal evaluateCoherenceHeuristic(String agentResponse) {
        // Check for basic coherence indicators
        boolean hasProperLength = agentResponse.length() >= 50;
        boolean hasSentences = agentResponse.contains(".");
        boolean hasStructure = agentResponse.contains("\n") || agentResponse.contains(",");
        boolean notRepetitive = !hasExcessiveRepetition(agentResponse);

        int score = 0;
        if (hasProperLength) score++;
        if (hasSentences) score++;
        if (hasStructure) score++;
        if (notRepetitive) score++;

        return BigDecimal.valueOf(0.50 + (score * 0.125));
    }

    /**
     * Heuristic toxicity check based on keywords.
     */
    private BigDecimal evaluateToxicityHeuristic(String agentResponse) {
        String[] toxicPatterns = {
            "hate", "kill", "attack", "violent", "abuse",
            "discriminate", "racist", "sexist"
        };

        String responseLower = agentResponse.toLowerCase();
        for (String pattern : toxicPatterns) {
            if (responseLower.contains(pattern)) {
                return new BigDecimal("0.30"); // Low score = toxic content detected
            }
        }

        return new BigDecimal("0.95"); // High score = no toxic content
    }

    /**
     * Check for excessive repetition in text.
     */
    private boolean hasExcessiveRepetition(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        if (words.length < 20) return false;

        Map<String, Integer> wordCounts = new HashMap<>();
        for (String word : words) {
            wordCounts.merge(word, 1, Integer::sum);
        }

        // Check if any word appears more than 20% of the time
        int threshold = words.length / 5;
        return wordCounts.values().stream().anyMatch(count -> count > threshold);
    }

    /**
     * Create an error result.
     */
    private TestExecution.MetricResult createErrorResult(EvaluationMetricType metricType, String errorMessage) {
        return TestExecution.MetricResult.builder()
            .metricType(metricType.name())
            .score(BigDecimal.ZERO)
            .passed(false)
            .reason("Evaluation error: " + errorMessage)
            .metadata(Map.of("error", true))
            .build();
    }

    // Request/Response DTOs for Python harness
    public record EvaluationRequest(
        String metricType,
        String userMessage,
        String agentResponse,
        Map<String, Object> contextData
    ) {}

    public record EvaluationResponse(
        String metricType,
        double score,
        String reason,
        Map<String, Object> metadata
    ) {}
}
