package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.client.AgentRuntimeClient;
import com.healthdata.agentvalidation.client.dto.AgentExecutionRequest;
import com.healthdata.agentvalidation.client.dto.AgentExecutionResponse;
import com.healthdata.agentvalidation.config.ValidationProperties;
import com.healthdata.agentvalidation.domain.entity.ProviderComparison;
import com.healthdata.agentvalidation.domain.entity.TestCase;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.enums.EvaluationMetricType;
import com.healthdata.agentvalidation.repository.ProviderComparisonRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for multi-provider A/B testing.
 * Compares quality, latency, and cost across Claude, Azure OpenAI, and Bedrock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderValidationService {

    private static final List<String> PROVIDERS = List.of("claude", "azure-openai", "bedrock");

    private final AgentRuntimeClient agentRuntimeClient;
    private final EvaluationService evaluationService;
    private final ProviderComparisonRepository providerComparisonRepository;
    private final ValidationProperties validationProperties;
    private final MeterRegistry meterRegistry;

    /**
     * Execute a test case with all available providers and compare results.
     */
    @Transactional
    public ProviderComparison compareProviders(TestCase testCase, String tenantId, String userId) {
        log.info("Comparing providers for test case: {}", testCase.getId());

        if (!validationProperties.getProviders().isComparisonEnabled()) {
            log.debug("Provider comparison is disabled");
            return null;
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        List<ProviderComparison.ProviderResult> results = new ArrayList<>();

        // Get healthy providers
        Map<String, AgentRuntimeClient.ProviderStatus> providerStatus;
        try {
            providerStatus = agentRuntimeClient.getProviderStatus();
        } catch (Exception e) {
            log.error("Failed to get provider status: {}", e.getMessage());
            providerStatus = Map.of();
        }

        // Execute with each healthy provider
        for (String provider : PROVIDERS) {
            AgentRuntimeClient.ProviderStatus status = providerStatus.get(provider);
            if (status != null && !status.healthy()) {
                log.info("Skipping unhealthy provider: {}", provider);
                continue;
            }

            try {
                ProviderComparison.ProviderResult result = executeWithProvider(
                    testCase, tenantId, userId, provider);
                results.add(result);

                // Record metrics
                meterRegistry.gauge(
                    "agent.validation.provider.score",
                    List.of(
                        io.micrometer.core.instrument.Tag.of("provider", provider),
                        io.micrometer.core.instrument.Tag.of("test_case", testCase.getId().toString())
                    ),
                    result.getEvaluationScore().doubleValue()
                );

            } catch (Exception e) {
                log.error("Error executing with provider {}: {}", provider, e.getMessage());
                results.add(createErrorResult(provider, e.getMessage()));
            }
        }

        // Analyze results
        ProviderComparison comparison = analyzeResults(testCase, tenantId, results);
        providerComparisonRepository.save(comparison);

        sample.stop(Timer.builder("agent.validation.provider.comparison.duration")
            .description("Provider comparison total duration")
            .register(meterRegistry));

        log.info("Provider comparison complete for test case {}: best quality={}, fastest={}, cheapest={}",
            testCase.getId(), comparison.getBestQualityProvider(),
            comparison.getFastestProvider(), comparison.getCheapestProvider());

        return comparison;
    }

    /**
     * Execute with a specific provider.
     */
    private ProviderComparison.ProviderResult executeWithProvider(
            TestCase testCase, String tenantId, String userId, String provider) {

        log.debug("Executing test case {} with provider {}", testCase.getId(), provider);

        String traceId = UUID.randomUUID().toString().replace("-", "");
        Instant startTime = Instant.now();

        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .agentType(testCase.getTestSuite().getAgentType())
            .userMessage(testCase.getUserMessage())
            .sessionId(UUID.randomUUID().toString())
            .contextData(testCase.getContextData())
            .maxIterations(10)
            .includeToolCalls(true)
            .includeTraceInfo(true)
            .build();

        try {
            AgentExecutionResponse response = agentRuntimeClient.executeAgentWithProvider(
                tenantId, userId, traceId, provider, request);

            // Evaluate response
            Map<String, BigDecimal> metricScores = new HashMap<>();
            BigDecimal totalScore = BigDecimal.ZERO;
            int metricCount = 0;

            for (EvaluationMetricType metricType : testCase.getRequiredMetrics()) {
                try {
                    TestExecution.MetricResult result = evaluationService.evaluateMetric(
                        metricType, testCase.getUserMessage(), response.getResponse(), testCase.getContextData());
                    metricScores.put(metricType.name(), result.getScore());
                    totalScore = totalScore.add(result.getScore());
                    metricCount++;
                } catch (Exception e) {
                    log.warn("Error evaluating metric {} for provider {}: {}",
                        metricType, provider, e.getMessage());
                }
            }

            BigDecimal avgScore = metricCount > 0 ?
                totalScore.divide(BigDecimal.valueOf(metricCount), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

            // Estimate cost
            BigDecimal cost = estimateCost(
                provider,
                response.getInputTokens(),
                response.getOutputTokens()
            );

            return ProviderComparison.ProviderResult.builder()
                .provider(provider)
                .modelVersion(response.getModelVersion())
                .response(response.getResponse())
                .evaluationScore(avgScore)
                .metricScores(metricScores)
                .latencyMs(response.getDurationMs())
                .inputTokens(response.getInputTokens())
                .outputTokens(response.getOutputTokens())
                .estimatedCost(cost)
                .success(true)
                .traceId(response.getTraceId())
                .build();

        } catch (Exception e) {
            return createErrorResult(provider, e.getMessage());
        }
    }

    /**
     * Create an error result for a failed provider execution.
     */
    private ProviderComparison.ProviderResult createErrorResult(String provider, String errorMessage) {
        return ProviderComparison.ProviderResult.builder()
            .provider(provider)
            .evaluationScore(BigDecimal.ZERO)
            .metricScores(Map.of())
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * Analyze provider results and build comparison.
     */
    private ProviderComparison analyzeResults(
            TestCase testCase, String tenantId, List<ProviderComparison.ProviderResult> results) {

        List<ProviderComparison.ProviderResult> successfulResults = results.stream()
            .filter(ProviderComparison.ProviderResult::isSuccess)
            .collect(Collectors.toList());

        String bestQualityProvider = null;
        BigDecimal bestQualityScore = BigDecimal.ZERO;
        String fastestProvider = null;
        Long fastestLatency = Long.MAX_VALUE;
        String cheapestProvider = null;
        BigDecimal cheapestCost = new BigDecimal("999999");

        Map<String, BigDecimal> costEstimates = new HashMap<>();

        for (ProviderComparison.ProviderResult result : successfulResults) {
            // Best quality
            if (result.getEvaluationScore().compareTo(bestQualityScore) > 0) {
                bestQualityScore = result.getEvaluationScore();
                bestQualityProvider = result.getProvider();
            }

            // Fastest
            if (result.getLatencyMs() != null && result.getLatencyMs() < fastestLatency) {
                fastestLatency = result.getLatencyMs();
                fastestProvider = result.getProvider();
            }

            // Cheapest
            if (result.getEstimatedCost() != null && result.getEstimatedCost().compareTo(cheapestCost) < 0) {
                cheapestCost = result.getEstimatedCost();
                cheapestProvider = result.getProvider();
            }

            costEstimates.put(result.getProvider(), result.getEstimatedCost());
        }

        // Generate recommendation
        String recommendation = generateRecommendation(
            bestQualityProvider, bestQualityScore,
            fastestProvider, fastestLatency,
            cheapestProvider, cheapestCost
        );

        return ProviderComparison.builder()
            .testCase(testCase)
            .tenantId(tenantId)
            .executionResults(results)
            .bestQualityProvider(bestQualityProvider)
            .bestQualityScore(bestQualityScore)
            .fastestProvider(fastestProvider)
            .fastestLatencyMs(fastestLatency < Long.MAX_VALUE ? fastestLatency : null)
            .cheapestProvider(cheapestProvider)
            .cheapestCost(cheapestCost.compareTo(new BigDecimal("999999")) < 0 ? cheapestCost : null)
            .costEstimates(costEstimates)
            .recommendation(recommendation)
            .executedAt(Instant.now())
            .build();
    }

    /**
     * Estimate cost based on token usage.
     */
    private BigDecimal estimateCost(String provider, Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null || outputTokens == null) {
            return null;
        }

        Map<String, BigDecimal> costPer1kTokens = validationProperties.getProviders().getCostEstimation();
        String costKey = provider + "-per-1k-tokens";

        BigDecimal rate = costPer1kTokens.getOrDefault(costKey, new BigDecimal("0.003"));
        int totalTokens = inputTokens + outputTokens;

        return rate.multiply(BigDecimal.valueOf(totalTokens))
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
    }

    /**
     * Generate recommendation based on comparison results.
     */
    private String generateRecommendation(
            String bestQuality, BigDecimal qualityScore,
            String fastest, Long latency,
            String cheapest, BigDecimal cost) {

        StringBuilder sb = new StringBuilder();

        if (bestQuality != null && bestQuality.equals(fastest) && bestQuality.equals(cheapest)) {
            sb.append(String.format("%s wins in all categories (quality: %.2f, latency: %dms, cost: $%.4f). ",
                bestQuality, qualityScore, latency, cost));
            sb.append("Recommended as the overall best choice.");
        } else {
            if (bestQuality != null) {
                sb.append(String.format("Use %s for quality-critical scenarios (score: %.2f). ",
                    bestQuality, qualityScore));
            }
            if (fastest != null && !fastest.equals(bestQuality)) {
                sb.append(String.format("Use %s for latency-sensitive cases (%dms). ",
                    fastest, latency));
            }
            if (cheapest != null && !cheapest.equals(bestQuality) && !cheapest.equals(fastest)) {
                sb.append(String.format("Use %s for cost optimization ($%.4f). ",
                    cheapest, cost));
            }
        }

        return sb.toString().trim();
    }
}
