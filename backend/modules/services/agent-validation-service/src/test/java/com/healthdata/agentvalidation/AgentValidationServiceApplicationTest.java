package com.healthdata.agentvalidation;

import com.healthdata.agentvalidation.config.ValidationProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Configuration and properties tests for Agent Validation Service.
 * Verifies that the ValidationProperties are loaded correctly from configuration.
 * <p>
 * Note: This test uses a minimal Spring context to test configuration loading
 * without requiring a full database connection. Full integration tests are
 * available in TestSuiteRepositoryIntegrationTest.
 */
@Tag("unit")
@SpringBootTest(classes = {ValidationProperties.class})
@EnableConfigurationProperties(ValidationProperties.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "hdim.validation.orchestrator.max-concurrent-tests=2",
    "hdim.validation.orchestrator.test-timeout-seconds=30",
    "hdim.validation.orchestrator.default-pass-threshold=0.80",
    "hdim.validation.evaluation.harness-url=http://localhost:8500",
    "hdim.validation.evaluation.timeout-seconds=10",
    "hdim.validation.jaeger.api-url=http://localhost:16686",
    "hdim.validation.jaeger.trace-fetch-timeout-seconds=5",
    "hdim.validation.reflection.enabled=false",
    "hdim.validation.reflection.calibration-threshold=0.15",
    "hdim.validation.regression.similarity-threshold=0.85",
    "hdim.validation.regression.quality-degradation-threshold=0.10",
    "hdim.validation.qa.auto-flag-score-threshold=0.70",
    "hdim.validation.qa.auto-flag-metric-threshold=0.70",
    "hdim.validation.qa.confidence-miscalibration-threshold=0.20",
    "hdim.validation.providers.comparison-enabled=false"
})
@DisplayName("Agent Validation Service Configuration Tests")
class AgentValidationServiceApplicationTest {

    @Autowired
    private ValidationProperties validationProperties;

    @Nested
    @DisplayName("ValidationProperties Loading Tests")
    class ValidationPropertiesTests {

        @Test
        @DisplayName("Should load validation properties")
        void shouldLoadValidationProperties() {
            assertThat(validationProperties).isNotNull();
        }

        @Test
        @DisplayName("Should have orchestrator config")
        void shouldHaveOrchestratorConfig() {
            assertThat(validationProperties.getOrchestrator()).isNotNull();
        }

        @Test
        @DisplayName("Should have evaluation config")
        void shouldHaveEvaluationConfig() {
            assertThat(validationProperties.getEvaluation()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Orchestrator Configuration Tests")
    class OrchestratorConfigTests {

        @Test
        @DisplayName("Should have max concurrent tests configured")
        void shouldHaveMaxConcurrentTests() {
            assertThat(validationProperties.getOrchestrator().getMaxConcurrentTests()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should have test timeout configured")
        void shouldHaveTestTimeout() {
            assertThat(validationProperties.getOrchestrator().getTestTimeoutSeconds()).isEqualTo(30);
        }

        @Test
        @DisplayName("Should have default pass threshold configured")
        void shouldHaveDefaultPassThreshold() {
            assertThat(validationProperties.getOrchestrator().getDefaultPassThreshold())
                .isEqualByComparingTo(new java.math.BigDecimal("0.80"));
        }
    }

    @Nested
    @DisplayName("Evaluation Configuration Tests")
    class EvaluationConfigTests {

        @Test
        @DisplayName("Should have harness URL configured")
        void shouldHaveHarnessUrl() {
            assertThat(validationProperties.getEvaluation().getHarnessUrl())
                .isEqualTo("http://localhost:8500");
        }

        @Test
        @DisplayName("Should have timeout configured")
        void shouldHaveTimeout() {
            assertThat(validationProperties.getEvaluation().getTimeoutSeconds()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Jaeger Configuration Tests")
    class JaegerConfigTests {

        @Test
        @DisplayName("Should have Jaeger API URL configured")
        void shouldHaveJaegerApiUrl() {
            assertThat(validationProperties.getJaeger().getApiUrl())
                .isEqualTo("http://localhost:16686");
        }

        @Test
        @DisplayName("Should have trace fetch timeout configured")
        void shouldHaveTraceFetchTimeout() {
            assertThat(validationProperties.getJaeger().getTraceFetchTimeoutSeconds()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Reflection Configuration Tests")
    class ReflectionConfigTests {

        @Test
        @DisplayName("Should have reflection disabled")
        void shouldHaveReflectionDisabled() {
            assertThat(validationProperties.getReflection().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should have calibration threshold configured")
        void shouldHaveCalibrationThreshold() {
            assertThat(validationProperties.getReflection().getCalibrationThreshold())
                .isEqualByComparingTo(new java.math.BigDecimal("0.15"));
        }
    }

    @Nested
    @DisplayName("Regression Configuration Tests")
    class RegressionConfigTests {

        @Test
        @DisplayName("Should have similarity threshold configured")
        void shouldHaveSimilarityThreshold() {
            assertThat(validationProperties.getRegression().getSimilarityThreshold())
                .isEqualByComparingTo(new java.math.BigDecimal("0.85"));
        }

        @Test
        @DisplayName("Should have quality degradation threshold configured")
        void shouldHaveQualityDegradationThreshold() {
            assertThat(validationProperties.getRegression().getQualityDegradationThreshold())
                .isEqualByComparingTo(new java.math.BigDecimal("0.10"));
        }
    }

    @Nested
    @DisplayName("QA Configuration Tests")
    class QaConfigTests {

        @Test
        @DisplayName("Should have auto-flag score threshold configured")
        void shouldHaveAutoFlagScoreThreshold() {
            assertThat(validationProperties.getQa().getAutoFlagScoreThreshold())
                .isEqualByComparingTo(new java.math.BigDecimal("0.70"));
        }

        @Test
        @DisplayName("Should have auto-flag metric threshold configured")
        void shouldHaveAutoFlagMetricThreshold() {
            assertThat(validationProperties.getQa().getAutoFlagMetricThreshold())
                .isEqualByComparingTo(new java.math.BigDecimal("0.70"));
        }

        @Test
        @DisplayName("Should have confidence miscalibration threshold configured")
        void shouldHaveConfidenceMiscalibrationThreshold() {
            assertThat(validationProperties.getQa().getConfidenceMiscalibrationThreshold())
                .isEqualByComparingTo(new java.math.BigDecimal("0.20"));
        }
    }

    @Nested
    @DisplayName("Providers Configuration Tests")
    class ProvidersConfigTests {

        @Test
        @DisplayName("Should have comparison disabled")
        void shouldHaveComparisonDisabled() {
            assertThat(validationProperties.getProviders().isComparisonEnabled()).isFalse();
        }
    }
}
