package com.healthdata.qualitymeasure.persistence;

import com.healthdata.qualitymeasure.projection.CohortMeasureRateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Cohort Measure Rate Repository
 *
 * Persistence layer for CohortMeasureRateProjection (aggregated read model)
 * Enables fast queries of population health compliance metrics
 * Multi-tenant isolation via tenantId parameter
 */
@Repository
public interface CohortMeasureRateRepository extends JpaRepository<CohortMeasureRateProjection, String> {

    /**
     * Find cohort compliance metrics by measure code and tenant
     * Multi-tenant isolation query
     */
    @Query("SELECT c FROM CohortMeasureRateProjection c WHERE c.measureCode = :measureCode AND c.tenantId = :tenantId")
    Optional<CohortMeasureRateProjection> findByMeasureCodeAndTenant(
        @Param("measureCode") String measureCode,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all cohorts for a tenant
     */
    @Query("SELECT c FROM CohortMeasureRateProjection c WHERE c.tenantId = :tenantId ORDER BY c.complianceRate DESC")
    List<CohortMeasureRateProjection> findAllByTenant(@Param("tenantId") String tenantId);

    /**
     * Find low-performing measures (below threshold)
     */
    @Query("SELECT c FROM CohortMeasureRateProjection c WHERE c.tenantId = :tenantId AND c.complianceRate < :threshold ORDER BY c.complianceRate ASC")
    List<CohortMeasureRateProjection> findLowPerformingMeasures(
        @Param("tenantId") String tenantId,
        @Param("threshold") float threshold
    );
}
