package com.healthdata.costanalysis.domain.repository;

import com.healthdata.costanalysis.domain.model.CostAnalysisCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CostAnalysisCacheRepository extends JpaRepository<CostAnalysisCache, UUID> {

    @Query("""
        SELECT c FROM CostAnalysisCache c
        WHERE c.tenantId = :tenantId
          AND c.analysisType = :analysisType
          AND c.analysisPeriod = :analysisPeriod
          AND (:serviceName IS NULL OR c.serviceName = :serviceName)
          AND c.expiresAt > CURRENT_TIMESTAMP
        ORDER BY c.createdAt DESC
        """)
    List<CostAnalysisCache> findValidCache(
        @Param("tenantId") String tenantId,
        @Param("analysisType") String analysisType,
        @Param("analysisPeriod") String analysisPeriod,
        @Param("serviceName") String serviceName
    );

    @Query("""
        SELECT c FROM CostAnalysisCache c
        WHERE c.tenantId = :tenantId
          AND c.analysisType = :analysisType
          AND c.analysisPeriod = :analysisPeriod
        ORDER BY c.createdAt DESC
        """)
    List<CostAnalysisCache> findByTenantAndTypeAndPeriod(
        @Param("tenantId") String tenantId,
        @Param("analysisType") String analysisType,
        @Param("analysisPeriod") String analysisPeriod
    );

    @Query("""
        SELECT c FROM CostAnalysisCache c
        WHERE c.tenantId = :tenantId
        ORDER BY c.createdAt DESC
        """)
    List<CostAnalysisCache> findRecentByTenant(@Param("tenantId") String tenantId, org.springframework.data.domain.Pageable pageable);

    default List<CostAnalysisCache> findRecentByTenant(String tenantId, int limit) {
        return findRecentByTenant(tenantId, org.springframework.data.domain.PageRequest.of(0, Math.max(limit, 1)));
    }

    @Modifying
    @Query("DELETE FROM CostAnalysisCache c WHERE c.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredCaches();

    @Query("""
        SELECT COALESCE(SUM(c.cacheHits) * 1.0 / NULLIF(COUNT(c), 0), 0.0)
        FROM CostAnalysisCache c
        WHERE c.tenantId = :tenantId
        """)
    double getCacheHitRatio(@Param("tenantId") String tenantId);
}
