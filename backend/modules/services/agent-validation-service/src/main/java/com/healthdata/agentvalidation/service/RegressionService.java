package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.config.ValidationProperties;
import com.healthdata.agentvalidation.domain.entity.GoldenResponse;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.repository.GoldenResponseRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for regression testing against golden responses.
 * Detects behavioral drift in agent outputs using semantic similarity.
 */
@Slf4j
@Service
public class RegressionService {

    private final GoldenResponseRepository goldenResponseRepository;
    private final WebClient evaluationWebClient;
    private final ValidationProperties validationProperties;
    private final MeterRegistry meterRegistry;

    private final Counter regressionDetectedCounter;
    private final AtomicReference<Double> lastSimilarityScore = new AtomicReference<>(1.0);

    public RegressionService(
            GoldenResponseRepository goldenResponseRepository,
            WebClient evaluationWebClient,
            ValidationProperties validationProperties,
            MeterRegistry meterRegistry) {
        this.goldenResponseRepository = goldenResponseRepository;
        this.evaluationWebClient = evaluationWebClient;
        this.validationProperties = validationProperties;
        this.meterRegistry = meterRegistry;

        this.regressionDetectedCounter = Counter.builder("agent.validation.regression.detected")
            .description("Count of regression detections")
            .register(meterRegistry);

        Gauge.builder("agent.validation.regression.similarity", lastSimilarityScore, AtomicReference::get)
            .description("Last semantic similarity score to golden response")
            .register(meterRegistry);
    }

    /**
     * Compare a response to its golden baseline.
     */
    public TestExecution.RegressionResult compareToGolden(
            String currentResponse, GoldenResponse goldenResponse) {

        if (goldenResponse == null) {
            log.debug("No golden response available for comparison");
            return null;
        }

        log.debug("Comparing response to golden response {}", goldenResponse.getId());

        try {
            // Calculate semantic similarity
            BigDecimal similarity = calculateSemanticSimilarity(currentResponse, goldenResponse.getResponse());

            // Calculate quality delta
            BigDecimal qualityDelta = calculateQualityDelta(goldenResponse);

            // Check for regression
            boolean regressionDetected = isRegressionDetected(similarity, qualityDelta);

            if (regressionDetected) {
                regressionDetectedCounter.increment();
                log.warn("Regression detected: similarity={}, threshold={}",
                    similarity, validationProperties.getRegression().getSimilarityThreshold());
            }

            lastSimilarityScore.set(similarity.doubleValue());

            // Record metric
            meterRegistry.gauge(
                "agent.validation.regression.similarity_score",
                List.of(
                    io.micrometer.core.instrument.Tag.of("golden_id", goldenResponse.getId().toString())
                ),
                similarity.doubleValue()
            );

            return TestExecution.RegressionResult.builder()
                .goldenResponseId(goldenResponse.getId())
                .semanticSimilarity(similarity)
                .qualityDelta(qualityDelta)
                .regressionDetected(regressionDetected)
                .comparisonDetails(buildComparisonDetails(similarity, qualityDelta, regressionDetected))
                .build();

        } catch (Exception e) {
            log.error("Error comparing to golden response: {}", e.getMessage(), e);
            return TestExecution.RegressionResult.builder()
                .goldenResponseId(goldenResponse.getId())
                .regressionDetected(false)
                .comparisonDetails("Error during comparison: " + e.getMessage())
                .build();
        }
    }

    /**
     * Calculate semantic similarity between two responses.
     */
    private BigDecimal calculateSemanticSimilarity(String current, String golden) {
        try {
            // Try to call embedding service
            EmbeddingResponse currentEmb = getEmbedding(current);
            EmbeddingResponse goldenEmb = getEmbedding(golden);

            if (currentEmb != null && goldenEmb != null) {
                return calculateCosineSimilarity(currentEmb.embedding(), goldenEmb.embedding());
            }
        } catch (Exception e) {
            log.warn("Failed to get embeddings, using fallback similarity: {}", e.getMessage());
        }

        // Fallback: Use lexical similarity
        return calculateLexicalSimilarity(current, golden);
    }

    /**
     * Get embedding from evaluation harness.
     */
    private EmbeddingResponse getEmbedding(String text) {
        try {
            return evaluationWebClient.post()
                .uri("/embed")
                .bodyValue(Map.of(
                    "text", text,
                    "model", validationProperties.getRegression().getEmbeddingModel()
                ))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();
        } catch (Exception e) {
            log.debug("Embedding service unavailable: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate cosine similarity between two vectors.
     */
    private BigDecimal calculateCosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return BigDecimal.ZERO;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(dotProduct / denominator)
            .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Calculate lexical similarity using Jaccard coefficient.
     */
    private BigDecimal calculateLexicalSimilarity(String current, String golden) {
        Set<String> currentTokens = tokenize(current);
        Set<String> goldenTokens = tokenize(golden);

        Set<String> intersection = new HashSet<>(currentTokens);
        intersection.retainAll(goldenTokens);

        Set<String> union = new HashSet<>(currentTokens);
        union.addAll(goldenTokens);

        if (union.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double jaccard = (double) intersection.size() / union.size();

        // Scale Jaccard to be more comparable to cosine similarity
        // (Jaccard tends to be lower, so we adjust)
        double adjusted = 0.5 + (jaccard * 0.5);

        return BigDecimal.valueOf(adjusted).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Tokenize text for lexical comparison.
     */
    private Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        return Arrays.stream(text.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .split("\\s+"))
            .filter(token -> token.length() > 2)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Calculate quality delta (placeholder - would use evaluation scores).
     */
    private BigDecimal calculateQualityDelta(GoldenResponse goldenResponse) {
        // In full implementation, would compare current eval score to golden's eval score
        // For now, return 0 (no delta)
        return BigDecimal.ZERO;
    }

    /**
     * Determine if regression is detected based on thresholds.
     */
    private boolean isRegressionDetected(BigDecimal similarity, BigDecimal qualityDelta) {
        BigDecimal similarityThreshold = validationProperties.getRegression().getSimilarityThreshold();
        BigDecimal qualityThreshold = validationProperties.getRegression().getQualityDegradationThreshold();

        // Regression if similarity is below threshold
        if (similarity.compareTo(similarityThreshold) < 0) {
            return true;
        }

        // Regression if quality degradation exceeds threshold (negative delta = worse quality)
        if (qualityDelta.negate().compareTo(qualityThreshold) > 0) {
            return true;
        }

        return false;
    }

    /**
     * Build human-readable comparison details.
     */
    private String buildComparisonDetails(BigDecimal similarity, BigDecimal qualityDelta, boolean regression) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Semantic similarity: %.2f%% (threshold: %.2f%%). ",
            similarity.multiply(BigDecimal.valueOf(100)),
            validationProperties.getRegression().getSimilarityThreshold().multiply(BigDecimal.valueOf(100))));

        if (!qualityDelta.equals(BigDecimal.ZERO)) {
            sb.append(String.format("Quality delta: %+.2f%%. ", qualityDelta.multiply(BigDecimal.valueOf(100))));
        }

        if (regression) {
            sb.append("REGRESSION DETECTED: Response has drifted significantly from baseline.");
        } else {
            sb.append("No regression detected.");
        }

        return sb.toString();
    }

    /**
     * Create a new golden response.
     */
    @Transactional
    public GoldenResponse createGoldenResponse(
            TestExecution execution,
            String approvedBy,
            String approvalNotes) {

        // Archive any existing golden response for this test case
        goldenResponseRepository.findByTestCaseIdAndArchivedFalse(execution.getTestCase().getId())
            .ifPresent(existing -> {
                existing.archive(approvedBy, "Superseded by new golden response");
                goldenResponseRepository.save(existing);
            });

        // Get embedding for the response
        float[] embedding = null;
        try {
            EmbeddingResponse embResponse = getEmbedding(execution.getAgentResponse());
            if (embResponse != null) {
                embedding = embResponse.embedding();
            }
        } catch (Exception e) {
            log.warn("Failed to compute embedding for golden response: {}", e.getMessage());
        }

        GoldenResponse golden = GoldenResponse.builder()
            .testCase(execution.getTestCase())
            .response(execution.getAgentResponse())
            .embeddingVector(embedding)
            .evaluationScore(execution.getEvaluationScore())
            .metricResults(execution.getMetricResults() != null ?
                new HashMap<>(execution.getMetricResults().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (Object) e.getValue()
                    ))) : null)
            .approvedBy(approvedBy)
            .approvedAt(Instant.now())
            .approvalNotes(approvalNotes)
            .llmProvider(execution.getLlmProvider())
            .build();

        return goldenResponseRepository.save(golden);
    }

    /**
     * Archive a golden response.
     */
    @Transactional
    public GoldenResponse archiveGoldenResponse(UUID goldenId, String archivedBy, String reason) {
        return goldenResponseRepository.findById(goldenId)
            .map(golden -> {
                golden.archive(archivedBy, reason);
                return goldenResponseRepository.save(golden);
            })
            .orElseThrow(() -> new IllegalArgumentException("Golden response not found: " + goldenId));
    }

    // DTOs
    private record EmbeddingResponse(float[] embedding, String model) {}
}
