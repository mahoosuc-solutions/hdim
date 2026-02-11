package com.healthdata.agentvalidation.repository;

import com.healthdata.agentvalidation.domain.entity.GoldenResponse;
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
 * Repository for GoldenResponse entities.
 */
@Repository
public interface GoldenResponseRepository extends JpaRepository<GoldenResponse, UUID> {

    /**
     * Find active golden response for a test case.
     */
    Optional<GoldenResponse> findByTestCaseIdAndArchivedFalse(UUID testCaseId);

    /**
     * Find all golden responses for a test case (including archived).
     */
    List<GoldenResponse> findByTestCaseIdOrderByCreatedAtDesc(UUID testCaseId);

    /**
     * Find archived golden responses for a test case.
     */
    List<GoldenResponse> findByTestCaseIdAndArchivedTrueOrderByArchivedAtDesc(UUID testCaseId);

    /**
     * Find golden responses by approver.
     */
    @Query("SELECT gr FROM GoldenResponse gr WHERE gr.testCase.testSuite.tenantId = :tenantId " +
           "AND gr.approvedBy = :approvedBy ORDER BY gr.approvedAt DESC")
    List<GoldenResponse> findByApprovedBy(
        @Param("tenantId") String tenantId,
        @Param("approvedBy") String approvedBy
    );

    /**
     * Find golden responses created within a time range.
     */
    @Query("SELECT gr FROM GoldenResponse gr WHERE gr.testCase.testSuite.tenantId = :tenantId " +
           "AND gr.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY gr.createdAt DESC")
    List<GoldenResponse> findByCreatedAtBetween(
        @Param("tenantId") String tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Find golden responses by LLM provider.
     */
    @Query("SELECT gr FROM GoldenResponse gr WHERE gr.testCase.testSuite.tenantId = :tenantId " +
           "AND gr.llmProvider = :llmProvider AND gr.archived = false")
    List<GoldenResponse> findByLlmProvider(
        @Param("tenantId") String tenantId,
        @Param("llmProvider") String llmProvider
    );

    /**
     * Count active golden responses for a test suite.
     */
    @Query("SELECT COUNT(gr) FROM GoldenResponse gr " +
           "WHERE gr.testCase.testSuite.id = :suiteId AND gr.archived = false")
    long countActiveByTestSuite(@Param("suiteId") UUID suiteId);

    /**
     * Find test cases without golden responses in a suite.
     */
    @Query("SELECT tc.id FROM TestCase tc " +
           "WHERE tc.testSuite.id = :suiteId " +
           "AND NOT EXISTS (SELECT gr FROM GoldenResponse gr WHERE gr.testCase = tc AND gr.archived = false)")
    List<UUID> findTestCasesWithoutGoldenResponse(@Param("suiteId") UUID suiteId);

    /**
     * Find golden response lineage (chain of previous versions).
     */
    @Query(value = "WITH RECURSIVE lineage AS (" +
           "  SELECT * FROM golden_responses WHERE id = :goldenId " +
           "  UNION ALL " +
           "  SELECT gr.* FROM golden_responses gr " +
           "  INNER JOIN lineage l ON gr.id = l.previous_golden_id" +
           ") SELECT * FROM lineage ORDER BY created_at DESC",
           nativeQuery = true)
    List<GoldenResponse> findLineage(@Param("goldenId") UUID goldenId);

    /**
     * Find all active golden responses for a tenant with pagination.
     */
    @Query("SELECT gr FROM GoldenResponse gr " +
           "WHERE gr.testCase.testSuite.tenantId = :tenantId AND gr.archived = false")
    Page<GoldenResponse> findAllActiveByTenant(@Param("tenantId") String tenantId, Pageable pageable);
}
