package com.healthdata.agentvalidation.controller;

import com.healthdata.agentvalidation.domain.entity.TestCase;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.enums.EvaluationMetricType;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.repository.TestCaseRepository;
import com.healthdata.agentvalidation.repository.TestExecutionRepository;
import com.healthdata.agentvalidation.service.EvaluationService;
import com.healthdata.agentvalidation.service.TestOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for managing test executions and test cases.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validation")
@RequiredArgsConstructor
@Tag(name = "Test Executions", description = "Execute and manage AI agent validation tests")
public class TestExecutionController {

    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final TestOrchestratorService testOrchestratorService;
    private final EvaluationService evaluationService;

    // Test Case Endpoints

    @Operation(summary = "Create a new test case")
    @PostMapping("/suites/{suiteId}/cases")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<TestCase> createTestCase(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID suiteId,
            @Valid @RequestBody CreateTestCaseRequest request) {

        log.info("Creating test case in suite {}", suiteId);

        // Note: In real implementation, would validate suite belongs to tenant
        TestCase testCase = TestCase.builder()
            .name(request.name())
            .description(request.description())
            .userMessage(request.userMessage())
            .contextData(request.contextData())
            .expectedBehavior(request.expectedBehavior())
            .requiredMetrics(request.requiredMetrics() != null ?
                new HashSet<>(request.requiredMetrics()) : new HashSet<>())
            .metricThresholds(request.metricThresholds() != null ?
                request.metricThresholds() : new HashMap<>())
            .clinicalSafetyCheck(request.clinicalSafetyCheck() != null ?
                request.clinicalSafetyCheck() : true)
            .executionPriority(request.executionPriority() != null ?
                request.executionPriority() : 100)
            .tags(request.tags() != null ? new HashSet<>(request.tags()) : new HashSet<>())
            .build();

        // Would need to set testSuite relationship here
        TestCase saved = testCaseRepository.save(testCase);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Get test cases for a suite")
    @GetMapping("/suites/{suiteId}/cases")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<List<TestCase>> getTestCasesForSuite(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID suiteId) {

        return ResponseEntity.ok(
            testCaseRepository.findByTestSuiteIdOrderByExecutionPriorityAsc(suiteId));
    }

    @Operation(summary = "Get a specific test case")
    @GetMapping("/cases/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<TestCase> getTestCase(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID caseId) {

        return testCaseRepository.findByIdWithSuite(caseId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update a test case")
    @PutMapping("/cases/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<TestCase> updateTestCase(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID caseId,
            @Valid @RequestBody UpdateTestCaseRequest request) {

        return testCaseRepository.findById(caseId)
            .map(testCase -> {
                if (request.name() != null) testCase.setName(request.name());
                if (request.description() != null) testCase.setDescription(request.description());
                if (request.userMessage() != null) testCase.setUserMessage(request.userMessage());
                if (request.contextData() != null) testCase.setContextData(request.contextData());
                if (request.metricThresholds() != null) testCase.setMetricThresholds(request.metricThresholds());
                if (request.clinicalSafetyCheck() != null) testCase.setClinicalSafetyCheck(request.clinicalSafetyCheck());
                if (request.executionPriority() != null) testCase.setExecutionPriority(request.executionPriority());
                return ResponseEntity.ok(testCaseRepository.save(testCase));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a test case")
    @DeleteMapping("/cases/{caseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTestCase(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID caseId) {

        return testCaseRepository.findById(caseId)
            .map(testCase -> {
                testCaseRepository.delete(testCase);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Test Execution Endpoints

    @Operation(summary = "Execute a single test case")
    @PostMapping("/cases/{caseId}/execute")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<TestExecution> executeTestCase(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable UUID caseId) {

        log.info("Executing test case {} for tenant {}", caseId, tenantId);

        return testCaseRepository.findByIdWithSuite(caseId)
            .map(testCase -> {
                TestExecution execution = testOrchestratorService.executeCase(testCase, tenantId, userId);
                return ResponseEntity.ok(execution);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Execute a test case asynchronously")
    @PostMapping("/cases/{caseId}/execute-async")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<Map<String, String>> executeTestCaseAsync(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable UUID caseId) {

        return testCaseRepository.findByIdWithSuite(caseId)
            .map(testCase -> {
                CompletableFuture<TestExecution> future =
                    testOrchestratorService.executeCaseAsync(testCase, tenantId, userId);

                return ResponseEntity.accepted().body(Map.of(
                    "status", "ACCEPTED",
                    "message", "Test case execution started",
                    "caseId", caseId.toString()
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get execution history for a test case")
    @GetMapping("/cases/{caseId}/executions")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<Page<TestExecution>> getExecutionHistory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID caseId,
            Pageable pageable) {

        return ResponseEntity.ok(testExecutionRepository.findByTestCaseId(caseId, pageable));
    }

    @Operation(summary = "Get a specific execution by ID")
    @GetMapping("/executions/{executionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<TestExecution> getExecution(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID executionId) {

        return testExecutionRepository.findByIdAndTenantId(executionId, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get executions pending QA review")
    @GetMapping("/executions/pending-review")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<List<TestExecution>> getPendingQAReview(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return ResponseEntity.ok(testExecutionRepository.findPendingQaReview(tenantId));
    }

    @Operation(summary = "Get recent failed executions")
    @GetMapping("/executions/failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<List<TestExecution>> getRecentFailures(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "24") int hoursBack) {

        Instant since = Instant.now().minusSeconds(hoursBack * 3600L);
        return ResponseEntity.ok(testExecutionRepository.findRecentFailures(tenantId, since));
    }

    @Operation(summary = "Get execution statistics")
    @GetMapping("/executions/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<ExecutionStats> getExecutionStats(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "24") int hoursBack) {

        Instant since = Instant.now().minusSeconds(hoursBack * 3600L);

        List<Object[]> passedCounts = testExecutionRepository.countByPassedForTenant(tenantId, since);
        List<Object[]> providerScores = testExecutionRepository.getAverageScoreByProvider(tenantId, since);

        long passed = 0, failed = 0;
        for (Object[] row : passedCounts) {
            if (Boolean.TRUE.equals(row[0])) {
                passed = (Long) row[1];
            } else {
                failed = (Long) row[1];
            }
        }

        return ResponseEntity.ok(new ExecutionStats(passed, failed, providerScores));
    }

    @Operation(summary = "Submit QA review decision")
    @PostMapping("/executions/{executionId}/qa-review")
    @PreAuthorize("hasRole('QUALITY_OFFICER')")
    public ResponseEntity<TestExecution> submitQAReview(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-User-ID") String userId,
            @PathVariable UUID executionId,
            @Valid @RequestBody QAReviewRequest request) {

        return testExecutionRepository.findByIdAndTenantId(executionId, tenantId)
            .map(execution -> {
                execution.setQaReviewStatus(request.decision());
                execution.setQaReviewComments(request.comments());
                execution.setQaReviewerId(userId);
                execution.setQaReviewedAt(Instant.now());
                execution.setStatus(
                    "APPROVED".equals(request.decision()) ?
                        TestStatus.QA_APPROVED : TestStatus.QA_REJECTED);

                return ResponseEntity.ok(testExecutionRepository.save(execution));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Direct Evaluation Endpoint (for testing harness connectivity)

    @Operation(summary = "Directly evaluate a response against metrics (dev/testing)")
    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<Map<String, TestExecution.MetricResult>> evaluateResponse(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody DirectEvaluationRequest request) {

        log.info("Direct evaluation request for {} metrics", request.metricTypes().size());

        Map<String, TestExecution.MetricResult> results = evaluationService.evaluateAll(
            request.metricTypes(),
            request.userMessage(),
            request.agentResponse(),
            request.contextData() != null ? request.contextData() : Map.of()
        );

        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Evaluate a single metric (dev/testing)")
    @PostMapping("/evaluate/{metricType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<TestExecution.MetricResult> evaluateSingleMetric(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable EvaluationMetricType metricType,
            @Valid @RequestBody DirectEvaluationRequest request) {

        log.info("Direct single metric evaluation: {}", metricType);

        TestExecution.MetricResult result = evaluationService.evaluateMetric(
            metricType,
            request.userMessage(),
            request.agentResponse(),
            request.contextData() != null ? request.contextData() : Map.of()
        );

        return ResponseEntity.ok(result);
    }

    // DTOs
    public record DirectEvaluationRequest(
        String userMessage,
        String agentResponse,
        Set<EvaluationMetricType> metricTypes,
        Map<String, Object> contextData
    ) {}

    public record CreateTestCaseRequest(
        String name,
        String description,
        String userMessage,
        Map<String, Object> contextData,
        Map<String, Object> expectedBehavior,
        Set<EvaluationMetricType> requiredMetrics,
        Map<String, BigDecimal> metricThresholds,
        Boolean clinicalSafetyCheck,
        Integer executionPriority,
        Set<String> tags
    ) {}

    public record UpdateTestCaseRequest(
        String name,
        String description,
        String userMessage,
        Map<String, Object> contextData,
        Map<String, BigDecimal> metricThresholds,
        Boolean clinicalSafetyCheck,
        Integer executionPriority
    ) {}

    public record QAReviewRequest(
        String decision, // APPROVED or REJECTED
        String comments
    ) {}

    public record ExecutionStats(
        long passed,
        long failed,
        List<Object[]> providerScores
    ) {}
}
