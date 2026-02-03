package com.healthdata.agentvalidation.repository;

import com.healthdata.agentvalidation.domain.entity.TestCase;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for TestCase entities.
 */
@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    /**
     * Find test case by ID with suite loaded.
     */
    @Query("SELECT tc FROM TestCase tc JOIN FETCH tc.testSuite WHERE tc.id = :id")
    Optional<TestCase> findByIdWithSuite(@Param("id") UUID id);

    /**
     * Find all test cases for a test suite.
     */
    List<TestCase> findByTestSuiteId(UUID testSuiteId);

    /**
     * Find test cases for a suite ordered by priority.
     */
    List<TestCase> findByTestSuiteIdOrderByExecutionPriorityAsc(UUID testSuiteId);

    /**
     * Find test cases by status.
     */
    List<TestCase> findByTestSuiteIdAndStatus(UUID testSuiteId, TestStatus status);

    /**
     * Find test cases by tags.
     */
    @Query("SELECT DISTINCT tc FROM TestCase tc JOIN tc.tags t " +
           "WHERE tc.testSuite.id = :suiteId AND t IN :tags")
    List<TestCase> findByTestSuiteIdAndTagsIn(
        @Param("suiteId") UUID suiteId,
        @Param("tags") Set<String> tags
    );

    /**
     * Find test cases requiring clinical safety check.
     */
    List<TestCase> findByTestSuiteIdAndClinicalSafetyCheckTrue(UUID testSuiteId);

    /**
     * Find test cases with golden responses.
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.testSuite.id = :suiteId " +
           "AND tc.goldenResponse IS NOT NULL")
    List<TestCase> findByTestSuiteIdWithGoldenResponse(@Param("suiteId") UUID suiteId);

    /**
     * Find test cases without golden responses.
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.testSuite.id = :suiteId " +
           "AND tc.goldenResponse IS NULL")
    List<TestCase> findByTestSuiteIdWithoutGoldenResponse(@Param("suiteId") UUID suiteId);

    /**
     * Count test cases by status for a suite.
     */
    @Query("SELECT tc.status, COUNT(tc) FROM TestCase tc " +
           "WHERE tc.testSuite.id = :suiteId GROUP BY tc.status")
    List<Object[]> countByStatusForSuite(@Param("suiteId") UUID suiteId);

    /**
     * Find test cases by name pattern within a suite.
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.testSuite.id = :suiteId " +
           "AND LOWER(tc.name) LIKE LOWER(CONCAT('%', :namePattern, '%'))")
    List<TestCase> findByNameContaining(
        @Param("suiteId") UUID suiteId,
        @Param("namePattern") String namePattern
    );

    /**
     * Find test cases across all suites for a tenant.
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.testSuite.tenantId = :tenantId")
    Page<TestCase> findAllByTenant(@Param("tenantId") String tenantId, Pageable pageable);
}
