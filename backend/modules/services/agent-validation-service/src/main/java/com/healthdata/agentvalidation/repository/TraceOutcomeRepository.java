package com.healthdata.agentvalidation.repository;

import com.healthdata.agentvalidation.domain.entity.TraceOutcome;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TraceOutcome entities.
 */
@Repository
public interface TraceOutcomeRepository extends JpaRepository<TraceOutcome, UUID> {

    /**
     * Find trace outcome by trace ID.
     */
    Optional<TraceOutcome> findByTraceId(String traceId);

    /**
     * Find trace outcome by test execution ID.
     */
    Optional<TraceOutcome> findByTestExecutionId(UUID testExecutionId);

    /**
     * Find traces with errors.
     */
    @Query("SELECT to FROM TraceOutcome to WHERE to.errorCount > 0 " +
           "ORDER BY to.createdAt DESC")
    List<TraceOutcome> findTracesWithErrors();

    /**
     * Find traces by evaluation result.
     */
    List<TraceOutcome> findByEvaluationPassedOrderByCreatedAtDesc(Boolean evaluationPassed);

    /**
     * Find traces within a time range.
     */
    @Query("SELECT to FROM TraceOutcome to WHERE to.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY to.createdAt DESC")
    List<TraceOutcome> findByCreatedAtBetween(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find long-running traces (above duration threshold).
     */
    @Query("SELECT to FROM TraceOutcome to WHERE to.totalDurationMs > :thresholdMs " +
           "ORDER BY to.totalDurationMs DESC")
    List<TraceOutcome> findLongRunningTraces(@Param("thresholdMs") Long thresholdMs);

    /**
     * Find traces with high tool invocation count.
     */
    @Query("SELECT to FROM TraceOutcome to WHERE to.toolInvocationCount > :threshold " +
           "ORDER BY to.toolInvocationCount DESC")
    List<TraceOutcome> findTracesWithManyToolCalls(@Param("threshold") int threshold);

    /**
     * Find traces with high LLM call count.
     */
    @Query("SELECT to FROM TraceOutcome to WHERE to.llmCallCount > :threshold " +
           "ORDER BY to.llmCallCount DESC")
    List<TraceOutcome> findTracesWithManyLlmCalls(@Param("threshold") int threshold);

    /**
     * Get trace statistics.
     */
    @Query("SELECT " +
           "  AVG(to.totalDurationMs), " +
           "  AVG(to.toolInvocationCount), " +
           "  AVG(to.llmCallCount), " +
           "  SUM(CASE WHEN to.errorCount > 0 THEN 1 ELSE 0 END), " +
           "  COUNT(to) " +
           "FROM TraceOutcome to " +
           "WHERE to.createdAt > :since")
    Object[] getTraceStatistics(@Param("since") Instant since);

    /**
     * Find traces with errors but passing evaluation.
     */
    @Query("SELECT to FROM TraceOutcome to WHERE to.errorCount > 0 " +
           "AND to.evaluationPassed = true " +
           "ORDER BY to.createdAt DESC")
    List<TraceOutcome> findTracesWithErrorsButPassing();

    /**
     * Find all traces for test suite executions.
     */
    @Query("SELECT to FROM TraceOutcome to " +
           "WHERE to.testExecution.testCase.testSuite.id = :suiteId " +
           "ORDER BY to.createdAt DESC")
    Page<TraceOutcome> findBySuiteId(@Param("suiteId") UUID suiteId, Pageable pageable);
}
