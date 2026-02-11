package com.healthdata.agentvalidation.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Prometheus metrics configuration for Agent Validation Service.
 * Defines custom metrics for test execution, evaluation, and quality tracking.
 */
@Configuration
public class MetricsConfig {

    private final Map<String, AtomicReference<Double>> scoreGauges = new ConcurrentHashMap<>();

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    // ============================================
    // Test Execution Metrics
    // ============================================

    @Bean
    public Counter testExecutionCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.test.executed")
                .description("Total test executions")
                .tag("type", "total")
                .register(registry);
    }

    @Bean
    public Counter testPassedCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.test.executed")
                .description("Passed test executions")
                .tag("type", "passed")
                .register(registry);
    }

    @Bean
    public Counter testFailedCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.test.executed")
                .description("Failed test executions")
                .tag("type", "failed")
                .register(registry);
    }

    @Bean
    public Timer testExecutionTimer(MeterRegistry registry) {
        return Timer.builder("agent.validation.test.duration")
                .description("Test execution duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    // ============================================
    // Evaluation Metrics
    // ============================================

    @Bean
    public Counter evaluationCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.evaluation.executed")
                .description("Total evaluations executed")
                .register(registry);
    }

    @Bean
    public Timer evaluationTimer(MeterRegistry registry) {
        return Timer.builder("agent.validation.evaluation.duration")
                .description("Evaluation duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    // ============================================
    // Reflection Metrics
    // ============================================

    @Bean
    public Counter reflectionCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.reflection.executed")
                .description("Total reflections executed")
                .register(registry);
    }

    @Bean
    public Counter calibrationAlertCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.reflection.calibration_alerts")
                .description("Confidence calibration alerts triggered")
                .register(registry);
    }

    // ============================================
    // Regression Testing Metrics
    // ============================================

    @Bean
    public Counter regressionCheckCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.regression.checks")
                .description("Total regression checks performed")
                .register(registry);
    }

    @Bean
    public Counter regressionAlertCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.regression.alerts")
                .description("Regression alerts triggered")
                .register(registry);
    }

    // ============================================
    // Provider Comparison Metrics
    // ============================================

    @Bean
    public Counter providerComparisonCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.provider.comparisons")
                .description("Total provider comparisons executed")
                .register(registry);
    }

    // ============================================
    // QA Integration Metrics
    // ============================================

    @Bean
    public Counter qaFlaggedCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.qa.flagged")
                .description("Responses flagged for QA review")
                .register(registry);
    }

    @Bean
    public Counter qaApprovedCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.qa.decisions")
                .description("QA review decisions")
                .tag("decision", "approved")
                .register(registry);
    }

    @Bean
    public Counter qaRejectedCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.qa.decisions")
                .description("QA review decisions")
                .tag("decision", "rejected")
                .register(registry);
    }

    // ============================================
    // Trace Correlation Metrics
    // ============================================

    @Bean
    public Counter traceCorrelationCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.trace.correlations")
                .description("Total trace correlations performed")
                .register(registry);
    }

    @Bean
    public Counter traceCorrelationFailureCounter(MeterRegistry registry) {
        return Counter.builder("agent.validation.trace.correlation_failures")
                .description("Failed trace correlations")
                .register(registry);
    }

    // ============================================
    // Score Gauge Factory
    // ============================================

    /**
     * Creates or retrieves a gauge for tracking metric scores.
     * Gauges are created dynamically based on test suite and metric type.
     */
    public void recordScore(MeterRegistry registry, String testSuite, String metricType, double score) {
        String key = testSuite + "." + metricType;
        AtomicReference<Double> ref = scoreGauges.computeIfAbsent(key, k -> {
            AtomicReference<Double> newRef = new AtomicReference<>(score);
            Gauge.builder("agent.validation.test.score", newRef, AtomicReference::get)
                    .description("Test score by metric type")
                    .tag("test_suite", testSuite)
                    .tag("metric_type", metricType)
                    .register(registry);
            return newRef;
        });
        ref.set(score);
    }

    /**
     * Records provider-specific quality score.
     */
    public void recordProviderScore(MeterRegistry registry, String provider, String testCase, double score) {
        String key = "provider." + provider + "." + testCase;
        AtomicReference<Double> ref = scoreGauges.computeIfAbsent(key, k -> {
            AtomicReference<Double> newRef = new AtomicReference<>(score);
            Gauge.builder("agent.validation.provider.score", newRef, AtomicReference::get)
                    .description("Quality score by provider")
                    .tag("provider", provider)
                    .tag("test_case", testCase)
                    .register(registry);
            return newRef;
        });
        ref.set(score);
    }

    /**
     * Records regression similarity score.
     */
    public void recordSimilarityScore(MeterRegistry registry, String testCase, double similarity) {
        String key = "regression." + testCase;
        AtomicReference<Double> ref = scoreGauges.computeIfAbsent(key, k -> {
            AtomicReference<Double> newRef = new AtomicReference<>(similarity);
            Gauge.builder("agent.validation.regression.similarity", newRef, AtomicReference::get)
                    .description("Semantic similarity to golden response")
                    .tag("test_case", testCase)
                    .register(registry);
            return newRef;
        });
        ref.set(similarity);
    }

    /**
     * Records confidence calibration delta.
     */
    public void recordConfidenceDelta(MeterRegistry registry, String agentType, double delta) {
        String key = "confidence." + agentType;
        AtomicReference<Double> ref = scoreGauges.computeIfAbsent(key, k -> {
            AtomicReference<Double> newRef = new AtomicReference<>(delta);
            Gauge.builder("agent.validation.reflection.confidence_delta", newRef, AtomicReference::get)
                    .description("Self-assessed vs external confidence delta")
                    .tag("agent_type", agentType)
                    .register(registry);
            return newRef;
        });
        ref.set(delta);
    }
}
