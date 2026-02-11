package com.healthdata.agentvalidation.repository;

import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TestExecution entities.
 */
@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, UUID> {

    /**
     * Find execution by ID and tenant.
     */
    Optional<TestExecution> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find execution by trace ID.
     */
    Optional<TestExecution> findByTraceId(String traceId);

    /**
     * Find executions for a test case.
     */
    List<TestExecution> findByTestCaseIdOrderByExecutedAtDesc(UUID testCaseId);

    /**
     * Find executions for a test case with pagination.
     */
    Page<TestExecution> findByTestCaseId(UUID testCaseId, Pageable pageable);

    /**
     * Find most recent execution for a test case.
     */
    Optional<TestExecution> findTopByTestCaseIdOrderByExecutedAtDesc(UUID testCaseId);

    /**
     * Find executions by status.
     */
    List<TestExecution> findByTenantIdAndStatus(String tenantId, TestStatus status);

    /**
     * Find executions flagged for QA review.
     */
    List<TestExecution> findByTenantIdAndStatusIn(String tenantId, List<TestStatus> statuses);

    /**
     * Find executions by provider.
     */
    List<TestExecution> findByTenantIdAndLlmProvider(String tenantId, String llmProvider);

    /**
     * Find executions within a time range.
     */
    @Query("SELECT te FROM TestExecution te WHERE te.tenantId = :tenantId " +
           "AND te.executedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY te.executedAt DESC")
    List<TestExecution> findByTenantIdAndExecutedAtBetween(
        @Param("tenantId") String tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find failed executions.
     */
    @Query("SELECT te FROM TestExecution te WHERE te.tenantId = :tenantId " +
           "AND te.passed = false AND te.executedAt > :since " +
           "ORDER BY te.executedAt DESC")
    List<TestExecution> findRecentFailures(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    /**
     * Find executions with low evaluation scores.
     */
    @Query("SELECT te FROM TestExecution te WHERE te.tenantId = :tenantId " +
           "AND te.evaluationScore < :threshold " +
           "ORDER BY te.evaluationScore ASC")
    List<TestExecution> findLowScoreExecutions(
        @Param("tenantId") String tenantId,
        @Param("threshold") BigDecimal threshold
    );

    /**
     * Count executions by pass/fail for a tenant.
     */
    @Query("SELECT te.passed, COUNT(te) FROM TestExecution te " +
           "WHERE te.tenantId = :tenantId AND te.executedAt > :since " +
           "GROUP BY te.passed")
    List<Object[]> countByPassedForTenant(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    /**
     * Get average evaluation score by provider.
     */
    @Query("SELECT te.llmProvider, AVG(te.evaluationScore), COUNT(te) " +
           "FROM TestExecution te WHERE te.tenantId = :tenantId " +
           "AND te.executedAt > :since GROUP BY te.llmProvider")
    List<Object[]> getAverageScoreByProvider(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    /**
     * Find executions pending QA review.
     */
    @Query("SELECT te FROM TestExecution te WHERE te.tenantId = :tenantId " +
           "AND te.status = 'FLAGGED_FOR_REVIEW' " +
           "ORDER BY te.executedAt ASC")
    List<TestExecution> findPendingQaReview(@Param("tenantId") String tenantId);

    /**
     * Get execution statistics for a test suite.
     */
    @Query("SELECT " +
           "  COUNT(te), " +
           "  SUM(CASE WHEN te.passed = true THEN 1 ELSE 0 END), " +
           "  AVG(te.evaluationScore), " +
           "  AVG(te.durationMs) " +
           "FROM TestExecution te " +
           "WHERE te.testCase.testSuite.id = :suiteId " +
           "AND te.executedAt > :since")
    Object[] getSuiteExecutionStats(
        @Param("suiteId") UUID suiteId,
        @Param("since") Instant since
    );
}
