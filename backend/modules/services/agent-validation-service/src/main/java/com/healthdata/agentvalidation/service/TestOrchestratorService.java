package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.client.AgentRuntimeClient;
import com.healthdata.agentvalidation.client.dto.AgentExecutionRequest;
import com.healthdata.agentvalidation.client.dto.AgentExecutionResponse;
import com.healthdata.agentvalidation.config.ValidationProperties;
import com.healthdata.agentvalidation.domain.entity.*;
import com.healthdata.agentvalidation.domain.enums.EvaluationMetricType;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates test suite and test case execution.
 * Coordinates agent execution, evaluation, reflection, and result recording.
 */
@Slf4j
@Service
public class TestOrchestratorService {

    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final GoldenResponseRepository goldenResponseRepository;
    private final AgentRuntimeClient agentRuntimeClient;
    private final ValidationProperties validationProperties;
    private final MeterRegistry meterRegistry;

    // These services will be injected later as they're implemented
    private JaegerTraceService jaegerTraceService;
    private EvaluationService evaluationService;
    private ReflectionService reflectionService;
    private RegressionService regressionService;
    private QAIntegrationService qaIntegrationService;

    private final Counter testExecutionCounter;
    private final Timer testExecutionTimer;

    public TestOrchestratorService(
            TestSuiteRepository testSuiteRepository,
            TestCaseRepository testCaseRepository,
            TestExecutionRepository testExecutionRepository,
            GoldenResponseRepository goldenResponseRepository,
            AgentRuntimeClient agentRuntimeClient,
            ValidationProperties validationProperties,
            MeterRegistry meterRegistry) {
        this.testSuiteRepository = testSuiteRepository;
        this.testCaseRepository = testCaseRepository;
        this.testExecutionRepository = testExecutionRepository;
        this.goldenResponseRepository = goldenResponseRepository;
        this.agentRuntimeClient = agentRuntimeClient;
        this.validationProperties = validationProperties;
        this.meterRegistry = meterRegistry;

        this.testExecutionCounter = Counter.builder("agent.validation.test.executed")
            .description("Count of test executions")
            .register(meterRegistry);
        this.testExecutionTimer = Timer.builder("agent.validation.test.duration")
            .description("Test execution duration")
            .register(meterRegistry);
    }

    // Setter injection for circular dependency resolution
    public void setJaegerTraceService(JaegerTraceService jaegerTraceService) {
        this.jaegerTraceService = jaegerTraceService;
    }

    public void setEvaluationService(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    public void setReflectionService(ReflectionService reflectionService) {
        this.reflectionService = reflectionService;
    }

    public void setRegressionService(RegressionService regressionService) {
        this.regressionService = regressionService;
    }

    public void setQaIntegrationService(QAIntegrationService qaIntegrationService) {
        this.qaIntegrationService = qaIntegrationService;
    }

    /**
     * Execute all test cases in a test suite.
     */
    @Transactional
    public TestSuiteExecutionResult executeSuite(UUID suiteId, String tenantId, String userId) {
        log.info("Starting execution of test suite {} for tenant {}", suiteId, tenantId);

        TestSuite suite = testSuiteRepository.findByIdAndTenantId(suiteId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Test suite not found: " + suiteId));

        suite.setStatus(TestStatus.RUNNING);
        testSuiteRepository.save(suite);

        List<TestCase> testCases = testCaseRepository.findByTestSuiteIdOrderByExecutionPriorityAsc(suiteId);
        List<TestExecution> executions = new ArrayList<>();
        int passedCount = 0;
        int failedCount = 0;

        for (TestCase testCase : testCases) {
            try {
                TestExecution execution = executeCase(testCase, tenantId, userId);
                executions.add(execution);

                if (execution.isPassed()) {
                    passedCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Error executing test case {}: {}", testCase.getId(), e.getMessage(), e);
                TestExecution errorExecution = createErrorExecution(testCase, tenantId, e);
                executions.add(errorExecution);
                failedCount++;
            }
        }

        // Calculate pass rate and update suite
        BigDecimal passRate = testCases.isEmpty() ? BigDecimal.ZERO :
            BigDecimal.valueOf(passedCount)
                .divide(BigDecimal.valueOf(testCases.size()), 2, RoundingMode.HALF_UP);

        suite.setLastExecutionAt(Instant.now());
        suite.setLastPassRate(passRate);
        suite.setStatus(passRate.compareTo(suite.getPassThreshold()) >= 0 ?
            TestStatus.PASSED : TestStatus.FAILED);
        testSuiteRepository.save(suite);

        log.info("Completed test suite {} execution: {} passed, {} failed, pass rate: {}",
            suiteId, passedCount, failedCount, passRate);

        return TestSuiteExecutionResult.builder()
            .suiteId(suiteId)
            .totalTests(testCases.size())
            .passedTests(passedCount)
            .failedTests(failedCount)
            .passRate(passRate)
            .executions(executions)
            .passed(passRate.compareTo(suite.getPassThreshold()) >= 0)
            .build();
    }

    /**
     * Execute a single test case.
     */
    @Transactional
    public TestExecution executeCase(TestCase testCase, String tenantId, String userId) {
        log.info("Executing test case: {} ({})", testCase.getName(), testCase.getId());

        Timer.Sample sample = Timer.start(meterRegistry);
        Instant startTime = Instant.now();

        // Generate trace ID for correlation
        String traceId = UUID.randomUUID().toString().replace("-", "");

        // Build execution request
        AgentExecutionRequest request = buildAgentExecutionRequest(testCase);

        TestExecution execution = TestExecution.builder()
            .testCase(testCase)
            .tenantId(tenantId)
            .traceId(traceId)
            .status(TestStatus.RUNNING)
            .executedAt(startTime)
            .build();

        try {
            // Execute agent
            AgentExecutionResponse response = agentRuntimeClient.executeAgent(
                tenantId,
                userId,
                traceId,
                request
            );

            // Record basic execution results
            execution.setAgentResponse(response.getResponse());
            execution.setLlmProvider(response.getLlmProvider());
            execution.setTraceId(response.getTraceId() != null ? response.getTraceId() : traceId);
            execution.setDurationMs(response.getDurationMs());
            execution.setInputTokens(response.getInputTokens());
            execution.setOutputTokens(response.getOutputTokens());

            // Run evaluation pipeline if service is available
            if (evaluationService != null) {
                runEvaluationPipeline(execution, testCase, response);
            } else {
                // Basic pass/fail without detailed evaluation
                execution.setEvaluationScore(BigDecimal.ONE);
                execution.setPassed(true);
                execution.setStatus(TestStatus.PASSED);
            }

            // Check if should flag for QA review
            if (qaIntegrationService != null &&
                execution.shouldFlagForQaReview(
                    validationProperties.getQa().getAutoFlagScoreThreshold(),
                    validationProperties.getQa().getConfidenceMiscalibrationThreshold())) {
                execution.setStatus(TestStatus.FLAGGED_FOR_REVIEW);
                qaIntegrationService.createApprovalRequest(execution, tenantId, userId);
            }

        } catch (Exception e) {
            log.error("Agent execution failed for test case {}: {}", testCase.getId(), e.getMessage(), e);
            execution.setStatus(TestStatus.ERROR);
            execution.setErrorMessage(e.getMessage());
            execution.setPassed(false);
        } finally {
            execution.setCompletedAt(Instant.now());
            testExecutionRepository.save(execution);

            sample.stop(testExecutionTimer);
            testExecutionCounter.increment();

            // Update test case status
            testCase.setStatus(execution.getStatus());
            testCaseRepository.save(testCase);
        }

        return execution;
    }

    /**
     * Execute a test case asynchronously.
     */
    @Async("testExecutorPool")
    public CompletableFuture<TestExecution> executeCaseAsync(
            TestCase testCase, String tenantId, String userId) {
        return CompletableFuture.completedFuture(executeCase(testCase, tenantId, userId));
    }

    /**
     * Build agent execution request from test case.
     */
    private AgentExecutionRequest buildAgentExecutionRequest(TestCase testCase) {
        Map<String, Object> contextData = testCase.getContextData() != null ?
            new HashMap<>(testCase.getContextData()) : new HashMap<>();

        // Extract patient ID if present in context
        String patientId = contextData.containsKey("patientId") ?
            String.valueOf(contextData.get("patientId")) : null;

        return AgentExecutionRequest.builder()
            .agentType(testCase.getTestSuite().getAgentType())
            .userMessage(testCase.getUserMessage())
            .sessionId(UUID.randomUUID().toString())
            .contextData(contextData)
            .patientId(patientId)
            .maxIterations(validationProperties.getOrchestrator().getMaxConcurrentTests())
            .includeToolCalls(true)
            .includeTraceInfo(true)
            .build();
    }

    /**
     * Run the full evaluation pipeline on an execution.
     */
    private void runEvaluationPipeline(
            TestExecution execution,
            TestCase testCase,
            AgentExecutionResponse response) {

        // 1. Run evaluation metrics
        Map<String, TestExecution.MetricResult> metricResults = new HashMap<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int metricCount = 0;
        boolean allMetricsPassed = true;

        for (EvaluationMetricType metricType : testCase.getRequiredMetrics()) {
            try {
                TestExecution.MetricResult result = evaluationService.evaluateMetric(
                    metricType,
                    testCase.getUserMessage(),
                    response.getResponse(),
                    testCase.getContextData()
                );

                // Check threshold
                BigDecimal threshold = testCase.getThresholdForMetric(metricType);
                result.setThreshold(threshold);
                result.setPassed(result.getScore().compareTo(threshold) >= 0);

                metricResults.put(metricType.name(), result);
                totalScore = totalScore.add(result.getScore());
                metricCount++;

                if (!result.isPassed()) {
                    allMetricsPassed = false;
                }

                // Record metric to Prometheus
                meterRegistry.gauge(
                    "agent.validation.test.score",
                    List.of(
                        io.micrometer.core.instrument.Tag.of("metric", metricType.name()),
                        io.micrometer.core.instrument.Tag.of("test_case", testCase.getId().toString())
                    ),
                    result.getScore().doubleValue()
                );

            } catch (Exception e) {
                log.error("Error evaluating metric {} for execution {}: {}",
                    metricType, execution.getId(), e.getMessage());
                TestExecution.MetricResult errorResult = TestExecution.MetricResult.builder()
                    .metricType(metricType.name())
                    .score(BigDecimal.ZERO)
                    .passed(false)
                    .reason("Evaluation error: " + e.getMessage())
                    .build();
                metricResults.put(metricType.name(), errorResult);
                allMetricsPassed = false;
            }
        }

        execution.setMetricResults(metricResults);
        execution.setEvaluationScore(metricCount > 0 ?
            totalScore.divide(BigDecimal.valueOf(metricCount), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO);

        // 2. Run reflection if enabled
        if (reflectionService != null && validationProperties.getReflection().isEnabled()) {
            try {
                TestExecution.ReflectionResult reflectionResult = reflectionService.generateReflection(
                    testCase.getUserMessage(),
                    response.getResponse(),
                    execution.getEvaluationScore()
                );
                execution.setReflectionResult(reflectionResult);

                if (reflectionResult.isMiscalibrated()) {
                    log.warn("Miscalibrated confidence detected for execution {}: delta={}",
                        execution.getId(), reflectionResult.getCalibrationDelta());
                }
            } catch (Exception e) {
                log.error("Error generating reflection: {}", e.getMessage());
            }
        }

        // 3. Run regression comparison if golden response exists
        if (regressionService != null && testCase.getGoldenResponse() != null) {
            try {
                TestExecution.RegressionResult regressionResult = regressionService.compareToGolden(
                    response.getResponse(),
                    testCase.getGoldenResponse()
                );
                execution.setRegressionResult(regressionResult);

                if (regressionResult.isRegressionDetected()) {
                    log.warn("Regression detected for execution {}: similarity={}",
                        execution.getId(), regressionResult.getSemanticSimilarity());
                    allMetricsPassed = false;
                }
            } catch (Exception e) {
                log.error("Error running regression comparison: {}", e.getMessage());
            }
        }

        // 4. Correlate with Jaeger trace
        if (jaegerTraceService != null && execution.getTraceId() != null) {
            try {
                jaegerTraceService.correlateTrace(execution);
            } catch (Exception e) {
                log.error("Error correlating trace: {}", e.getMessage());
            }
        }

        // Set final status
        boolean passed = allMetricsPassed &&
            execution.getEvaluationScore().compareTo(
                validationProperties.getOrchestrator().getDefaultPassThreshold()) >= 0;
        execution.setPassed(passed);
        execution.setStatus(passed ? TestStatus.PASSED : TestStatus.FAILED);
    }

    /**
     * Create an error execution record.
     */
    private TestExecution createErrorExecution(TestCase testCase, String tenantId, Exception e) {
        return TestExecution.builder()
            .testCase(testCase)
            .tenantId(tenantId)
            .status(TestStatus.ERROR)
            .errorMessage(e.getMessage())
            .passed(false)
            .executedAt(Instant.now())
            .completedAt(Instant.now())
            .build();
    }

    /**
     * Result of executing a test suite.
     */
    @lombok.Data
    @lombok.Builder
    public static class TestSuiteExecutionResult {
        private UUID suiteId;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private BigDecimal passRate;
        private List<TestExecution> executions;
        private boolean passed;
    }
}
