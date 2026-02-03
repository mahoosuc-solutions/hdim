package com.healthdata.agentvalidation.repository;

import com.healthdata.agentvalidation.domain.entity.ProviderComparison;
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
 * Repository for ProviderComparison entities.
 */
@Repository
public interface ProviderComparisonRepository extends JpaRepository<ProviderComparison, UUID> {

    /**
     * Find comparison by ID and tenant.
     */
    Optional<ProviderComparison> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find comparisons for a test case.
     */
    List<ProviderComparison> findByTestCaseIdOrderByExecutedAtDesc(UUID testCaseId);

    /**
     * Find most recent comparison for a test case.
     */
    Optional<ProviderComparison> findTopByTestCaseIdOrderByExecutedAtDesc(UUID testCaseId);

    /**
     * Find comparisons by tenant with pagination.
     */
    Page<ProviderComparison> findByTenantIdOrderByExecutedAtDesc(String tenantId, Pageable pageable);

    /**
     * Find comparisons where a specific provider won quality.
     */
    List<ProviderComparison> findByTenantIdAndBestQualityProvider(String tenantId, String provider);

    /**
     * Find comparisons where a specific provider was fastest.
     */
    List<ProviderComparison> findByTenantIdAndFastestProvider(String tenantId, String provider);

    /**
     * Find comparisons where a specific provider was cheapest.
     */
    List<ProviderComparison> findByTenantIdAndCheapestProvider(String tenantId, String provider);

    /**
     * Find comparisons within a time range.
     */
    @Query("SELECT pc FROM ProviderComparison pc WHERE pc.tenantId = :tenantId " +
           "AND pc.executedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY pc.executedAt DESC")
    List<ProviderComparison> findByExecutedAtBetween(
        @Param("tenantId") String tenantId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );

    /**
     * Get provider win statistics.
     */
    @Query("SELECT pc.bestQualityProvider, COUNT(pc) " +
           "FROM ProviderComparison pc " +
           "WHERE pc.tenantId = :tenantId AND pc.executedAt > :since " +
           "GROUP BY pc.bestQualityProvider")
    List<Object[]> countQualityWinsByProvider(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    /**
     * Get provider speed statistics.
     */
    @Query("SELECT pc.fastestProvider, COUNT(pc), AVG(pc.fastestLatencyMs) " +
           "FROM ProviderComparison pc " +
           "WHERE pc.tenantId = :tenantId AND pc.executedAt > :since " +
           "GROUP BY pc.fastestProvider")
    List<Object[]> getSpeedStatsByProvider(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    /**
     * Get provider cost statistics.
     */
    @Query("SELECT pc.cheapestProvider, COUNT(pc), AVG(pc.cheapestCost) " +
           "FROM ProviderComparison pc " +
           "WHERE pc.tenantId = :tenantId AND pc.executedAt > :since " +
           "GROUP BY pc.cheapestProvider")
    List<Object[]> getCostStatsByProvider(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );

    /**
     * Find comparisons for a test suite.
     */
    @Query("SELECT pc FROM ProviderComparison pc " +
           "WHERE pc.testCase.testSuite.id = :suiteId " +
           "ORDER BY pc.executedAt DESC")
    Page<ProviderComparison> findBySuiteId(@Param("suiteId") UUID suiteId, Pageable pageable);

    /**
     * Get overall provider rankings.
     */
    @Query("SELECT " +
           "  pc.bestQualityProvider, " +
           "  COUNT(pc), " +
           "  AVG(pc.bestQualityScore), " +
           "  AVG(pc.fastestLatencyMs), " +
           "  AVG(pc.cheapestCost) " +
           "FROM ProviderComparison pc " +
           "WHERE pc.tenantId = :tenantId AND pc.executedAt > :since " +
           "GROUP BY pc.bestQualityProvider " +
           "ORDER BY COUNT(pc) DESC")
    List<Object[]> getProviderRankings(
        @Param("tenantId") String tenantId,
        @Param("since") Instant since
    );
}
