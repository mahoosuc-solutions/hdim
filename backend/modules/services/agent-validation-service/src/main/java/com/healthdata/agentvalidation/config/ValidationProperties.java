package com.healthdata.agentvalidation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the Agent Validation Service.
 */
@Data
@Component
@ConfigurationProperties(prefix = "hdim.validation")
public class ValidationProperties {

    private OrchestratorConfig orchestrator = new OrchestratorConfig();
    private JaegerConfig jaeger = new JaegerConfig();
    private EvaluationConfig evaluation = new EvaluationConfig();
    private ReflectionConfig reflection = new ReflectionConfig();
    private RegressionConfig regression = new RegressionConfig();
    private QaConfig qa = new QaConfig();
    private ProvidersConfig providers = new ProvidersConfig();

    @Data
    public static class OrchestratorConfig {
        private int maxConcurrentTests = 10;
        private int testTimeoutSeconds = 300;
        private BigDecimal defaultPassThreshold = new BigDecimal("0.80");
    }

    @Data
    public static class JaegerConfig {
        private String apiUrl = "http://localhost:16686";
        private int traceFetchTimeoutSeconds = 30;
        private int maxSpansPerTrace = 1000;
    }

    @Data
    public static class EvaluationConfig {
        private String harnessUrl = "http://localhost:8500";
        private int timeoutSeconds = 60;
        private List<String> defaultMetrics = List.of(
            "RELEVANCY", "FAITHFULNESS", "HALLUCINATION", "HIPAA_COMPLIANCE"
        );
    }

    @Data
    public static class ReflectionConfig {
        private boolean enabled = true;
        private BigDecimal calibrationThreshold = new BigDecimal("0.15");
        private String promptTemplate;
    }

    @Data
    public static class RegressionConfig {
        private BigDecimal similarityThreshold = new BigDecimal("0.85");
        private BigDecimal qualityDegradationThreshold = new BigDecimal("0.10");
        private String embeddingModel = "text-embedding-3-small";
    }

    @Data
    public static class QaConfig {
        private BigDecimal autoFlagScoreThreshold = new BigDecimal("0.70");
        private BigDecimal autoFlagMetricThreshold = new BigDecimal("0.70");
        private BigDecimal confidenceMiscalibrationThreshold = new BigDecimal("0.20");
        private List<String> flagHarmLevels = List.of("MEDIUM", "HIGH");
    }

    @Data
    public static class ProvidersConfig {
        private boolean comparisonEnabled = true;
        private Map<String, BigDecimal> costEstimation = Map.of(
            "claude-per-1k-tokens", new BigDecimal("0.003"),
            "azure-openai-per-1k-tokens", new BigDecimal("0.002"),
            "bedrock-per-1k-tokens", new BigDecimal("0.0015")
        );
    }
}
