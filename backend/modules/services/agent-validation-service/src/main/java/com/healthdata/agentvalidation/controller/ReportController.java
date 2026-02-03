package com.healthdata.agentvalidation.controller;

import com.healthdata.agentvalidation.domain.entity.*;
import com.healthdata.agentvalidation.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for generating validation reports and analytics.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validation/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Generate AI agent validation reports and analytics")
public class ReportController {

    private final TestSuiteRepository testSuiteRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final GoldenResponseRepository goldenResponseRepository;
    private final TraceOutcomeRepository traceOutcomeRepository;
    private final ProviderComparisonRepository providerComparisonRepository;

    @Operation(summary = "Generate overall validation summary")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<ValidationSummary> getValidationSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "7") int daysBack) {

        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);

        List<TestSuite> suites = testSuiteRepository.findByTenantIdAndActiveTrue(tenantId);
        List<TestSuite> failingSuites = testSuiteRepository.findFailingTestSuites(tenantId);

        List<Object[]> passedCounts = testExecutionRepository.countByPassedForTenant(tenantId, since);
        List<Object[]> providerScores = testExecutionRepository.getAverageScoreByProvider(tenantId, since);

        long totalExecutions = 0, passedExecutions = 0;
        for (Object[] row : passedCounts) {
            long count = (Long) row[1];
            totalExecutions += count;
            if (Boolean.TRUE.equals(row[0])) {
                passedExecutions = count;
            }
        }

        BigDecimal overallPassRate = totalExecutions > 0 ?
            BigDecimal.valueOf(passedExecutions)
                .divide(BigDecimal.valueOf(totalExecutions), 4, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;

        Map<String, ProviderStats> providerStats = new HashMap<>();
        for (Object[] row : providerScores) {
            String provider = (String) row[0];
            BigDecimal avgScore = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            long count = (Long) row[2];
            providerStats.put(provider, new ProviderStats(provider, avgScore, count));
        }

        return ResponseEntity.ok(new ValidationSummary(
            suites.size(),
            failingSuites.size(),
            totalExecutions,
            passedExecutions,
            overallPassRate,
            providerStats,
            since
        ));
    }

    @Operation(summary = "Generate test suite report")
    @GetMapping("/suites/{suiteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<TestSuiteReport> getTestSuiteReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID suiteId,
            @RequestParam(defaultValue = "7") int daysBack) {

        return testSuiteRepository.findByIdAndTenantId(suiteId, tenantId)
            .map(suite -> {
                Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);

                List<TestCase> testCases = testCaseRepository.findByTestSuiteIdOrderByExecutionPriorityAsc(suiteId);
                Object[] stats = testExecutionRepository.getSuiteExecutionStats(suiteId, since);

                long goldenCount = goldenResponseRepository.countActiveByTestSuite(suiteId);
                List<UUID> missingGolden = goldenResponseRepository.findTestCasesWithoutGoldenResponse(suiteId);

                // Calculate test case results
                List<TestCaseResult> testCaseResults = testCases.stream()
                    .map(tc -> {
                        Optional<TestExecution> latestExecution =
                            testExecutionRepository.findTopByTestCaseIdOrderByExecutedAtDesc(tc.getId());

                        return new TestCaseResult(
                            tc.getId(),
                            tc.getName(),
                            tc.getStatus(),
                            latestExecution.map(TestExecution::getEvaluationScore).orElse(null),
                            latestExecution.map(TestExecution::isPassed).orElse(false),
                            latestExecution.map(TestExecution::getExecutedAt).orElse(null),
                            tc.getGoldenResponse() != null
                        );
                    })
                    .collect(Collectors.toList());

                return ResponseEntity.ok(new TestSuiteReport(
                    suite.getId(),
                    suite.getName(),
                    suite.getUserStoryType(),
                    suite.getTargetRole(),
                    suite.getAgentType(),
                    suite.getPassThreshold(),
                    suite.getLastPassRate(),
                    suite.getLastExecutionAt(),
                    testCases.size(),
                    goldenCount,
                    missingGolden.size(),
                    stats,
                    testCaseResults
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Generate provider comparison report")
    @GetMapping("/providers")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER', 'EVALUATOR')")
    public ResponseEntity<ProviderComparisonReport> getProviderComparisonReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "7") int daysBack) {

        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);

        List<Object[]> qualityWins = providerComparisonRepository.countQualityWinsByProvider(tenantId, since);
        List<Object[]> speedStats = providerComparisonRepository.getSpeedStatsByProvider(tenantId, since);
        List<Object[]> costStats = providerComparisonRepository.getCostStatsByProvider(tenantId, since);
        List<Object[]> rankings = providerComparisonRepository.getProviderRankings(tenantId, since);

        return ResponseEntity.ok(new ProviderComparisonReport(
            qualityWins,
            speedStats,
            costStats,
            rankings,
            since
        ));
    }

    @Operation(summary = "Generate trace analysis report")
    @GetMapping("/traces")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<TraceAnalysisReport> getTraceAnalysisReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "7") int daysBack) {

        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);

        Object[] stats = traceOutcomeRepository.getTraceStatistics(since);
        List<TraceOutcome> tracesWithErrors = traceOutcomeRepository.findTracesWithErrors();
        List<TraceOutcome> longRunning = traceOutcomeRepository.findLongRunningTraces(10000L);
        List<TraceOutcome> errorButPassing = traceOutcomeRepository.findTracesWithErrorsButPassing();

        return ResponseEntity.ok(new TraceAnalysisReport(
            stats,
            tracesWithErrors.size(),
            longRunning.size(),
            errorButPassing.size(),
            since
        ));
    }

    @Operation(summary = "Generate regression report")
    @GetMapping("/regressions")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<RegressionReport> getRegressionReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "7") int daysBack) {

        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);

        List<TestExecution> recentExecutions =
            testExecutionRepository.findByTenantIdAndExecutedAtBetween(tenantId, since, Instant.now());

        List<RegressionItem> regressions = recentExecutions.stream()
            .filter(e -> e.getRegressionResult() != null && e.getRegressionResult().isRegressionDetected())
            .map(e -> new RegressionItem(
                e.getId(),
                e.getTestCase().getId(),
                e.getTestCase().getName(),
                e.getRegressionResult().getSemanticSimilarity(),
                e.getRegressionResult().getQualityDelta(),
                e.getExecutedAt()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(new RegressionReport(
            regressions.size(),
            regressions,
            since
        ));
    }

    @Operation(summary = "Generate QA review report")
    @GetMapping("/qa-review")
    @PreAuthorize("hasAnyRole('ADMIN', 'QUALITY_OFFICER')")
    public ResponseEntity<QAReviewReport> getQAReviewReport(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "30") int daysBack) {

        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);

        List<TestExecution> pending = testExecutionRepository.findPendingQaReview(tenantId);
        List<TestExecution> allInPeriod =
            testExecutionRepository.findByTenantIdAndExecutedAtBetween(tenantId, since, Instant.now());

        long approved = allInPeriod.stream()
            .filter(e -> "QA_APPROVED".equals(e.getStatus().name()))
            .count();
        long rejected = allInPeriod.stream()
            .filter(e -> "QA_REJECTED".equals(e.getStatus().name()))
            .count();

        return ResponseEntity.ok(new QAReviewReport(
            pending.size(),
            approved,
            rejected,
            since
        ));
    }

    // Report DTOs
    public record ValidationSummary(
        int totalSuites,
        int failingSuites,
        long totalExecutions,
        long passedExecutions,
        BigDecimal overallPassRate,
        Map<String, ProviderStats> providerStats,
        Instant since
    ) {}

    public record ProviderStats(
        String provider,
        BigDecimal averageScore,
        long executionCount
    ) {}

    public record TestSuiteReport(
        UUID suiteId,
        String name,
        com.healthdata.agentvalidation.domain.enums.UserStoryType userStoryType,
        String targetRole,
        String agentType,
        BigDecimal passThreshold,
        BigDecimal lastPassRate,
        Instant lastExecutionAt,
        int testCaseCount,
        long goldenResponseCount,
        int missingGoldenCount,
        Object[] executionStats,
        List<TestCaseResult> testCaseResults
    ) {}

    public record TestCaseResult(
        UUID id,
        String name,
        com.healthdata.agentvalidation.domain.enums.TestStatus status,
        BigDecimal lastScore,
        boolean passed,
        Instant lastExecutedAt,
        boolean hasGoldenResponse
    ) {}

    public record ProviderComparisonReport(
        List<Object[]> qualityWins,
        List<Object[]> speedStats,
        List<Object[]> costStats,
        List<Object[]> rankings,
        Instant since
    ) {}

    public record TraceAnalysisReport(
        Object[] statistics,
        int tracesWithErrors,
        int longRunningTraces,
        int errorButPassing,
        Instant since
    ) {}

    public record RegressionReport(
        int totalRegressions,
        List<RegressionItem> regressions,
        Instant since
    ) {}

    public record RegressionItem(
        UUID executionId,
        UUID testCaseId,
        String testCaseName,
        BigDecimal similarity,
        BigDecimal qualityDelta,
        Instant detectedAt
    ) {}

    public record QAReviewReport(
        int pendingReviews,
        long approvedCount,
        long rejectedCount,
        Instant since
    ) {}
}
